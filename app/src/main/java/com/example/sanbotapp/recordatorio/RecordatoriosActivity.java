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
import com.example.sanbotapp.util.DateTimeUtils;
import com.example.sanbotapp.util.VozParser;
import com.example.sanbotapp.util.WizardFlowController;

import java.util.Calendar;
import java.util.List;

public class RecordatoriosActivity extends BaseActivity {


    // ── Vistas principales ────────────────────────────────────────────────────
    private LinearLayout containerRecordatorios;
    private TextView tvVacio;
    private RecordatorioRepository repo;

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

    // Referencias débiles al diálogo activo
    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<EditText> etTituloRef;
    private java.lang.ref.WeakReference<TextView> tvHoraRef;
    private java.lang.ref.WeakReference<TextView> tvFechaRef;
    private java.lang.ref.WeakReference<TextView> tvAnticipacionRef;

    // ── Gestores modularizados ───────────────────────────────────────────────
    private RecordatorioDialogManager dialogManager;
    private RecordatorioAdapter adapter;

    // Handler en hilo principal (sustituye new Thread + sleep)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService dbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

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
        fechaSeleccionadaMs = DateTimeUtils.fechaHoyInicio();

        dialogManager = new RecordatorioDialogManager(this, new RecordatorioDialogManager.RecordatorioDialogListener() {
            @Override public void onAnunciarCampo(CampoVozEspera campo) { anunciarCampoYEsperarToque(campo); }
            @Override public void onCabezaTocada() { RecordatoriosActivity.this.onCabezaTocada(); }
            @Override public void onSetMicUI(View btn, TextView tv) { setMicUI(btn, tv); }
            @Override public void onPararVoz() { pararVoz(); }
        });

        adapter = new RecordatorioAdapter(this, new RecordatorioAdapter.OnRecordatorioClickListener() {
            @Override public void onRecordatorioClick(Recordatorio r) { dialogManager.mostrarDialogoDetalle(r); }
            @Override public void onEditarClick(Recordatorio r) { dialogManager.mostrarDialogoAnadir(r); }
            @Override public void onEliminarClick(Recordatorio r) { confirmarEliminar(r); }
        });

        findViewById(R.id.btnAnadirRecordatorio).setOnClickListener(v -> dialogManager.mostrarDialogoAnadir(null));

