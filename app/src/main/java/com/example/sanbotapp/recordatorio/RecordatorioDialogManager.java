package com.example.sanbotapp.recordatorio;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.DateTimeUtils;

import java.lang.ref.WeakReference;

/**
 * Gestor de diálogos para recordatorios.
 * Extraído de RecordatoriosActivity para cumplir con el principio de Responsabilidad Única (SRP).
 */
public class RecordatorioDialogManager {

    public interface RecordatorioDialogListener {
        void onAnunciarCampo(CampoVozEspera campo);
        void onCabezaTocada();
        void onSetMicUI(View btn, TextView tv);
        void onPararVoz();
    }

    private final RecordatoriosActivity activity;
    private final RecordatorioDialogListener listener;

    // Estado del diálogo
    private int horaSeleccionada;
    private long fechaSeleccionadaMs;
    private int anticipacionSeleccionada;

    public RecordatorioDialogManager(RecordatoriosActivity activity, RecordatorioDialogListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void mostrarDialogoAnadir(final Recordatorio existente) {
        boolean esEdicion = (existente != null);
        
        // Inicializar estado local
        horaSeleccionada = esEdicion ? existente.getHoraMinutos() : 9 * 60;
        fechaSeleccionadaMs = esEdicion ? existente.getFechaMs() : System.currentTimeMillis();
        anticipacionSeleccionada = esEdicion ? existente.getAnticipacionMinutos() : 10;

        // Sincronizar estado con la Activity (para que el asistente de voz lo vea)
        activity.sincronizarEstadoDialogo(horaSeleccionada, fechaSeleccionadaMs, anticipacionSeleccionada, esEdicion);

        final View dv = LayoutInflater.from(activity).inflate(R.layout.dialog_anadir_recordatorio, null);

        final EditText etTitulo = dv.findViewById(R.id.etTituloRecordatorio);
        final TextView tvHora = dv.findViewById(R.id.tvHoraDialogRec);
        final TextView tvFecha = dv.findViewById(R.id.tvFechaDialogRec);
        final TextView tvAnticValor = dv.findViewById(R.id.tvAnticipacionRec);
        final View btnAnticMenos = dv.findViewById(R.id.btnAnticMenos);
        final View btnAnticMas = dv.findViewById(R.id.btnAnticMas);

        if (esEdicion) {
            etTitulo.setText(existente.getTitulo());
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);
        actualizarDisplayFecha(tvFecha, fechaSeleccionadaMs);
        actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));
        tvFecha.setOnClickListener(v -> abrirDatePicker(tvFecha));

