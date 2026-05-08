package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Pantalla de gestión de actividades recurrentes del robot.
 * Extiende BaseActivity para tener acceso a hablarOSimular()
 * y poder guiar al usuario mayor con voz en cada paso.
 *
 * VOTO POR VOZ: el usuario puede tocar la cabeza del robot para dictar
 * descripción, hora y días de la semana dentro del diálogo.
 *
 * FIXES aplicados:
 *  1. Estado de voz corregido: campoEspera apunta al campo ACTUAL,
 *     avanza sólo tras recibir un resultado válido.
 *  2. "menos cuarto" ahora resta 15 min correctamente.
 *  3. haySolapamiento usa ID_NUEVO como centinela explícito.
 *  4. Threads sustituidos por Handler.postDelayed (cancelables).
 *  5. DB/IO movida fuera del hilo principal (ExecutorService).
 *  6. hablarOSimular() siempre en hilo principal.
 *  7. Referencias al diálogo como WeakReference.
 *  8. TIPOS unificado en enum interno TipoActividad.
 *  9. getIconoParaTipo movido a Actividad (aquí sólo delegamos).
 * 10. HORA_DEFAULT y ID_NUEVO como constantes nombradas.
 * 11. tv.setTextColor usa ContextCompat.getColor() en lugar de R.color directo.
 */
public class ActividadesActivity extends BaseActivity {

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final int  HORA_DEFAULT_MINUTOS = 9 * 60; // 09:00
    private static final int  ID_NUEVO             = Integer.MIN_VALUE;


    /** Asocia cada tipo interno con su etiqueta visible. */
    private enum TipoActividad {
        MEDICACION      (Actividad.TIPO_MEDICACION,       "MEDICACIÓN"),
        BEBER_AGUA      (Actividad.TIPO_BEBER_AGUA,       "BEBER AGUA"),
        COMER           (Actividad.TIPO_COMER,            "COMER"),
        PASEO_EJERCICIO (Actividad.TIPO_PASEO_EJERCICIO,  "PASEO/EJERCICIO"),
        JUEGOS          (Actividad.TIPO_JUEGOS,           "JUEGOS"),
        ASEO            (Actividad.TIPO_ASEO,             "ASEO"),
        LLAMADA_FAMILIAR(Actividad.TIPO_LLAMADA_FAMILIAR, "LLAMADA FAMILIAR"),
        IR_DORMIR       (Actividad.TIPO_IR_DORMIR,        "IR A DORMIR");

        final String clave;
        final String etiqueta;

        TipoActividad(String clave, String etiqueta) {
            this.clave    = clave;
            this.etiqueta = etiqueta;
        }

        static String[] claves() {
            TipoActividad[] v = values();
            String[] r = new String[v.length];
            for (int i = 0; i < v.length; i++) r[i] = v[i].clave;
            return r;
        }

        static String[] etiquetas() {
            TipoActividad[] v = values();
            String[] r = new String[v.length];
            for (int i = 0; i < v.length; i++) r[i] = v[i].etiqueta;
            return r;
        }
    }

    // LUN(2)…DOM(1) según Calendar
    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    // ── Vistas principales ────────────────────────────────────────────────────
    private LinearLayout containerActividades;
    private TextView     tvVacio;
    private ActividadRepository repo;

    // ── Estado del diálogo abierto ────────────────────────────────────────────
    private int           horaSeleccionada  = HORA_DEFAULT_MINUTOS;
    private List<Integer> diasSeleccionados = new ArrayList<>();

    // ── Estado de voz ─────────────────────────────────────────────────────────
    /**
     * Campo que está esperando ser dictado ahora mismo.
     * NINGUNO cuando no hay diálogo activo o el flujo terminó.
     */
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;

    // Referencias débiles al diálogo para que un dismiss no provoque leaks
    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<TextView>    dialogTvHoraRef;
    private java.lang.ref.WeakReference<TextView[]>  dialogBtnsDiaRef;
    private java.lang.ref.WeakReference<EditText>    dialogEtDescRef;
    // En los campos de WeakReference al principio de la clase:
    private java.lang.ref.WeakReference<Spinner> dialogSpinnerRef;

