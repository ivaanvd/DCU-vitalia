package com.example.sanbotapp.recordatorio;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

/**
 * Clase encargada de gestionar la UI del "Wizard" de Recordatorios.
 * Aplica el principio de Responsabilidad Única para liberar a la Activity.
 */
public class RecordatorioWizardHelper {

    private final AlertDialog dialog;
    private final View root;

    public RecordatorioWizardHelper(AlertDialog dialog, View root) {
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
        View etTitulo = root.findViewById(R.id.etTituloRecordatorio);
        View tvHora = root.findViewById(R.id.tvHoraDialogRec);
        View tvFecha = root.findViewById(R.id.tvFechaDialogRec);
        View containerAntic = root.findViewById(R.id.containerAnticipacion);

        if (etTitulo != null) etTitulo.setBackgroundResource(R.drawable.bg_campo_descripcion);
        if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_campo_descripcion);
        if (tvFecha != null) tvFecha.setBackgroundResource(R.drawable.bg_campo_descripcion);
        if (containerAntic != null) containerAntic.setPadding(0, 0, 0, 0);

        View dotContainer = root.findViewById(R.id.dotContainerRec);
        if (dotContainer != null) dotContainer.setVisibility(View.VISIBLE);

        // 2. Ocultar todos los contenedores de pasos
        int[] stepsIds = {R.id.stepRec1Container, R.id.stepRec2Container, R.id.stepRec3Container, 
                         R.id.stepRec4Container, R.id.summaryContainerRec, R.id.editChoiceContainerRec, R.id.stepRec0Container};
        for (int id : stepsIds) {
            View v = root.findViewById(id);
            if (v != null) v.setVisibility(View.GONE);
        }

        int step = 0;
        
        // Manejo de paso 0 (Asistente inicial)
        if (campo == CampoVozEspera.NINGUNO) {
            if (dotContainer != null) dotContainer.setVisibility(View.GONE);
            View step0 = root.findViewById(R.id.stepRec0Container);
            if (step0 != null) step0.setVisibility(View.VISIBLE);
            configurarBotonesNavegacion(0, campo, false, false);
            return;
        }

        // 3. Mostrar paso correspondiente
        switch (campo) {
            case TITULO:
                step = 1;
                mostrarPaso(R.id.stepRec1Container);
                if (etTitulo != null) etTitulo.setBackgroundResource(R.drawable.bg_field_active);
                break;
            case HORA:
                step = 2;
                mostrarPaso(R.id.stepRec2Container);
                if (tvHora != null) tvHora.setBackgroundResource(R.drawable.bg_field_active);
                break;
            case FECHA:
                step = 3;
                mostrarPaso(R.id.stepRec3Container);
                if (tvFecha != null) tvFecha.setBackgroundResource(R.drawable.bg_field_active);
                break;
            case ANTICIPACION:
                step = 4;
                mostrarPaso(R.id.stepRec4Container);
                if (containerAntic != null) containerAntic.setPadding(4, 4, 4, 4);
                break;
            case CAMPO_EDITAR:
                step = 0;
                mostrarPaso(R.id.editChoiceContainerRec);
                break;
            case CONFIRMACION_CAMPO:
                step = obtenerPasoDeCampo(campoAConfirmar);
                mostrarPasoCorrespondienteACampo(campoAConfirmar);
                resaltarCampo(campoAConfirmar);
                break;
            case RESUMEN_FINAL:
                step = 5;
                mostrarPaso(R.id.summaryContainerRec);
                break;
            case ELECCION_EDICION:
                step = 5;
                mostrarPaso(R.id.editChoiceContainerRec);
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
        if (campo == CampoVozEspera.TITULO) mostrarPaso(R.id.stepRec1Container);
        else if (campo == CampoVozEspera.HORA) mostrarPaso(R.id.stepRec2Container);
        else if (campo == CampoVozEspera.FECHA) mostrarPaso(R.id.stepRec3Container);
        else if (campo == CampoVozEspera.ANTICIPACION) mostrarPaso(R.id.stepRec4Container);
    }

    private int obtenerPasoDeCampo(CampoVozEspera campo) {
        if (campo == CampoVozEspera.TITULO) return 1;
        if (campo == CampoVozEspera.HORA) return 2;
        if (campo == CampoVozEspera.FECHA) return 3;
        if (campo == CampoVozEspera.ANTICIPACION) return 4;
        return 0;
    }

    private void resaltarCampo(CampoVozEspera campo) {
        if (campo == CampoVozEspera.TITULO) {
            View v = root.findViewById(R.id.etTituloRecordatorio);
            if (v != null) v.setBackgroundResource(R.drawable.bg_field_active);
        } else if (campo == CampoVozEspera.HORA) {
            View v = root.findViewById(R.id.tvHoraDialogRec);
            if (v != null) v.setBackgroundResource(R.drawable.bg_field_active);
        } else if (campo == CampoVozEspera.FECHA) {
            View v = root.findViewById(R.id.tvFechaDialogRec);
            if (v != null) v.setBackgroundResource(R.drawable.bg_field_active);
        } else if (campo == CampoVozEspera.ANTICIPACION) {
            View v = root.findViewById(R.id.containerAnticipacion);
            if (v != null) v.setPadding(4, 4, 4, 4);
        }
    }

    private void configurarBotonesNavegacion(int step, CampoVozEspera campo, boolean isAssistantActive, boolean isEditingFromSummary) {
        Button btnAnt = root.findViewById(R.id.btnWizardAnteriorRec);
        Button btnSig = root.findViewById(R.id.btnWizardSiguienteRec);
        View micContainer = root.findViewById(R.id.containerMicEstadoRec);

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
        int[] dotIds = {R.id.dotStepRec1, R.id.dotStepRec2, R.id.dotStepRec3, R.id.dotStepRec4};
        for (int i = 0; i < dotIds.length; i++) {
            View dot = root.findViewById(dotIds[i]);
            if (dot != null) {
                dot.setBackgroundResource(i < step ? R.drawable.step_dot_active : R.drawable.step_dot_inactive);
            }
        }
    }
}
