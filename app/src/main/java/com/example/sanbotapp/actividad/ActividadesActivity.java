package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
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

public class ActividadesActivity extends BaseActivity {

    private static final int  HORA_DEFAULT_MINUTOS = 9 * 60; // 09:00
    private static final int  ID_NUEVO             = Integer.MIN_VALUE;

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

    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    private LinearLayout containerActividades;
    private TextView     tvVacio;
    private ActividadRepository     repo;
    private com.example.sanbotapp.recordatorio.RecordatorioRepository recordatorioRepo;

    private int           horaSeleccionada  = HORA_DEFAULT_MINUTOS;
    private List<Integer> diasSeleccionados = new ArrayList<>();
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;
    private String valorPendienteConfirmar = "";
    private CampoVozEspera campoAConfirmar = CampoVozEspera.NINGUNO;

    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<TextView>    dialogTvHoraRef;
    private java.lang.ref.WeakReference<TextView[]>  dialogBtnsDiaRef;
    private java.lang.ref.WeakReference<EditText>    dialogEtDescRef;
    private java.lang.ref.WeakReference<Spinner> dialogSpinnerRef;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService dbExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividades);
        setupTopBackBanner("Actividades");

        containerActividades = findViewById(R.id.containerActividades);
        tvVacio              = findViewById(R.id.tvVacioActividades);
        repo                 = new ActividadRepository(this);
        recordatorioRepo     = new com.example.sanbotapp.recordatorio.RecordatorioRepository(this);

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

    @Override
    protected void onCabezaTocada() {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;
        if (campoEspera == CampoVozEspera.NINGUNO) return;

        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.postDelayed(this::escuchar, 300);
    }

    private void anunciarCampoYEsperarToque(CampoVozEspera campo) {
        campoEspera = campo;
        String instruccion;
        switch (campo) {
            case DESCRIPCION:
                instruccion = obtenerInstruccionDescripcionAct();
                break;
            case TIPO:
                instruccion = "¡Hola! Vamos a añadir una actividad. ¿Qué tipo de actividad es? Puede ser: medicina, comer, pasear, jugar o aseo. Toca mi cabeza para hablar.";
                break;
            case HORA:
                instruccion = "¿A qué hora prefieres hacerlo? Dime por ejemplo: 'A las diez de la mañana'. Toca mi cabeza y dímelo.";
                break;
            case DIA_SEMANA:
                instruccion = "¿Qué días de la semana? Di los días que quieras, como 'Lunes y Jueves'. Toca mi cabeza y dímelo.";
                break;
            case CAMPO_EDITAR:
                instruccion = obtenerInstruccionDinamicaAct();
                break;
            case CONFIRMACION_CAMPO:
                instruccion = "He entendido '" + valorPendienteConfirmar + "'. ¿Es correcto? Di: sí o no.";
                break;
            default:
                return;
        }
        hablarEnMain(instruccion);
    }

    /**
     * Devuelve una instrucción personalizada para el campo descripción según el tipo de actividad.
     */
    private String obtenerInstruccionDescripcionAct() {
        Spinner spinner = dialogSpinnerRef != null ? dialogSpinnerRef.get() : null;
        if (spinner == null) return "¿Qué detalles quieres añadir? Toca mi cabeza y dímelo.";

        String tipoLabel = spinner.getSelectedItem().toString();
        if (tipoLabel.equalsIgnoreCase("MEDICACIÓN")) {
            return "¿Qué medicina te toca tomar? Por ejemplo: 'Paracetamol'. Toca mi cabeza y dímelo.";
        } else if (tipoLabel.equalsIgnoreCase("COMER")) {
            return "¿Qué vas a comer hoy? Por ejemplo: 'Sopa de verduras'. Toca mi cabeza y dímelo.";
        } else if (tipoLabel.equalsIgnoreCase("LLAMADA FAMILIAR")) {
            return "¿A quién vas a llamar? Por ejemplo: 'A mi hija María'. Toca mi cabeza y dímelo.";
        } else if (tipoLabel.equalsIgnoreCase("PASEO/EJERCICIO")) {
            return "¿A dónde vas a ir a caminar? Por ejemplo: 'Al parque del retiro'. Toca mi cabeza y dímelo.";
        }
        return "¿Qué detalles quieres añadir a esta actividad? Toca mi cabeza y dímelo.";
    }

    /**
     * Muestra/Oculta y renombra el campo de detalles según el tipo de actividad.
     */
    private void actualizarCampoDinamico(String tipoLabel) {
        android.view.View container = dialogRef.get().findViewById(R.id.containerDetallesDinamicos);
        TextView tvLabel = dialogRef.get().findViewById(R.id.tvLabelDetalleDinamico);
        
        if (container == null || tvLabel == null) return;

        boolean mostrar = true;
        String nuevoLabel = "Información adicional";

        if (tipoLabel.equalsIgnoreCase("MEDICACIÓN")) {
            nuevoLabel = "¿Qué medicamento es?";
        } else if (tipoLabel.equalsIgnoreCase("COMER")) {
            nuevoLabel = "¿Qué vas a comer?";
        } else if (tipoLabel.equalsIgnoreCase("LLAMADA FAMILIAR")) {
            nuevoLabel = "¿A quién vas a llamar?";
        } else if (tipoLabel.equalsIgnoreCase("PASEO/EJERCICIO")) {
            nuevoLabel = "¿A dónde vas?";
        } else {
            mostrar = false;
        }

        tvLabel.setText(nuevoLabel);
        container.setVisibility(mostrar ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String obtenerInstruccionDinamicaAct() {
        Spinner  spinner = dialogSpinnerRef != null ? dialogSpinnerRef.get() : null;
        String   tipoLabel = (spinner != null) ? (String) spinner.getSelectedItem() : "la seleccionada";

        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        String btnAction = "Añadir";
        if (dlg != null && dlg.findViewById(R.id.btnGuardarDialogActividad) instanceof Button) {
            btnAction = ((Button)dlg.findViewById(R.id.btnGuardarDialogActividad)).getText().toString();
        }

        return "La actividad es de tipo " + tipoLabel + ". ¿Quieres cambiar algo más o prefieres '" + btnAction + "' ya? Toca mi cabeza.";
    }

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        String tGlobal = texto.toLowerCase().trim();

        // ── Comandos globales ────────────────────────────────────────────────
        String btnText = "añadir";
        if (dlg.findViewById(R.id.btnGuardarDialogActividad) instanceof Button) {
            btnText = ((Button) dlg.findViewById(R.id.btnGuardarDialogActividad)).getText().toString().toLowerCase();
        }

        boolean quiereConfirmar = tGlobal.equalsIgnoreCase("confirmar") 
                || tGlobal.equalsIgnoreCase("aceptar") 
                || tGlobal.contains("guardar")
                || (btnText.contains("añadir") && tGlobal.contains("añadir"))
                || tGlobal.contains(btnText);

        if (quiereConfirmar) {
            hablarEnMain("Entendido.");
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnGuardarDialogActividad) != null) {
                    dlg.findViewById(R.id.btnGuardarDialogActividad).performClick();
                }
            });
            return;
        }

        if (tGlobal.contains("cancelar") || tGlobal.contains("atrás") || tGlobal.contains("atras") || tGlobal.contains("cerrar")) {
            hablarEnMain("Vale, cerramos.");
            runOnUiThread(dlg::dismiss);
            return;
        }

        // ── Manejo por estados ───────────────────────────────────────────────
        switch (campoEspera) {
            case CONFIRMACION_CAMPO: {
                if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto") || tGlobal.contains("vale") || tGlobal.contains("bueno") || tGlobal.contains("está bien")) {
                    aplicarValorConfirmadoAct();
                    hablarEnMain("Perfecto.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR), 1000);
                } else {
                    hablarEnMain("Vaya, lo siento. Intentémoslo de nuevo.");
                    CampoVozEspera volverA = campoAConfirmar;
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(volverA), 1500);
                }
                break;
            }

            case DESCRIPCION: {
                valorPendienteConfirmar = texto;
                campoAConfirmar = CampoVozEspera.DESCRIPCION;
                anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                break;
            }

            case TIPO: {
                String[] claves = TipoActividad.claves();
                String[] etiquetas = TipoActividad.etiquetas();
                String match = null;
                for (int i = 0; i < etiquetas.length; i++) {
                    if (tGlobal.contains(etiquetas[i].toLowerCase()) || tGlobal.contains(claves[i].toLowerCase())) {
                        match = etiquetas[i];
                        break;
                    }
                }
                if (match != null) {
                    valorPendienteConfirmar = match;
                    campoAConfirmar = CampoVozEspera.TIPO;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No reconocí ese tipo. Inténtalo de nuevo.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO), 2500);
                }
                break;
            }

            case HORA: {
                int minutos = parsearHoraVoz(texto);
                if (minutos != -1) {
                    int h = minutos / 60;
                    int m = minutos % 60;
                    valorPendienteConfirmar = String.format("%02d:%02d", h, m);
                    campoAConfirmar = CampoVozEspera.HORA;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No te entendí bien la hora. Inténtalo de nuevo.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.HORA), 2500);
                }
                break;
            }

            case DIA_SEMANA: {
                List<Integer> dias = parsearDiasVoz(texto);
                if (!dias.isEmpty()) {
                    valorPendienteConfirmar = texto;
                    campoAConfirmar = CampoVozEspera.DIA_SEMANA;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No entendí los días. Di por ejemplo: lunes y jueves.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 2500);
                }
                break;
            }

            case CAMPO_EDITAR: {
                if (tGlobal.contains("descripción") || tGlobal.contains("descripcion") || tGlobal.contains("detalle")) {
                    hablarEnMain("Entendido.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION), 1000);
                } else if (tGlobal.contains("tipo") || tGlobal.contains("categoría") || tGlobal.contains("categoria")) {
                    hablarEnMain("Vale.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO), 1000);
                } else if (tGlobal.contains("hora") || tGlobal.contains("momento") || tGlobal.contains("cuándo")) {
                    hablarEnMain("De acuerdo.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.HORA), 1000);
                } else if (tGlobal.contains("día") || tGlobal.contains("dias") || tGlobal.contains("semana")) {
                    hablarEnMain("Cambiamos los días.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 1000);
                } else {
                    hablarEnMain("No te entendí. Di por ejemplo: descripción o tipo.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR), 2500);
                }
                break;
            }
        }
    }

    private void aplicarValorConfirmadoAct() {
        runOnUiThread(() -> {
            switch (campoAConfirmar) {
                case DESCRIPCION:
                    if (dialogEtDescRef != null && dialogEtDescRef.get() != null) dialogEtDescRef.get().setText(valorPendienteConfirmar);
                    break;
                case TIPO:
                    Spinner spinner = dialogSpinnerRef != null ? dialogSpinnerRef.get() : null;
                    if (spinner != null) {
                        String[] etiquetas = TipoActividad.etiquetas();
                        for (int i = 0; i < etiquetas.length; i++) {
                            if (etiquetas[i].equalsIgnoreCase(valorPendienteConfirmar)) {
                                spinner.setSelection(i);
                                break;
                            }
                        }
                    }
                    break;
                case HORA:
                    int mTotal = parsearHoraVoz(valorPendienteConfirmar);
                    if (mTotal != -1) {
                        horaSeleccionada = mTotal;
                        if (dialogTvHoraRef != null && dialogTvHoraRef.get() != null) actualizarDisplayHora(dialogTvHoraRef.get(), mTotal);
                    }
                    break;
                case DIA_SEMANA:
                    List<Integer> dias = parsearDiasVoz(valorPendienteConfirmar);
                    if (!dias.isEmpty()) {
                        diasSeleccionados.clear();
                        diasSeleccionados.addAll(dias);
                        if (dialogBtnsDiaRef != null && dialogBtnsDiaRef.get() != null) actualizarBotonesDia(dialogBtnsDiaRef.get(), VALORES_DIA, diasSeleccionados);
                    }
                    break;
            }
        });
    }

    private void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada  = (existente != null) ? existente.getHoraMinutos() : HORA_DEFAULT_MINUTOS;
        diasSeleccionados = (existente != null && existente.getDiasSemana() != null)
                ? new ArrayList<>(existente.getDiasSemana()) : new ArrayList<>();
        campoEspera = CampoVozEspera.NINGUNO;

        final View dv = LayoutInflater.from(this).inflate(R.layout.dialog_anadir_actividad, null);
        final TextView tvHora     = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc     = dv.findViewById(R.id.etDescripcionActividad);
        final Spinner  spinner    = dv.findViewById(R.id.spinnerTipoActividad);
        final TextView tvTitulo   = dv.findViewById(R.id.tvTituloDialogActividad);
        final Button   btnGuardar = dv.findViewById(R.id.btnGuardarDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");
        btnGuardar.setText(existente != null ? "GUARDAR" : "AÑADIR");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TipoActividad.etiquetas());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (existente != null) {
            etDesc.setText(existente.getDescripcion());
            String[] claves = TipoActividad.claves();
            for (int i = 0; i < claves.length; i++) {
                if (claves[i].equals(existente.getTipo())) { spinner.setSelection(i); break; }
            }
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);

        // Listener para actualizar campo dinámico según el tipo
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                actualizarCampoDinamico(dv, parent.getItemAtPosition(position).toString());
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

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

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));

        dialogTvHoraRef  = new java.lang.ref.WeakReference<>(tvHora);
        dialogBtnsDiaRef = new java.lang.ref.WeakReference<>(btnsDia);
        dialogEtDescRef  = new java.lang.ref.WeakReference<>(etDesc);
        dialogSpinnerRef = new java.lang.ref.WeakReference<>(spinner);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Evitar que el teclado salga solo
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        dialogRef = new java.lang.ref.WeakReference<>(dialog);

        dialog.setOnDismissListener(d -> {
            campoEspera = CampoVozEspera.NINGUNO;
            mainHandler.removeCallbacksAndMessages(null);
            pararVoz(); // Se calla al cerrar
        });

        dv.findViewById(R.id.btnCancelarDialogActividad).setOnClickListener(v -> dialog.dismiss());
        dv.findViewById(R.id.btnCancelarDialogActividad2).setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            final String desc = etDesc.getText().toString().trim();
            final String tipo = TipoActividad.claves()[spinner.getSelectedItemPosition()];

            // VALIDACIÓN: Descripción obligatoria para ciertos tipos
            if (desc.isEmpty()) {
                if (tipo.equals(Actividad.TIPO_MEDICACION) || tipo.equals(Actividad.TIPO_COMER) 
                    || tipo.equals(Actividad.TIPO_LLAMADA_FAMILIAR) || tipo.equals(Actividad.TIPO_PASEO_EJERCICIO)) {
                    
                    String campoFaltante = "la información";
                    if (tipo.equals(Actividad.TIPO_MEDICACION)) campoFaltante = "el nombre de la medicina";
                    else if (tipo.equals(Actividad.TIPO_COMER)) campoFaltante = "qué vas a comer";
                    else if (tipo.equals(Actividad.TIPO_LLAMADA_FAMILIAR)) campoFaltante = "a quién vas a llamar";

                    hablarEnMain("Por favor, dime " + campoFaltante + ". Es necesario para poder ayudarte mejor.");
                    mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION), 2500);
                    return;
                }
            }

            // VALIDACIÓN: Días de la semana obligatorios
            if (diasSeleccionados == null || diasSeleccionados.isEmpty()) {
                hablarEnMain("Faltan los días de la semana. Por favor, selecciona al menos uno.");
                mainHandler.postDelayed(() -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA), 2500);
                return;
            }

            final int    duracion  = new Actividad(0, tipo, horaSeleccionada, "").getDuracionMinutos();
            final int    idExcluir = (existente != null) ? existente.getId() : ID_NUEVO;

            dbExecutor.execute(() -> {
                final boolean solapa = haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, idExcluir);
                runOnUiThread(() -> {
                    if (solapa) {
                        new AlertDialog.Builder(ActividadesActivity.this)
                                .setTitle("Solapamiento")
                                .setMessage("Ya hay una actividad que se solapa. ¿Deseas guardarla de todos modos?")
                                .setPositiveButton("Sí", (d, w) -> { guardarActividad(existente, tipo, desc); dialog.dismiss(); })
                                .setNegativeButton("No", null).show();
                    } else {
                        guardarActividad(existente, tipo, desc);
                        dialog.dismiss();
                    }
                });
            });
        });

        dialog.show();
        mainHandler.postDelayed(() -> {
            if (existente == null) {
                anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
            } else {
                anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR);
            }
        }, 400);
    }

    private String parsearTipoVoz(String texto) {
        texto = texto.toLowerCase();
        if (texto.contains("medicación") || texto.contains("medicacion") || texto.contains("medicina") || texto.contains("pastilla")) return Actividad.TIPO_MEDICACION;
        if (texto.contains("agua") || texto.contains("beber")) return Actividad.TIPO_BEBER_AGUA;
        if (texto.contains("comer") || texto.contains("comida") || texto.contains("almuerzo") || texto.contains("desayuno") || texto.contains("cena")) return Actividad.TIPO_COMER;
        if (texto.contains("paseo") || texto.contains("ejercicio") || texto.contains("andar") || texto.contains("caminar")) return Actividad.TIPO_PASEO_EJERCICIO;
        if (texto.contains("juego") || texto.contains("jugar") || texto.contains("juegos")) return Actividad.TIPO_JUEGOS;
        if (texto.contains("aseo") || texto.contains("ducha") || texto.contains("baño") || texto.contains("higiene")) return Actividad.TIPO_ASEO;
        if (texto.contains("llamada") || texto.contains("familiar") || texto.contains("teléfono") || texto.contains("telefono") || texto.contains("familia")) return Actividad.TIPO_LLAMADA_FAMILIAR;
        if (texto.contains("dormir") || texto.contains("cama") || texto.contains("descansar") || texto.contains("noche")) return Actividad.TIPO_IR_DORMIR;
        return null;
    }

    private void renderizarLista() {
        dbExecutor.execute(() -> {
            final List<Actividad> lista = repo.getAll();
            Collections.sort(lista, (a, b) -> Integer.compare(a.getHoraMinutos(), b.getHoraMinutos()));
            runOnUiThread(() -> {
                containerActividades.removeAllViews();
                if (lista.isEmpty()) tvVacio.setVisibility(View.VISIBLE);
                else {
                    tvVacio.setVisibility(View.GONE);
                    for (Actividad a : lista) containerActividades.addView(crearItemActividad(a));
                }
            });
        });
    }

    private boolean haySolapamiento(List<Integer> dias, int horaMinutos, int duracion, int idAExcluir) {
        List<Actividad> lista = repo.getAll();
        for (Actividad a : lista) {
            if (a.getId() == idAExcluir) continue;
            if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado()) && a.coincideHoy()) continue;
            if (tienenDiaComun(dias, a.getDiasSemana())) {
                int s1 = horaMinutos, e1 = s1 + duracion;
                int s2 = a.getHoraMinutos(), e2 = s2 + a.getDuracionMinutos();
                if (s1 < e2 && s2 < e1) return true;
            }
        }
        return false;
    }

    private boolean tienenDiaComun(List<Integer> d1, List<Integer> d2) {
        if (d1 == null || d2 == null) return false;
        for (Integer d : d1) if (d2.contains(d)) return true;
        return false;
    }

    private View crearItemActividad(final Actividad a) {
        View item = LayoutInflater.from(this).inflate(R.layout.item_actividad, containerActividades, false);
        ((TextView) item.findViewById(R.id.tvHoraItem)).setText(a.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvTipoItem)).setText(a.getTipoLabel());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor(a.getColorHex()));
        bg.setCornerRadius(dpToPx(14));
        item.setBackground(bg);
        item.setOnClickListener(v -> mostrarDialogoDetalle(a));
        View btnEditar = item.findViewById(R.id.btnEditarItem);
        if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado())) btnEditar.setVisibility(View.GONE);
        else btnEditar.setOnClickListener(v -> mostrarDialogoAnadir(a));
        item.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> confirmarEliminar(a));
        return item;
    }

    /**
     * Muestra/Oculta y renombra el campo de detalles según el tipo de actividad.
     */
    private void actualizarCampoDinamico(View dv, String tipoEtiqueta) {
        View container = dv.findViewById(R.id.containerDetallesDinamicos);
        TextView tvLabel = dv.findViewById(R.id.tvLabelDetalleDinamico);
        if (container == null || tvLabel == null) return;

        boolean mostrar = true;
        String nuevoLabel = "Información adicional";

        if (tipoEtiqueta.equalsIgnoreCase("MEDICACIÓN")) {
            nuevoLabel = "¿Qué medicamento es?";
        } else if (tipoEtiqueta.equalsIgnoreCase("COMER")) {
            nuevoLabel = "¿Qué vas a comer?";
        } else if (tipoEtiqueta.equalsIgnoreCase("LLAMADA FAMILIAR")) {
            nuevoLabel = "¿A quién vas a llamar?";
        } else if (tipoEtiqueta.equalsIgnoreCase("PASEO/EJERCICIO")) {
            nuevoLabel = "¿A dónde vas?";
        } else {
            mostrar = false;
        }

        tvLabel.setText(nuevoLabel);
        container.setVisibility(mostrar ? View.VISIBLE : View.GONE);
    }

    private void guardarActividad(Actividad existente, String tipo, String desc) {
        dbExecutor.execute(() -> {
            if (existente == null) {
                hablarEnMain("¡Listo! He añadido la actividad.");
                Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
                nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.add(nueva);
            } else {
                hablarEnMain("Perfecto. He guardado los cambios.");
                existente.setTipo(tipo); existente.setHoraMinutos(horaSeleccionada);
                existente.setDescripcion(desc); existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.update(existente);
            }
            runOnUiThread(this::renderizarLista);
        });
    }

    private void mostrarDialogoDetalle(final Actividad a) {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_detalle_actividad, null);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL); bg.setColor(Color.parseColor(a.getColorHex()));
        dv.findViewById(R.id.frameEmojiDetAct).setBackground(bg);
        ((android.widget.ImageView) dv.findViewById(R.id.tvEmojiDetAct)).setImageResource(a.getIconoRes());
        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText((a.getDescripcion() != null && !a.getDescripcion().isEmpty()) ? a.getDescripcion() : "—");
        int[] ids = { R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue, R.id.detDiaVie, R.id.detDiaSab, R.id.detDiaDom };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < ids.length; i++) {
            TextView tv = dv.findViewById(ids[i]);
            boolean activo = dias != null && dias.contains(VALORES_DIA[i]);
            tv.setBackgroundResource(activo ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            tv.setTextColor(activo ? Color.WHITE : Color.BLACK);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmarEliminar(final Actividad a) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Seguro?").setPositiveButton("Sí", (d, w) -> {
            hablarEnMain("He eliminado la actividad.");
            dbExecutor.execute(() -> { repo.delete(a.getId()); runOnUiThread(this::renderizarLista); });
        }).setNegativeButton("No", null).show();
    }

    private Integer parsearHoraVoz(String texto) {
        texto = texto.toLowerCase().trim();
        boolean esTarde = texto.contains("tarde") || texto.contains("noche") || texto.contains("pm");
        boolean esMañana = texto.contains("mañana") || texto.contains("am");

        // 1. Reemplazo de palabras comunes por números
        texto = texto.replace("media", "30").replace("cuarto", "15");
        
        // 2. Patrón "H y M" o "H : M" o "H M"
        // Buscamos algo como "10 y 20", "10 20", "diez y veinte"
        // Primero intentamos extraer los números (palabras o dígitos)
        int h = -1, m = -1;
        
        // Mapa de palabras a números
        String[] palabras = {"cero","una","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez",
                            "once","doce","trece","catorce","quince","dieciséis","diecisiete","dieciocho","diecinueve","veinte",
                            "veintiuno","veintidós","veintitrés","veinticuatro","veinticinco","veintiséis","veintisiete","veintiocho","veintinueve","treinta",
                            "cuarenta","cincuenta"};
        int[] valores = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,40,50};

        // Reemplazar palabras por números en el texto para facilitar regex
        for (int i = palabras.length - 1; i >= 0; i--) {
            texto = texto.replace(palabras[i], String.valueOf(valores[i]));
        }

        // Regex para "H menos M"
        java.util.regex.Matcher matMenos = java.util.regex.Pattern.compile("(\\d{1,2})\\s*menos\\s*(\\d{1,2})").matcher(texto);
        if (matMenos.find()) {
            h = Integer.parseInt(matMenos.group(1));
            m = Integer.parseInt(matMenos.group(2));
            h--; if (h < 0) h = 23;
            m = 60 - m;
            return ajustarTarde(h, esTarde, esMañana) * 60 + m;
        }

        // Regex para "H y M" o "H M" o "H:M"
        java.util.regex.Matcher matY = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(?:y|:|\\s)\\s*(\\d{1,2})").matcher(texto);
        if (matY.find()) {
            h = Integer.parseInt(matY.group(1));
            m = Integer.parseInt(matY.group(2));
        } else {
            // Solo hora
            java.util.regex.Matcher matH = java.util.regex.Pattern.compile("(\\d{1,2})").matcher(texto);
            if (matH.find()) {
                h = Integer.parseInt(matH.group(1));
                m = 0;
            }
        }

        if (h != -1 && h <= 23 && m <= 59) {
            if (h < 13) h = ajustarTarde(h, esTarde, esMañana);
            return h * 60 + m;
        }

        return null;
    }

    private int ajustarTarde(int h, boolean esTarde, boolean esMañana) {
        int res = (h == 12) ? 0 : h;
        if (esTarde) return (h == 12) ? 12 : res + 12;
        if (esMañana) return res;
        if (res >= 1 && res <= 6) return res + 12; // Heurística
        return h;
    }

    private List<Integer> parsearDiasVoz(String t) {
        t = t.toLowerCase(); List<Integer> d = new ArrayList<>();
        String[] n = {"lunes","martes","miércoles","jueves","viernes","sábado","domingo"};
        String[] a = {"lunes","martes","miercoles","jueves","viernes","sabado","domingo"};
        for (int i = 0; i < n.length; i++) if (t.contains(n[i]) || t.contains(a[i])) d.add(VALORES_DIA[i]);
        return d;
    }

    private void actualizarDisplayAnticipacion(TextView tv, int min) {
        if (min <= 0) tv.setText("Sin aviso previo");
        else tv.setText(min + " minutos antes");
    }

    /** Parsea un número hablado (0-60) */
    private Integer parsearNumeroVoz(String texto) {
        texto = texto.toLowerCase().trim();
        if (texto.contains("cero") || texto.contains("ningun")) return 0;

        String[] palabras = {"una","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez",
                "once","doce","trece","catorce","quince","veinte","treinta","cuarenta","cincuenta","sesenta"};
        int[] valores = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,20,30,40,50,60};

        for (int i = 0; i < palabras.length; i++) {
            if (texto.contains(palabras[i])) return valores[i];
        }

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1));

        return null;
    }

    private void hablarEnMain(String t) { runOnUiThread(() -> hablarOSimular(t)); }
    private void abrirTimePicker(TextView tv) {
        new TimePickerDialog(this, (v, h, m) -> { horaSeleccionada = h*60+m; actualizarDisplayHora(tv, horaSeleccionada); },
                horaSeleccionada/60, horaSeleccionada%60, true).show();
    }
    private void actualizarDisplayHora(TextView tv, int m) { tv.setText(String.format("%02d:%02d", m/60, m%60)); }
    private void actualizarBotonesDia(TextView[] b, int[] v, List<Integer> s) {
        for (int i=0; i<b.length; i++) {
            boolean sel = s.contains(v[i]);
            b[i].setBackgroundResource(sel ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            b[i].setTextColor(sel ? Color.WHITE : Color.BLACK);
        }
    }
    private float dpToPx(int dp) { return dp * getResources().getDisplayMetrics().density; }
}