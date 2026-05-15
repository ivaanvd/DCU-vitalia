package com.example.sanbotapp.actividad;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.sanbotapp.R;
import java.util.List;

public class ActividadAdapter {

    public interface OnActividadClickListener {
        void onActividadClick(Actividad a);
        void onEditarClick(Actividad a);
        void onEliminarClick(Actividad a);
    }

    private final Context context;
    private final OnActividadClickListener listener;

    public ActividadAdapter(Context context, OnActividadClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public View getView(Actividad a, android.view.ViewGroup parent) {
        View item = LayoutInflater.from(context).inflate(R.layout.item_actividad, parent, false);
        
        ((TextView) item.findViewById(R.id.tvHoraItem)).setText(a.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvTipoItem)).setText(a.getTipoLabel());
        
        ImageView ivIcono = item.findViewById(R.id.ivIconoItem);
        if (ivIcono != null) {
            ivIcono.setImageResource(a.getIconoRes());
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor(a.getColorHex()));
        bg.setCornerRadius(dpToPx(14));
        item.setBackground(bg);

        item.setOnClickListener(v -> listener.onActividadClick(a));

        View btnEditar = item.findViewById(R.id.btnEditarItem);
        if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado())) {
            btnEditar.setVisibility(View.GONE);
        } else {
            btnEditar.setOnClickListener(v -> listener.onEditarClick(a));
        }

        item.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> listener.onEliminarClick(a));

        return item;
    }

    private float dpToPx(int dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
