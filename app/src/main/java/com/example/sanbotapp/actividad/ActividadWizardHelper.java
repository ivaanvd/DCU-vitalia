package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;
import java.util.List;

/**
 * Clase encargada de gestionar la UI del "Wizard" de Actividades.
 * Aplica el principio de Responsabilidad Única para liberar a la Activity.
 */
public class ActividadWizardHelper {

    private final AlertDialog dialog;
    private final View root;
    private final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };

    public ActividadWizardHelper(AlertDialog dialog, View root) {
        this.dialog = dialog;
        this.root = root;
    }

    /**
     * Actualiza visualmente el resaltado del campo activo y los indicadores de progreso (bolitas).
     */
    public void actualizarProgresoYResaltado(CampoVozEspera campo, CampoVozEspera campoAConfirmar, 
                                            boolean isAssistantActive, boolean isEditingFromSummary) {
        if (dialog == null) return;

        // 1. Limpiar resaltados previos
        View spinnerContainer = root.findViewById(R.id.step1Container);
        View etDesc = root.findViewById(R.id.etDescripcionActividad);
        View tvHora = root.findViewById(R.id.tvHoraDialogActividad);
        View containerDias = root.findViewById(R.id.containerDiasSemana);

        if (etDesc != null) etDesc.setBackgroundResource(R.drawable.bg_campo_descripcion);
        if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_campo_descripcion);
        if (containerDias != null) containerDias.setPadding(0, 0, 0, 0);

        View dotContainer = root.findViewById(R.id.dotContainerAct);
        if (dotContainer != null) dotContainer.setVisibility(View.VISIBLE);

        // 2. Ocultar todos los contenedores de pasos
        int[] stepsIds = {R.id.step1Container, R.id.containerDetallesDinamicos, R.id.step3Container, 
                         R.id.step4Container, R.id.summaryContainerAct, R.id.editChoiceContainerAct, R.id.step0Container};
        for (int id : stepsIds) {
            View v = root.findViewById(id);
            if (v != null) v.setVisibility(View.GONE);
        }

        int step = 0;
        
        // Manejo de paso 0 (Asistente inicial)
        if (campo == CampoVozEspera.NINGUNO) {
            if (dotContainer != null) dotContainer.setVisibility(View.GONE);
            View step0 = root.findViewById(R.id.step0Container);
            if (step0 != null) step0.setVisibility(View.VISIBLE);
            configurarBotonesNavegacion(0, campo, false, false);
            return;
        }

        // 3. Mostrar paso correspondiente
        switch (campo) {
            case TIPO:
                step = 1;
                mostrarPaso(R.id.step1Container);
                break;
            case DESCRIPCION:
                step = 2;
                mostrarPaso(R.id.containerDetallesDinamicos);
                if (etDesc != null) etDesc.setBackgroundResource(R.drawable.bg_field_active);
                break;
            case HORA:
                step = 3;
                mostrarPaso(R.id.step3Container);
                if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                break;
            case DIA_SEMANA:
                step = 4;
                mostrarPaso(R.id.step4Container);
                break;
            case CAMPO_EDITAR:
                step = 0;
                mostrarPaso(R.id.editChoiceContainerAct);
                break;
            case CONFIRMACION_CAMPO:
                step = obtenerPasoDeCampo(campoAConfirmar);
                mostrarPasoCorrespondienteACampo(campoAConfirmar);
                resaltarCampo(campoAConfirmar);
                break;
            case RESUMEN_FINAL:
                step = 5;
                mostrarPaso(R.id.summaryContainerAct);
                break;
            case ELECCION_EDICION:
                step = 5;
                mostrarPaso(R.id.editChoiceContainerAct);
                break;
        }

        configurarBotonesNavegacion(step, campo, isAssistantActive, isEditingFromSummary);
        actualizarBolitasProgreso(step);
    }

    private void mostrarPaso(int id) {
        View v = root.findViewById(id);
        if (v != null) v.setVisibility(View.VISIBLE);
    }

    private void mostrarPasoCorrespondienteACampo(CampoVozEspera campo) {
        if (campo == CampoVozEspera.TIPO) mostrarPaso(R.id.step1Container);
        else if (campo == CampoVozEspera.DESCRIPCION) mostrarPaso(R.id.containerDetallesDinamicos);
        else if (campo == CampoVozEspera.HORA) mostrarPaso(R.id.step3Container);
        else if (campo == CampoVozEspera.DIA_SEMANA) mostrarPaso(R.id.step4Container);
    }

    private int obtenerPasoDeCampo(CampoVozEspera campo) {
        if (campo == CampoVozEspera.TIPO) return 1;
        if (campo == CampoVozEspera.DESCRIPCION) return 2;
        if (campo == CampoVozEspera.HORA) return 3;
        if (campo == CampoVozEspera.DIA_SEMANA) return 4;
        return 0;
    }

    private void resaltarCampo(CampoVozEspera campo) {
        if (campo == CampoVozEspera.DESCRIPCION) {
            View v = root.findViewById(R.id.etDescripcionActividad);
            if (v != null) v.setBackgroundResource(R.drawable.bg_field_active);
        } else if (campo == CampoVozEspera.HORA) {
            View v = root.findViewById(R.id.tvHoraDialogActividad);
            if (v != null) v.setBackgroundResource(R.drawable.bg_field_active);
        } else if (campo == CampoVozEspera.DIA_SEMANA) {
            View v = root.findViewById(R.id.containerDiasSemana);
            if (v != null) v.setPadding(4, 4, 4, 4);
        }
    }

    private void configurarBotonesNavegacion(int step, CampoVozEspera campo, boolean isAssistantActive, boolean isEditingFromSummary) {
        Button btnAnt = root.findViewById(R.id.btnWizardAnterior);
        Button btnSig = root.findViewById(R.id.btnWizardSiguiente);
        View micContainer = root.findViewById(R.id.containerMicEstadoAct);

        if (btnAnt == null || btnSig == null) return;

        // Ocultar botones en paso 0
        if (step == 0 && campo == CampoVozEspera.NINGUNO) {
            btnAnt.setVisibility(View.GONE);
            btnSig.setVisibility(View.GONE);
            if (micContainer != null) micContainer.setVisibility(View.GONE);
            return;
        }

        btnAnt.setVisibility(View.VISIBLE);
        btnSig.setVisibility(View.VISIBLE);
        
        // El micrófono solo se muestra si el asistente está activo y no estamos en confirmación o resumen
        boolean mostrarMic = isAssistantActive && 
                            campo != CampoVozEspera.CONFIRMACION_CAMPO && 
                            campo != CampoVozEspera.RESUMEN_FINAL && 
                            campo != CampoVozEspera.ELECCION_EDICION;
        if (micContainer != null) micContainer.setVisibility(mostrarMic ? View.VISIBLE : View.GONE);

        if (campo == CampoVozEspera.CONFIRMACION_CAMPO) {
            btnAnt.setText("REPETIR");
            btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
            btnSig.setText("CONFIRMAR");
            btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
        } else if (campo == CampoVozEspera.RESUMEN_FINAL) {
            btnAnt.setText("no, cambiar");
            btnAnt.setBackgroundResource(R.drawable.bg_btn_cancelar);
            btnSig.setText("sí, guardar");
            btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
        } else if (campo == CampoVozEspera.ELECCION_EDICION) {
            btnAnt.setText("VOLVER");
            btnAnt.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
            btnSig.setVisibility(View.GONE);
        } else if (isEditingFromSummary) {
            btnAnt.setText("VOLVER");
            btnSig.setText("CONFIRMAR");
            btnSig.setBackgroundResource(R.drawable.bg_btn_guardar);
        } else {
            btnAnt.setText(step <= 1 ? "CANCELAR" : "ANTERIOR");
            btnAnt.setBackgroundResource(step <= 1 ? R.drawable.bg_btn_cancelar : R.drawable.bg_btn_wizard_nav);
            btnSig.setText("SIGUIENTE");
            btnSig.setBackgroundResource(R.drawable.bg_btn_wizard_nav);
        }
    }

    private void actualizarBolitasProgreso(int step) {
        int[] dotIds = {R.id.dotStep1, R.id.dotStep2, R.id.dotStep3, R.id.dotStep4};
        for (int i = 0; i < dotIds.length; i++) {
            View dot = root.findViewById(dotIds[i]);
            if (dot != null) {
                dot.setBackgroundResource(i < step ? R.drawable.step_dot_active : R.drawable.step_dot_inactive);
            }
        }
    }

    public void actualizarBotonesDia(TextView[] btns, List<Integer> seleccionados) {
        for (int i = 0; i < btns.length; i++) {
            boolean sel = seleccionados.contains(VALORES_DIA[i]);
            btns[i].setBackgroundResource(sel ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
            btns[i].setTextColor(sel ? Color.WHITE : Color.BLACK);
        }
    }
}