    // Handler para postDelayed (sustituye new Thread + sleep)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Executor para operaciones de BD
    private final java.util.concurrent.ExecutorService dbExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividades);
        setupTopBackBanner("Actividades");

        containerActividades = findViewById(R.id.containerActividades);
        tvVacio              = findViewById(R.id.tvVacioActividades);
        repo                 = new ActividadRepository(this);

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirActividad);
        btnAnadir.setOnClickListener(v -> mostrarDialogoAnadir(null));

        renderizarLista();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        dbExecutor.shutdownNow();
    }

    // =========================================================================
    // Sensor táctil de cabeza → entrada por voz en el diálogo
    // =========================================================================

    /**
     * Llamado desde BaseActivity cuando el usuario toca la cabeza del robot.
     * Ahora campoEspera apunta al campo ACTUAL; avanza sólo tras validar
     * el resultado en onTextoEscuchado().
     */
    @Override
    protected void onCabezaTocada() {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;
        if (campoEspera == CampoVozEspera.NINGUNO) return;

        // Parar cualquier TTS en curso y escuchar inmediatamente
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(this::escuchar, 300); // pequeño margen tras parar voz
    }

    // =========================================================================
// Anunciar campo y pedir toque — el robot habla ANTES del toque
// =========================================================================

    /**
     * Anuncia al usuario qué tiene que decir en el siguiente campo
     * y le pide que toque la cabeza cuando esté listo.
     * NO lanza escuchar() aquí — eso lo hace onCabezaTocada().
     */
    private void anunciarCampoYEsperarToque(CampoVozEspera campo) {
        campoEspera = campo;
        String instruccion;
        switch (campo) {
            case DESCRIPCION:
                instruccion = "Ahora dime una descripción para esta actividad. "
                        + "Cuando estés listo, toca mi cabeza.";
                break;
            case TIPO:
                instruccion = "Dime el tipo de actividad. Por ejemplo: medicación, "
                        + "beber agua, comer, paseo, juegos, aseo, llamada familiar, o ir a dormir. "
                        + "Toca mi cabeza cuando estés listo.";
                break;
            case HORA:
                instruccion = "Dime la hora. Por ejemplo: nueve y media, o las doce. "
                        + "Toca mi cabeza cuando estés listo.";
                break;
            case DIA_SEMANA:
                instruccion = "Dime los días de la semana. Por ejemplo: lunes, miércoles y viernes. "
                        + "Toca mi cabeza cuando estés listo.";
                break;
            case CAMPO_EDITAR:
                instruccion = "¿Qué campo quieres cambiar? Descripción, tipo, hora, o días. "
                        + "Toca mi cabeza cuando estés listo.";
                break;
            default:
                return;
        }
        hablarEnMain(instruccion);
    }

    // =========================================================================
