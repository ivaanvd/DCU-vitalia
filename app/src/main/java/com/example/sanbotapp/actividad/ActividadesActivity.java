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

    private static final int HORA_DEFAULT_MINUTOS = 9 * 60; // 09:00
    private static final int ID_NUEVO = Integer.MIN_VALUE;

    private enum TipoActividad {
        MEDICACION(Actividad.TIPO_MEDICACION, "MEDICACIÓN"),
        BEBER_AGUA(Actividad.TIPO_BEBER_AGUA, "BEBER AGUA"),
        COMER(Actividad.TIPO_COMER, "COMER"),
        PASEO_EJERCICIO(Actividad.TIPO_PASEO_EJERCICIO, "PASEO/EJERCICIO"),
        JUEGOS(Actividad.TIPO_JUEGOS, "JUEGOS"),
        ASEO(Actividad.TIPO_ASEO, "ASEO"),
        LLAMADA_FAMILIAR(Actividad.TIPO_LLAMADA_FAMILIAR, "LLAMADA FAMILIAR"),
        IR_DORMIR(Actividad.TIPO_IR_DORMIR, "IR A DORMIR");

        final String clave;
        final String etiqueta;

        TipoActividad(String clave, String etiqueta) {
            this.clave = clave;
            this.etiqueta = etiqueta;
        }

        static String[] claves() {
            TipoActividad[] v = values();
            String[] r = new String[v.length];
            for (int i = 0; i < v.length; i++)
                r[i] = v[i].clave;
            return r;
        }

        static String[] etiquetas() {
            TipoActividad[] v = values();
            String[] r = new String[v.length];
            for (int i = 0; i < v.length; i++)
                r[i] = v[i].etiqueta;
            return r;
        }
    }

    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    private LinearLayout containerActividades;
    private TextView tvVacio;
    private ActividadRepository repo;
    private com.example.sanbotapp.recordatorio.RecordatorioRepository recordatorioRepo;

    private int horaSeleccionada = HORA_DEFAULT_MINUTOS;
    private List<Integer> diasSeleccionados = new ArrayList<>();
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;
    private String valorPendienteConfirmar = "";
    private CampoVozEspera campoAConfirmar = CampoVozEspera.NINGUNO;

    private boolean isAssistantActive = false;
    private boolean isEditingFromSummary = false;
    private boolean isManual = false;
    private String tipoSeleccionado = Actividad.TIPO_OTROS;

    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<TextView> dialogTvHoraRef;
    private java.lang.ref.WeakReference<TextView[]> dialogBtnsDiaRef;
    private java.lang.ref.WeakReference<EditText> dialogEtDescRef;
    private java.util.Map<String, Button> typeButtons = new java.util.HashMap<>();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService dbExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividades);
        setupTopBackBanner("Actividades");

        containerActividades = findViewById(R.id.containerActividades);
        tvVacio = findViewById(R.id.tvVacioActividades);
        repo = new ActividadRepository(this);
        recordatorioRepo = new com.example.sanbotapp.recordatorio.RecordatorioRepository(this);

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirActividad);
        btnAnadir.setOnClickListener(v -> mostrarDialogoAnadir(null));

        dbExecutor.execute(this::renderizarLista);
    }

    @Override
    protected void onRobotServiceReady() {
        super.onRobotServiceReady();
        // Solo renderizamos la lista, ya no saludamos cada vez para no cansar al usuario
        dbExecutor.execute(this::renderizarLista);
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
        if (dlg == null || !dlg.isShowing())
            return;
        if (campoEspera == CampoVozEspera.NINGUNO)
            return;

        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
                    mainHandler.post(this::escuchar);
    }

    private void anunciarCampoYEsperarToque(CampoVozEspera campo) {
        // Si venimos de resumen o de elegir qué editar, marcamos que estamos editando
        if (campoEspera == CampoVozEspera.RESUMEN_FINAL || campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (campo != CampoVozEspera.RESUMEN_FINAL && campo != CampoVozEspera.CONFIRMACION_CAMPO) {
                isEditingFromSummary = true;
            }
        }

        campoEspera = campo;
        String instruccion;
        switch (campo) {
            case DESCRIPCION:
                instruccion = obtenerInstruccionDescripcionAct();
                break;
            case TIPO:
                instruccion = "¿Qué tipo de actividad vamos a añadir? Puedes elegir una de las opciones que aparecen en pantalla.";
                break;
            case HORA:
                instruccion = "¿Y a qué hora te gustaría realizarla? Por ejemplo, puedes decir: a las diez y media.";
                break;
            case DIA_SEMANA:
                instruccion = "Ya casi terminamos. ¿Qué días de la semana quieres que te avise?";
                break;
            case CAMPO_EDITAR:
                instruccion = obtenerInstruccionDinamicaAct();
                break;
            case CONFIRMACION_CAMPO:
                instruccion = "He anotado '" + valorPendienteConfirmar + "'. ¿Está bien así?";
                break;
            case RESUMEN_FINAL:
                isEditingFromSummary = false;
                instruccion = "¿Es todo correcto? Si es así, pulsa 'sí, guardar'. Si quieres cambiar algo, pulsa 'no, cambiar'.";
                gestionarFeedbackHardware("SUMMARY_START");
                break;
            case ELECCION_EDICION:
                isEditingFromSummary = true;
                instruccion = "¿Qué campo quieres cambiar? Pulsa uno de los botones de la pantalla.";
                gestionarFeedbackHardware("THINKING_START");
                break;
            default:
                return;
        }
        if (campo == CampoVozEspera.RESUMEN_FINAL) isEditingFromSummary = false;
        if (campo == CampoVozEspera.ELECCION_EDICION) isEditingFromSummary = true;
        
        if (campo == CampoVozEspera.CAMPO_EDITAR || campo == CampoVozEspera.ELECCION_EDICION) {
            hablarEnMain(instruccion, com.qihancloud.opensdk.function.beans.EmotionsType.QUESTION);
        } else {
            hablarEnMain(instruccion);
        }
        actualizarProgresoYResaltadoAct(campo);
    }

    /**
     * Actualiza visualmente el campo activo y los indicadores de progreso.
     * Mejora Área 2: Feedback visual.
     */
    private void actualizarProgresoYResaltadoAct(CampoVozEspera campo) {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null) return;

        runOnUiThread(() -> {
            // 1. Limpiar resaltados previos
            View spinner = dlg.findViewById(R.id.spinnerTipoActividad);
            View etDesc = dlg.findViewById(R.id.etDescripcionActividad);
            View tvHora = dlg.findViewById(R.id.tvHoraDialogActividad);
            View containerDias = dlg.findViewById(R.id.containerDiasSemana);

            if (spinner != null) spinner.setBackgroundResource(R.drawable.bg_spinner_custom);
            if (etDesc != null) etDesc.setBackgroundResource(R.drawable.bg_campo_descripcion);
            if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_campo_descripcion);
            if (containerDias != null) containerDias.setPadding(0, 0, 0, 0);

            View dotContainer = dlg.findViewById(R.id.dotContainerAct);
            if (dotContainer != null) dotContainer.setVisibility(View.VISIBLE);

            // 2. Resaltar campo actual y calcular paso de progreso
            int step = 0;
            View s1 = dlg.findViewById(R.id.step1Container);
            View s2 = dlg.findViewById(R.id.containerDetallesDinamicos);
            View s3 = dlg.findViewById(R.id.step3Container);
            View s4 = dlg.findViewById(R.id.step4Container);
            View sum = dlg.findViewById(R.id.summaryContainerAct);
            View edt = dlg.findViewById(R.id.editChoiceContainerAct);
            View step0 = dlg.findViewById(R.id.step0Container);

            if (s1 != null) s1.setVisibility(View.GONE);
            if (s2 != null) s2.setVisibility(View.GONE);
            if (s3 != null) s3.setVisibility(View.GONE);
            if (s4 != null) s4.setVisibility(View.GONE);
            if (sum != null) sum.setVisibility(View.GONE);
            if (edt != null) edt.setVisibility(View.GONE);

            if (campo == CampoVozEspera.NINGUNO) {
                if (s1 != null) s1.setVisibility(View.GONE);
                if (s2 != null) s2.setVisibility(View.GONE);
                if (s3 != null) s3.setVisibility(View.GONE);
                if (s4 != null) s4.setVisibility(View.GONE);
                if (sum != null) sum.setVisibility(View.GONE);
                if (edt != null) edt.setVisibility(View.GONE);
                if (dotContainer != null) dotContainer.setVisibility(View.GONE);
                
                // OCULTAR MIC Y BOTONES EN PASO 0
                View mic = dlg.findViewById(R.id.containerMicEstadoAct);
                Button btnAnt = dlg.findViewById(R.id.btnWizardAnterior);
                Button btnSig = dlg.findViewById(R.id.btnWizardSiguiente);
                if (mic != null) mic.setVisibility(View.GONE);
                if (btnAnt != null) btnAnt.setVisibility(View.GONE);
                if (btnSig != null) btnSig.setVisibility(View.GONE);
                
                if (step0 != null) step0.setVisibility(View.VISIBLE);
                return;
            }

            // MOSTRAR MIC POR DEFECTO EN EL RESTO DE PASOS
            View mic = dlg.findViewById(R.id.containerMicEstadoAct);
            if (mic != null) mic.setVisibility(View.VISIBLE);

            // Siempre ocultar el paso 0 si no estamos en NINGUNO
            if (step0 != null) step0.setVisibility(View.GONE);

            switch (campo) {
                case TIPO:
                    step = 1;
                    if (s1 != null) s1.setVisibility(View.VISIBLE);
                    break;
                case DESCRIPCION:
                    step = 2;
                    if (s2 != null) s2.setVisibility(View.VISIBLE);
                    if (etDesc != null) etDesc.setBackgroundResource(R.drawable.bg_field_active);
                    break;
                case HORA:
                    step = 3;
                    if (s3 != null) s3.setVisibility(View.VISIBLE);
                    if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                    break;
                case DIA_SEMANA:
                    step = 4;
                    if (s4 != null) s4.setVisibility(View.VISIBLE);
                    break;
                case CAMPO_EDITAR:
                    step = 0; // Menu de edicion es como un paso intermedio
                    if (edt != null) edt.setVisibility(View.VISIBLE);
                    break;
                case CONFIRMACION_CAMPO:
                    // Mostrar solo el paso que se está confirmando y resaltarlo
                    if (campoAConfirmar == CampoVozEspera.TIPO) { 
                        step = 1; 
                        if (s1 != null) s1.setVisibility(View.VISIBLE); 
                    }
                    else if (campoAConfirmar == CampoVozEspera.DESCRIPCION) { 
                        step = 2; 
                        if (s2 != null) s2.setVisibility(View.VISIBLE); 
                        if (etDesc != null) etDesc.setBackgroundResource(R.drawable.bg_field_active);
                    }
                    else if (campoAConfirmar == CampoVozEspera.HORA) { 
                        step = 3; 
                        if (s3 != null) s3.setVisibility(View.VISIBLE); 
                        if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                    }
                    else if (campoAConfirmar == CampoVozEspera.DIA_SEMANA) { 
                        step = 4; 
                        if (s4 != null) s4.setVisibility(View.VISIBLE); 
                        if (containerDias != null) containerDias.setPadding(4, 4, 4, 4);
                    }
                    break;
                case RESUMEN_FINAL:
                    step = 5;
                    View summary = dlg.findViewById(R.id.summaryContainerAct);
                    if (summary != null) {
                        summary.setVisibility(View.VISIBLE);
                        actualizarTextoResumen(dlg);
                    }
                    break;
                case ELECCION_EDICION:
                    step = 5;
                    View editChoice = dlg.findViewById(R.id.editChoiceContainerAct);
                    if (editChoice != null) editChoice.setVisibility(View.VISIBLE);
                    break;
            }

            // Actualizar botones de navegación y colores
            Button btnAnt = dlg.findViewById(R.id.btnWizardAnterior);
            Button btnSig = dlg.findViewById(R.id.btnWizardSiguiente);
            
            boolean isStep0 = (dlg.findViewById(R.id.step0Container) != null && dlg.findViewById(R.id.step0Container).getVisibility() == View.VISIBLE);
            boolean isManual = !isAssistantActive; // Necesitamos esta variable

            if (btnAnt != null && btnSig != null) {
                if (isStep0) {
                    btnAnt.setVisibility(View.GONE);
                    btnSig.setVisibility(View.GONE);
                } else if (campo == CampoVozEspera.CONFIRMACION_CAMPO) {
                    // Solo Repetir y Confirmar
                    btnAnt.setVisibility(View.VISIBLE);
                    btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("REPETIR");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    btnSig.setText("CONFIRMAR");
                    btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else if (campo == CampoVozEspera.RESUMEN_FINAL) {
                    btnAnt.setVisibility(View.VISIBLE);
                    btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("no, cambiar");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar);
                    btnSig.setText("sí, guardar");
                    btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else if (campo == CampoVozEspera.ELECCION_EDICION) {
                    btnAnt.setVisibility(View.VISIBLE);
                    btnSig.setVisibility(View.GONE); // No hay siguiente aquí, se eligen botones
                    btnAnt.setText("VOLVER");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                } else if (isEditingFromSummary) {
                    // MODO EDICIÓN DESDE RESUMEN
                    btnAnt.setVisibility(View.VISIBLE);
                    btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("VOLVER");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    btnSig.setText("CONFIRMAR");
                    btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else {
                    btnAnt.setVisibility(View.VISIBLE);
                    btnSig.setVisibility(View.VISIBLE);
                    
                    if (campo == CampoVozEspera.CAMPO_EDITAR) {
                        btnAnt.setText("CANCELAR");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar);
                        btnSig.setText("GUARDAR");
                        btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                    } else if (step == 1) {
                        btnAnt.setText("CANCELAR");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar);
                        btnSig.setText("SIGUIENTE");
                        btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    } else if (step == 4) {
                        btnAnt.setText("ANTERIOR");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                        btnSig.setText("SIGUIENTE"); // Ahora va al resumen
                        btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    } else {
                        btnAnt.setText("ANTERIOR");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                        btnSig.setText("SIGUIENTE");
                        btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    }
                }
            }

            // 3. Actualizar bolitas de progreso
            View progressLayout = dlg.findViewById(R.id.dotContainerAct);
            View micLayout = dlg.findViewById(R.id.containerMicEstadoAct);
            
            if (progressLayout != null) progressLayout.setVisibility(isStep0 ? View.GONE : View.VISIBLE);
            if (micLayout != null) {
                // No mostrar micro si es manual o paso 0 o resumen/elección o confirmación
                boolean ocultarMic = isStep0 || isManual || campo == CampoVozEspera.RESUMEN_FINAL 
                    || campo == CampoVozEspera.ELECCION_EDICION || campo == CampoVozEspera.CONFIRMACION_CAMPO;
                micLayout.setVisibility(ocultarMic ? View.GONE : View.VISIBLE);
            }

            int[] dotIds = {R.id.dotStep1, R.id.dotStep2, R.id.dotStep3, R.id.dotStep4};
            for (int i = 0; i < dotIds.length; i++) {
                View dot = dlg.findViewById(dotIds[i]);
                if (dot != null) {
                    dot.setBackgroundResource(i < step ? R.drawable.step_dot_active : R.drawable.step_dot_inactive);
                }
            }
        });
    }

    /**
     * Devuelve una instrucción personalizada para el campo descripción según el
     * tipo de actividad.
     */
    private String obtenerInstruccionDescripcionAct() {
        String tipo = tipoSeleccionado;
        if (tipo == null) tipo = "";

        if (tipo.equals(Actividad.TIPO_MEDICACION)) {
            return "¿Qué medicina te toca tomar? Por ejemplo: 'Paracetamol'. Toca mi cabeza y dímelo.";
        } else if (tipo.equals(Actividad.TIPO_COMER)) {
            return "¿Qué vas a comer hoy? Por ejemplo: 'Sopa de verduras'. Toca mi cabeza y dímelo.";
        } else if (tipo.equals(Actividad.TIPO_LLAMADA_FAMILIAR)) {
            return "¿A quién vas a llamar? Por ejemplo: 'A mi hija María'. Toca mi cabeza y dímelo.";
        } else if (tipo.equals(Actividad.TIPO_PASEO_EJERCICIO)) {
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

        if (container == null || tvLabel == null)
            return;

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
        String tipo = tipoSeleccionado;
        String tipoLabel = (tipo != null) ? tipo : "la seleccionada";

        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        String btnAction = "GUARDAR";
        if (dlg != null && dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnAction = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString();
        }

        return "La actividad es de tipo " + tipoLabel + ". ¿Quieres cambiar algo más o prefieres '" + btnAction
                + "' ya? Toca mi cabeza.";
    }

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty())
            return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing())
            return;

        String tGlobal = texto.toLowerCase().trim();

        // ── Comandos globales ────────────────────────────────────────────────
        String btnText = "guardar";
        if (dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnText = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString().toLowerCase();
        }

        boolean quiereConfirmar = tGlobal.equalsIgnoreCase("confirmar")
                || tGlobal.equalsIgnoreCase("aceptar")
                || tGlobal.contains("guardar")
                || (btnText.contains("añadir") && tGlobal.contains("añadir"))
                || tGlobal.contains(btnText)
                || tGlobal.equalsIgnoreCase("siguiente");

        if (quiereConfirmar) {
            hablarEnMain("Entendido.");
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnWizardSiguiente) != null) {
                    dlg.findViewById(R.id.btnWizardSiguiente).performClick();
                }
            });
            return;
        }

        if (tGlobal.contains("cancelar") || tGlobal.contains("atrás") || tGlobal.contains("atras")
                || tGlobal.contains("cerrar")) {
            hablarEnMain("Vale.");
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnWizardAnterior) != null) {
                    dlg.findViewById(R.id.btnWizardAnterior).performClick();
                }
            });
            return;
        }

        if (campoEspera == CampoVozEspera.RESUMEN_FINAL) {
            if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto") || tGlobal.contains("bien")) {
                hablarEnMain("¡Perfecto! Guardando...");
                runOnUiThread(() -> {
                    if (dlg.findViewById(R.id.btnWizardSiguiente) != null) {
                        dlg.findViewById(R.id.btnWizardSiguiente).performClick();
                    }
                });
            } else if (tGlobal.contains("no") || tGlobal.contains("cambiar") || tGlobal.contains("error")) {
                hablarEnMain("De acuerdo. ¿Qué quieres cambiar?");
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            }
            return;
        }

        if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (tGlobal.contains("tipo") || tGlobal.contains("actividad")) {
                hablarEnMain("Vale, cambiamos el tipo.");
                anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
            } else if (tGlobal.contains("hora")) {
                hablarEnMain("De acuerdo, la hora.");
                anunciarCampoYEsperarToque(CampoVozEspera.HORA);
            } else if (tGlobal.contains("día") || tGlobal.contains("dia") || tGlobal.contains("semana")) {
                hablarEnMain("Entendido, los días.");
                anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
            } else if (tGlobal.contains("descripción") || tGlobal.contains("descripcion") || tGlobal.contains("detalle")) {
                hablarEnMain("Cambiamos los detalles.");
                anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
            } else if (tGlobal.contains("volver") || tGlobal.contains("resumen")) {
                hablarEnMain("Volvemos al resumen.");
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
            }
            return;
        }

        // ── Manejo por estados ───────────────────────────────────────────────
        switch (campoEspera) {
            case CONFIRMACION_CAMPO: {
                if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto")
                        || tGlobal.contains("vale") || tGlobal.contains("bueno") || tGlobal.contains("está bien")) {
                    aplicarValorConfirmadoAct();
                    hablarEnMain("Perfecto.");
                } else {
                    hablarEnMain("Vaya, lo siento. Intentémoslo de nuevo.");
                    CampoVozEspera volverA = campoAConfirmar;
                    anunciarCampoYEsperarToque(volverA);
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
                    anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
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
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
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
                    anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
                }
                break;
            }

            case CAMPO_EDITAR: {
                if (tGlobal.contains("descripción") || tGlobal.contains("descripcion") || tGlobal.contains("detalle")) {
                    hablarEnMain("Entendido.");
                    anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
                } else if (tGlobal.contains("tipo") || tGlobal.contains("categoría") || tGlobal.contains("categoria")) {
                    hablarEnMain("Vale.");
                    anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
                } else if (tGlobal.contains("hora") || tGlobal.contains("momento") || tGlobal.contains("cuándo")) {
                    hablarEnMain("De acuerdo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                } else if (tGlobal.contains("día") || tGlobal.contains("dias") || tGlobal.contains("semana")) {
                    hablarEnMain("Cambiamos los días.");
                    anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
                } else {
                    hablarEnMain("No te entendí. Di por ejemplo: descripción o tipo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR);
                }
                break;
            }
        }
    }

    private void aplicarValorConfirmadoAct() {
        runOnUiThread(() -> {
            switch (campoAConfirmar) {
                case DESCRIPCION:
                    if (dialogEtDescRef != null && dialogEtDescRef.get() != null)
                        dialogEtDescRef.get().setText(valorPendienteConfirmar);
                    break;
                case TIPO:
                    String[] etiquetas = TipoActividad.etiquetas();
                    String[] claves = TipoActividad.claves();
                    for (int i = 0; i < etiquetas.length; i++) {
                        if (etiquetas[i].equalsIgnoreCase(valorPendienteConfirmar)) {
                            seleccionarTipoUI(claves[i]);
                            break;
                        }
                    }
                    break;
                case HORA:
                    int mTotal = parsearHoraVoz(valorPendienteConfirmar);
                    if (mTotal != -1) {
                        horaSeleccionada = mTotal;
                        if (dialogTvHoraRef != null && dialogTvHoraRef.get() != null)
                            actualizarDisplayHora(dialogTvHoraRef.get(), mTotal);
                    }
                    break;
                case DIA_SEMANA:
                    List<Integer> dias = parsearDiasVoz(valorPendienteConfirmar);
                    if (!dias.isEmpty()) {
                        diasSeleccionados.clear();
                        diasSeleccionados.addAll(dias);
                        if (dialogBtnsDiaRef != null && dialogBtnsDiaRef.get() != null)
                            actualizarBotonesDia(dialogBtnsDiaRef.get(), VALORES_DIA, diasSeleccionados);
                    }
                    break;
            }

            // Progresar al siguiente paso automáticamente
            CampoVozEspera siguiente = CampoVozEspera.RESUMEN_FINAL;
            if (isEditingFromSummary) {
                siguiente = CampoVozEspera.RESUMEN_FINAL;
            } else {
                if (campoAConfirmar == CampoVozEspera.TIPO) siguiente = CampoVozEspera.DESCRIPCION;
                else if (campoAConfirmar == CampoVozEspera.DESCRIPCION) siguiente = CampoVozEspera.HORA;
                else if (campoAConfirmar == CampoVozEspera.HORA) siguiente = CampoVozEspera.DIA_SEMANA;
            }

            anunciarCampoYEsperarToque(siguiente);
        });
    }

    private void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada = (existente != null) ? existente.getHoraMinutos() : HORA_DEFAULT_MINUTOS;
        diasSeleccionados = (existente != null && existente.getDiasSemana() != null)
                ? new ArrayList<>(existente.getDiasSemana())
                : new ArrayList<>();
        campoEspera = CampoVozEspera.NINGUNO;
        isEditingFromSummary = false;

        final View dv = LayoutInflater.from(this).inflate(R.layout.dialog_anadir_actividad, null);
        final TextView tvHora = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc = dv.findViewById(R.id.etDescripcionActividad);
        final TextView tvTitulo = dv.findViewById(R.id.tvTituloDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");

        if (existente != null) {
            etDesc.setText(existente.getDescripcion());
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);

        // Gestión de Selección de Tipo (Botones)
        setupBotonesTipo(dv);
        if (existente != null) {
            seleccionarTipoUI(existente.getTipo());
        } else {
            seleccionarTipoUI(Actividad.TIPO_MEDICACION);
        }

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

        dialogTvHoraRef = new java.lang.ref.WeakReference<>(tvHora);
        dialogBtnsDiaRef = new java.lang.ref.WeakReference<>(btnsDia);
        dialogEtDescRef = new java.lang.ref.WeakReference<>(etDesc);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        dialogRef = new java.lang.ref.WeakReference<>(dialog);

        // MEJORA P0: Botón de micrófono en pantalla
        View btnMic = dv.findViewById(R.id.btnMicDialogAct);
        TextView tvEstadoMic = dv.findViewById(R.id.tvEstadoMicAct);
        if (btnMic != null && tvEstadoMic != null) {
            setMicUI(btnMic, tvEstadoMic);
            btnMic.setOnClickListener(v -> onCabezaTocada());
        }

        dialog.setOnDismissListener(d -> {
            setMicUI(null, null);
            campoEspera = CampoVozEspera.NINGUNO;
            mainHandler.removeCallbacksAndMessages(null);
            pararVoz();
        });

        // BOTONES NAVEGACIÓN WIZARD
        Button btnSig = dv.findViewById(R.id.btnWizardSiguiente);
        Button btnAnt = dv.findViewById(R.id.btnWizardAnterior);

        btnAnt.setOnClickListener(v -> {
            gestionarFeedbackHardware("CANCELADO");
            
            // Si estamos editando un campo concreto viniendo del resumen, volver siempre al resumen
            if (isEditingFromSummary && campoEspera != CampoVozEspera.ELECCION_EDICION && campoEspera != CampoVozEspera.RESUMEN_FINAL) {
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                return;
            }

            switch (campoEspera) {
                case TIPO:
                case CAMPO_EDITAR:
                    dialog.dismiss();
                    break;
                case DESCRIPCION:
                    anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
                    break;
                case HORA:
                    anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
                    break;
                case DIA_SEMANA:
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                    break;
                case CONFIRMACION_CAMPO:
                    // REPETIR
                    anunciarCampoYEsperarToque(campoAConfirmar);
                    break;
                case RESUMEN_FINAL:
                    // NO, CAMBIAR
                    anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
                    break;
                case ELECCION_EDICION:
                    // VOLVER al resumen
                    anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    break;
                default:
                    if (isEditingFromSummary) {
                        anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    } else {
                        dialog.dismiss();
                    }
                    break;
            }
        });

        btnSig.setOnClickListener(v -> {
            switch (campoEspera) {
                case TIPO:
                    if (tipoSeleccionado == null || tipoSeleccionado.isEmpty()) {
                        hablarEnMain("Por favor, selecciona qué actividad vas a hacer.");
                        return;
                    }
                    if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    else anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
                    break;
                case DESCRIPCION:
                    if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    else anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                    break;
                case HORA:
                    if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    else anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
                    break;
                case DIA_SEMANA:
                    if (diasSeleccionados == null || diasSeleccionados.isEmpty()) {
                        hablarEnMain("Dime qué días quieres repetir la actividad.");
                        return;
                    }
                    anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    break;
                case CONFIRMACION_CAMPO:
                    aplicarValorConfirmadoAct();
                    break;
                case RESUMEN_FINAL:
                case CAMPO_EDITAR:
                case ELECCION_EDICION:
                    validarYGuardarActividad(existente, etDesc, dialog);
                    break;
                default:
                    if (isEditingFromSummary) {
                        anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    }
                    break;
            }
        });

        // Listeners para botones de edición granular (ELECCION_EDICION)
        dv.findViewById(R.id.btnEditTipo).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.TIPO));
        dv.findViewById(R.id.btnEditDesc).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION));
        dv.findViewById(R.id.btnEditHora).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.HORA));
        dv.findViewById(R.id.btnEditDias).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA));

        dv.findViewById(R.id.btnCancelarDialogActividad2).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Gestión de Paso 0 (Asistente)
        View step0 = dv.findViewById(R.id.step0Container);
        if (existente == null) {
            actualizarProgresoYResaltadoAct(CampoVozEspera.NINGUNO);
            
            hablarOSimular("Hola, soy Vitalia. ¿Quieres que te ayude a añadir esta actividad o prefieres hacerlo tú solo?");

            dv.findViewById(R.id.btnEmpezarAsistenteAct).setOnClickListener(v -> {
                isAssistantActive = true;
                step0.setVisibility(View.GONE);
                anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
            });

            dv.findViewById(R.id.btnManualAct).setOnClickListener(v -> {
                isAssistantActive = false;
                isManual = true;
                step0.setVisibility(View.GONE);
                pararVoz();
                mainHandler.removeCallbacksAndMessages(null);
                anunciarCampoYEsperarToque(CampoVozEspera.TIPO); 
            });
            
            // Forzar actualización visual para ocultar mic/dots en paso 0
            actualizarProgresoYResaltadoAct(CampoVozEspera.NINGUNO);
        } else {
            if (step0 != null) step0.setVisibility(View.GONE);
            anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
        }
    }

    private void setupBotonesTipo(View dv) {
        typeButtons.clear();
        typeButtons.put(Actividad.TIPO_MEDICACION, dv.findViewById(R.id.btnTypeMed));
        typeButtons.put(Actividad.TIPO_COMER, dv.findViewById(R.id.btnTypeCom));
        typeButtons.put(Actividad.TIPO_BEBER_AGUA, dv.findViewById(R.id.btnTypeAgu));
        typeButtons.put(Actividad.TIPO_PASEO_EJERCICIO, dv.findViewById(R.id.btnTypePas));
        typeButtons.put(Actividad.TIPO_ASEO, dv.findViewById(R.id.btnTypeAse));
        typeButtons.put(Actividad.TIPO_JUEGOS, dv.findViewById(R.id.btnTypeJue));
        typeButtons.put(Actividad.TIPO_LLAMADA_FAMILIAR, dv.findViewById(R.id.btnTypeFam));
        typeButtons.put(Actividad.TIPO_IR_DORMIR, dv.findViewById(R.id.btnTypeDor));

        for (java.util.Map.Entry<String, Button> entry : typeButtons.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().setOnClickListener(v -> {
                    seleccionarTipoUI(entry.getKey());
                    actualizarCampoDinamico(dv, entry.getKey());
                });
            }
        }
    }

    private void seleccionarTipoUI(String tipo) {
        this.tipoSeleccionado = tipo;
        for (java.util.Map.Entry<String, Button> entry : typeButtons.entrySet()) {
            if (entry.getValue() != null) {
                boolean isSelected = entry.getKey().equals(tipo);
                entry.getValue().setBackgroundResource(isSelected ? R.drawable.bg_tipo_seleccionado : R.drawable.bg_tipo_normal);
                entry.getValue().setTextColor(isSelected ? android.graphics.Color.WHITE : android.graphics.Color.BLACK);
            }
        }
    }

    private void actualizarTextoResumen(AlertDialog dlg) {
        String desc = "";
        if (dialogEtDescRef != null && dialogEtDescRef.get() != null) {
            desc = dialogEtDescRef.get().getText().toString().trim();
        }

        String tipoLabel = "(Desconocido)";
        String[] claves = TipoActividad.claves();
        String[] etiquetas = TipoActividad.etiquetas();
        for(int i=0; i<claves.length; i++) {
            if(claves[i].equals(tipoSeleccionado)) {
                tipoLabel = etiquetas[i];
                break;
            }
        }

        TextView tvTipo = dlg.findViewById(R.id.tvSummaryTipoAct);
        TextView tvDesc = dlg.findViewById(R.id.tvSummaryDescAct);
        TextView tvHora = dlg.findViewById(R.id.tvSummaryHoraAct);
        TextView tvDias = dlg.findViewById(R.id.tvSummaryDiasAct);

        if (tvTipo != null) tvTipo.setText(tipoLabel);
        if (tvDesc != null) tvDesc.setText(desc.isEmpty() ? "(Vacío)" : desc);
        if (tvHora != null) tvHora.setText(String.format("%02d:%02d", horaSeleccionada / 60, horaSeleccionada % 60));
        
        if (tvDias != null) {
            if (diasSeleccionados.isEmpty()) {
                tvDias.setText("(Ninguno)");
            } else {
                StringBuilder sbDias = new StringBuilder();
                String[] nombres = { "Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom" };
                for (int i = 0; i < VALORES_DIA.length; i++) {
                    if (diasSeleccionados.contains(VALORES_DIA[i])) {
                        sbDias.append(nombres[i]).append(" ");
                    }
                }
                tvDias.setText(sbDias.toString().trim());
            }
        }
    }

    private void validarYGuardarActividad(Actividad existente, EditText etDesc, AlertDialog dialog) {
        final String desc = etDesc.getText().toString().trim();
        final String tipo = tipoSeleccionado;

        if (desc.isEmpty() && (tipo.equals(Actividad.TIPO_MEDICACION) || tipo.equals(Actividad.TIPO_COMER)
                || tipo.equals(Actividad.TIPO_LLAMADA_FAMILIAR) || tipo.equals(Actividad.TIPO_PASEO_EJERCICIO))) {
            gestionarFeedbackHardware("ERROR");
            hablarEnMain("Por favor, dime los detalles. Es necesario para ayudarte mejor.");
            return;
        }

        if (diasSeleccionados == null || diasSeleccionados.isEmpty()) {
            hablarEnMain("Selecciona al menos un día de la semana.");
            return;
        }

        final int duracion = new Actividad(0, tipo, horaSeleccionada, "").getDuracionMinutos();
        final int idExcluir = (existente != null) ? existente.getId() : ID_NUEVO;

        dbExecutor.execute(() -> {
            final boolean solapa = haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, idExcluir);
            runOnUiThread(() -> {
                if (solapa) {
                    new AlertDialog.Builder(this)
                            .setTitle("Solapamiento")
                            .setMessage("Ya hay otra actividad a esta hora. ¿Guardar de todos modos?")
                            .setPositiveButton("Sí", (d, w) -> {
                                guardarActividad(existente, tipo, desc);
                                dialog.dismiss();
                            })
                            .setNegativeButton("No", null).show();
                } else {
                    guardarActividad(existente, tipo, desc);
                    dialog.dismiss();
                }
            });
        });
    }


    private void seleccionarTipoYPasar(String tipoClave) {
        seleccionarTipoUI(tipoClave);
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
    }

    private void seleccionarHoraYPasar(int h, int m) {
        horaSeleccionada = h * 60 + m;
        TextView tvHora = dialogTvHoraRef != null ? dialogTvHoraRef.get() : null;
        if (tvHora != null) actualizarDisplayHora(tvHora, horaSeleccionada);
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
    }

    private String parsearTipoVoz(String texto) {
        texto = texto.toLowerCase();
        if (texto.contains("medicación") || texto.contains("medicacion") || texto.contains("medicina")
                || texto.contains("pastilla"))
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
        if (texto.contains("aseo") || texto.contains("ducha") || texto.contains("baño") || texto.contains("higiene"))
            return Actividad.TIPO_ASEO;
        if (texto.contains("llamada") || texto.contains("familiar") || texto.contains("teléfono")
                || texto.contains("telefono") || texto.contains("familia"))
            return Actividad.TIPO_LLAMADA_FAMILIAR;
        if (texto.contains("dormir") || texto.contains("cama") || texto.contains("descansar")
                || texto.contains("noche"))
            return Actividad.TIPO_IR_DORMIR;
        return null;
    }

    private void renderizarLista() {
        dbExecutor.execute(() -> {
            final List<Actividad> lista = repo.getAll();
            Collections.sort(lista, (a, b) -> Integer.compare(a.getHoraMinutos(), b.getHoraMinutos()));
            runOnUiThread(() -> {
                containerActividades.removeAllViews();
                if (lista.isEmpty())
                    tvVacio.setVisibility(View.VISIBLE);
                else {
                    tvVacio.setVisibility(View.GONE);
                    for (Actividad a : lista)
                        containerActividades.addView(crearItemActividad(a));
                }
            });
        });
    }

    private boolean haySolapamiento(List<Integer> dias, int horaMinutos, int duracion, int idAExcluir) {
        List<Actividad> lista = repo.getAll();
        for (Actividad a : lista) {
            if (a.getId() == idAExcluir)
                continue;
            if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado()) && a.coincideHoy())
                continue;
            if (tienenDiaComun(dias, a.getDiasSemana())) {
                int s1 = horaMinutos, e1 = s1 + duracion;
                int s2 = a.getHoraMinutos(), e2 = s2 + a.getDuracionMinutos();
                if (s1 < e2 && s2 < e1)
                    return true;
            }
        }
        return false;
    }

    private boolean tienenDiaComun(List<Integer> d1, List<Integer> d2) {
        if (d1 == null || d2 == null)
            return false;
        for (Integer d : d1)
            if (d2.contains(d))
                return true;
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
        if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado()))
            btnEditar.setVisibility(View.GONE);
        else
            btnEditar.setOnClickListener(v -> mostrarDialogoAnadir(a));
        item.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> confirmarEliminar(a));
        return item;
    }

    /**
     * Muestra/Oculta y renombra el campo de detalles según el tipo de actividad.
     */
    private void actualizarCampoDinamico(View dv, String tipoInterno) {
        TextView tvLabel = dv.findViewById(R.id.tvLabelDetalleDinamico);
        if (tvLabel == null) return;

        String nuevoLabel = "Información adicional";
        if (Actividad.TIPO_MEDICACION.equals(tipoInterno)) {
            nuevoLabel = "¿Qué medicamento es?";
        } else if (Actividad.TIPO_COMER.equals(tipoInterno)) {
            nuevoLabel = "¿Qué vas a comer?";
        } else if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(tipoInterno)) {
            nuevoLabel = "¿A quién vas a llamar?";
        } else if (Actividad.TIPO_PASEO_EJERCICIO.equals(tipoInterno)) {
            nuevoLabel = "¿A dónde vas?";
        }
        tvLabel.setText(nuevoLabel);
    }

    private void guardarActividad(Actividad existente, String tipo, String desc) {
        gestionarFeedbackHardware("CELEBRACION");
        dbExecutor.execute(() -> {
            if (existente == null) {
                hablarEnMain("¡Enhorabuena! Se ha creado la actividad correctamente.");
                Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
                nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.add(nueva);
            } else {
                hablarEnMain("¡Perfecto! Se ha guardado la edición de la actividad con éxito.");
                existente.setTipo(tipo);
                existente.setHoraMinutos(horaSeleccionada);
                existente.setDescripcion(desc);
                existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.update(existente);
            }
            runOnUiThread(this::renderizarLista);
        });
    }

    private void mostrarDialogoDetalle(final Actividad a) {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_detalle_actividad, null);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(a.getColorHex()));
        dv.findViewById(R.id.frameEmojiDetAct).setBackground(bg);
        ((android.widget.ImageView) dv.findViewById(R.id.tvEmojiDetAct)).setImageResource(a.getIconoRes());
        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());
        
        // Adaptar etiqueta de descripción según el tipo (MEJORA SOLICITADA)
        TextView tvLabelDesc = dv.findViewById(R.id.tvLabelDescDetAct);
        if (tvLabelDesc != null) {
            String tipo = a.getTipo();
            if (Actividad.TIPO_MEDICACION.equals(tipo)) tvLabelDesc.setText("Medicamento:");
            else if (Actividad.TIPO_COMER.equals(tipo)) tvLabelDesc.setText("Comida:");
            else if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(tipo)) tvLabelDesc.setText("Llamar a:");
            else if (Actividad.TIPO_PASEO_EJERCICIO.equals(tipo)) tvLabelDesc.setText("Lugar del paseo:");
            else tvLabelDesc.setText("Detalles:");
        }

        ((TextView) dv.findViewById(R.id.tvDescDetAct))
                .setText((a.getDescripcion() != null && !a.getDescripcion().isEmpty()) ? a.getDescripcion() : "—");
        int[] ids = { R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue, R.id.detDiaVie, R.id.detDiaSab,
                R.id.detDiaDom };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < ids.length; i++) {
            TextView tv = dv.findViewById(ids[i]);
            boolean activo = dias != null && dias.contains(VALORES_DIA[i]);
            tv.setBackgroundResource(activo ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            tv.setTextColor(activo ? Color.WHITE : Color.BLACK);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmarEliminar(final Actividad a) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Seguro?").setPositiveButton("Sí", (d, w) -> {
            hablarEnMain("He eliminado la actividad.");
            dbExecutor.execute(() -> {
                repo.delete(a.getId());
                runOnUiThread(this::renderizarLista);
            });
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
        String[] palabras = { "cero", "una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
                "veinte",
                "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis", "veintisiete",
                "veintiocho", "veintinueve", "treinta",
                "cuarenta", "cincuenta" };
        int[] valores = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                26, 27, 28, 29, 30, 40, 50 };

        // Reemplazar palabras por números en el texto para facilitar regex
        for (int i = palabras.length - 1; i >= 0; i--) {
            texto = texto.replace(palabras[i], String.valueOf(valores[i]));
        }

        // Regex para "H menos M"
        java.util.regex.Matcher matMenos = java.util.regex.Pattern.compile("(\\d{1,2})\\s*menos\\s*(\\d{1,2})")
                .matcher(texto);
        if (matMenos.find()) {
            h = Integer.parseInt(matMenos.group(1));
            m = Integer.parseInt(matMenos.group(2));
            h--;
            if (h < 0)
                h = 23;
            m = 60 - m;
            return ajustarTarde(h, esTarde, esMañana) * 60 + m;
        }

        // Regex para "H y M" o "H M" o "H:M"
        java.util.regex.Matcher matY = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(?:y|:|\\s)\\s*(\\d{1,2})")
                .matcher(texto);
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
            if (h < 13)
                h = ajustarTarde(h, esTarde, esMañana);
            return h * 60 + m;
        }

        return -1;
    }

    private int ajustarTarde(int h, boolean esTarde, boolean esMañana) {
        int res = (h == 12) ? 0 : h;
        if (esTarde)
            return (h == 12) ? 12 : res + 12;
        if (esMañana)
            return res;
        if (res >= 1 && res <= 6)
            return res + 12; // Heurística
        return h;
    }

    private List<Integer> parsearDiasVoz(String t) {
        t = t.toLowerCase();
        List<Integer> d = new ArrayList<>();
        String[] n = { "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo" };
        String[] a = { "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo" };
        for (int i = 0; i < n.length; i++)
            if (t.contains(n[i]) || t.contains(a[i]))
                d.add(VALORES_DIA[i]);
        return d;
    }

    private void actualizarDisplayAnticipacion(TextView tv, int min) {
        if (min <= 0)
            tv.setText("Sin aviso previo");
        else
            tv.setText(min + " minutos antes");
    }

    /** Parsea un número hablado (0-60) */
    private Integer parsearNumeroVoz(String texto) {
        texto = texto.toLowerCase().trim();
        if (texto.contains("cero") || texto.contains("ningun"))
            return 0;

        String[] palabras = { "una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta" };
        int[] valores = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 30, 40, 50, 60 };

        for (int i = 0; i < palabras.length; i++) {
            if (texto.contains(palabras[i]))
                return valores[i];
        }

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(texto);
        if (m.find())
            return Integer.parseInt(m.group(1));

        return null;
    }

    private void hablarEnMain(String t) {
        hablarEnMain(t, null);
    }

    private void hablarEnMain(String t, com.qihancloud.opensdk.function.beans.EmotionsType emotion) {
        runOnUiThread(() -> hablarOSimular(t, emotion));
    }

    private void abrirTimePicker(TextView tv) {
        new TimePickerDialog(this, (v, h, m) -> {
            horaSeleccionada = h * 60 + m;
            actualizarDisplayHora(tv, horaSeleccionada);
        },
                horaSeleccionada / 60, horaSeleccionada % 60, true).show();
    }

    private void actualizarDisplayHora(TextView tv, int m) {
        tv.setText(String.format("%02d:%02d", m / 60, m % 60));
    }

    private void actualizarBotonesDia(TextView[] b, int[] v, List<Integer> s) {
        for (int i = 0; i < b.length; i++) {
            boolean sel = s.contains(v[i]);
            b[i].setBackgroundResource(sel ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            b[i].setTextColor(sel ? Color.WHITE : Color.BLACK);
        }
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}