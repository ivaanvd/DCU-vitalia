package com.example.sanbotapp.actividad;

import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Actividad {

    // ── Tipos ─────────────────────────────────────────────────────────────────
    public static final String TIPO_MEDICACION       = "MEDICACION";
    public static final String TIPO_BEBER_AGUA       = "BEBER_AGUA";
    public static final String TIPO_COMER            = "COMER";
    public static final String TIPO_PASEO_EJERCICIO  = "PASEO_EJERCICIO";
    public static final String TIPO_JUEGOS           = "JUEGOS";
    public static final String TIPO_ASEO             = "ASEO";
    public static final String TIPO_LLAMADA_FAMILIAR = "LLAMADA_FAMILIAR";
    public static final String TIPO_IR_DORMIR        = "IR_DORMIR";
    public static final String TIPO_OTROS            = "OTROS";

    // ── Estados ───────────────────────────────────────────────────────────────
    public static final String ESTADO_PENDIENTE  = "PENDIENTE";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_POSPUESTA  = "POSPUESTA";

    // ── Campos ────────────────────────────────────────────────────────────────
    private int           id;
    private String        tipo;
    private String        estado;
    private int           horaMinutos;         // minutos desde medianoche
    private List<Integer> diasSemana;          // Calendar.MONDAY=2 … SUNDAY=1
    private String        descripcion;
    private int           idActividadOriginal; // >0 si fue creada al posponer
    private boolean       creadaPorSistema;    // true = no se puede volver a posponer

    // ── Constructores ─────────────────────────────────────────────────────────

    /** Constructor vacío para deserialización (JSON, Room, etc.). */
    public Actividad() {}

    /**
     * Constructor principal para creación manual.
     * Estado inicial: PENDIENTE. No creada por sistema.
     */
    public Actividad(int id, String tipo, int horaMinutos, String descripcion) {
        this.id                  = id;
        this.tipo                = tipo;
        this.horaMinutos         = horaMinutos;
        this.descripcion         = descripcion;
        this.estado              = ESTADO_PENDIENTE;
        this.diasSemana          = new ArrayList<>();
        this.idActividadOriginal = 0;
        this.creadaPorSistema    = false;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int           getId()                  { return id; }
    public String        getTipo()                { return tipo; }
    public String        getEstado()              { return estado; }
    public int           getHoraMinutos()         { return horaMinutos; }
    public List<Integer> getDiasSemana()          { return diasSemana; }
    public String        getDescripcion()         { return descripcion; }
    public int           getIdActividadOriginal() { return idActividadOriginal; }
    public boolean       isCreadaPorSistema()     { return creadaPorSistema; }

    public void setId(int id)                                 { this.id = id; }
    public void setTipo(String tipo)                          { this.tipo = tipo; }
    public void setEstado(String estado)                      { this.estado = estado; }
    public void setHoraMinutos(int horaMinutos)               { this.horaMinutos = horaMinutos; }
    public void setDiasSemana(List<Integer> diasSemana)       { this.diasSemana = diasSemana; }
    public void setDescripcion(String descripcion)            { this.descripcion = descripcion; }
    public void setIdActividadOriginal(int id)                { this.idActividadOriginal = id; }
    public void setCreadaPorSistema(boolean creadaPorSistema) { this.creadaPorSistema = creadaPorSistema; }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /** Devuelve la hora en formato HH:mm. */
    public String getHoraFormateada() {
        return String.format("%02d:%02d", horaMinutos / 60, horaMinutos % 60);
    }

    /**
     * Devuelve true si la actividad está programada para hoy.
     * Si la lista de días está vacía se asume que aplica todos los días.
     */
    public boolean coincideHoy() {
        if (diasSemana == null || diasSemana.isEmpty()) return true;
        int hoy = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return diasSemana.contains(hoy);
    }

    /** Devuelve el color de fondo asociado al tipo, en formato #RRGGBB. */
    public String getColorHex() {
        return TipoActividad.fromString(tipo).getColorHex();
    }

    /** Devuelve la etiqueta visible para el usuario correspondiente al tipo. */
    public String getTipoLabel() {
        return TipoActividad.fromString(tipo).getLabel();
    }

    /**
     * Devuelve el drawable resource id del icono asociado al tipo.
     */
    public int getIconoRes() {
        return TipoActividad.fromString(tipo).getIconoRes();
    }

    /**
     * Devuelve la duración estimada en minutos según el tipo.
     */
    public int getDuracionMinutos() {
        return TipoActividad.fromString(tipo).getDuracionMinutos();
    }
}