        btnAnticMenos.setOnClickListener(v -> {
            if (anticipacionSeleccionada >= 5) {
                anticipacionSeleccionada -= 5;
                activity.setAnticipacionSeleccionada(anticipacionSeleccionada);
                actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);
            }
        });
        btnAnticMas.setOnClickListener(v -> {
            if (anticipacionSeleccionada < 60) {
                anticipacionSeleccionada += 5;
                activity.setAnticipacionSeleccionada(anticipacionSeleccionada);
                actualizarDisplayAnticipacion(tvAnticValor, anticipacionSeleccionada);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        // Pasar referencias a la Activity para el asistente de voz
        activity.setDialogRefs(dialog, etTitulo, tvHora, tvFecha, tvAnticValor);

        // Configurar Micro
        View btnMic = dv.findViewById(R.id.btnMicDialogRec);
        TextView tvEstadoMic = dv.findViewById(R.id.tvEstadoMicRec);
        if (btnMic != null && tvEstadoMic != null) {
            listener.onSetMicUI(btnMic, tvEstadoMic);
            btnMic.setOnClickListener(v -> listener.onCabezaTocada());
        }

        dialog.setOnDismissListener(d -> {
            listener.onSetMicUI(null, null);
            listener.onPararVoz();
            activity.limpiarEstadoAsistente();
        });

        // Wizard Buttons
        Button btnAnt = dv.findViewById(R.id.btnWizardAnteriorRec);
        Button btnSig = dv.findViewById(R.id.btnWizardSiguienteRec);

        btnAnt.setOnClickListener(v -> activity.onWizardAnterior(dialog));
        btnSig.setOnClickListener(v -> activity.onWizardSiguiente(dialog, existente, etTitulo));

        // Edición granular
        dv.findViewById(R.id.btnEditTituloRec).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.TITULO));
        dv.findViewById(R.id.btnEditHoraRec).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.HORA));
        dv.findViewById(R.id.btnEditFechaRec).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.FECHA));
        dv.findViewById(R.id.btnEditAnticRec).setOnClickListener(v -> listener.onAnunciarCampo(CampoVozEspera.ANTICIPACION));
        dv.findViewById(R.id.btnCerrarDialogRec).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Paso inicial del asistente (Paso 0) - MEJORA: Siempre dar la opción (incluso al editar)
        View step0 = dv.findViewById(R.id.stepRec0Container);
        activity.actualizarProgresoYResaltadoRec(CampoVozEspera.NINGUNO);

        View btnAsistente = dv.findViewById(R.id.btnEmpezarAsistenteRec);
        if (btnAsistente != null) {
            btnAsistente.setOnClickListener(v -> {
                if (step0 != null) step0.setVisibility(View.GONE);
                activity.setIsAssistantActive(true);
                listener.onAnunciarCampo(esEdicion ? CampoVozEspera.ELECCION_EDICION : CampoVozEspera.TITULO);
            });
        }
        View btnManual = dv.findViewById(R.id.btnManualRec);
        if (btnManual != null) {
            btnManual.setOnClickListener(v -> {
                if (step0 != null) step0.setVisibility(View.GONE);
                activity.activarModoManual();
                // Si es edición, llevar a la elección de campo, no al resumen
                listener.onAnunciarCampo(esEdicion ? CampoVozEspera.ELECCION_EDICION : CampoVozEspera.TITULO);
            });
        }

    }


    public void mostrarDialogoDetalle(final Recordatorio r) {
        View dv = LayoutInflater.from(activity).inflate(R.layout.dialog_detalle_recordatorio, null);

        ((TextView) dv.findViewById(R.id.tvTituloDetRec)).setText(
                r.getTitulo() != null ? r.getTitulo().toUpperCase() : "SIN TÍTULO");
        ((TextView) dv.findViewById(R.id.tvHoraDetRec)).setText(r.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvFechaDetRec)).setText(r.getFechaFormateada());

        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dv.findViewById(R.id.btnCerrarDetRec).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void abrirTimePicker(final TextView tv) {
        DateTimeUtils.mostrarTimePicker(activity, horaSeleccionada, (h, m, total) -> {
            horaSeleccionada = total;
            activity.setHoraSeleccionada(horaSeleccionada);
            actualizarDisplayHora(tv, horaSeleccionada);
        });
    }

    private void abrirDatePicker(final TextView tv) {
        DateTimeUtils.mostrarDatePicker(activity, fechaSeleccionadaMs, (ms) -> {
            fechaSeleccionadaMs = ms;
            activity.setFechaSeleccionadaMs(fechaSeleccionadaMs);
            actualizarDisplayFecha(tv, fechaSeleccionadaMs);
        });
    }

    private void actualizarDisplayHora(TextView tv, int minutos) {
        tv.setText(DateTimeUtils.formatearHora(minutos));
    }

    private void actualizarDisplayFecha(TextView tv, long ms) {
        Recordatorio tmp = new Recordatorio();
        tmp.setFechaMs(ms);
        tv.setText(tmp.getFechaFormateada());
    }

    private void actualizarDisplayAnticipacion(TextView tv, int min) {
        if (min <= 0) tv.setText("Sin aviso previo");
        else tv.setText(min + " minutos antes");
    }

    public int getHoraSeleccionada() { return horaSeleccionada; }
    public long getFechaSeleccionadaMs() { return fechaSeleccionadaMs; }
    public int getAnticipacionSeleccionada() { return anticipacionSeleccionada; }
}