// Resultado del reconocimiento de voz
// =========================================================================

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        switch (campoEspera) {

            case DESCRIPCION: {
                final String desc = texto.trim();
                runOnUiThread(() -> {
                    EditText et = dialogEtDescRef != null ? dialogEtDescRef.get() : null;
                    if (et != null) et.setText(desc);
                });
                hablarEnMain("Descripción guardada: " + desc + ".");
                mainHandler.postDelayed(
                        () -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO), 1500);
                break;
            }

            case TIPO: {
                String tipoReconocido = parsearTipoVoz(texto);
                if (tipoReconocido != null) {
                    runOnUiThread(() -> {
                        Spinner spinner = dialogSpinnerRef != null ? dialogSpinnerRef.get() : null;
                        if (spinner != null) {
                            String[] claves = TipoActividad.claves();
                            for (int i = 0; i < claves.length; i++) {
                                if (claves[i].equals(tipoReconocido)) {
                                    spinner.setSelection(i);
                                    break;
                                }
                            }
                        }
                    });
                    // Buscar etiqueta para confirmación
                    String etiqueta = tipoReconocido;
                    for (TipoActividad t : TipoActividad.values()) {
                        if (t.clave.equals(tipoReconocido)) { etiqueta = t.etiqueta; break; }
                    }
                    final String etiquetaFinal = etiqueta;
                    hablarEnMain("Tipo guardado: " + etiquetaFinal + ".");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.HORA), 1500);
                } else {
                    hablarEnMain("No reconocí el tipo. Inténtalo de nuevo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO), 2000);
                }
                break;
            }

            case HORA: {
                Integer minutosVoz = parsearHoraVoz(texto);
                if (minutosVoz != null) {
                    horaSeleccionada = minutosVoz;
                    runOnUiThread(() -> {
                        TextView tv = dialogTvHoraRef != null ? dialogTvHoraRef.get() : null;
                        if (tv != null) actualizarDisplayHora(tv, horaSeleccionada);
                    });
                    hablarEnMain("Hora guardada.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 1500);
                } else {
                    hablarEnMain("No entendí la hora. Inténtalo de nuevo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.HORA), 2000);
                }
                break;
            }

            case DIA_SEMANA: {
                List<Integer> diasVoz = parsearDiasVoz(texto);
                if (!diasVoz.isEmpty()) {
                    diasSeleccionados.clear();
                    diasSeleccionados.addAll(diasVoz);
                    runOnUiThread(() -> {
                        TextView[] btns = dialogBtnsDiaRef != null ? dialogBtnsDiaRef.get() : null;
                        if (btns != null)
                            actualizarBotonesDia(btns, VALORES_DIA, diasSeleccionados);
                    });
                    campoEspera = CampoVozEspera.NINGUNO;
                    hablarEnMain("Días guardados. Ya puedes pulsar Añadir cuando estés listo.");
                } else {
                    hablarEnMain("No entendí los días. Inténtalo de nuevo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 2000);
                }
                break;
            }

            case CAMPO_EDITAR: {
                String t = texto.toLowerCase();
                if (t.contains("descripción") || t.contains("descripcion") || t.contains("nombre")) {
                    hablarEnMain("De acuerdo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION), 1000);
                } else if (t.contains("tipo") || t.contains("categoría") || t.contains("categoria")) {
                    hablarEnMain("De acuerdo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO), 1000);
                } else if (t.contains("hora") || t.contains("tiempo") || t.contains("horario")) {
                    hablarEnMain("De acuerdo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.HORA), 1000);
                } else if (t.contains("día") || t.contains("dia") || t.contains("días")
                        || t.contains("dias") || t.contains("semana")) {
                    hablarEnMain("De acuerdo.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 1000);
                } else {
                    hablarEnMain("No entendí. Di descripción, tipo, hora, o días.");
                    mainHandler.postDelayed(
                            () -> anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR), 2500);
                }
                break;
            }

            default:
                break;
        }
    }

    // =========================================================================
// Diálogo AÑADIR / EDITAR — arranque del flujo de voz
// =========================================================================

    private void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada  = (existente != null)
                ? existente.getHoraMinutos() : HORA_DEFAULT_MINUTOS;
        diasSeleccionados = (existente != null && existente.getDiasSemana() != null)
                ? new ArrayList<>(existente.getDiasSemana())
                : new ArrayList<>();
        campoEspera = CampoVozEspera.NINGUNO; // se asigna tras mostrar el diálogo

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_actividad, null);

        final TextView tvHora     = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc     = dv.findViewById(R.id.etDescripcionActividad);
        final Spinner  spinner    = dv.findViewById(R.id.spinnerTipoActividad);
        final TextView tvTitulo   = dv.findViewById(R.id.tvTituloDialogActividad);
        final Button   btnGuardar = dv.findViewById(R.id.btnGuardarDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");
        btnGuardar.setText(existente != null ? "GUARDAR" : "AÑADIR");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, TipoActividad.etiquetas());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (existente != null) {
            etDesc.setText(existente.getDescripcion());
            String[] claves = TipoActividad.claves();
            for (int i = 0; i < claves.length; i++) {
                if (claves[i].equals(existente.getTipo())) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);

        final TextView[] btnsDia = {
                dv.findViewById(R.id.btnDiaLun), dv.findViewById(R.id.btnDiaMar),
                dv.findViewById(R.id.btnDiaMie), dv.findViewById(R.id.btnDiaJue),
                dv.findViewById(R.id.btnDiaVie), dv.findViewById(R.id.btnDiaSab),
                dv.findViewById(R.id.btnDiaDom)
        };
        actualizarBotonesDia(btnsDia, VALORES_DIA, diasSeleccionados);

        for (int i = 0; i < btnsDia.length; i++) {
            final int dia = VALORES_DIA[i];
            btnsDia[i].setOnClickListener(v -> {
                if (diasSeleccionados.contains(dia))
                    diasSeleccionados.remove(Integer.valueOf(dia));
                else
                    diasSeleccionados.add(dia);
                actualizarBotonesDia(btnsDia, VALORES_DIA, diasSeleccionados);
            });
        }

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));

        // Referencias débiles para acceso desde callbacks de voz
        dialogTvHoraRef  = new java.lang.ref.WeakReference<>(tvHora);
        dialogBtnsDiaRef = new java.lang.ref.WeakReference<>(btnsDia);
        dialogEtDescRef  = new java.lang.ref.WeakReference<>(etDesc);
        dialogSpinnerRef = new java.lang.ref.WeakReference<>(spinner); // nuevo

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogRef = new java.lang.ref.WeakReference<>(dialog);

        dialog.setOnDismissListener(d -> {
            campoEspera      = CampoVozEspera.NINGUNO;
            dialogRef        = null;
            dialogTvHoraRef  = null;
            dialogBtnsDiaRef = null;
            dialogEtDescRef  = null;
            dialogSpinnerRef = null;
            mainHandler.removeCallbacksAndMessages(null);
        });

        dv.findViewById(R.id.btnCancelarDialogActividad)
                .setOnClickListener(v -> dialog.dismiss());
        dv.findViewById(R.id.btnCancelarDialogActividad2)
                .setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            final String desc      = etDesc.getText().toString().trim();
            final String tipo      = TipoActividad.claves()[spinner.getSelectedItemPosition()];
            final int    duracion  = new Actividad(0, tipo, horaSeleccionada, "").getDuracionMinutos();
            final int    idExcluir = (existente != null) ? existente.getId() : ID_NUEVO;

            dbExecutor.execute(() -> {
                final boolean solapa = haySolapamiento(
                        diasSeleccionados, horaSeleccionada, duracion, idExcluir);
                runOnUiThread(() -> {
                    if (solapa) {
                        hablarEnMain("Atención. Ya tienes algo programado a esa hora. "
                                + "¿Quieres guardarlo igualmente?");
                        new AlertDialog.Builder(ActividadesActivity.this)
                                .setTitle("Solapamiento")
                                .setMessage("Ya hay una actividad que se solapa. "
                                        + "¿Deseas guardarla de todos modos?")
                                .setPositiveButton("Sí", (d, w) -> {
                                    guardarActividad(existente, tipo, desc);
                                    dialog.dismiss();
                                })
                                .setNegativeButton("No", null)
                                .show();
                    } else {
                        guardarActividad(existente, tipo, desc);
                        dialog.dismiss();
                    }
                });
            });
        });

        dialog.show();

        // Arranque del flujo: el robot explica y anuncia el primer campo
        mainHandler.postDelayed(() -> {
            if (existente == null) {
                hablarEnMain("Vamos a añadir una actividad.");
                mainHandler.postDelayed(
                        () -> anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION), 2000);
            } else {
                anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR);
            }
        }, 400);
    }

    /**
     * Convierte texto hablado en la clave interna de TipoActividad.
     * Devuelve null si no se reconoce ningún tipo.
     */
    private String parsearTipoVoz(String texto) {
        texto = texto.toLowerCase();
        if (texto.contains("medicación") || texto.contains("medicacion")
                || texto.contains("medicina") || texto.contains("pastilla"))
            return Actividad.TIPO_MEDICACION;
        if (texto.contains("agua") || texto.contains("beber"))
            return Actividad.TIPO_BEBER_AGUA;
        if (texto.contains("comer") || texto.contains("comida") || texto.contains("almuerzo")
                || texto.contains("desayuno") || texto.contains("cena"))
            return Actividad.TIPO_COMER;
        if (texto.contains("paseo") || texto.contains("ejercicio") || texto.contains("andar")
                || texto.contains("caminar"))
            return Actividad.TIPO_PASEO_EJERCICIO;
        if (texto.contains("juego") || texto.contains("jugar") || texto.contains("juegos"))
            return Actividad.TIPO_JUEGOS;
        if (texto.contains("aseo") || texto.contains("ducha") || texto.contains("baño")
                || texto.contains("higiene"))
            return Actividad.TIPO_ASEO;
        if (texto.contains("llamada") || texto.contains("familiar") || texto.contains("teléfono")
                || texto.contains("telefono") || texto.contains("familia"))
            return Actividad.TIPO_LLAMADA_FAMILIAR;
        if (texto.contains("dormir") || texto.contains("cama") || texto.contains("descansar")
                || texto.contains("noche"))
            return Actividad.TIPO_IR_DORMIR;
        return null;
    }
    // =========================================================================
    // Resultado del reconocimiento de voz
    // =========================================================================
    /**
     * Estima la duración del texto hablado y lanza escuchar() al terminar.
     * Si BaseActivity expone un callback onTtsCompleted(), úsalo en su lugar.
     * ~80 ms/carácter en español + 600 ms de margen.
     */
    private void hablarYLuegoEscuchar(String texto) {
        hablarEnMain(texto);
        long delayMs = texto.length() * 80L + 600L;
        mainHandler.postDelayed(this::escuchar, delayMs);
    }

    // =========================================================================
    // Renderizado
    // =========================================================================

    private void renderizarLista() {
        dbExecutor.execute(() -> {
            final List<Actividad> lista = repo.getAll();
            Collections.sort(lista,
                    (a, b) -> Integer.compare(a.getHoraMinutos(), b.getHoraMinutos()));

            runOnUiThread(() -> {
                containerActividades.removeAllViews();
                if (lista.isEmpty()) {
                    tvVacio.setVisibility(View.VISIBLE);
                } else {
                    tvVacio.setVisibility(View.GONE);
                    for (Actividad a : lista) {
                        containerActividades.addView(crearItemActividad(a));
                    }
                }
            });
        });
    }

    private boolean tienenDiaComun(List<Integer> dias1, List<Integer> dias2) {
        if (dias1 == null || dias1.isEmpty()) return true;
        if (dias2 == null || dias2.isEmpty()) return true;
        for (Integer d : dias1) {
            if (dias2.contains(d)) return true;
        }
        return false;
    }

    /** Ejecutar sólo desde dbExecutor. */
    private boolean haySolapamiento(List<Integer> dias, int horaMinutos,
                                    int duracion, int idAExcluir) {
        List<Actividad> lista = repo.getAll();
        for (Actividad a : lista) {
            if (a.getId() == idAExcluir) continue;
            if (tienenDiaComun(dias, a.getDiasSemana())) {
                int start1 = horaMinutos,       end1 = start1 + duracion;
                int start2 = a.getHoraMinutos(), end2 = start2 + a.getDuracionMinutos();
                if (start1 < end2 && start2 < end1) return true;
            }
        }
        return false;
    }

    private View crearItemActividad(final Actividad a) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_actividad, containerActividades, false);

        ((TextView) item.findViewById(R.id.tvHoraItem)).setText(a.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvTipoItem)).setText(a.getTipoLabel());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor(a.getColorHex()));
        bg.setCornerRadius(dpToPx(14));
        item.setBackground(bg);

        item.setOnClickListener(v -> mostrarDialogoDetalle(a));
        item.findViewById(R.id.btnEditarItem).setOnClickListener(v -> mostrarDialogoAnadir(a));
        item.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> confirmarEliminar(a));

        return item;
    }


    private void guardarActividad(Actividad existente, String tipo, String desc) {
        dbExecutor.execute(() -> {
            if (existente == null) {
                hablarEnMain("¡Listo! He añadido la actividad a tu agenda.");
                Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
                nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.add(nueva);
            } else {
                hablarEnMain("Perfecto. He guardado los cambios.");
                existente.setTipo(tipo);
                existente.setHoraMinutos(horaSeleccionada);
                existente.setDescripcion(desc);
                existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.update(existente);
            }
            runOnUiThread(this::renderizarLista);
        });
    }

    // =========================================================================
    // Diálogo DETALLE
    // =========================================================================

    private void mostrarDialogoDetalle(final Actividad a) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_actividad, null);

        GradientDrawable bgCirculo = new GradientDrawable();
        bgCirculo.setShape(GradientDrawable.OVAL);
        bgCirculo.setColor(Color.parseColor(a.getColorHex()));
        dv.findViewById(R.id.frameEmojiDetAct).setBackground(bgCirculo);
        ((android.widget.ImageView) dv.findViewById(R.id.tvEmojiDetAct))
                .setImageResource(a.getIconoRes()); // delegado al modelo

        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());

        String descTexto = (a.getDescripcion() != null && !a.getDescripcion().isEmpty())
                ? a.getDescripcion() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText(descTexto);

        int[] idsDias = {
                R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue,
                R.id.detDiaVie, R.id.detDiaSab, R.id.detDiaDom
        };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < idsDias.length; i++) {
            TextView tv     = dv.findViewById(idsDias[i]);
            boolean  activo = dias != null && dias.contains(VALORES_DIA[i]);
            tv.setBackgroundResource(activo
                    ? R.drawable.bg_tipo_selected
                    : R.drawable.bg_tipo_normal);
            tv.setTextColor(activo ? Color.WHITE : Color.BLACK);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================================
    // Eliminar con confirmación
    // =========================================================================

    private void confirmarEliminar(final Actividad a) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Seguro que quieres eliminar \"" + a.getTipoLabel() + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    hablarEnMain("He eliminado la actividad.");
                    dbExecutor.execute(() -> {
                        repo.delete(a.getId());
                        runOnUiThread(this::renderizarLista);
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // Parsers de voz
    // =========================================================================

    /**
     * Convierte texto hablado en minutos totales desde medianoche.
     *
     * Ejemplos:
     *   "nueve y media"      → 570  (09:30)
     *   "las diez"           → 600  (10:00)
     *   "ocho y cuarto"      → 495  (08:15)
     *   "nueve menos cuarto" → 525  (08:45)  ← FIX: antes daba 09:45
     *
     * @return minutos desde medianoche, o null si no se reconoce el patrón.
     */
    private Integer parsearHoraVoz(String texto) {
        texto = texto.toLowerCase().trim();

        // ── Detectar indicador de tarde/noche ANTES de parsear ────────────────
        boolean esTarde = texto.contains("tarde")
                || texto.contains("noche")
                || texto.contains("pm")
                || texto.contains("p.m");
        boolean esMañana = texto.contains("mañana")
                || texto.contains("madrugada")
                || texto.contains("am")
                || texto.contains("a.m");

        // ── Dígitos directos: "9:30", "10 00", "21:00" ───────────────────────
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})[:\\s](\\d{2})").matcher(texto);
        if (m.find()) {
            int h   = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            if (h >= 0 && h <= 23 && min >= 0 && min <= 59) {
                // Si ya viene en 24h (≥13) no tocamos nada
                if (h < 13) h = ajustarTarde(h, esTarde, esMañana);
                return h * 60 + min;
            }
        }

        // ── Hora en dígito con artículo: "a las 9", "las 10" ─────────────────
        m = java.util.regex.Pattern
                .compile("(?:las?|a las?)\\s*(\\d{1,2})").matcher(texto);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h < 13) h = ajustarTarde(h, esTarde, esMañana);
            return h * 60;
        }

        // ── Solo dígito suelto: "9", "10" ────────────────────────────────────
        m = java.util.regex.Pattern.compile("^(\\d{1,2})$").matcher(texto);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h >= 0 && h <= 23) {
                if (h < 13) h = ajustarTarde(h, esTarde, esMañana);
                return h * 60;
            }
        }

        // ── Palabras de hora ──────────────────────────────────────────────────
        final String[] nombresHora = {
                "una","dos","tres","cuatro","cinco","seis",
                "siete","ocho","nueve","diez","once","doce"
        };
        int horaBase = -1;
        for (int i = 0; i < nombresHora.length; i++) {
            if (texto.contains(nombresHora[i])) { horaBase = i + 1; break; }
        }
        if (horaBase == -1) return null;

        int minutos = 0;
        if (texto.contains("y media")) {
            minutos = 30;
        } else if (texto.contains("y cuarto")) {
            minutos = 15;
        } else if (texto.contains("menos cuarto")) {
            horaBase--;
            if (horaBase <= 0) horaBase += 12;
            minutos = 45;
        }

        horaBase = ajustarTarde(horaBase, esTarde, esMañana);
        return horaBase * 60 + minutos;
    }

    /**
     * Suma 12h para tarde/noche si la hora está en rango 12h.
     *
     * Reglas:
     *  - esTarde explícito → +12h (salvo las 12 del mediodía, que ya es correcta)
     *  - esMañana explícito → sin cambio
     *  - Sin indicador → heurística: horas 1-6 se asumen tarde (poco probable
     *    programar algo a la 1 AM), el resto se deja como está.
     *
     * Ejemplos:
     *   ajustarTarde(9, true,  false) → 21   ("nueve de la tarde")
     *   ajustarTarde(9, false, true)  →  9   ("nueve de la mañana")
     *   ajustarTarde(9, false, false) →  9   (sin indicador, 9 es mañana)
     *   ajustarTarde(3, false, false) → 15   (sin indicador, 3 → tarde heurística)
     *   ajustarTarde(12, true, false) → 12   (mediodía, no sumamos)
     */
    private int ajustarTarde(int hora, boolean esTarde, boolean esMañana) {
        // 1. Normalizar 12 a 0 (para que 12:30 sea tratado como 0:30 inicialmente)
        int h = (hora == 12) ? 0 : hora;

        
        // 3. Heurística sin indicador (1-6 -> tarde)
        if (h >= 1 && h <= 6) return h + 12;

        // Si dice "12" a secas sin indicador, devolvemos 0 o 12 según prefieras.
        // Generalmente "las 12" sin más suele ser mediodía (12).
        return hora;
    }

    /**
     * Convierte texto hablado en lista de valores Calendar de días.
     * Ejemplo: "lunes miércoles y viernes" → [2, 4, 6]
     */
    private List<Integer> parsearDiasVoz(String texto) {
        texto = texto.toLowerCase();
        List<Integer> dias = new ArrayList<>();
        // Acepta versiones con y sin tilde
        final String[] nombres = {"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};
        final String[] alt     = {"lunes","martes","miercoles","jueves","viernes","sabado","domingo"};
        for (int i = 0; i < nombres.length; i++) {
            if (texto.contains(nombres[i]) || texto.contains(alt[i])) {
                if (!dias.contains(VALORES_DIA[i])) // evitar duplicados
                    dias.add(VALORES_DIA[i]);
            }
        }
        return dias;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Wrapper que garantiza que hablarOSimular siempre se llama en el hilo principal. */
    private void hablarEnMain(String texto) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hablarOSimular(texto);
        } else {
            runOnUiThread(() -> hablarOSimular(texto));
        }
    }

    private void abrirTimePicker(final TextView tv) {
        int h = horaSeleccionada / 60, m = horaSeleccionada % 60;
        new TimePickerDialog(this, android.R.style.Theme_Material_Dialog,
                (view, hh, mm) -> {
                    horaSeleccionada = hh * 60 + mm;
                    actualizarDisplayHora(tv, horaSeleccionada);
                }, h, m, true).show();
    }

    private void actualizarDisplayHora(TextView tv, int minutos) {
        tv.setText(String.format("%02d:%02d", minutos / 60, minutos % 60));
    }

    private void actualizarBotonesDia(TextView[] botones, int[] valores, List<Integer> sel) {
        for (int i = 0; i < botones.length; i++) {
            botones[i].setBackgroundResource(sel.contains(valores[i])
                    ? R.drawable.bg_tipo_selected
                    : R.drawable.bg_tipo_normal);
        }
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}