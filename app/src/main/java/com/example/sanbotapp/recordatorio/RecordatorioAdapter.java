package com.example.sanbotapp.recordatorio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.sanbotapp.R;

import java.util.List;

/**
 * Adaptador para renderizar la lista de recordatorios.
 * Extraído de RecordatoriosActivity para cumplir con SRP.
 */
public class RecordatorioAdapter {

    public interface OnRecordatorioClickListener {
        void onRecordatorioClick(Recordatorio r);
        void onEditarClick(Recordatorio r);
        void onEliminarClick(Recordatorio r);
    }

    private final Context context;
    private final OnRecordatorioClickListener listener;

    public RecordatorioAdapter(Context context, OnRecordatorioClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Crea y configura la vista para un ítem de recordatorio.
     */
    public View getView(final Recordatorio r, ViewGroup parent) {
        View item = LayoutInflater.from(context)
                .inflate(R.layout.item_recordatorio, parent, false);

        TextView tvHora = item.findViewById(R.id.tvHoraItemRec);
        TextView tvFecha = item.findViewById(R.id.tvFechaItemRec);
        TextView tvTitulo = item.findViewById(R.id.tvTituloItemRec);

        if (tvHora != null) tvHora.setText(r.getHoraFormateada());
        if (tvFecha != null) tvFecha.setText(r.getFechaFormateada());
        if (tvTitulo != null) {
            String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty())
                    ? r.getTitulo().toUpperCase()
                    : "SIN TÍTULO";
            tvTitulo.setText(titulo);
        }

        item.setOnClickListener(v -> listener.onRecordatorioClick(r));
        
        View btnEditar = item.findViewById(R.id.btnEditarItemRec);
        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> listener.onEditarClick(r));
        }

        View btnEliminar = item.findViewById(R.id.btnEliminarItemRec);
        if (btnEliminar != null) {
            btnEliminar.setOnClickListener(v -> listener.onEliminarClick(r));
        }

        return item;
    }
}
