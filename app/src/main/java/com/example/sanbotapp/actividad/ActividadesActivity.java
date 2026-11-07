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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.DateTimeUtils;
import com.example.sanbotapp.util.VozComandos;
import com.example.sanbotapp.util.VozParser;
import com.example.sanbotapp.util.WizardFlowController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActividadesActivity extends BaseActivity {

    private static final int HORA_DEFAULT_MINUTOS = 9 * 60; // 09:00
    private static final int ID_NUEVO = Integer.MIN_VALUE;

    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    private LinearLayout containerActividades;
    private TextView tvVacio;
    private ActividadRepository repo;

    private int horaSeleccionada = HORA_DEFAULT_MINUTOS;
    private List<Integer> diasSeleccionados = new ArrayList<>();
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;
    private String valorPendienteConfirmar = "";
    private CampoVozEspera campoAConfirmar = CampoVozEspera.NINGUNO;

    private boolean isAssistantActive = false;
    private boolean isEditingFromSummary = false;
    private String tipoSeleccionado = Actividad.TIPO_OTROS;

    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<TextView> dialogTvHoraRef;
    private java.lang.ref.WeakReference<TextView[]> dialogBtnsDiaRef;
    private java.lang.ref.WeakReference<EditText> dialogEtDescRef;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService dbExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

    private ActividadDialogManager dialogManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividades);
        setupTopBackBanner(getString(R.string.titulo_actividades));

        containerActividades = findViewById(R.id.containerActividades);
        tvVacio = findViewById(R.id.tvVacioActividades);
        repo = new ActividadRepository(this);

        dialogManager = new ActividadDialogManager(this, new ActividadDialogManager.ActividadDialogListener() {
            @Override public void onAnunciarCampo(CampoVozEspera campo) { anunciarCampoYEsperarToque(campo); }
            @Override public void onCabezaTocada() { ActividadesActivity.this.onCabezaTocada(); }
            @Override public void onSetMicUI(View btn, TextView tv) { setMicUI(btn, tv); }
            @Override public void onPararVoz() { pararVoz(); }
        }, repo);

        findViewById(R.id.btnAnadirActividad).setOnClickListener(v -> dialogManager.mostrarDialogoAnadir(null));
        dbExecutor.execute(this::renderizarLista);
    }

    public void setDialogRefs(AlertDialog d, TextView tvH, TextView[] btnsD, EditText etD) {
        this.dialogRef = new java.lang.ref.WeakReference<>(d);
        this.dialogTvHoraRef = new java.lang.ref.WeakReference<>(tvH);
        this.dialogBtnsDiaRef = new java.lang.ref.WeakReference<>(btnsD);
        this.dialogEtDescRef = new java.lang.ref.WeakReference<>(etD);
    }
    public void setTipoSeleccionado(String t) { this.tipoSeleccionado = t; }
    public void setHoraSeleccionada(int h) { this.horaSeleccionada = h; }
    public List<Integer> getDiasSeleccionados() { return this.diasSeleccionados; }
    public void setIsAssistantActive(boolean active) { this.isAssistantActive = active; }

    public void activarModoManual() {
        this.isAssistantActive = false;
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
    }
    public void clearAssistantState() {

        this.campoEspera = CampoVozEspera.NINGUNO;
        this.isAssistantActive = false;
        this.isEditingFromSummary = false;
    }

    @Override
    protected void onRobotServiceReady() {
        super.onRobotServiceReady();
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
        if (dlg == null || !dlg.isShowing() || campoEspera == CampoVozEspera.NINGUNO) return;
        pararVoz();
        mainHandler.removeCallbacksAndMessages(null);
        mainHandler.post(this::escuchar);
    }

    private void anunciarCampoYEsperarToque(CampoVozEspera campo) {
        if (campoEspera == CampoVozEspera.RESUMEN_FINAL || campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (campo != CampoVozEspera.RESUMEN_FINAL && campo != CampoVozEspera.CONFIRMACION_CAMPO) {
                isEditingFromSummary = true;
            }
        }
        campoEspera = campo;
        actualizarProgresoYResaltadoAct(campo);

        String context = null;
        if (campo == CampoVozEspera.DESCRIPCION) context = obtenerInstruccionDescripcionAct();
        else if (campo == CampoVozEspera.CONFIRMACION_CAMPO) context = valorPendienteConfirmar;
        else if (campo == CampoVozEspera.CAMPO_EDITAR) context = obtenerInstruccionDinamicaAct();
        else if (campo == CampoVozEspera.RESUMEN_FINAL) {
            isEditingFromSummary = false;
            context = generarResumenVozAct();
        }

        String instruccion = WizardFlowController.getInstruccionCampo(this, campo, context);
        if (isAssistantActive) {
            hablarEnMain(instruccion);
        }
    }


    public void actualizarProgresoYResaltadoAct(CampoVozEspera campo) {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null) return;
        ActividadWizardHelper helper = new ActividadWizardHelper(dlg, dlg.findViewById(android.R.id.content));
        helper.actualizarProgresoYResaltado(campo, campoAConfirmar, isAssistantActive, isEditingFromSummary);
        if (campo == CampoVozEspera.RESUMEN_FINAL) actualizarTextoResumen(dlg);
    }

    private String obtenerInstruccionDescripcionAct() {
        String t = tipoSeleccionado;
        if (Actividad.TIPO_MEDICACION.equals(t)) return getString(R.string.act_instr_desc_medicacion);
        if (Actividad.TIPO_COMER.equals(t)) return getString(R.string.act_instr_desc_comer);
        if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(t)) return getString(R.string.act_instr_desc_llamada);
        if (Actividad.TIPO_PASEO_EJERCICIO.equals(t)) return getString(R.string.act_instr_desc_paseo);
        return getString(R.string.act_instr_desc_otros);
    }

    private String obtenerInstruccionDinamicaAct() {
        String btnAction = getString(R.string.accion_guardar).toUpperCase();
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg != null && dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnAction = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString();
        }
        return getString(R.string.act_instr_dinamica, TipoActividad.fromString(tipoSeleccionado).getLabel(this), btnAction);
    }

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        String tGlobal = texto.toLowerCase().trim();
        String btnText = getString(R.string.accion_guardar).toLowerCase();
        if (dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnText = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString().toLowerCase();
        }

        if (VozComandos.coincide(this, tGlobal, R.array.voz_confirmar) || VozComandos.contieneAlguna(tGlobal, btnText)) {
            hablarEnMain(getString(R.string.voz_entendido));
            runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardSiguiente) != null) dlg.findViewById(R.id.btnWizardSiguiente).performClick(); });
            return;
        }

        if (VozComandos.coincide(this, tGlobal, R.array.voz_cancelar)) {
            hablarEnMain(getString(R.string.voz_vale));
            runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardAnterior) != null) dlg.findViewById(R.id.btnWizardAnterior).performClick(); });
            return;
        }

        if (campoEspera == CampoVozEspera.RESUMEN_FINAL) {
            if (VozComandos.coincide(this, tGlobal, R.array.voz_si)) {
                hablarEnMain(getString(R.string.voz_perfecto_guardando));
                runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardSiguiente) != null) dlg.findViewById(R.id.btnWizardSiguiente).performClick(); });
            } else {
                hablarEnMain(getString(R.string.voz_de_acuerdo_que_cambiar));
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            }
            return;
        }

        if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_tipo)) anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
            else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_descripcion)) anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
            else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_hora)) anunciarCampoYEsperarToque(CampoVozEspera.HORA);
            else if (VozComandos.coincide(this, tGlobal, R.array.voz_campo_dias)) anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
            return;
        }

        if (campoEspera == CampoVozEspera.CONFIRMACION_CAMPO) {
            if (VozComandos.coincide(this, tGlobal, R.array.voz_si)) aplicarValorConfirmadoAct();
            else { hablarEnMain(getString(R.string.voz_lo_siento_dimelo_otra_vez)); anunciarCampoYEsperarToque(campoAConfirmar); }
            return;
        }

        procesarEntradaCampo(tGlobal);
    }

    private void procesarEntradaCampo(String tGlobal) {
        VozParser parser = VozParser.para(this);
        switch (campoEspera) {
            case TIPO:
                String tipo = parser.parsearTipoVoz(tGlobal);
                if (tipo != null) {
                    valorPendienteConfirmar = tipo;
                    campoAConfirmar = CampoVozEspera.TIPO;
                    runOnUiThread(() -> {
                        dialogManager.seleccionarTipoUI(tipo);
                    });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain(getString(R.string.act_tipo_no_entendido));
                break;

            case DESCRIPCION:
                valorPendienteConfirmar = tGlobal;
                campoAConfirmar = CampoVozEspera.DESCRIPCION;
                runOnUiThread(() -> { if (dialogEtDescRef.get() != null) dialogEtDescRef.get().setText(valorPendienteConfirmar); });
                anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                break;
            case HORA:
                int h = parser.parsearHoraVoz(tGlobal);
                if (h != -1) {
                    valorPendienteConfirmar = tGlobal;
                    campoAConfirmar = CampoVozEspera.HORA;
                    int finalH = h;
                    runOnUiThread(() -> { if (dialogTvHoraRef.get() != null) dialogTvHoraRef.get().setText(DateTimeUtils.formatearHora(finalH)); });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain(getString(R.string.act_hora_no_entendida));
                break;
            case DIA_SEMANA:
                List<Integer> dias = parser.parsearDiasVoz(tGlobal);
                if (!dias.isEmpty()) {
                    valorPendienteConfirmar = tGlobal;
                    campoAConfirmar = CampoVozEspera.DIA_SEMANA;
                    runOnUiThread(() -> {
                        if (dialogBtnsDiaRef.get() != null) {
                            new ActividadWizardHelper(null, null).actualizarBotonesDia(dialogBtnsDiaRef.get(), dias);
                        }
                    });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain(getString(R.string.act_dias_no_entendidos));
                break;

        }
    }

    private void aplicarValorConfirmadoAct() {
        VozParser parser = VozParser.para(this);
        runOnUiThread(() -> {
            switch (campoAConfirmar) {
                case DESCRIPCION: if (dialogEtDescRef.get() != null) dialogEtDescRef.get().setText(valorPendienteConfirmar); break;
                case TIPO: dialogManager.seleccionarTipoUI(parser.parsearTipoVoz(valorPendienteConfirmar)); break;
                case HORA: 
                    horaSeleccionada = parser.parsearHoraVoz(valorPendienteConfirmar);
                    if (dialogTvHoraRef.get() != null) dialogTvHoraRef.get().setText(DateTimeUtils.formatearHora(horaSeleccionada));
                    break;
                case DIA_SEMANA:
                    List<Integer> dias = parser.parsearDiasVoz(valorPendienteConfirmar);
                    diasSeleccionados.clear(); diasSeleccionados.addAll(dias);
                    if (dialogBtnsDiaRef.get() != null) new ActividadWizardHelper(null, null).actualizarBotonesDia(dialogBtnsDiaRef.get(), diasSeleccionados);
                    break;
            }
            anunciarCampoYEsperarToque(WizardFlowController.getSiguienteCampo(campoAConfirmar, isEditingFromSummary, true));
        });
    }

    public void onWizardAnterior(AlertDialog dialog) {
        gestionarFeedbackHardware("CANCELADO");
        if (isEditingFromSummary && campoEspera != CampoVozEspera.ELECCION_EDICION && campoEspera != CampoVozEspera.RESUMEN_FINAL) {
            anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL); return;
        }
        if (campoEspera == CampoVozEspera.CONFIRMACION_CAMPO) anunciarCampoYEsperarToque(campoAConfirmar);
        else if (campoEspera == CampoVozEspera.RESUMEN_FINAL) anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
        else if (campoEspera == CampoVozEspera.ELECCION_EDICION) anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL);
        else {
            CampoVozEspera anterior = WizardFlowController.getCampoAnterior(campoEspera, true);
            if (anterior == CampoVozEspera.NINGUNO) dialog.dismiss();
            else anunciarCampoYEsperarToque(anterior);
        }
    }

    public void onWizardSiguiente(AlertDialog dialog, Actividad existente, EditText etDesc) {
        switch (campoEspera) {
            case TIPO: anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.DESCRIPCION); break;
            case DESCRIPCION: anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.HORA); break;
            case HORA: anunciarCampoYEsperarToque(isEditingFromSummary ? CampoVozEspera.RESUMEN_FINAL : CampoVozEspera.DIA_SEMANA); break;
            case DIA_SEMANA: anunciarCampoYEsperarToque(CampoVozEspera.RESUMEN_FINAL); break;
            case CONFIRMACION_CAMPO: aplicarValorConfirmadoAct(); break;
            default: validarYGuardarActividad(existente, etDesc, dialog); break;
        }
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
                    ActividadAdapter adapter = new ActividadAdapter(this, new ActividadAdapter.OnActividadClickListener() {
                        @Override public void onActividadClick(Actividad a) { dialogManager.mostrarDialogoDetalle(a); }
                        @Override public void onEditarClick(Actividad a) { dialogManager.mostrarDialogoAnadir(a); }
                        @Override public void onEliminarClick(Actividad a) { confirmarEliminar(a); }
                    });
                    for (Actividad a : lista) containerActividades.addView(adapter.getView(a, containerActividades));
                }
            });
        });
    }

    private void guardarActividad(Actividad existente, String tipo, String desc) {
        gestionarFeedbackHardware("CELEBRACION");
        dbExecutor.execute(() -> {
            if (existente == null) {
                hablarEnMain(getString(R.string.act_creada));
                Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
                nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.add(nueva);
            } else {
                hablarEnMain(getString(R.string.act_editada));
                existente.setTipo(tipo); existente.setHoraMinutos(horaSeleccionada);
                existente.setDescripcion(desc); existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.update(existente);
            }
            runOnUiThread(this::renderizarLista);
        });
    }

    private void validarYGuardarActividad(Actividad existente, EditText etDesc, AlertDialog dialog) {
        final String desc = etDesc.getText().toString().trim();
        final String tipo = tipoSeleccionado;
        if (diasSeleccionados.isEmpty()) { hablarEnMain(getString(R.string.act_selecciona_dia)); return; }
        final int duracion = new Actividad(0, tipo, horaSeleccionada, "").getDuracionMinutos();
        dbExecutor.execute(() -> {
            final boolean solapa = repo.haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, (existente != null ? existente.getId() : ID_NUEVO));
            runOnUiThread(() -> {
                if (solapa) {
                    new AlertDialog.Builder(this).setTitle(R.string.act_solapamiento_titulo).setMessage(R.string.act_solapamiento_mensaje)
                        .setPositiveButton(R.string.accion_si, (d, w) -> { guardarActividad(existente, tipo, desc); dialog.dismiss(); })
                        .setNegativeButton(R.string.accion_no, null).show();
                } else { guardarActividad(existente, tipo, desc); dialog.dismiss(); }
            });
        });
    }

    private void confirmarEliminar(Actividad a) {
        new AlertDialog.Builder(this).setTitle(R.string.accion_eliminar).setMessage(R.string.act_eliminar_mensaje)
            .setPositiveButton(R.string.accion_si, (d, w) -> { dbExecutor.execute(() -> { repo.delete(a.getId()); runOnUiThread(this::renderizarLista); }); })
            .setNegativeButton(R.string.accion_no, null).show();
    }

    private String generarResumenVozAct() {
        String desc = (dialogEtDescRef != null && dialogEtDescRef.get() != null) ? dialogEtDescRef.get().getText().toString().trim() : "";
        String tLabel = TipoActividad.fromString(tipoSeleccionado).getLabel(this);
        String hora = DateTimeUtils.formatearHora(horaSeleccionada);
        return desc.isEmpty()
                ? getString(R.string.act_resumen_voz, tLabel, hora)
                : getString(R.string.act_resumen_voz_detalles, tLabel, hora, desc);
    }

    private void actualizarTextoResumen(AlertDialog dlg) {
        String desc = (dialogEtDescRef != null && dialogEtDescRef.get() != null) ? dialogEtDescRef.get().getText().toString().trim() : "";
        TipoActividad tipo = TipoActividad.fromString(tipoSeleccionado);
        String tLabel = tipo.getLabel(this);

        ((TextView) dlg.findViewById(R.id.tvSummaryTipoAct)).setText(tLabel);
        ((TextView) dlg.findViewById(R.id.tvSummaryDescAct)).setText(desc.isEmpty() ? getString(R.string.act_sin_detalles) : desc.toUpperCase());
        ((TextView) dlg.findViewById(R.id.tvSummaryHoraAct)).setText(DateTimeUtils.formatearHora(horaSeleccionada));

        // Icono y Color
        View frame = dlg.findViewById(R.id.frameEmojiSummaryAct);
        ImageView iv = dlg.findViewById(R.id.ivIconSummaryAct);
        if (frame != null && iv != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor(tipo.getColorHex()));
            frame.setBackground(bg);
            iv.setImageResource(tipo.getIconoRes());
        }

        // Días en pills
        LinearLayout container = dlg.findViewById(R.id.containerDiasSummaryAct);
        if (container != null) {
            container.removeAllViews();
            if (diasSeleccionados.size() == 7) {
                ActividadUIUtils.agregarPillDia(this, container, getString(R.string.dias_todos));
            } else {
                String[] n = getResources().getStringArray(R.array.dias_abrev);
                for (int i = 0; i < VALORES_DIA.length; i++) {
                    if (diasSeleccionados.contains(VALORES_DIA[i])) {
                        ActividadUIUtils.agregarPillDia(this, container, n[i]);
                    }
                }
            }
        }
    }


}