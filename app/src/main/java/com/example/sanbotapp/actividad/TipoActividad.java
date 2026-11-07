package com.example.sanbotapp.actividad;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.sanbotapp.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Enumeración que centraliza las propiedades de cada tipo de actividad.
 * Evita el uso de switches repetitivos en el modelo y la UI.
 * Los textos visibles se resuelven desde recursos para soportar varios idiomas.
 */
public enum TipoActividad {
    MEDICACION(R.string.tipo_medicacion, "#4A90E2", R.drawable.ic_medicacion, 5),
    BEBER_AGUA(R.string.tipo_beber_agua, "#29B6C8", R.drawable.ic_agua, 5),
    COMER(R.string.tipo_comer, "#F07070", R.drawable.ic_comida, 30),
    PASEO_EJERCICIO(R.string.tipo_paseo_ejercicio, "#F5A623", R.drawable.ic_ejercicio, 45),
    JUEGOS(R.string.tipo_juegos, "#6BBF59", R.drawable.ic_puzzle, 30),
    ASEO(R.string.tipo_aseo, "#4DB6AC", R.drawable.ic_aseo, 15),
    LLAMADA_FAMILIAR(R.string.tipo_llamada_familiar, "#E9658B", R.drawable.ic_llamada, 20),
    IR_DORMIR(R.string.tipo_ir_dormir, "#9B79D4", R.drawable.ic_dormir, 10),
    OTROS(R.string.tipo_otros, "#9E9E9E", R.drawable.ic_calendario, 30);

    @StringRes private final int labelRes;
    private final String colorHex;
    private final int iconoRes;
    private final int duracionMinutos;

    public static class Paso {
        public final String texto;
        public final int iconoRes;
        public Paso(String texto, int iconoRes) {
            this.texto = texto;
            this.iconoRes = iconoRes;
        }
    }

    TipoActividad(@StringRes int labelRes, String colorHex, int iconoRes, int duracionMinutos) {
        this.labelRes = labelRes;
        this.colorHex = colorHex;
        this.iconoRes = iconoRes;
        this.duracionMinutos = duracionMinutos;
    }

    public String getLabel(Context context) { return context.getString(labelRes); }
    public String getColorHex() { return colorHex; }
    public int getIconoRes() { return iconoRes; }
    public int getDuracionMinutos() { return duracionMinutos; }

    public List<Paso> getPasos(Context context) {
        List<Paso> pasos = new ArrayList<>();
        switch (this) {
            case MEDICACION:
                pasos.add(paso(context, R.string.paso_medicacion_1, R.drawable.ic_aseo));
                pasos.add(paso(context, R.string.paso_medicacion_2, R.drawable.ic_medicacion));
                pasos.add(paso(context, R.string.paso_medicacion_3, R.drawable.ic_medicacion));
                break;
            case BEBER_AGUA:
                pasos.add(paso(context, R.string.paso_agua_1, R.drawable.ic_agua));
                pasos.add(paso(context, R.string.paso_agua_2, R.drawable.ic_agua));
                pasos.add(paso(context, R.string.paso_agua_3, R.drawable.ic_agua));
                break;
            case COMER:
                pasos.add(paso(context, R.string.paso_comer_1, R.drawable.ic_comida));
                pasos.add(paso(context, R.string.paso_comer_2, R.drawable.ic_comida));
                pasos.add(paso(context, R.string.paso_comer_3, R.drawable.ic_comida));
                pasos.add(paso(context, R.string.paso_comer_4, R.drawable.ic_agua));
                break;
            case PASEO_EJERCICIO:
                pasos.add(paso(context, R.string.paso_ejercicio_1, R.drawable.ic_ejercicio));
                pasos.add(paso(context, R.string.paso_ejercicio_2, R.drawable.ic_ejercicio));
                pasos.add(paso(context, R.string.paso_ejercicio_3, R.drawable.ic_ejercicio));
                pasos.add(paso(context, R.string.paso_ejercicio_4, R.drawable.ic_ejercicio));
                pasos.add(paso(context, R.string.paso_ejercicio_5, R.drawable.ic_ejercicio));
                break;
            case JUEGOS:
                pasos.add(paso(context, R.string.paso_juegos_1, R.drawable.ic_puzzle));
                pasos.add(paso(context, R.string.paso_juegos_2, R.drawable.ic_puzzle));
                pasos.add(paso(context, R.string.paso_juegos_3, R.drawable.ic_puzzle));
                break;
            case ASEO:
                pasos.add(paso(context, R.string.paso_aseo_1, R.drawable.ic_aseo));
                pasos.add(paso(context, R.string.paso_aseo_2, R.drawable.ic_aseo));
                pasos.add(paso(context, R.string.paso_aseo_3, R.drawable.ic_aseo));
                break;
            case LLAMADA_FAMILIAR:
                pasos.add(paso(context, R.string.paso_llamada_1, R.drawable.ic_llamada));
                pasos.add(paso(context, R.string.paso_llamada_2, R.drawable.ic_llamada));
                pasos.add(paso(context, R.string.paso_llamada_3, R.drawable.ic_llamada));
                break;
            case IR_DORMIR:
                pasos.add(paso(context, R.string.paso_dormir_1, R.drawable.ic_dormir));
                pasos.add(paso(context, R.string.paso_dormir_2, R.drawable.ic_dormir));
                pasos.add(paso(context, R.string.paso_dormir_3, R.drawable.ic_dormir));
                pasos.add(paso(context, R.string.paso_dormir_4, R.drawable.ic_dormir));
                break;
            default:
                pasos.add(paso(context, R.string.paso_otros_1, R.drawable.ic_calendario));
                break;
        }
        return pasos;
    }

    private static Paso paso(Context context, @StringRes int textoRes, int iconoRes) {
        return new Paso(context.getString(textoRes), iconoRes);
    }

    /**
     * Convierte un string de tipo (ej. de la DB) en el Enum correspondiente.
     */
    public static TipoActividad fromString(String tipo) {
        if (tipo == null) return OTROS;
        try {
            return valueOf(tipo.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return OTROS;
        }
    }
}
