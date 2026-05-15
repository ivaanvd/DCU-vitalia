package com.example.sanbotapp.actividad;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.sanbotapp.R;
import java.util.List;

/**
 * Utilidades de UI comunes para Actividades.
 * Evita la duplicación de lógica de diálogos entre MainActivity y ActividadesActivity.
 */
public class ActividadUIUtils {

    public static void mostrarDialogoDetalle(Activity activity, Actividad a) {
        View dv = LayoutInflater.from(activity).inflate(R.layout.dialog_detalle_actividad, null);
        
        // Círculo de color
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(a.getColorHex()));
        dv.findViewById(R.id.frameEmojiDetAct).setBackground(bg);
        
        // Icono
        ((ImageView) dv.findViewById(R.id.tvEmojiDetAct)).setImageResource(a.getIconoRes());
        
        // Hora y Tipo
        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());
        
        // Etiqueta dinámica de descripción
        TextView tvLabelDesc = dv.findViewById(R.id.tvLabelDescDetAct);
        if (tvLabelDesc != null) {
            String tipo = a.getTipo();
            if (Actividad.TIPO_MEDICACION.equals(tipo)) tvLabelDesc.setText("Medicamento:");
            else if (Actividad.TIPO_COMER.equals(tipo)) tvLabelDesc.setText("Comida:");
            else if (Actividad.TIPO_LLAMADA_FAMILIAR.equals(tipo)) tvLabelDesc.setText("Llamar a:");
            else if (Actividad.TIPO_PASEO_EJERCICIO.equals(tipo)) tvLabelDesc.setText("Lugar del paseo:");
            else tvLabelDesc.setText("Detalles:");
        }

        // Descripción
        String desc = (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) 
                ? a.getDescripcion().toUpperCase() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText(desc);

        // Días de la semana (Solo los programados, en formato lista de píldoras grandes)
        LinearLayout containerDias = dv.findViewById(R.id.containerDiasDetAct);
        if (containerDias != null) {
            containerDias.setOrientation(LinearLayout.HORIZONTAL);
            containerDias.setGravity(android.view.Gravity.CENTER);
            List<Integer> dias = a.getDiasSemana();
            String[] nombresDias = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
            final int[] VALORES_DIA = {2, 3, 4, 5, 6, 7, 1};

            if (dias == null || dias.isEmpty() || dias.size() == 7) {
                agregarPillDia(activity, containerDias, "TODOS LOS DÍAS");
            } else {
                for (int i = 0; i < VALORES_DIA.length; i++) {
                    if (dias.contains(VALORES_DIA[i])) {
                        agregarPillDia(activity, containerDias, nombresDias[i]);
                    }
                }
            }
        }



        AlertDialog dialog = new AlertDialog.Builder(activity).setView(dv).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public static void agregarPillDia(Activity activity, LinearLayout container, String texto) {
        TextView tv = new TextView(activity);
        tv.setText(texto);
        tv.setTextSize(20); // Un poco más pequeño para que quepan en fila
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(android.view.Gravity.CENTER);
        
        int paddingH = (int) (14 * activity.getResources().getDisplayMetrics().density);
        int paddingV = (int) (8 * activity.getResources().getDisplayMetrics().density);
        tv.setPadding(paddingH, paddingV, paddingH, paddingV);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(100); // Píldora
        bg.setColor(Color.parseColor("#1C1C1E"));
        tv.setBackground(bg);
        
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins((int) (4 * activity.getResources().getDisplayMetrics().density), 0, (int) (4 * activity.getResources().getDisplayMetrics().density), 0);
        tv.setLayoutParams(params);
        
        container.addView(tv);
    }


}
