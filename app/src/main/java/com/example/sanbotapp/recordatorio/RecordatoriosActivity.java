package com.example.sanbotapp.recordatorio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordatoriosActivity extends BaseActivity {

    // ── Constante configurable ────────────────────────────────────────────────
    /**
     * Milisegundos de espera entre que el robot termina de hablar y se activa el
     * micro.
     * Auméntalo si el micro se corta antes de que el robot acabe.
     */
    private static final int DELAY_MICRO_MS = 3000;

    // ── Vistas principales ────────────────────────────────────────────────────
    private LinearLayout containerRecordatorios;
    private TextView tvVacio;
    private RecordatorioRepository repo;
    private com.example.sanbotapp.actividad.ActividadRepository actividadRepo;

    // ── Estado del diálogo ────────────────────────────────────────────────────
    private int horaSeleccionada = 9 * 60;
    private long fechaSeleccionadaMs;
    private int anticipacionSeleccionada = 10;

    // ── Estado de voz ─────────────────────────────────────────────────────────
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;
    private String valorPendienteConfirmar = "";
    private CampoVozEspera campoAConfirmar = CampoVozEspera.NINGUNO;
    private boolean isAssistantActive = false;
    private boolean isEditingFromSummary = false;
    private boolean isManual = false;

    // Referencias débiles al diálogo activo
    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<EditText> etTituloRef;
    private java.lang.ref.WeakReference<TextView> tvHoraRef;
    private java.lang.ref.WeakReference<TextView> tvFechaRef;
    private java.lang.ref.WeakReference<TextView> tvAnticipacionRef;

    // Handler en hilo principal (sustituye new Thread + sleep)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordatorios);
        setupTopBackBanner("Recordatorios");

        containerRecordatorios = findViewById(R.id.containerRecordatorios);
        tvVacio = findViewById(R.id.tvVacioRecordatorios);
        repo = new RecordatorioRepository(this);
        actividadRepo = new com.example.sanbotapp.actividad.ActividadRepository(this);
        fechaSeleccionadaMs = fechaHoyInicio();

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirRecordatorio);
        btnAnadir.setOnClickListener(v -> mostrarDialogoAnadir(null));

        renderizarLista();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }

    // =========================================================================
    // Sensor táctil de cabeza → entrada por voz
    // =========================================================================

    /**
     * Llamado desde BaseActivity cuando el usuario toca la cabeza del robot.
     * Ahora solo activa la escucha, ya que la instrucción se dio antes.
     */
    @Override
    protected void onCabezaTocada() {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing())
            return;
        if (campoEspera == CampoVozEspera.NINGUNO)
            return;

        // Parar cualquier TTS en curso y escuchar inmediatamente
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.post(this::escuchar); // pequeño margen tras parar voz
    }

    /**
     * Anuncia al usuario qué tiene que decir en el siguiente campo
     * y le pide que toque la cabeza cuando esté listo.
     */
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
            case TITULO:
                instruccion = "¡Hola! Vamos a añadir un recordatorio. ¿Qué quieres que te recuerde? Por ejemplo: 'Cita con el médico'. Toca mi cabeza para decírmelo.";
                break;
            case HORA:
                instruccion = "¿A qué hora te pongo el aviso? Dime por ejemplo: 'A las cinco de la tarde'. Toca mi cabeza.";
                break;
            case FECHA:
                instruccion = "¿Para qué día es? Dime la fecha, como 'Doce de Octubre'. Toca mi cabeza y dímelo.";
                break;
            case CAMPO_EDITAR:
                instruccion = obtenerInstruccionDinamicaRec();
                break;
            case ANTICIPACION:
                instruccion = "¿Cuántos minutos antes quieres que te avise? Dime un número, como 'Diez'. Toca mi cabeza.";
                break;
            case CONFIRMACION_CAMPO:
                instruccion = "He entendido '" + valorPendienteConfirmar + "'. ¿Es correcto? Di: sí o no.";
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
        if (campo == CampoVozEspera.CAMPO_EDITAR || campo == CampoVozEspera.ELECCION_EDICION) {
            hablarEnMain(instruccion, com.qihancloud.opensdk.function.beans.EmotionsType.QUESTION);
        } else {
            hablarEnMain(instruccion);
        }
        actualizarProgresoYResaltadoRec(campo);
    }

    /**
     * Actualiza visualmente el campo activo y los indicadores de progreso.
     * Mejora Área 2: Feedback visual.
     */
    private void actualizarProgresoYResaltadoRec(CampoVozEspera campo) {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null) return;

        runOnUiThread(() -> {
            // 1. Limpiar resaltados previos
            View etTitulo = dlg.findViewById(R.id.etTituloRecordatorio);
            View tvHora = dlg.findViewById(R.id.tvHoraDialogRec);
            View tvFecha = dlg.findViewById(R.id.tvFechaDialogRec);
            View containerAntic = dlg.findViewById(R.id.containerAnticipacion);

            if (etTitulo != null) etTitulo.setBackgroundResource(R.drawable.bg_campo_descripcion);
            if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_campo_descripcion);
            if (tvFecha != null) tvFecha.setBackgroundResource(R.drawable.bg_campo_descripcion);
            if (containerAntic != null) containerAntic.setPadding(0, 0, 0, 0);

            // 2. Resaltar campo actual y calcular paso de progreso
            int step = 0;
            View s1 = dlg.findViewById(R.id.stepRec1Container);
            View s2 = dlg.findViewById(R.id.stepRec2Container);
            View s3 = dlg.findViewById(R.id.stepRec3Container);
            View s4 = dlg.findViewById(R.id.stepRec4Container);
            View sum = dlg.findViewById(R.id.summaryContainerRec);
            View edt = dlg.findViewById(R.id.editChoiceContainerRec);

            if (s1 != null) s1.setVisibility(View.GONE);
            if (s2 != null) s2.setVisibility(View.GONE);
            if (s3 != null) s3.setVisibility(View.GONE);
            if (s4 != null) s4.setVisibility(View.GONE);
            if (sum != null) sum.setVisibility(View.GONE);
            if (edt != null) edt.setVisibility(View.GONE);

            View dotContainer = dlg.findViewById(R.id.dotContainerRec);
            if (dotContainer != null) dotContainer.setVisibility(View.VISIBLE);

            if (campo == CampoVozEspera.NINGUNO) {
                if (s1 != null) s1.setVisibility(View.GONE);
                if (s2 != null) s2.setVisibility(View.GONE);
                if (s3 != null) s3.setVisibility(View.GONE);
                if (s4 != null) s4.setVisibility(View.GONE);
                if (sum != null) sum.setVisibility(View.GONE);
                if (edt != null) edt.setVisibility(View.GONE);
                if (dotContainer != null) dotContainer.setVisibility(View.GONE);
                
                // OCULTAR MIC Y BOTONES EN PASO 0
                View mic = dlg.findViewById(R.id.containerMicEstadoRec);
                android.widget.Button btnAnt = dlg.findViewById(R.id.btnWizardAnteriorRec);
                android.widget.Button btnSig = dlg.findViewById(R.id.btnWizardSiguienteRec);
                if (mic != null) mic.setVisibility(View.GONE);
                if (btnAnt != null) btnAnt.setVisibility(View.GONE);
                if (btnSig != null) btnSig.setVisibility(View.GONE);

                View step0 = dlg.findViewById(R.id.stepRec0Container);
                if (step0 != null) step0.setVisibility(View.VISIBLE);
                return;
            }

            // MOSTRAR MIC POR DEFECTO EN EL RESTO DE PASOS
            View mic = dlg.findViewById(R.id.containerMicEstadoRec);
            if (mic != null) mic.setVisibility(View.VISIBLE);

            // Ocultar paso 0 si no estamos en NINGUNO
            View step0 = dlg.findViewById(R.id.stepRec0Container);
            if (step0 != null) step0.setVisibility(View.GONE);

            switch (campo) {
                case TITULO:
                    step = 1;
                    if (s1 != null) s1.setVisibility(View.VISIBLE);
                    if (etTitulo != null) etTitulo.setBackgroundResource(R.drawable.bg_field_active);
                    break;
                case HORA:
                    step = 2;
                    if (s2 != null) s2.setVisibility(View.VISIBLE);
                    if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                    break;
                case FECHA:
                    step = 3;
                    if (s3 != null) s3.setVisibility(View.VISIBLE);
                    if (tvFecha != null) tvFecha.setBackgroundResource(R.drawable.bg_field_active);
                    break;
                case ANTICIPACION:
                    step = 4;
                    if (s4 != null) s4.setVisibility(View.VISIBLE);
                    if (containerAntic != null) {
                        containerAntic.setPadding(4, 4, 4, 4);
                    }
                    break;
                case CONFIRMACION_CAMPO:
                    if (campoAConfirmar == CampoVozEspera.TITULO) { 
                        step = 1; 
                        if (s1 != null) s1.setVisibility(View.VISIBLE); 
                        if (etTitulo != null) etTitulo.setBackgroundResource(R.drawable.bg_field_active);
                    }
                    else if (campoAConfirmar == CampoVozEspera.HORA) { 
                        step = 2; 
                        if (s2 != null) s2.setVisibility(View.VISIBLE); 
                        if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                    }
                    else if (campoAConfirmar == CampoVozEspera.FECHA) { 
                        step = 3; 
                        if (s3 != null) s3.setVisibility(View.VISIBLE); 
                        if (tvFecha != null) tvFecha.setBackgroundResource(R.drawable.bg_field_active);
                    }
                    else if (campoAConfirmar == CampoVozEspera.ANTICIPACION) { 
                        step = 4; 
                        if (s4 != null) s4.setVisibility(View.VISIBLE); 
                        if (containerAntic != null) containerAntic.setPadding(4, 4, 4, 4);
                    }
                    break;
                case RESUMEN_FINAL:
                    step = 5;
                    if (sum != null) {
                        sum.setVisibility(View.VISIBLE);
                        actualizarTextoResumenRec(dlg);
                    }
                    break;
                case ELECCION_EDICION:
                    step = 5;
                    if (edt != null) edt.setVisibility(View.VISIBLE);
                    break;
                case CAMPO_EDITAR:
                    step = 0;
                    if (edt != null) edt.setVisibility(View.VISIBLE);
                    break;
            }

            // 3. Actualizar botones de navegación
            android.widget.Button btnAnt = dlg.findViewById(R.id.btnWizardAnteriorRec);
            android.widget.Button btnSig = dlg.findViewById(R.id.btnWizardSiguienteRec);
            boolean isStep0 = (dlg.findViewById(R.id.stepRec0Container) != null && dlg.findViewById(R.id.stepRec0Container).getVisibility() == View.VISIBLE);
            boolean isManual = !isAssistantActive;

            if (btnAnt != null && btnSig != null) {
                if (isStep0) {
                    btnAnt.setVisibility(View.GONE);
                    btnSig.setVisibility(View.GONE);
                } else if (campo == CampoVozEspera.CONFIRMACION_CAMPO) {
                    btnAnt.setVisibility(View.VISIBLE); btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("REPETIR"); btnSig.setText("CONFIRMAR");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav); btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else if (campo == CampoVozEspera.RESUMEN_FINAL) {
                    btnAnt.setVisibility(View.VISIBLE); btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("NO, CAMBIAR"); btnSig.setText("SÍ, GUARDAR");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar); btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else if (campo == CampoVozEspera.ELECCION_EDICION) {
                    btnAnt.setVisibility(View.VISIBLE); btnSig.setVisibility(View.GONE);
                    btnAnt.setText("VOLVER"); btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                } else if (isEditingFromSummary) {
                    btnAnt.setVisibility(View.VISIBLE); btnSig.setVisibility(View.VISIBLE);
                    btnAnt.setText("VOLVER"); btnSig.setText("CONFIRMAR");
                    btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav); btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                } else {
                    btnAnt.setVisibility(View.VISIBLE); btnSig.setVisibility(View.VISIBLE);
                    if (campo == CampoVozEspera.CAMPO_EDITAR) {
                        btnAnt.setText("CANCELAR"); btnSig.setText("GUARDAR");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar); btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
                    } else if (step == 1) {
                        btnAnt.setText("CANCELAR"); btnSig.setText("SIGUIENTE");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar); btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    } else {
                        btnAnt.setText("ANTERIOR"); btnSig.setText("SIGUIENTE");
                        btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav); btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
                    }
                }
            }

            // 4. Actualizar bolitas de progreso y micro
            View progressLayout = dlg.findViewById(R.id.dotContainerRec);
            View micLayout = dlg.findViewById(R.id.containerMicEstadoRec);
            if (progressLayout != null) progressLayout.setVisibility(isStep0 ? View.GONE : View.VISIBLE);
            if (micLayout != null) {
                boolean ocultarMic = isStep0 || isManual || campo == CampoVozEspera.RESUMEN_FINAL || campo == CampoVozEspera.ELECCION_EDICION || campo == CampoVozEspera.CONFIRMACION_CAMPO;
                micLayout.setVisibility(ocultarMic ? View.GONE : View.VISIBLE);
            }

            // 3. Actualizar bolitas de progreso
            int[] dotIds = {R.id.dotStepRec1, R.id.dotStepRec2, R.id.dotStepRec3, R.id.dotStepRec4};
            for (int i = 0; i < dotIds.length; i++) {
                View dot = dlg.findViewById(dotIds[i]);
                if (dot != null) {
                    dot.setBackgroundResource(i < step ? R.drawable.step_dot_active : R.drawable.step_dot_inactive);
                }
            }
        });
    }

    private String obtenerInstruccionDinamicaRec() {
        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        String titulo = (etTitulo != null) ? etTitulo.getText().toString().trim() : "";

        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        String btnAction = "Añadir";
        if (dlg != null && dlg.findViewById(R.id.btnWizardSiguienteRec) instanceof android.widget.Button) {
            btnAction = ((android.widget.Button) dlg.findViewById(R.id.btnWizardSiguienteRec)).getText().toString();
        }

        if (titulo.isEmpty() || titulo.equalsIgnoreCase("Sin título")) {
            return "Vamos a crear un recordatorio. ¿Qué título le ponemos? Toca mi cabeza.";
        }

        return "El recordatorio es '" + titulo + "'. ¿Quieres cambiar algo más o prefieres '" + btnAction
                + "' ya? Toca mi cabeza.";
    }

    // =========================================================================
    // Resultado del reconocimiento de voz
    // =========================================================================

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty())
            return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing())
            return;

        String tGlobal = texto.toLowerCase().trim();

        // CASOS ESPECIALES: RESUMEN Y ELECCIÓN DE EDICIÓN
        if (campoEspera == CampoVozEspera.RESUMEN_FINAL) {
            if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto") || tGlobal.contains("guardar")) {
                hablarEnMain("Entendido, guardando...");
                gestionarFeedbackHardware("CELEBRACION");
                // Simular click en Siguiente (que es Guardar en este estado)
                android.widget.Button btnSig = dlg.findViewById(R.id.btnWizardSiguienteRec);
                if (btnSig != null) btnSig.performClick();
            } else if (tGlobal.contains("no") || tGlobal.contains("cambiar") || tGlobal.contains("error") || tGlobal.contains("mal")) {
                hablarEnMain("¿Qué quieres cambiar?");
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            }
            return;
        }

        if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (tGlobal.contains("título") || tGlobal.contains("titulo")) {
                hablarEnMain("Cambiamos el título.");
                anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
            } else if (tGlobal.contains("hora")) {
                hablarEnMain("Vale, la hora.");
                anunciarCampoYEsperarToque(CampoVozEspera.HORA);
            } else if (tGlobal.contains("fecha") || tGlobal.contains("día") || tGlobal.contains("dia")) {
                hablarEnMain("Entendido, la fecha.");
                anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
            } else if (tGlobal.contains("aviso") || tGlobal.contains("antelación") || tGlobal.contains("antelacion")) {
                hablarEnMain("De acuerdo, el aviso.");
                anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
            } else if (tGlobal.contains("volver") || tGlobal.contains("resumen")) {
                hablarEnMain("Volvemos al resumen.");
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
            }
            return;
        }

        // ── Comandos globales ────────────────────────────────────────────────
        String btnText = "añadir";
        if (dlg.findViewById(R.id.btnWizardSiguienteRec) instanceof android.widget.Button) {
            btnText = ((android.widget.Button) dlg.findViewById(R.id.btnWizardSiguienteRec)).getText().toString()
                    .toLowerCase();
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
                if (dlg.findViewById(R.id.btnWizardSiguienteRec) != null) {
                    dlg.findViewById(R.id.btnWizardSiguienteRec).performClick();
                }
            });
            return;
        }

        if (tGlobal.contains("cancelar") || tGlobal.contains("atrás") || tGlobal.contains("atras")
                || tGlobal.contains("cerrar")) {
            hablarEnMain("Vale.");
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnWizardAnteriorRec) != null) {
                    dlg.findViewById(R.id.btnWizardAnteriorRec).performClick();
                }
            });
            return;
        }

        // ── Manejo por estados ───────────────────────────────────────────────
        switch (campoEspera) {
            case CONFIRMACION_CAMPO: {
                if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto")
                        || tGlobal.contains("vale") || tGlobal.contains("bueno") || tGlobal.contains("está bien")) {
                    aplicarValorConfirmadoRec();
                    hablarEnMain("Perfecto.");
                } else if (tGlobal.contains("no") || tGlobal.contains("repetir") || tGlobal.contains("mal")) {
                    hablarEnMain("Vaya, lo siento. Intentémoslo de nuevo.");
                    CampoVozEspera volverA = campoAConfirmar;
                    anunciarCampoYEsperarToque(volverA);
                }
                break;
            }

            case TITULO: {
                valorPendienteConfirmar = texto;
                campoAConfirmar = CampoVozEspera.TITULO;
                anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
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

            case FECHA: {
                long[] fechaVoz = parsearFechaVoz(texto);
                if (fechaVoz != null) {
                    valorPendienteConfirmar = new java.text.SimpleDateFormat("dd 'de' MMMM",
                            new java.util.Locale("es", "ES")).format(new java.util.Date(fechaVoz[0]));
                    campoAConfirmar = CampoVozEspera.FECHA;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No entendí la fecha. Dime por ejemplo: doce de octubre.");
                    anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
                }
                break;
            }

            case ANTICIPACION: {
                Integer min = parsearNumeroVoz(texto);
                if (min != null) {
                    valorPendienteConfirmar = min + " minutos";
                    campoAConfirmar = CampoVozEspera.ANTICIPACION;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No entendí el número. Intenta de nuevo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
                }
                break;
            }

            case CAMPO_EDITAR: {
                if (tGlobal.contains("título") || tGlobal.contains("titulo") || tGlobal.contains("nombre")) {
                    hablarEnMain("De acuerdo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
                } else if (tGlobal.contains("descripción") || tGlobal.contains("descripcion")
                        || tGlobal.contains("detalle")) {
                    hablarEnMain("Entendido.");
                    anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
                } else if (tGlobal.contains("hora") || tGlobal.contains("momento") || tGlobal.contains("cuándo")) {
                    hablarEnMain("Vale.");
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                } else if (tGlobal.contains("fecha") || tGlobal.contains("día") || tGlobal.contains("dia")) {
                    hablarEnMain("Cambiamos la fecha.");
                    anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
                } else if (tGlobal.contains("antelación") || tGlobal.contains("antelacion")
                        || tGlobal.contains("aviso")) {
                    hablarEnMain("De acuerdo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
                } else {
                    hablarEnMain("No te entendí. Di por ejemplo: título o hora.");
                    anunciarCampoYEsperarToque(CampoVozEspera.CAMPO_EDITAR);
                }
                break;
            }
        }
    }

    private void aplicarValorConfirmadoRec() {
        runOnUiThread(() -> {
            switch (campoAConfirmar) {
                case TITULO:
                    if (etTituloRef != null && etTituloRef.get() != null)
                        etTituloRef.get().setText(valorPendienteConfirmar);
                    break;
                case HORA:
                    int m = parsearHoraVoz(valorPendienteConfirmar);
                    if (m != -1) {
                        horaSeleccionada = m;
                        if (tvHoraRef != null && tvHoraRef.get() != null)
                            actualizarDisplayHora(tvHoraRef.get(), m);
                    }
                    break;
                case FECHA:
                    long[] fechaVoz = parsearFechaVoz(valorPendienteConfirmar);
                    if (fechaVoz != null) {
                        fechaSeleccionadaMs = fechaVoz[0];
                        if (tvFechaRef != null && tvFechaRef.get() != null)
                            actualizarDisplayFecha(tvFechaRef.get(), fechaSeleccionadaMs);
                    }
                    break;
                case ANTICIPACION:
                    String clean = valorPendienteConfirmar.replace(" minutos", "").trim();
                    try {
                        anticipacionSeleccionada = Integer.parseInt(clean);
                        if (tvAnticipacionRef != null && tvAnticipacionRef.get() != null)
                            actualizarDisplayAnticipacion(tvAnticipacionRef.get(), anticipacionSeleccionada);
                    } catch (Exception ignored) {
                    }
                    break;
            }

            // Progresar al siguiente paso automáticamente
            CampoVozEspera siguiente = CampoVozEspera.RESUMEN_FINAL;
            if (isEditingFromSummary) {
                siguiente = CampoVozEspera.RESUMEN_FINAL;
            } else {
                if (campoAConfirmar == CampoVozEspera.TITULO) siguiente = CampoVozEspera.HORA;
                else if (campoAConfirmar == CampoVozEspera.HORA) siguiente = CampoVozEspera.FECHA;
                else if (campoAConfirmar == CampoVozEspera.FECHA) siguiente = CampoVozEspera.ANTICIPACION;
            }

            anunciarCampoYEsperarToque(siguiente);
        });
    }

    // ── Flag para saber si el diálogo es de edición o de creación ────────────
    private boolean esDialogoEdicion = false;

    // =========================================================================
    // Renderizado
    // =========================================================================

    private void renderizarLista() {
        new Thread(() -> {
            final List<Recordatorio> lista = repo.getFuturos();
            runOnUiThread(() -> {
                containerRecordatorios.removeAllViews();
                if (lista.isEmpty()) {
                    tvVacio.setVisibility(View.VISIBLE);
                } else {
                    tvVacio.setVisibility(View.GONE);
                    for (final Recordatorio r : lista) {
                        containerRecordatorios.addView(crearItemRecordatorio(r));
                    }
                }
            });
        }).start();
    }

    private boolean haySolapamiento(long fechaMs, int horaMinutos, int idAExcluir) {
        // 1. Comprobar contra otros recordatorios
        for (Recordatorio r : repo.getFuturos()) {
            if (r.getId() == idAExcluir)
                continue;
            if (r.getFechaMs() == fechaMs) {
                int s1 = horaMinutos, e1 = s1 + 30;
                int s2 = r.getHoraMinutos(), e2 = s2 + 30;
                if (s1 < e2 && s2 < e1)
                    return true;
            }
        }

        // 2. Comprobar contra actividades (si coinciden en día)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(fechaMs);
        int diaSemanaRec = cal.get(java.util.Calendar.DAY_OF_WEEK);

        for (com.example.sanbotapp.actividad.Actividad a : actividadRepo.getAll()) {
            // Si la actividad ya está COMPLETADA hoy, no bloquea el solapamiento.
            if (com.example.sanbotapp.actividad.Actividad.ESTADO_COMPLETADA.equals(a.getEstado()) && a.coincideHoy()) {
                // Solo ignoramos si el recordatorio es para HOY.
                // Pero como fechaMs es el inicio del día, comparamos:
                if (fechaMs == fechaHoyInicio())
                    continue;
            }

            if (a.getDiasSemana().contains(diaSemanaRec)) {
                int s1 = horaMinutos, e1 = s1 + 30;
                int s2 = a.getHoraMinutos(), e2 = s2 + a.getDuracionMinutos();
                if (s1 < e2 && s2 < e1)
                    return true;
            }
        }

        return false;
    }

    private View crearItemRecordatorio(final Recordatorio r) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_recordatorio, containerRecordatorios, false);

        ((TextView) item.findViewById(R.id.tvHoraItemRec)).setText(r.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvFechaItemRec)).setText(r.getFechaFormateada());
        ((TextView) item.findViewById(R.id.tvTituloItemRec)).setText(
                r.getTitulo() != null && !r.getTitulo().isEmpty()
                        ? r.getTitulo().toUpperCase()
                        : "SIN TÍTULO");

        item.setOnClickListener(v -> mostrarDialogoDetalle(r));
        item.findViewById(R.id.btnEditarItemRec).setOnClickListener(v -> mostrarDialogoAnadir(r));
        item.findViewById(R.id.btnEliminarItemRec).setOnClickListener(v -> confirmarEliminar(r));

        return item;
    }

    // =========================================================================
    // Diálogo AÑADIR / EDITAR
    // =========================================================================

    private void mostrarDialogoAnadir(final Recordatorio existente) {
        esDialogoEdicion = (existente != null);
        isAssistantActive = !esDialogoEdicion; // Por defecto activo si es nuevo
        horaSeleccionada = esDialogoEdicion ? existente.getHoraMinutos() : 9 * 60;
        fechaSeleccionadaMs = esDialogoEdicion ? existente.getFechaMs() : fechaHoyInicio();
        anticipacionSeleccionada = esDialogoEdicion ? existente.getAnticipacionMinutos() : 10;
        campoEspera = CampoVozEspera.NINGUNO;
        isEditingFromSummary = false;

        final View dv = LayoutInflater.from(this).inflate(R.layout.dialog_anadir_recordatorio, null);

        final EditText etTitulo = dv.findViewById(R.id.etTituloRecordatorio);
        final TextView tvHora = dv.findViewById(R.id.tvHoraDialogRec);
        final View tvFechaCont = dv.findViewById(R.id.tvFechaDialogRec);
        final TextView tvFechaTexto = (TextView) dv.findViewById(R.id.tvFechaDialogRec); // Usamos el mismo para el texto
        final TextView tvAnticValor = dv.findViewById(R.id.tvAnticipacionRec); // ID actualizado
        final View btnAnticMenos = dv.findViewById(R.id.btnAnticMenos); // Si existen
        final View btnAnticMas = dv.findViewById(R.id.btnAnticMas); // Si existen

        actualizarDisplayHora(tvHora, horaSeleccionada);
        actualizarDisplayFecha(tvFechaTexto, fechaSeleccionadaMs);
        actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);

        if (esDialogoEdicion) {
            etTitulo.setText(existente.getTitulo());
        }

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));
        tvFechaCont.setOnClickListener(v -> abrirDatePicker(tvFechaTexto));

        btnAnticMenos.setOnClickListener(v -> {
            if (anticipacionSeleccionada >= 5) {
                anticipacionSeleccionada -= 5;
                actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);
            }
        });
        btnAnticMas.setOnClickListener(v -> {
            if (anticipacionSeleccionada < 60) {
                anticipacionSeleccionada += 5;
                actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);
            }
        });

        // Referencias débiles para acceso desde callbacks de voz
        etTituloRef = new java.lang.ref.WeakReference<>(etTitulo);
        tvHoraRef = new java.lang.ref.WeakReference<>(tvHora);
        tvFechaRef = new java.lang.ref.WeakReference<>(tvFechaTexto);
        tvAnticipacionRef = new java.lang.ref.WeakReference<>(tvAnticValor);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
        dialogRef = new java.lang.ref.WeakReference<>(dialog);

        // Micro
        View btnMic = dv.findViewById(R.id.btnMicDialogRec);
        TextView tvEstadoMic = dv.findViewById(R.id.tvEstadoMicRec);
        if (btnMic != null && tvEstadoMic != null) {
            setMicUI(btnMic, tvEstadoMic);
            btnMic.setOnClickListener(v -> onCabezaTocada());
        }

        dialog.setOnDismissListener(d -> {
            setMicUI(null, null);
            campoEspera = CampoVozEspera.NINGUNO;
            dialogRef = null;
            etTituloRef = null;
            tvHoraRef = null;
            tvFechaRef = null;
            tvAnticipacionRef = null;
            mainHandler.removeCallbacksAndMessages(null);
            pararVoz();
        });

        // Listeners de navegación Wizard
        android.widget.Button btnAnt = dv.findViewById(R.id.btnWizardAnteriorRec);
        android.widget.Button btnSig = dv.findViewById(R.id.btnWizardSiguienteRec);

        btnAnt.setOnClickListener(v -> {
            gestionarFeedbackHardware("CANCELADO");
            
            // Si estamos editando un campo concreto viniendo del resumen, volver siempre al resumen
            if (isEditingFromSummary && campoEspera != CampoVozEspera.ELECCION_EDICION && campoEspera != CampoVozEspera.RESUMEN_FINAL) {
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                return;
            }

            if (campoEspera == CampoVozEspera.CONFIRMACION_CAMPO) {
                hablarEnMain("De acuerdo, dímelo otra vez.");
                anunciarCampoYEsperarToque(campoAConfirmar);
            } else if (campoEspera == CampoVozEspera.RESUMEN_FINAL) {
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            } else if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
            } else if (campoEspera == CampoVozEspera.CAMPO_EDITAR) {
                dialog.dismiss();
            } else {
                switch (campoEspera) {
                    case TITULO: dialog.dismiss(); break;
                    case HORA: anunciarCampoYEsperarToque(CampoVozEspera.TITULO); break;
                    case FECHA: anunciarCampoYEsperarToque(CampoVozEspera.HORA); break;
                    case ANTICIPACION: anunciarCampoYEsperarToque(CampoVozEspera.FECHA); break;
                    default: 
                        dialog.dismiss();
                        break;
                }
            }
        });

        btnSig.setOnClickListener(v -> {
            if (campoEspera == CampoVozEspera.CONFIRMACION_CAMPO) {
                aplicarValorConfirmadoRec();
            } else if (campoEspera == CampoVozEspera.RESUMEN_FINAL || campoEspera == CampoVozEspera.CAMPO_EDITAR) {
                validarYGuardarRecordatorio(existente, etTitulo, dialog);
            } else {
                switch (campoEspera) {
                    case TITULO: 
                        if (etTitulo != null && etTitulo.getText().toString().trim().isEmpty()) {
                            hablarEnMain("Por favor, dime un título para el recordatorio.");
                            return;
                        }
                        if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                        else anunciarCampoYEsperarToque(CampoVozEspera.HORA); 
                        break;
                    case HORA: 
                        if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                        else anunciarCampoYEsperarToque(CampoVozEspera.FECHA); 
                        break;
                    case FECHA: 
                        if (isEditingFromSummary) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                        else anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION); 
                        break;
                    case ANTICIPACION: anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL); break;
                    default:
                        if (isEditingFromSummary) {
                            anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                        } else {
                            validarYGuardarRecordatorio(existente, etTitulo, dialog);
                        }
                }
            }
        });

        // Listeners de Edición Granular
        dv.findViewById(R.id.btnEditTituloRec).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.TITULO));
        dv.findViewById(R.id.btnEditHoraRec).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.HORA));
        dv.findViewById(R.id.btnEditFechaRec).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.FECHA));
        dv.findViewById(R.id.btnEditAnticRec).setOnClickListener(v -> anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION));
        dv.findViewById(R.id.btnCerrarDialogRec).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // MEJORA ÁREA 4: Gestión de Paso 0 (Asistente)
        View step0 = dv.findViewById(R.id.stepRec0Container);
        if (esDialogoEdicion) {
            if (step0 != null) step0.setVisibility(View.GONE);
            anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
        } else {
            actualizarProgresoYResaltadoRec(CampoVozEspera.NINGUNO);
            dv.findViewById(R.id.btnEmpezarAsistenteRec).setOnClickListener(v -> {
                step0.setVisibility(View.GONE);
                anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
            });

            dv.findViewById(R.id.btnManualRec).setOnClickListener(v -> {
                step0.setVisibility(View.GONE);
                isAssistantActive = false;
                isManual = true;
                pararVoz();
                mainHandler.removeCallbacksAndMessages(null);
                anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
            });
        }
    }

    private void seleccionarHoraYPasarRec(int h, int m) {
        horaSeleccionada = h * 60 + m;
        TextView tvHora = tvHoraRef != null ? tvHoraRef.get() : null;
        if (tvHora != null) actualizarDisplayHora(tvHora, horaSeleccionada);
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
    }

    private void seleccionarFechaYPasarRec(int diasOffset) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, diasOffset);
        fechaSeleccionadaMs = cal.getTimeInMillis();
        TextView tvFecha = tvFechaRef != null ? tvFechaRef.get() : null;
        if (tvFecha != null) actualizarDisplayFecha(tvFecha, fechaSeleccionadaMs);
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
    }

    private void validarYGuardarRecordatorio(Recordatorio existente, EditText etTitulo, AlertDialog dialog) {
        String titulo = etTitulo.getText().toString().trim();
        if (titulo.isEmpty()) {
            etTitulo.setError("Introduce un título");
            hablarEnMain("Por favor, introduce un título.");
            anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
            return;
        }
        guardarRecordatorio(existente, titulo, "");
        dialog.dismiss();
    }

    private void actualizarTextoResumenRec(AlertDialog dlg) {
        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        String titulo = (etTitulo != null) ? etTitulo.getText().toString().trim() : "(Sin título)";

        Recordatorio r = new Recordatorio();
        r.setFechaMs(fechaSeleccionadaMs);

        TextView tvTitulo = dlg.findViewById(R.id.tvSummaryTituloRec);
        TextView tvFecha = dlg.findViewById(R.id.tvSummaryFechaRec);
        TextView tvHora = dlg.findViewById(R.id.tvSummaryHoraRec);
        TextView tvAntic = dlg.findViewById(R.id.tvSummaryAnticRec);

        if (tvTitulo != null) tvTitulo.setText(titulo);
        if (tvFecha != null) tvFecha.setText(r.getFechaFormateada());
        if (tvHora != null) tvHora.setText(String.format("%02d:%02d", horaSeleccionada / 60, horaSeleccionada % 60));
        if (tvAntic != null) {
            if (anticipacionSeleccionada <= 0) tvAntic.setText("Sin aviso");
            else tvAntic.setText(anticipacionSeleccionada + " min antes");
        }
    }

    private void guardarRecordatorio(Recordatorio existente, String titulo, String desc) {
        gestionarFeedbackHardware("EXITO");
        if (existente == null) {
            hablarEnMain("¡Enhorabuena! Se ha creado el recordatorio correctamente.");
            repo.add(
                    new Recordatorio(0, titulo, horaSeleccionada, fechaSeleccionadaMs, desc, anticipacionSeleccionada));
        } else {
            hablarEnMain("¡Perfecto! Se ha guardado la edición del recordatorio con éxito.");
            existente.setTitulo(titulo);
            existente.setHoraMinutos(horaSeleccionada);
            existente.setFechaMs(fechaSeleccionadaMs);
            existente.setDescripcion(desc);
            existente.setAnticipacionMinutos(anticipacionSeleccionada);
            repo.update(existente);
        }
        renderizarLista();
    }

    // =========================================================================
    // Diálogo DETALLE
    // =========================================================================

    private void mostrarDialogoDetalle(final Recordatorio r) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_recordatorio, null);

        ((TextView) dv.findViewById(R.id.tvTituloDetRec)).setText(
                r.getTitulo() != null ? r.getTitulo().toUpperCase() : "SIN TÍTULO");
        ((TextView) dv.findViewById(R.id.tvHoraDetRec)).setText(r.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvFechaDetRec)).setText(r.getFechaFormateada());
        String desc = (r.getDescripcion() != null && !r.getDescripcion().isEmpty())
                ? r.getDescripcion()
                : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetRec)).setText(desc);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCerrarDetRec).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================================
    // Confirmar eliminar
    // =========================================================================

    private void confirmarEliminar(final Recordatorio r) {
        String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty())
                ? r.getTitulo()
                : "este recordatorio";
        new AlertDialog.Builder(this)
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Seguro que quieres eliminar \"" + titulo + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    repo.delete(r.getId());
                    renderizarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // Parsers de voz
    // =========================================================================

    /**
     * Convierte texto hablado en minutos desde medianoche.
     * Ejemplos: "nueve y media" → 570, "las doce" → 720, "ocho menos cuarto" → 465
     * Devuelve null si no se reconoce el patrón.
     */
    private Integer parsearHoraVoz(String texto) {
        texto = texto.toLowerCase().trim();
        boolean esTarde = texto.contains("tarde") || texto.contains("noche") || texto.contains("pm");
        boolean esMañana = texto.contains("mañana") || texto.contains("am");

        // 1. Reemplazo de palabras comunes por números
        texto = texto.replace("media", "30").replace("cuarto", "15");

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

        int h = -1, m = -1;

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

        return null;
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

    /**
     * Convierte texto hablado en timestamp Unix (ms) de la fecha indicada a las
     * 00:00.
     *
     * Ejemplos:
     * "quince de marzo de dos mil veinticinco" → timestamp del 15/03/2025 00:00
     * "el tres de enero" → timestamp del 03/01/año actual 00:00
     * "22 de septiembre de 2026" → timestamp del 22/09/2026 00:00
     *
     * @return long[]{timestampMs} o null si no se reconoce.
     */
    private long[] parsearFechaVoz(String texto) {
        texto = texto.toLowerCase().trim();

        // ── Detectar mes ──────────────────────────────────────────────────────
        final String[] MESES = {
                "enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        };
        int mes = -1;
        for (int i = 0; i < MESES.length; i++) {
            if (texto.contains(MESES[i])) {
                mes = i + 1;
                break;
            }
        }
        if (mes == -1)
            return null;

        // ── Detectar día ──────────────────────────────────────────────────────
        final String[] NUMEROS = {
                "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho",
                "diecinueve", "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro",
                "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve", "treinta",
                "treinta y uno"
        };
        final String[] NUMEROS_ALT = {
                "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho",
                "diecinueve", "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro",
                "veinticinco", "veintiseis", "veintisiete", "veintiocho", "veintinueve", "treinta",
                "treinta y uno"
        };

        int dia = -1;
        // Primero intentar dígito
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(\\d{1,2})\\b").matcher(texto);
        while (m.find()) {
            int c = Integer.parseInt(m.group(1));
            if (c >= 1 && c <= 31) {
                dia = c;
                break;
            }
        }
        // Si no, palabras (de mayor a menor para evitar "tres" dentro de "treinta")
        if (dia == -1) {
            for (int i = NUMEROS.length - 1; i >= 0; i--) {
                if (texto.contains(NUMEROS[i]) || texto.contains(NUMEROS_ALT[i])) {
                    dia = i + 1;
                    break;
                }
            }
        }
        if (dia == -1 || dia < 1 || dia > 31)
            return null;

        // ── Detectar año ──────────────────────────────────────────────────────
        int anio = Calendar.getInstance().get(Calendar.YEAR); // por defecto año actual

        // Dígitos: "2025", "2026"
        m = java.util.regex.Pattern.compile("\\b(20\\d{2})\\b").matcher(texto);
        if (m.find()) {
            anio = Integer.parseInt(m.group(1));
        } else {
            // Palabras: "dos mil veinticinco" → 2025, "dos mil veintiséis" → 2026
            final String[][] ANIOS_PALABRAS = {
                    { "dos mil veintiuno", "2021" },
                    { "dos mil veintidós", "2022" }, { "dos mil veintidos", "2022" },
                    { "dos mil veintitrés", "2023" }, { "dos mil veintitres", "2023" },
                    { "dos mil veinticuatro", "2024" },
                    { "dos mil veinticinco", "2025" },
                    { "dos mil veintiséis", "2026" }, { "dos mil veintiseis", "2026" },
                    { "dos mil veintisiete", "2027" },
                    { "dos mil veintiocho", "2028" },
                    { "dos mil veintinueve", "2029" },
                    { "dos mil treinta", "2030" },
            };
            for (String[] par : ANIOS_PALABRAS) {
                if (texto.contains(par[0])) {
                    anio = Integer.parseInt(par[1]);
                    break;
                }
            }
        }

        // ── Construir timestamp ───────────────────────────────────────────────
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new long[] { cal.getTimeInMillis() };
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Wrapper que garantiza que hablarOSimular se llama siempre en el hilo
     * principal.
     */
    private void hablarEnMain(String texto) {
        hablarEnMain(texto, null);
    }

    private void hablarEnMain(String texto, com.qihancloud.opensdk.function.beans.EmotionsType emotion) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hablarOSimular(texto, emotion);
        } else {
            runOnUiThread(() -> hablarOSimular(texto, emotion));
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

    private void abrirDatePicker(final TextView tv) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(fechaSeleccionadaMs);
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, day, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    fechaSeleccionadaMs = c.getTimeInMillis();
                    actualizarDisplayFecha(tv, fechaSeleccionadaMs);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void actualizarDisplayHora(TextView tv, int minutos) {
        tv.setText(String.format("%02d:%02d", minutos / 60, minutos % 60));
    }

    private void actualizarDisplayFecha(TextView tv, long ms) {
        Recordatorio tmp = new Recordatorio();
        tmp.setFechaMs(ms);
        tv.setText(tmp.getFechaFormateada());
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

    private long fechaHoyInicio() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long construirTimestampRecordatorio(long fechaMs, int horaMinutos) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fechaMs);
        c.set(Calendar.HOUR_OF_DAY, horaMinutos / 60);
        c.set(Calendar.MINUTE, horaMinutos % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private boolean esRecordatorioFuturo(long fechaMs, int horaMinutos) {
        return construirTimestampRecordatorio(fechaMs, horaMinutos) > System.currentTimeMillis();
    }
}