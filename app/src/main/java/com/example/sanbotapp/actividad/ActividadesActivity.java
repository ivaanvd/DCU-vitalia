package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * Pantalla de gestión de actividades recurrentes del robot.
 * Extiende BaseActivity para tener acceso a hablarOSimular()
 * y poder guiar al usuario mayor con voz en cada paso.
 *
 * VOTO POR VOZ: el usuario puede tocar la cabeza del robot para dictar
 * descripción, hora y días de la semana dentro del diálogo.
 */
public class ActividadesActivity extends BaseActivity {

    private LinearLayout       containerActividades;
    private TextView           tvVacio;
    private ActividadRepository repo;

    // ── Estado del diálogo de añadir/editar ──────────────────────────────────
    private int           horaSeleccionada  = 9 * 60; // 09:00 por defecto
    private List<Integer> diasSeleccionados = new ArrayList<>();

    // ── Estado de voz ─────────────────────────────────────────────────────────
    /** Campo del diálogo actualmente esperando voz. NINGUNO si no hay diálogo abierto. */
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;

    /** Referencias vivas al diálogo abierto para actualizar sus vistas desde onCabezaTocada(). */
    private TextView   dialogTvHora;
    private TextView[] dialogBtnsDia;
    private EditText   dialogEtDesc;
    private AlertDialog dialogActivo;

