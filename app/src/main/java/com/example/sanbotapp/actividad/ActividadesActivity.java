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
        setupTopBackBanner("Actividades");

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

        String instruccion = WizardFlowController.getInstruccionCampo(campo, context);
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
        if (Actividad.TIPO_MEDICACION.equals(t)) return "¿Qué medicina te toca tomar? Por ejemplo: 'Paracetamol'.";
        if (Actividad.TIPO_COMER.equals(t)) return "¿Qué vas a comer hoy? Por ejemplo: 'Sopa de verduras'.";
        if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(t)) return "¿A quién vas a llamar? Por ejemplo: 'A mi hija María'.";
        if (Actividad.TIPO_PASEO_EJERCICIO.equals(t)) return "¿A dónde vas a ir a caminar? Por ejemplo: 'Al parque'.";
        return "¿Qué detalles quieres añadir? Toca mi cabeza.";
    }

    private String obtenerInstruccionDinamicaAct() {
        String btnAction = "GUARDAR";
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg != null && dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnAction = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString();
        }
        return "La actividad es de tipo " + (tipoSeleccionado != null ? tipoSeleccionado : "seleccionado") + ". ¿Quieres cambiar algo o '" + btnAction + "' ya?";
    }

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        String tGlobal = texto.toLowerCase().trim();
        String btnText = "guardar";
        if (dlg.findViewById(R.id.btnWizardSiguiente) instanceof Button) {
            btnText = ((Button) dlg.findViewById(R.id.btnWizardSiguiente)).getText().toString().toLowerCase();
        }

        if (tGlobal.contains("confirmar") || tGlobal.contains("aceptar") || tGlobal.contains("guardar") || tGlobal.contains("siguiente") || tGlobal.contains(btnText)) {
            hablarEnMain("Entendido.");
            runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardSiguiente) != null) dlg.findViewById(R.id.btnWizardSiguiente).performClick(); });
            return;
        }

        if (tGlobal.contains("cancelar") || tGlobal.contains("atrás") || tGlobal.contains("cerrar")) {
            hablarEnMain("Vale.");
            runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardAnterior) != null) dlg.findViewById(R.id.btnWizardAnterior).performClick(); });
            return;
        }

        if (campoEspera == CampoVozEspera.RESUMEN_FINAL) {
            if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto")) {
                hablarEnMain("¡Perfecto! Guardando...");
                runOnUiThread(() -> { if (dlg.findViewById(R.id.btnWizardSiguiente) != null) dlg.findViewById(R.id.btnWizardSiguiente).performClick(); });
            } else {
                hablarEnMain("De acuerdo. ¿Qué quieres cambiar?");
                anunciarCampoYEsperarToque(CampoVozEspera.ELECCION_EDICION);
            }
            return;
        }

        if (campoEspera == CampoVozEspera.ELECCION_EDICION) {
            if (tGlobal.contains("tipo")) anunciarCampoYEsperarToque(CampoVozEspera.TIPO);
            else if (tGlobal.contains("descripción") || tGlobal.contains("detalle")) anunciarCampoYEsperarToque(CampoVozEspera.DESCRIPCION);
            else if (tGlobal.contains("hora")) anunciarCampoYEsperarToque(CampoVozEspera.HORA);
            else if (tGlobal.contains("día")) anunciarCampoYEsperarToque(CampoVozEspera.DIA_SEMANA);
            return;
        }

        if (campoEspera == CampoVozEspera.CONFIRMACION_CAMPO) {
            if (tGlobal.contains("sí") || tGlobal.contains("si") || tGlobal.contains("correcto") || tGlobal.contains("así es")) aplicarValorConfirmadoAct();
            else { hablarEnMain("Lo siento, dímelo otra vez."); anunciarCampoYEsperarToque(campoAConfirmar); }
            return;
        }

        procesarEntradaCampo(tGlobal);
    }

    private void procesarEntradaCampo(String tGlobal) {
        switch (campoEspera) {
            case TIPO:
                String tipo = VozParser.parsearTipoVoz(tGlobal);
                if (tipo != null) {
                    valorPendienteConfirmar = tipo;
                    campoAConfirmar = CampoVozEspera.TIPO;
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain("No te entendí el tipo. Prueba con: Medicación o Paseo.");
                break;
            case DESCRIPCION:
                valorPendienteConfirmar = tGlobal;
                campoAConfirmar = CampoVozEspera.DESCRIPCION;
                runOnUiThread(() -> { if (dialogEtDescRef.get() != null) dialogEtDescRef.get().setText(valorPendienteConfirmar); });
                anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                break;
            case HORA:
                int h = VozParser.parsearHoraVoz(tGlobal);
                if (h != -1) {
                    valorPendienteConfirmar = tGlobal;
                    campoAConfirmar = CampoVozEspera.HORA;
                    int finalH = h;
                    runOnUiThread(() -> { if (dialogTvHoraRef.get() != null) dialogTvHoraRef.get().setText(DateTimeUtils.formatearHora(finalH)); });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain("Dime la hora, por ejemplo: a las diez y media.");
                break;
            case DIA_SEMANA:
                List<Integer> dias = VozParser.parsearDiasVoz(tGlobal);
                if (!dias.isEmpty()) {
                    valorPendienteConfirmar = tGlobal;
                    campoAConfirmar = CampoVozEspera.DIA_SEMANA;
                    runOnUiThread(() -> {
                        if (dialogBtnsDiaRef.get() != null) {
                            new ActividadWizardHelper(null, null).actualizarBotonesDia(dialogBtnsDiaRef.get(), dias);
                        }
                    });
                    anunciarCampoYEsperarToque(CampoVozEspera.CONFIRMACION_CAMPO);
                } else hablarEnMain("Dime los días, por ejemplo: los lunes y jueves.");
                break;

        }
    }

    private void aplicarValorConfirmadoAct() {
        runOnUiThread(() -> {
            switch (campoAConfirmar) {
                case DESCRIPCION: if (dialogEtDescRef.get() != null) dialogEtDescRef.get().setText(valorPendienteConfirmar); break;
                case TIPO: dialogManager.seleccionarTipoUI(VozParser.parsearTipoVoz(valorPendienteConfirmar)); break;
                case HORA: 
                    horaSeleccionada = VozParser.parsearHoraVoz(valorPendienteConfirmar);
                    if (dialogTvHoraRef.get() != null) dialogTvHoraRef.get().setText(DateTimeUtils.formatearHora(horaSeleccionada));
                    break;
                case DIA_SEMANA:
                    List<Integer> dias = VozParser.parsearDiasVoz(valorPendienteConfirmar);
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
                hablarEnMain("¡Enhorabuena! Has creado la actividad correctamente.");
                Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
                nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
                repo.add(nueva);
            } else {
                hablarEnMain("¡Enhorabuena! Has editado la actividad correctamente.");
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
        if (diasSeleccionados.isEmpty()) { hablarEnMain("Selecciona al menos un día."); return; }
        final int duracion = new Actividad(0, tipo, horaSeleccionada, "").getDuracionMinutos();
        dbExecutor.execute(() -> {
            final boolean solapa = repo.haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, (existente != null ? existente.getId() : ID_NUEVO));
            runOnUiThread(() -> {
                if (solapa) {
                    new AlertDialog.Builder(this).setTitle("Solapamiento").setMessage("Ya hay otra actividad. ¿Guardar?")
                        .setPositiveButton("Sí", (d, w) -> { guardarActividad(existente, tipo, desc); dialog.dismiss(); })
                        .setNegativeButton("No", null).show();
                } else { guardarActividad(existente, tipo, desc); dialog.dismiss(); }
            });
        });
    }

    private void confirmarEliminar(Actividad a) {
        new AlertDialog.Builder(this).setTitle("Eliminar").setMessage("¿Borrar esta actividad?")
            .setPositiveButton("Sí", (d, w) -> { dbExecutor.execute(() -> { repo.delete(a.getId()); runOnUiThread(this::renderizarLista); }); })
            .setNegativeButton("No", null).show();
    }

    private String generarResumenVozAct() {
        String desc = (dialogEtDescRef != null && dialogEtDescRef.get() != null) ? dialogEtDescRef.get().getText().toString().trim() : "";
        String tLabel = TipoActividad.fromString(tipoSeleccionado).getLabel();
        return "Actividad de " + tLabel + " a las " + DateTimeUtils.formatearHora(horaSeleccionada) + (desc.isEmpty() ? "" : ", con detalles: " + desc);
    }

    private void actualizarTextoResumen(AlertDialog dlg) {
        String desc = (dialogEtDescRef != null && dialogEtDescRef.get() != null) ? dialogEtDescRef.get().getText().toString().trim() : "";
        TipoActividad tipo = TipoActividad.fromString(tipoSeleccionado);
        String tLabel = tipo.getLabel();

        ((TextView) dlg.findViewById(R.id.tvSummaryTipoAct)).setText(tLabel);
        ((TextView) dlg.findViewById(R.id.tvSummaryDescAct)).setText(desc.isEmpty() ? "(SIN DETALLES)" : desc.toUpperCase());
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
                ActividadUIUtils.agregarPillDia(this, container, "TODOS LOS DÍAS");
            } else {
                String[] n = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
                for (int i = 0; i < VALORES_DIA.length; i++) {
                    if (diasSeleccionados.contains(VALORES_DIA[i])) {
                        ActividadUIUtils.agregarPillDia(this, container, n[i]);
                    }
                }
            }
        }
    }


}