        dbExecutor.execute(this::renderizarLista);
    }

    // ── Métodos para que el DialogManager sincronice el estado ───────────────

    public void sincronizarEstadoDialogo(int hora, long fecha, int anticipacion, boolean esEdicion) {
        this.horaSeleccionada = hora;
        this.fechaSeleccionadaMs = fecha;
        this.anticipacionSeleccionada = anticipacion;
        this.isAssistantActive = !esEdicion;
        this.campoEspera = CampoVozEspera.NINGUNO;
        this.isEditingFromSummary = false;
    }

    public void setDialogRefs(AlertDialog dialog, EditText etTitulo, TextView tvHora, TextView tvFecha, TextView tvAntic) {
        this.dialogRef = new java.lang.ref.WeakReference<>(dialog);
        this.etTituloRef = new java.lang.ref.WeakReference<>(etTitulo);
        this.tvHoraRef = new java.lang.ref.WeakReference<>(tvHora);
        this.tvFechaRef = new java.lang.ref.WeakReference<>(tvFecha);
        this.tvAnticipacionRef = new java.lang.ref.WeakReference<>(tvAntic);
    }

    public void limpiarEstadoAsistente() {
        this.campoEspera = CampoVozEspera.NINGUNO;
        this.dialogRef = null;
        this.etTituloRef = null;
        this.tvHoraRef = null;
        this.tvFechaRef = null;
        this.tvAnticipacionRef = null;
        mainHandler.removeCallbacksAndMessages(null);
    }

    public void activarModoManual() {
        this.isAssistantActive = false;
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
    }

    public void setHoraSeleccionada(int h) { this.horaSeleccionada = h; }
    public void setFechaSeleccionadaMs(long ms) { this.fechaSeleccionadaMs = ms; }
    public void setAnticipacionSeleccionada(int a) { this.anticipacionSeleccionada = a; }
    public void setIsAssistantActive(boolean active) { this.isAssistantActive = active; }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        dbExecutor.shutdownNow();
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
        if (campoEspera == CampoVozEspera.RESUMEN_FINAL || campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (campo != CampoVozEspera.RESUMEN_FINAL && campo != CampoVozEspera.CONFIRMACION_CAMPO) {
                isEditingFromSummary = true;
            }
        }
        
        campoEspera = campo;
        actualizarProgresoYResaltadoRec(campo);

        String context = null;
        if (campo == CampoVozEspera.CAMPO_EDITAR) context = obtenerInstruccionDinamicaRec();
        else if (campo == CampoVozEspera.CONFIRMACION_CAMPO) context = valorPendienteConfirmar;
        else if (campo == CampoVozEspera.RESUMEN_FINAL) {
            isEditingFromSummary = false;
            context = generarResumenVozRec();
        }

        String instruccion = WizardFlowController.getInstruccionCampo(campo, context);
        if (isAssistantActive) {
            hablarEnMain(instruccion);
        }
    }


    public void actualizarProgresoYResaltadoRec(CampoVozEspera campo) {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null) return;

        RecordatorioWizardHelper helper = new RecordatorioWizardHelper(dlg, dlg.findViewById(android.R.id.content));
        helper.actualizarProgresoYResaltado(campo, campoAConfirmar, isAssistantActive, isEditingFromSummary);
        
        if (campo == CampoVozEspera.RESUMEN_FINAL) {
            actualizarTextoResumenRec(dlg);
        }
    }

    private void actualizarTextoResumenRec(AlertDialog dlg) {
        TextView tvTitulo = dlg.findViewById(R.id.tvSummaryTituloRec);
        TextView tvHora = dlg.findViewById(R.id.tvSummaryHoraRec);
        TextView tvFecha = dlg.findViewById(R.id.tvSummaryFechaRec);
        TextView tvAntic = dlg.findViewById(R.id.tvSummaryAnticRec);

        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        if (tvTitulo != null && etTitulo != null) tvTitulo.setText(etTitulo.getText().toString().toUpperCase());
        if (tvHora != null) tvHora.setText(com.example.sanbotapp.util.DateTimeUtils.formatearHora(horaSeleccionada));
        if (tvFecha != null) {
            Recordatorio tmp = new Recordatorio();
            tmp.setFechaMs(fechaSeleccionadaMs);
            tvFecha.setText(tmp.getFechaFormateada().toUpperCase());
        }
        if (tvAntic != null) {
            if (anticipacionSeleccionada <= 0) tvAntic.setText("SIN AVISO PREVIO");
            else tvAntic.setText("AVISAR " + anticipacionSeleccionada + " MINUTOS ANTES");
        }
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
                runOnUiThread(() -> { if (etTituloRef != null && etTituloRef.get() != null) etTituloRef.get().setText(valorPendienteConfirmar); });
                anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                break;
            }

            case HORA: {
                int minutos = VozParser.parsearHoraVoz(texto);
                if (minutos != -1) {
                    int h = minutos / 60;
                    int m = minutos % 60;
                    valorPendienteConfirmar = String.format("%02d:%02d", h, m);
                    campoAConfirmar = CampoVozEspera.HORA;
                    runOnUiThread(() -> { if (tvHoraRef != null && tvHoraRef.get() != null) tvHoraRef.get().setText(valorPendienteConfirmar); });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No te entendí bien la hora. Inténtalo de nuevo.");
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                }
                break;
            }

            case FECHA: {
                Long fechaMs = VozParser.parsearFechaVoz(texto);
                if (fechaMs != null) {
                    valorPendienteConfirmar = new java.text.SimpleDateFormat("dd 'de' MMMM",
                            new java.util.Locale("es", "ES")).format(new java.util.Date(fechaMs));
                    campoAConfirmar = CampoVozEspera.FECHA;
                    runOnUiThread(() -> { 
                        if (tvFechaRef != null && tvFechaRef.get() != null) {
                            Recordatorio tmp = new Recordatorio();
                            tmp.setFechaMs(fechaMs);
                            tvFechaRef.get().setText(tmp.getFechaFormateada());
                        }
                    });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain("No entendí la fecha. Dime por ejemplo: doce de octubre.");
                    anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
                }
                break;
            }

            case ANTICIPACION: {
                Integer min = VozParser.parsearNumeroVoz(texto);
                if (min != null) {
                    valorPendienteConfirmar = min + " minutos";
                    campoAConfirmar = CampoVozEspera.ANTICIPACION;
                    runOnUiThread(() -> { if (tvAnticipacionRef != null && tvAnticipacionRef.get() != null) tvAnticipacionRef.get().setText(valorPendienteConfirmar); });
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
                    horaSeleccionada = VozParser.parsearHoraVoz(valorPendienteConfirmar);
                    if (tvHoraRef != null && tvHoraRef.get() != null)
                        tvHoraRef.get().setText(DateTimeUtils.formatearHora(horaSeleccionada));
                    break;
                case FECHA:
                    fechaSeleccionadaMs = VozParser.parsearFechaVoz(valorPendienteConfirmar);
                    if (tvFechaRef != null && tvFechaRef.get() != null) {
                        Recordatorio tmp = new Recordatorio();
                        tmp.setFechaMs(fechaSeleccionadaMs);
                        tvFechaRef.get().setText(tmp.getFechaFormateada());
                    }
                    break;
                case ANTICIPACION:
                    anticipacionSeleccionada = VozParser.parsearNumeroVoz(valorPendienteConfirmar);
                    if (tvAnticipacionRef != null && tvAnticipacionRef.get() != null)
                        tvAnticipacionRef.get().setText(anticipacionSeleccionada + " minutos antes");
                    break;
            }

            anunciarCampoYEsperarToque(WizardFlowController.getSiguienteCampo(campoAConfirmar, isEditingFromSummary, false));
        });
    }

    public void onWizardAnterior(AlertDialog dialog) {
        gestionarFeedbackHardware("CANCELADO");
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
        } else {
            CampoVozEspera anterior = WizardFlowController.getCampoAnterior(campoEspera, false);
            if (anterior == CampoVozEspera.NINGUNO) dialog.dismiss();
            else anunciarCampoYEsperarToque(anterior);
        }
    }

    public void onWizardSiguiente(AlertDialog dialog, Recordatorio existente, EditText etTitulo) {
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
                    anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.HORA);
                    break;
                case HORA:
                    anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.FECHA);
                    break;
                case FECHA:
                    anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.ANTICIPACION);
                    break;
                case ANTICIPACION:
                    anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
                    break;
                default:
                    validarYGuardarRecordatorio(existente, etTitulo, dialog);
            }
        }
    }

    private void renderizarLista() {
        dbExecutor.execute(() -> {
            final List<Recordatorio> lista = repo.getFuturos();
            runOnUiThread(() -> {
                containerRecordatorios.removeAllViews();
                if (lista.isEmpty()) {
                    tvVacio.setVisibility(View.VISIBLE);
                } else {
                    tvVacio.setVisibility(View.GONE);
                    for (final Recordatorio r : lista) {
                        containerRecordatorios.addView(adapter.getView(r, containerRecordatorios));
                    }
                }
            });
        });
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

    // ── Flag para saber si el diálogo es de edición o de creación ────────────
    private boolean esDialogoEdicion = false;

    // =========================================================================
    // Renderizado
    // =========================================================================
    private void guardarRecordatorio(Recordatorio existente, String titulo, String desc) {
        gestionarFeedbackHardware("EXITO");
        dbExecutor.execute(() -> {
            if (existente == null) {
                hablarEnMain("¡Enhorabuena! Has creado el recordatorio correctamente.");
                repo.add(new Recordatorio(0, titulo, horaSeleccionada, fechaSeleccionadaMs, desc, anticipacionSeleccionada));
            } else {
                hablarEnMain("¡Enhorabuena! Has editado el recordatorio correctamente.");
                existente.setTitulo(titulo);
                existente.setHoraMinutos(horaSeleccionada);
                existente.setFechaMs(fechaSeleccionadaMs);
                existente.setDescripcion(desc);
                existente.setAnticipacionMinutos(anticipacionSeleccionada);
                repo.update(existente);
            }
            runOnUiThread(this::renderizarLista);
        });
    }

    private void confirmarEliminar(final Recordatorio r) {
        String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty()) ? r.getTitulo() : "este recordatorio";
        new AlertDialog.Builder(this)
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Seguro que quieres eliminar \"" + titulo + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    dbExecutor.execute(() -> {
                        repo.delete(r.getId());
                        runOnUiThread(this::renderizarLista);
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String generarResumenVozRec() {
        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        String titulo = (etTitulo != null) ? etTitulo.getText().toString().trim() : "(Sin título)";
        String hora = String.format("%02d:%02d", horaSeleccionada / 60, horaSeleccionada % 60);
        return "Recordatorio: " + titulo + " a las " + hora;
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
