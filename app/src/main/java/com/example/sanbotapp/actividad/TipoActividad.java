package com.example.sanbotapp.actividad;

import com.example.sanbotapp.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Enumeración que centraliza las propiedades de cada tipo de actividad.
 * Evita el uso de switches repetitivos en el modelo y la UI.
 */
public enum TipoActividad {
    MEDICACION("MEDICACIÓN", "#4A90E2", R.drawable.ic_medicacion, 5),
    BEBER_AGUA("BEBER AGUA", "#29B6C8", R.drawable.ic_agua, 5),
    COMER("COMER", "#F07070", R.drawable.ic_comida, 30),
    PASEO_EJERCICIO("PASEO/EJERCICIO", "#F5A623", R.drawable.ic_ejercicio, 45),
    JUEGOS("JUEGOS", "#6BBF59", R.drawable.ic_puzzle, 30),
    ASEO("ASEO", "#4DB6AC", R.drawable.ic_aseo, 15),
    LLAMADA_FAMILIAR("LLAMADA FAMILIAR", "#E9658B", R.drawable.ic_llamada, 20),
    IR_DORMIR("IR A DORMIR", "#9B79D4", R.drawable.ic_dormir, 10),
    OTROS("OTRO", "#9E9E9E", R.drawable.ic_calendario, 30);

    private final String label;
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

    TipoActividad(String label, String colorHex, int iconoRes, int duracionMinutos) {
        this.label = label;
        this.colorHex = colorHex;
        this.iconoRes = iconoRes;
        this.duracionMinutos = duracionMinutos;
    }

    public String getLabel() { return label; }
    public String getColorHex() { return colorHex; }
    public int getIconoRes() { return iconoRes; }
    public int getDuracionMinutos() { return duracionMinutos; }

    public List<Paso> getPasos() {
        List<Paso> pasos = new ArrayList<>();
        switch (this) {
            case MEDICACION:
                pasos.add(new Paso("Lávate las manos", R.drawable.ic_aseo));
                pasos.add(new Paso("Ve a por la medicación", R.drawable.ic_medicacion));
                pasos.add(new Paso("Toma tu medicación", R.drawable.ic_medicacion));
                break;
            case BEBER_AGUA:
                pasos.add(new Paso("Ve a por un vaso de agua", R.drawable.ic_agua));
                pasos.add(new Paso("Bebe despacio, sin prisa", R.drawable.ic_agua));
                pasos.add(new Paso("Recuerda hidratarte cada hora", R.drawable.ic_agua));
                break;
            case COMER:
                pasos.add(new Paso("Prepara la mesa con calma", R.drawable.ic_comida));
                pasos.add(new Paso("Sirve tu comida", R.drawable.ic_comida));
                pasos.add(new Paso("Come despacio y mastica bien", R.drawable.ic_comida));
                pasos.add(new Paso("Bebe agua durante la comida", R.drawable.ic_agua));
                break;
            case PASEO_EJERCICIO:
                pasos.add(new Paso("Siéntate en una silla", R.drawable.ic_ejercicio));
                pasos.add(new Paso("Respira profundo", R.drawable.ic_ejercicio));
                pasos.add(new Paso("Gira el cuello suavemente", R.drawable.ic_ejercicio));
                pasos.add(new Paso("Mueve los codos", R.drawable.ic_ejercicio));
                pasos.add(new Paso("Levanta los brazos", R.drawable.ic_ejercicio));
                break;
            case JUEGOS:
                pasos.add(new Paso("Busca un lugar tranquilo", R.drawable.ic_puzzle));
                pasos.add(new Paso("Realiza el ejercicio propuesto", R.drawable.ic_puzzle));
                pasos.add(new Paso("Descansa un momento", R.drawable.ic_puzzle));
                break;
            case ASEO:
                pasos.add(new Paso("Prepara lo que necesitas", R.drawable.ic_aseo));
                pasos.add(new Paso("Realiza tu higiene personal", R.drawable.ic_aseo));
                pasos.add(new Paso("Recoge y deja todo ordenado", R.drawable.ic_aseo));
                break;
            case LLAMADA_FAMILIAR:
                pasos.add(new Paso("Piensa en alguien a quien llamar", R.drawable.ic_llamada));
                pasos.add(new Paso("Marca el número", R.drawable.ic_llamada));
                pasos.add(new Paso("Saluda y pregunta cómo están", R.drawable.ic_llamada));
                break;
            case IR_DORMIR:
                pasos.add(new Paso("Apaga las luces de la casa", R.drawable.ic_dormir));
                pasos.add(new Paso("Prepara tu ropa para mañana", R.drawable.ic_dormir));
                pasos.add(new Paso("Ponte cómodo en la cama", R.drawable.ic_dormir));
                pasos.add(new Paso("Respira profundo y descansa", R.drawable.ic_dormir));
                break;
            default:
                pasos.add(new Paso("Realiza la actividad con calma", R.drawable.ic_calendario));
                break;
        }
        return pasos;
    }

    /**
     * Convierte un string de tipo (ej. de la DB) en el Enum correspondiente.
     */
    public static TipoActividad fromString(String tipo) {
        if (tipo == null) return OTROS;
        try {
            return valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTROS;
        }
    }
}
