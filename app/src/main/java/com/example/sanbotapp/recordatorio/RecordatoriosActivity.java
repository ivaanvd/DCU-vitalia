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
import com.example.sanbotapp.util.VozComandos;
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
        setupTopBackBanner(getString(R.string.titulo_recordatorios));

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

        String instruccion = WizardFlowController.getInstruccionCampo(this, campo, context);
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
            if (anticipacionSeleccionada <= 0) tvAntic.setText(getString(R.string.rec_sin_aviso_previo).toUpperCase());
            else tvAntic.setText(getResources().getQuantityString(R.plurals.rec_avisar_minutos_antes, anticipacionSeleccionada, anticipacionSeleccionada).toUpperCase());
        }
    }





    private String obtenerInstruccionDinamicaRec() {
        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        String titulo = (etTitulo != null) ? etTitulo.getText().toString().trim() : "";

        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        String btnAction = getString(R.string.accion_anadir);
        if (dlg != null && dlg.findViewById(R.id.btnWizardSiguienteRec) instanceof android.widget.Button) {
            btnAction = ((android.widget.Button) dlg.findViewById(R.id.btnWizardSiguienteRec)).getText().toString();
        }

        if (titulo.isEmpty() || titulo.equalsIgnoreCase(getString(R.string.rec_sin_titulo))) {
            return getString(R.string.rec_instr_crear);
        }

        return getString(R.string.rec_instr_dinamica, titulo, btnAction);
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
            if (VozComandos.coincide(this, tGlobal, R.array.voz_si) || VozComandos.coincide(this, tGlobal, R.array.voz_guardar)) {
                hablarEnMain(getString(R.string.voz_entendido_guardando));
                gestionarFeedbackHardware("CELEBRACION");
                // Simular click en Siguiente (que es Guardar en este estado)
                android.widget.Button btnSig = dlg.findViewById(R.id.btnWizardSiguienteRec);
                if (btnSig != null) btnSig.performClick();
            } else if (VozComandos.coincide(this, tGlobal, R.array.voz_no)) {
                hablarEnMain(getString(R.string.voz_que_quieres_cambiar));
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            }
            return;
        }

        if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_titulo)) {
                hablarEnMain(getString(R.string.voz_cambiamos_titulo));
                anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
            } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_hora)) {
                hablarEnMain(getString(R.string.voz_vale_la_hora));
                anunciarCampoYEsperarToque(CampoVozEspera.HORA);
            } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_fecha)) {
                hablarEnMain(getString(R.string.voz_entendido_la_fecha));
                anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
            } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_aviso)) {
                hablarEnMain(getString(R.string.voz_de_acuerdo_el_aviso));
                anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
            } else if (VozComandos.coincide(this, tGlobal, R.array.voz_volver_resumen)) {
                hablarEnMain(getString(R.string.voz_volvemos_resumen));
                anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
            }
            return;
        }

        // ── Comandos globales ────────────────────────────────────────────────
        String btnText = getString(R.string.accion_anadir).toLowerCase();
        if (dlg.findViewById(R.id.btnWizardSiguienteRec) instanceof android.widget.Button) {
            btnText = ((android.widget.Button) dlg.findViewById(R.id.btnWizardSiguienteRec)).getText().toString()
                    .toLowerCase();
        }

        boolean quiereConfirmar = VozComandos.coincide(this, tGlobal, R.array.voz_confirmar)
                || VozComandos.contieneAlguna(tGlobal, btnText);

        if (quiereConfirmar) {
            hablarEnMain(getString(R.string.voz_entendido));
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnWizardSiguienteRec) != null) {
                    dlg.findViewById(R.id.btnWizardSiguienteRec).performClick();
                }
            });
            return;
        }

        if (VozComandos.coincide(this, tGlobal, R.array.voz_cancelar)) {
            hablarEnMain(getString(R.string.voz_vale));
            runOnUiThread(() -> {
                if (dlg.findViewById(R.id.btnWizardAnteriorRec) != null) {
                    dlg.findViewById(R.id.btnWizardAnteriorRec).performClick();
                }
            });
            return;
        }

        VozParser parser = VozParser.para(this);

        // ── Manejo por estados ───────────────────────────────────────────────
        switch (campoEspera) {
            case CONFIRMACION_CAMPO: {
                if (VozComandos.coincide(this, tGlobal, R.array.voz_si)) {
                    aplicarValorConfirmadoRec();
                    hablarEnMain(getString(R.string.voz_perfecto));
                } else if (VozComandos.coincide(this, tGlobal, R.array.voz_no)) {
                    hablarEnMain(getString(R.string.voz_lo_siento_otra_vez));
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
                int minutos = parser.parsearHoraVoz(texto);
                if (minutos != -1) {
                    int h = minutos / 60;
                    int m = minutos % 60;
                    valorPendienteConfirmar = String.format(java.util.Locale.ROOT, "%02d:%02d", h, m);
                    campoAConfirmar = CampoVozEspera.HORA;
                    runOnUiThread(() -> { if (tvHoraRef != null && tvHoraRef.get() != null) tvHoraRef.get().setText(valorPendienteConfirmar); });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain(getString(R.string.rec_hora_no_entendida));
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                }
                break;
            }

            case FECHA: {
                Long fechaMs = parser.parsearFechaVoz(texto);
                if (fechaMs != null) {
                    valorPendienteConfirmar = DateTimeUtils.formatearDiaMes(this, fechaMs);
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
                    hablarEnMain(getString(R.string.rec_fecha_no_entendida));
                    anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
                }
                break;
            }

            case ANTICIPACION: {
                Integer min = parser.parsearNumeroVoz(texto);
                if (min != null) {
                    valorPendienteConfirmar = getResources().getQuantityString(R.plurals.minutos_n, min, min);
                    campoAConfirmar = CampoVozEspera.ANTICIPACION;
                    runOnUiThread(() -> { if (tvAnticipacionRef != null && tvAnticipacionRef.get() != null) tvAnticipacionRef.get().setText(valorPendienteConfirmar); });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else {
                    hablarEnMain(getString(R.string.rec_numero_no_entendido));
                    anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
                }
                break;
            }


            case CAMPO_EDITAR: {
                if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_titulo)) {
                    hablarEnMain(getString(R.string.voz_de_acuerdo));
                    anunciarCampoYEsperarToque(CampoVozEspera.TITULO);
                } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_descripcion)) {
                    hablarEnMain(getString(R.string.voz_entendido));
                    anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
                } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_hora)) {
                    hablarEnMain(getString(R.string.voz_vale));
                    anunciarCampoYEsperarToque(CampoVozEspera.HORA);
                } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_fecha)) {
                    hablarEnMain(getString(R.string.voz_cambiamos_fecha));
                    anunciarCampoYEsperarToque(CampoVozEspera.FECHA);
                } else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_aviso)) {
                    hablarEnMain(getString(R.string.voz_de_acuerdo));
                    anunciarCampoYEsperarToque(CampoVozEspera.ANTICIPACION);
                } else {
                    hablarEnMain(getString(R.string.rec_campo_no_entendido));
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
                    horaSeleccionada = VozParser.para(this).parsearHoraVoz(valorPendienteConfirmar);
                    if (tvHoraRef != null && tvHoraRef.get() != null)
                        tvHoraRef.get().setText(DateTimeUtils.formatearHora(horaSeleccionada));
                    break;
                case FECHA:
                    fechaSeleccionadaMs = VozParser.para(this).parsearFechaVoz(valorPendienteConfirmar);
                    if (tvFechaRef != null && tvFechaRef.get() != null) {
                        Recordatorio tmp = new Recordatorio();
                        tmp.setFechaMs(fechaSeleccionadaMs);
                        tvFechaRef.get().setText(tmp.getFechaFormateada());
                    }
                    break;
                case ANTICIPACION:
                    anticipacionSeleccionada = VozParser.para(this).parsearNumeroVoz(valorPendienteConfirmar);
                    if (tvAnticipacionRef != null && tvAnticipacionRef.get() != null)
                        tvAnticipacionRef.get().setText(getResources().getQuantityString(R.plurals.minutos_antes, anticipacionSeleccionada, anticipacionSeleccionada));
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
            hablarEnMain(getString(R.string.voz_de_acuerdo_otra_vez));
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
                        hablarEnMain(getString(R.string.rec_pide_titulo));
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
            etTitulo.setError(getString(R.string.rec_introduce_titulo));
            hablarEnMain(getString(R.string.rec_por_favor_introduce_titulo));
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
                hablarEnMain(getString(R.string.rec_creado));
                repo.add(new Recordatorio(0, titulo, horaSeleccionada, fechaSeleccionadaMs, desc, anticipacionSeleccionada));
            } else {
                hablarEnMain(getString(R.string.rec_editado));
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
        String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty()) ? r.getTitulo() : getString(R.string.rec_este_recordatorio);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rec_eliminar_titulo)
                .setMessage(getString(R.string.rec_eliminar_mensaje, titulo))
                .setPositiveButton(R.string.accion_eliminar, (d, w) -> {
                    dbExecutor.execute(() -> {
                        repo.delete(r.getId());
                        runOnUiThread(this::renderizarLista);
                    });
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    private String generarResumenVozRec() {
        EditText etTitulo = etTituloRef != null ? etTituloRef.get() : null;
        String titulo = (etTitulo != null) ? etTitulo.getText().toString().trim() : getString(R.string.rec_sin_titulo_paren);
        String hora = String.format(java.util.Locale.ROOT, "%02d:%02d", horaSeleccionada / 60, horaSeleccionada % 60);
        return getString(R.string.rec_resumen_voz, titulo, hora);
    }

    private void actualizarDisplayFecha(TextView tv, long ms) {
        Recordatorio tmp = new Recordatorio();
        tmp.setFechaMs(ms);
        tv.setText(tmp.getFechaFormateada());
    }

    private void actualizarDisplayAnticipacion(TextView tv, int min) {
        if (min <= 0)
            tv.setText(R.string.rec_sin_aviso_previo);
        else
            tv.setText(getResources().getQuantityString(R.plurals.minutos_antes, min, min));
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