    // Tipos del spinner (por orden de aparición)
    private static final String[] TIPOS = {
            Actividad.TIPO_MEDICACION,
            Actividad.TIPO_BEBER_AGUA,
            Actividad.TIPO_COMER,
            Actividad.TIPO_PASEO_EJERCICIO,
            Actividad.TIPO_JUEGOS,
            Actividad.TIPO_ASEO,
            Actividad.TIPO_LLAMADA_FAMILIAR,
            Actividad.TIPO_IR_DORMIR
    };
    private static final String[] ETIQUETAS_TIPOS = {
            "MEDICACIÓN",
            "BEBER AGUA",
            "COMER",
            "PASEO/EJERCICIO",
            "JUEGOS",
            "ASEO",
            "LLAMADA FAMILIAR",
            "IR A DORMIR"
    };
    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 }; // LUN…DOM (Calendar)

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

    // =========================================================================
    // Sensor táctil de cabeza → entrada por voz en el diálogo
    // =========================================================================

    /**
     * Llamado automáticamente desde BaseActivity cuando el usuario toca
     * la cabeza del robot (sensores 11, 12 o 13).
     *
     * Avanza al siguiente campo del diálogo en orden:
     *   DESCRIPCION → HORA → DIA_SEMANA → NINGUNO (completo)
     */
    @Override
    protected void onCabezaTocada() {
        if (dialogActivo == null || !dialogActivo.isShowing()) return;

        switch (campoEspera) {

            case DESCRIPCION:
                campoEspera = CampoVozEspera.HORA; // siguiente tras escuchar
                hablarOSimular("Dime una descripción para esta actividad.");
                new Thread(() -> { sleep(1800); escuchar(); }).start();
                break;

            case HORA:
                campoEspera = CampoVozEspera.DIA_SEMANA;
                hablarOSimular("Ahora dime la hora. Por ejemplo: nueve y media, o las doce.");
                new Thread(() -> { sleep(2200); escuchar(); }).start();
                break;

            case DIA_SEMANA:
                campoEspera = CampoVozEspera.NINGUNO;
                hablarOSimular("Dime los días. Por ejemplo: lunes, miércoles y viernes.");
                new Thread(() -> { sleep(2500); escuchar(); }).start();
                break;

            case NINGUNO:
            default:
                hablarOSimular("Todos los campos están rellenos. Pulsa Añadir cuando estés listo.");
                break;
        }
    }

    // =========================================================================
    // Resultado del reconocimiento de voz
    // =========================================================================

    /**
     * Recibe el texto reconocido y lo aplica al campo correspondiente.
     *
     * NOTA: campoEspera ya avanzó al campo SIGUIENTE antes de llamar a escuchar(),
     * por eso procesamos el campo ANTERIOR al valor actual de campoEspera.
     */
    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        if (dialogActivo == null || !dialogActivo.isShowing()) return;

        switch (campoEspera) {

            case HORA:
                // El campo anterior era DESCRIPCION
                runOnUiThread(() -> {
                    if (dialogEtDesc != null) dialogEtDesc.setText(texto);
                });
                hablarOSimular("Descripción guardada: " + texto
                        + ". Toca mi cabeza para decirme la hora.");
                break;

            case DIA_SEMANA:
                // El campo anterior era HORA
                runOnUiThread(() -> {
                    Integer minutosVoz = parsearHoraVoz(texto);
                    if (minutosVoz != null) {
                        horaSeleccionada = minutosVoz;
                        if (dialogTvHora != null)
                            actualizarDisplayHora(dialogTvHora, horaSeleccionada);
                        hablarOSimular("Hora guardada. Toca mi cabeza para decirme los días de la semana.");
                    } else {
                        hablarOSimular("No entendí la hora. Toca mi cabeza de nuevo para intentarlo.");
                        campoEspera = CampoVozEspera.HORA; // reintento
                    }
                });
                break;

            case NINGUNO:
                // El campo anterior era DIA_SEMANA
                runOnUiThread(() -> {
                    List<Integer> diasVoz = parsearDiasVoz(texto);
                    if (!diasVoz.isEmpty()) {
                        diasSeleccionados.clear();
                        diasSeleccionados.addAll(diasVoz);
                        if (dialogBtnsDia != null)
                            actualizarBotonesDia(dialogBtnsDia, VALORES_DIA, diasSeleccionados);
                        hablarOSimular("Días guardados. Ya puedes pulsar Añadir cuando estés listo.");
                    } else {
                        hablarOSimular("No entendí los días. Toca mi cabeza de nuevo para repetirlos.");
                        campoEspera = CampoVozEspera.DIA_SEMANA; // reintento
                    }
                });
                break;

            default:
                break;
        }
    }

    // =========================================================================
    // Renderizado
    // =========================================================================

    private void renderizarLista() {
        containerActividades.removeAllViews();
        List<Actividad> lista = repo.getAll();

        Collections.sort(lista, (a, b) -> Integer.compare(a.getHoraMinutos(), b.getHoraMinutos()));

        if (lista.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
            for (final Actividad a : lista) {
                containerActividades.addView(crearItemActividad(a));
            }
        }
    }

    private boolean tienenDiaComun(List<Integer> dias1, List<Integer> dias2) {
        if (dias1 == null || dias1.isEmpty()) return true;
        if (dias2 == null || dias2.isEmpty()) return true;
        for (Integer d : dias1) {
            if (dias2.contains(d)) return true;
        }
        return false;
    }

    private boolean haySolapamiento(List<Integer> dias, int horaMinutos, int duracion, int idAExcluir) {
        List<Actividad> lista = repo.getAll();
        for (Actividad a : lista) {
            if (a.getId() == idAExcluir) continue;
            if (tienenDiaComun(dias, a.getDiasSemana())) {
                int start1 = horaMinutos, end1 = start1 + duracion;
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

    // =========================================================================
    // Diálogo AÑADIR / EDITAR
    // =========================================================================

    private void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada  = (existente != null) ? existente.getHoraMinutos() : 9 * 60;
        diasSeleccionados = (existente != null && existente.getDiasSemana() != null)
                ? new ArrayList<>(existente.getDiasSemana())
                : new ArrayList<>();

        // Resetear estado de voz
        campoEspera = CampoVozEspera.NINGUNO;

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_actividad, null);

        final TextView tvHora    = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc    = dv.findViewById(R.id.etDescripcionActividad);
        final Spinner  spinner   = dv.findViewById(R.id.spinnerTipoActividad);
        final TextView tvTitulo  = dv.findViewById(R.id.tvTituloDialogActividad);
        final Button   btnGuardar = dv.findViewById(R.id.btnGuardarDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");
        btnGuardar.setText(existente != null ? "GUARDAR" : "AÑADIR");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ETIQUETAS_TIPOS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (existente != null) {
            etDesc.setText(existente.getDescripcion());
            for (int i = 0; i < TIPOS.length; i++) {
                if (TIPOS[i].equals(existente.getTipo())) { spinner.setSelection(i); break; }
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
                if (diasSeleccionados.contains(dia)) diasSeleccionados.remove(Integer.valueOf(dia));
                else diasSeleccionados.add(dia);
                actualizarBotonesDia(btnsDia, VALORES_DIA, diasSeleccionados);
            });
        }

        tvHora.setOnClickListener(v -> {
            hablarOSimular("Elige la hora a la que quieres programar la actividad.");
            abrirTimePicker(tvHora);
        });

        // ── Guardar referencias vivas para acceso desde onCabezaTocada() ─────
        dialogTvHora  = tvHora;
        dialogBtnsDia = btnsDia;
        dialogEtDesc  = etDesc;

        dialogActivo = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialogActivo.getWindow() != null)
            dialogActivo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Al cerrar el diálogo, limpiar estado de voz
        dialogActivo.setOnDismissListener(d -> {
            campoEspera   = CampoVozEspera.NINGUNO;
            dialogActivo  = null;
            dialogTvHora  = null;
            dialogBtnsDia = null;
            dialogEtDesc  = null;
        });

        dv.findViewById(R.id.btnCancelarDialogActividad).setOnClickListener(v -> dialogActivo.dismiss());
        dv.findViewById(R.id.btnCancelarDialogActividad2).setOnClickListener(v -> dialogActivo.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String desc = etDesc.getText().toString().trim();
            String tipo = TIPOS[spinner.getSelectedItemPosition()];
            Actividad temp    = new Actividad(0, tipo, horaSeleccionada, "");
            int       duracion    = temp.getDuracionMinutos();
            int       idExistente = (existente != null) ? existente.getId() : -1;

            if (haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, idExistente)) {
                hablarOSimular("Atención. Ya tienes algo programado a esa hora. ¿Quieres guardarlo igualmente?");
                new AlertDialog.Builder(ActividadesActivity.this)
                        .setTitle("Solapamiento")
                        .setMessage("Ya hay una actividad que se solapa. ¿Deseas guardarla de todos modos?")
                        .setPositiveButton("Sí", (d, w) -> {
                            guardarActividad(existente, tipo, desc);
                            dialogActivo.dismiss();
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                guardarActividad(existente, tipo, desc);
                dialogActivo.dismiss();
            }
        });

        dialogActivo.show();

        // ── VOZ: el robot explica el flujo al abrir el diálogo ───────────────
        new Thread(() -> {
            sleep(400);
            if (existente == null) {
                hablarOSimular("Vamos a añadir una actividad. Toca mi cabeza para dictarme cada campo. Empezamos por la descripción.");
            } else {
                hablarOSimular("Puedes tocar mi cabeza para cambiar la descripción, la hora o los días.");
            }
            runOnUiThread(() -> campoEspera = CampoVozEspera.DESCRIPCION);
        }).start();
    }

    private void guardarActividad(Actividad existente, String tipo, String desc) {
        if (existente == null) {
            hablarOSimular("¡Listo! He añadido la actividad a tu agenda.");
            Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
            nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
            repo.add(nueva);
        } else {
            hablarOSimular("Perfecto. He guardado los cambios.");
            existente.setTipo(tipo);
            existente.setHoraMinutos(horaSeleccionada);
            existente.setDescripcion(desc);
            existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
            repo.update(existente);
        }
        renderizarLista();
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
                .setImageResource(getIconoParaTipo(a.getTipo()));

        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());

        String desc = (a.getDescripcion() != null && !a.getDescripcion().isEmpty())
                ? a.getDescripcion() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText(desc);

        int[] idsDias = {
                R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue,
                R.id.detDiaVie, R.id.detDiaSab, R.id.detDiaDom
        };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < idsDias.length; i++) {
            TextView tv = dv.findViewById(idsDias[i]);
            boolean activo = dias != null && dias.contains(VALORES_DIA[i]);
            tv.setBackgroundResource(activo
                    ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            tv.setTextColor(Color.parseColor("#1C1C1E"));
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
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
                    hablarOSimular("He eliminado la actividad.");
                    repo.delete(a.getId());
                    renderizarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // Parsers de voz
    // =========================================================================

    /**
     * Convierte texto hablado en minutos totales desde medianoche.
     * Ejemplos: "nueve y media" → 570, "las diez" → 600, "ocho y cuarto" → 495
     * Devuelve null si no se reconoce el patrón.
     */
    private Integer parsearHoraVoz(String texto) {
        texto = texto.toLowerCase().trim();

        // Dígitos directos: "9:30", "10 00"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})[:\\s](\\d{2})").matcher(texto);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            if (h >= 0 && h <= 23 && min >= 0 && min <= 59) return h * 60 + min;
        }

        // Solo hora en dígito: "a las 9", "las 10"
        m = java.util.regex.Pattern.compile("(?:las?|a las?)\\s*(\\d{1,2})").matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1)) * 60;

        // Solo dígito suelto: "9", "10"
        m = java.util.regex.Pattern.compile("^(\\d{1,2})$").matcher(texto.trim());
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h >= 0 && h <= 23) return h * 60;
        }

        // Palabras de hora
        String[] nombresHora = {"una","dos","tres","cuatro","cinco","seis",
                "siete","ocho","nueve","diez","once","doce"};
        int horaBase = -1;
        for (int i = 0; i < nombresHora.length; i++) {
            if (texto.contains(nombresHora[i])) { horaBase = i + 1; break; }
        }
        if (horaBase == -1) return null;

        int minutos = 0;
        if (texto.contains("y media"))        minutos = 30;
        else if (texto.contains("y cuarto"))  minutos = 15;
        else if (texto.contains("menos cuarto")) { horaBase++; minutos = 45; }

        return horaBase * 60 + minutos;
    }

    /**
     * Convierte texto hablado en lista de valores Calendar de días.
     * Ejemplo: "lunes miércoles y viernes" → [2, 4, 6]
     */
    private List<Integer> parsearDiasVoz(String texto) {
        texto = texto.toLowerCase();
        List<Integer> dias = new ArrayList<>();
        // VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 } → LUN…DOM
        String[] nombres = {"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};
        String[] alt     = {"lunes","martes","miercoles","jueves","viernes","sabado","domingo"};
        for (int i = 0; i < nombres.length; i++) {
            if (texto.contains(nombres[i]) || texto.contains(alt[i])) {
                dias.add(VALORES_DIA[i]);
            }
        }
        return dias;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
                    ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
        }
    }

    private int getIconoParaTipo(String tipo) {
        switch (tipo) {
            case Actividad.TIPO_MEDICACION:       return R.drawable.ic_medicacion;
            case Actividad.TIPO_BEBER_AGUA:       return R.drawable.ic_agua;
            case Actividad.TIPO_COMER:            return R.drawable.ic_comida;
            case Actividad.TIPO_PASEO_EJERCICIO:  return R.drawable.ic_ejercicio;
            case Actividad.TIPO_JUEGOS:           return R.drawable.ic_puzzle;
            case Actividad.TIPO_ASEO:             return R.drawable.ic_aseo;
            case Actividad.TIPO_LLAMADA_FAMILIAR: return R.drawable.ic_llamada;
            case Actividad.TIPO_IR_DORMIR:        return R.drawable.ic_dormir;
            default:                              return R.drawable.ic_calendario;
        }
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}