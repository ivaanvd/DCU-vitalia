package com.example.sanbotapp.actividad;

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

    // ── Estados ───────────────────────────────────────────────────────────────
    public static final String ESTADO_PENDIENTE  = "PENDIENTE";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_POSPUESTA  = "POSPUESTA";

    // ── Campos ────────────────────────────────────────────────────────────────
    private int          id;
    private String       tipo;
    private String       estado;
    private int          horaMinutos;          // minutos desde medianoche
    private List<Integer> diasSemana;          // Calendar.MONDAY=2 … SUNDAY=1
    private String       descripcion;
    private int          idActividadOriginal;  // >0 si fue creada por sistema al posponer
    private boolean      creadaPorSistema;     // true = generada al posponer, no se puede volver a posponer

    // ── Constructores ─────────────────────────────────────────────────────────
    
    /*
     * Pre: Se instancia sin datos por defecto (normalmente desde JSON)
     * Post: Retorna el espacio base del objeto vacío
     */
    public Actividad() {}

    /*
     * Pre: Instanciación manual con parámetros conocidos requeridos
     * Post: Prepara todos los campos, asumiendo estado inicial 'pendiente' y no proveniente del sistema
     */
    public Actividad(int id, String tipo, int horaMinutos, String descripcion) {
        this.id           = id;
        this.tipo         = tipo;
        this.horaMinutos  = horaMinutos;
        this.descripcion  = descripcion;
        this.estado       = ESTADO_PENDIENTE;
        this.diasSemana   = new ArrayList<>();
        this.idActividadOriginal = 0;
        this.creadaPorSistema    = false;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int           getId()                    { return id; }
    public String        getTipo()                  { return tipo; }
    public String        getEstado()                { return estado; }
    public int           getHoraMinutos()           { return horaMinutos; }
    public List<Integer> getDiasSemana()            { return diasSemana; }
    public String        getDescripcion()           { return descripcion; }
    public int           getIdActividadOriginal()   { return idActividadOriginal; }
    public boolean       isCreadaPorSistema()       { return creadaPorSistema; }

    public void setId(int id)                                    { this.id = id; }
    public void setTipo(String tipo)                             { this.tipo = tipo; }
    public void setEstado(String estado)                         { this.estado = estado; }
    public void setHoraMinutos(int horaMinutos)                  { this.horaMinutos = horaMinutos; }
    public void setDiasSemana(List<Integer> diasSemana)          { this.diasSemana = diasSemana; }
    public void setDescripcion(String descripcion)               { this.descripcion = descripcion; }
    public void setIdActividadOriginal(int id)                   { this.idActividadOriginal = id; }
    public void setCreadaPorSistema(boolean creadaPorSistema)    { this.creadaPorSistema = creadaPorSistema; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /*
     * Pre: Horario en la clase instanciado adecuadamente con entero de minutos
     * Post: Devuelve formato legible visual estándar HH:mm
     */
    public String getHoraFormateada() {
        return String.format("%02d:%02d", horaMinutos / 60, horaMinutos % 60);
    }

    /*
     * Pre: Instancia con listas de días asignadas
     * Post: Devuelve true si la lista intercepta con el día del entorno de ejecución de la máquina actual
     */
    public boolean coincideHoy() {
        if (diasSemana == null || diasSemana.isEmpty()) return true;
        int hoy = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return diasSemana.contains(hoy);
    }

    /*
     * Pre: Tipo pre-condicionado existente de la clase
     * Post: Extrae el valor String para asignar un color de background amigable de acuerdo a la lógica material
     */
    public String getColorHex() {
        switch (tipo) {
            case TIPO_MEDICACION:       return "#4A90E2"; // azul  (igual que imagen)
            case TIPO_BEBER_AGUA:       return "#29B6C8"; // cian
            case TIPO_COMER:            return "#F07070"; // salmón/rojo (igual que imagen)
            case TIPO_PASEO_EJERCICIO:  return "#F5A623"; // naranja
            case TIPO_JUEGOS:           return "#6BBF59"; // verde (igual que imagen)
            case TIPO_ASEO:             return "#4DB6AC"; // verde azulado
            case TIPO_LLAMADA_FAMILIAR: return "#E9658B"; // rosa
            case TIPO_IR_DORMIR:        return "#9B79D4"; // morado
            default:                    return "#9E9E9E";
        }
    }

    /*
     * Pre: Tipo guardado en backend interno técnico
     * Post: Lo evalúa y entrega String con descripción legible al usuario formal en panel principal
     */
    public String getTipoLabel() {
        switch (tipo) {
            case TIPO_MEDICACION:       return "MEDICACIÓN";
            case TIPO_BEBER_AGUA:       return "BEBER AGUA";
            case TIPO_COMER:            return "COMER";
            case TIPO_PASEO_EJERCICIO:  return "PASEO/EJERCICIO";
            case TIPO_JUEGOS:           return "JUEGOS";
            case TIPO_ASEO:             return "ASEO";
            case TIPO_LLAMADA_FAMILIAR: return "LLAMADA FAMILIAR";
            case TIPO_IR_DORMIR:        return "IR A DORMIR";
            default:                    return "OTRO";
        }
    }

    /*
     * Pre: Tipo de actividad asignado abstractamente
     * Post: Retorna valor aproximado estándar recomendado (hardcoded) de uso para esa labor temporalmente
     */
    public int getDuracionMinutos() {
        switch (tipo) {
            case TIPO_MEDICACION:       return 5;
            case TIPO_BEBER_AGUA:       return 5;
            case TIPO_COMER:            return 30;
            case TIPO_PASEO_EJERCICIO:  return 45;
            case TIPO_JUEGOS:           return 30;
            case TIPO_ASEO:             return 15;
            case TIPO_LLAMADA_FAMILIAR: return 20;
            case TIPO_IR_DORMIR:        return 10;
            default:                    return 30;
        }
    }
}