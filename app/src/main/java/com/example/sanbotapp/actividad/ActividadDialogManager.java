package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.DateTimeUtils;
import com.example.sanbotapp.util.WizardFlowController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActividadDialogManager {

    public interface ActividadDialogListener {
        void onAnunciarCampo(CampoVozEspera campo);
        void onCabezaTocada();
        void onSetMicUI(View btn, TextView tv);
        void onPararVoz();
    }

    private final ActividadesActivity activity;
    private final ActividadDialogListener listener;
    private final ActividadRepository repo;

    private String tipoSeleccionado = Actividad.TIPO_MEDICACION;
    private int horaSeleccionada = 9 * 60;
    private List<Integer> diasSeleccionados;
    private final Map<String, Button> typeButtons = new HashMap<>();
    
    private final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    public ActividadDialogManager(ActividadesActivity activity, ActividadDialogListener listener, ActividadRepository repo) {
        this.activity = activity;
        this.listener = listener;
        this.repo = repo;
    }

    public void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada = (existente != null) ? existente.getHoraMinutos() : (9 * 60);
        diasSeleccionados = activity.getDiasSeleccionados();
        diasSeleccionados.clear();
        if (existente != null && existente.getDiasSemana() != null) {
            diasSeleccionados.addAll(existente.getDiasSemana());
        }

        
        final View dv = LayoutInflater.from(activity).inflate(R.layout.dialog_anadir_actividad, null);
        final TextView tvHora = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc = dv.findViewById(R.id.etDescripcionActividad);
        final TextView tvTitulo = dv.findViewById(R.id.tvTituloDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");
        if (existente != null) etDesc.setText(existente.getDescripcion());
        actualizarDisplayHora(tvHora, horaSeleccionada);

        setupBotonesTipo(dv);
        seleccionarTipoUI(existente != null ? existente.getTipo() : Actividad.TIPO_MEDICACION);

        final TextView[] btnsDia = {
                dv.findViewById(R.id.btnDiaLun), dv.findViewById(R.id.btnDiaMar),
                dv.findViewById(R.id.btnDiaMie), dv.findViewById(R.id.btnDiaJue),
                dv.findViewById(R.id.btnDiaVie), dv.findViewById(R.id.btnDiaSab),
                dv.findViewById(R.id.btnDiaDom)
        };
        
        ActividadWizardHelper helper = new ActividadWizardHelper(null, dv);
        helper.actualizarBotonesDia(btnsDia, diasSeleccionados);

        for (int i = 0; i < btnsDia.length; i++) {
            final int dia = VALORES_DIA[i];
            btnsDia[i].setOnClickListener(v -> {
                if (diasSeleccionados.contains(dia)) diasSeleccionados.remove(Integer.valueOf(dia));
                else diasSeleccionados.add(dia);
                helper.actualizarBotonesDia(btnsDia, diasSeleccionados);
            });
        }

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        // Configurar referencias en la actividad para que el asistente funcione
        activity.setDialogRefs(dialog, tvHora, btnsDia, etDesc);

        View btnMic = dv.findViewById(R.id.btnMicDialogAct);
        TextView tvEstadoMic = dv.findViewById(R.id.tvEstadoMicAct);
        if (btnMic != null && tvEstadoMic != null) {
            listener.onSetMicUI(btnMic, tvEstadoMic);
            btnMic.setOnClickListener(v -> listener.onCabezaTocada());
        }

        dialog.setOnDismissListener(d -> {
            listener.onSetMicUI(null, null);
            listener.onPararVoz();
            activity.clearAssistantState();
        });

        // NAVEGACIÓN
        Button btnAnt = dv.findViewById(R.id.btnWizardAnterior);
        Button btnSig = dv.findViewById(R.id.btnWizardSiguiente);

        btnAnt.setOnClickListener(v -> activity.onWizardAnterior(dialog));
        btnSig.setOnClickListener(v -> activity.onWizardSiguiente(dialog, existente, etDesc));

        dv.findViewById(R.id.btnEditTipo).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.TIPO));
        dv.findViewById(R.id.btnEditDesc).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.DESCRIPCION));
        dv.findViewById(R.id.btnEditHora).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.HORA));
        dv.findViewById(R.id.btnEditDias).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.DIA_SEMANA));
        dv.findViewById(R.id.btnCancelarDialogActividad2).setOnClickListener(v -> dialog.dismiss());

        dialog.show();


        // Paso inicial del asistente (Paso 0) - MEJORA: Siempre dar la opción (incluso al editar)
        View step0 = dv.findViewById(R.id.step0Container);
        activity.actualizarProgresoYResaltadoAct(CampoVozEspera.NINGUNO);

        View btnAsistente = dv.findViewById(R.id.btnEmpezarAsistenteAct);
        if (btnAsistente != null) {
            btnAsistente.setOnClickListener(v -> {
                if (step0 != null) step0.setVisibility(View.GONE);
                activity.setIsAssistantActive(true);
                listener.onAnunciarCampo(existente != null ? CampoVozEspera.ELECCION_EDICION : CampoVozEspera.TIPO);
            });
        }
        View btnManual = dv.findViewById(R.id.btnManualAct);
        if (btnManual != null) {
            btnManual.setOnClickListener(v -> {
                if (step0 != null) step0.setVisibility(View.GONE);
                activity.activarModoManual();
                // Si es edición, llevar a la elección de campo, no al resumen
                listener.onAnunciarCampo(existente != null ? CampoVozEspera.ELECCION_EDICION : CampoVozEspera.TIPO);
            });
        }

    }



    public void mostrarDialogoDetalle(final Actividad a) {
        ActividadUIUtils.mostrarDialogoDetalle(activity, a);
    }

    private void setupBotonesTipo(View dv) {
        typeButtons.clear();
        typeButtons.put(Actividad.TIPO_MEDICACION, dv.findViewById(R.id.btnTypeMed));
        typeButtons.put(Actividad.TIPO_BEBER_AGUA, dv.findViewById(R.id.btnTypeAgu));
        typeButtons.put(Actividad.TIPO_COMER, dv.findViewById(R.id.btnTypeCom));
        typeButtons.put(Actividad.TIPO_PASEO_EJERCICIO, dv.findViewById(R.id.btnTypePas));
        typeButtons.put(Actividad.TIPO_ASEO, dv.findViewById(R.id.btnTypeAse));
        typeButtons.put(Actividad.TIPO_JUEGOS, dv.findViewById(R.id.btnTypeJue));
        typeButtons.put(Actividad.TIPO_LLAMADA_FAMILIAR, dv.findViewById(R.id.btnTypeFam));
        typeButtons.put(Actividad.TIPO_IR_DORMIR, dv.findViewById(R.id.btnTypeDor));


        for (Map.Entry<String, Button> entry : typeButtons.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().setOnClickListener(v -> {
                    seleccionarTipoUI(entry.getKey());
                    actualizarCampoDinamico(dv, entry.getKey());
                });
            }
        }
    }

    public void seleccionarTipoUI(String tipo) {
        this.tipoSeleccionado = tipo;
        activity.setTipoSeleccionado(tipo);
        for (Map.Entry<String, Button> entry : typeButtons.entrySet()) {
            if (entry.getValue() != null) {
                boolean isSelected = entry.getKey().equals(tipo);
                entry.getValue().setBackgroundResource(isSelected ? R.drawable.bg_tipo_seleccionado : R.drawable.bg_tipo_normal);
                entry.getValue().setTextColor(isSelected ? Color.WHITE : Color.BLACK);
            }
        }
    }

    private void actualizarCampoDinamico(View dv, String tipoInterno) {
        TextView tvLabel = dv.findViewById(R.id.tvLabelDetalleDinamico);
        if (tvLabel == null) return;
        String nuevoLabel = "Información adicional";
        if (Actividad.TIPO_MEDICACION.equals(tipoInterno)) nuevoLabel = "¿Qué medicamento es?";
        else if (Actividad.TIPO_COMER.equals(tipoInterno)) nuevoLabel = "¿Qué vas a comer?";
        else if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(tipoInterno)) nuevoLabel = "¿A quién vas a llamar?";
        else if (Actividad.TIPO_PASEO_EJERCICIO.equals(tipoInterno)) nuevoLabel = "¿A dónde vas?";
        tvLabel.setText(nuevoLabel);
    }

    private void abrirTimePicker(TextView tv) {
        DateTimeUtils.mostrarTimePicker(activity, horaSeleccionada, (h, m, total) -> {
            horaSeleccionada = total;
            activity.setHoraSeleccionada(horaSeleccionada);
            actualizarDisplayHora(tv, horaSeleccionada);
        });
    }

    private void actualizarDisplayHora(TextView tv, int m) {
        tv.setText(DateTimeUtils.formatearHora(m));
    }

    public String getTipoSeleccionado() { return tipoSeleccionado; }
    public int getHoraSeleccionada() { return horaSeleccionada; }
    public List<Integer> getDiasSeleccionados() { return diasSeleccionados; }
}
