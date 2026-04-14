package com.example.sanbotapp.recordatorio;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Recordatorio: evento puntual para un día y hora concretos.
 * Ej. Cita médica, Comida familiar.
 */
public class Recordatorio {

    private int    id;
    private String titulo;
    private int    horaMinutos;  // minutos desde medianoche
    private long   fechaMs;      // fecha en milisegundos (solo día, sin hora)
    private String descripcion;

    // ── Constructores ─────────────────────────────────────────────────────────
    
    /*
     * Pre: Se instancia sin datos por defecto (normalmente desde JSON)
     * Post: Retorna el espacio base del objeto nulo
     */
    public Recordatorio() {}

    /*
     * Pre: Se requieren los datos básicos del evento eventual programado
     * Post: Inicializa completamente la entidad Recordatorio puntual a enviar a memoria de BD
     */
    public Recordatorio(int id, String titulo, int horaMinutos, long fechaMs, String descripcion) {
        this.id          = id;
        this.titulo      = titulo;
        this.horaMinutos = horaMinutos;
        this.fechaMs     = fechaMs;
        this.descripcion = descripcion;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    getId()          { return id; }
    public String getTitulo()      { return titulo; }
    public int    getHoraMinutos() { return horaMinutos; }
    public long   getFechaMs()     { return fechaMs; }
    public String getDescripcion() { return descripcion; }

    public void setId(int id)                { this.id = id; }
    public void setTitulo(String titulo)     { this.titulo = titulo; }
    public void setHoraMinutos(int h)        { this.horaMinutos = h; }
    public void setFechaMs(long fechaMs)     { this.fechaMs = fechaMs; }
    public void setDescripcion(String desc)  { this.descripcion = desc; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /*
     * Pre: Horario cargado desde la BD u originado manual
     * Post: Genera cadena simple legible humana HH:mm en interfaz usuario nativa de Java
     */
    public String getHoraFormateada() {
        return String.format("%02d:%02d", horaMinutos / 60, horaMinutos % 60);
    }

    /*
     * Pre: Long timestamp existente en registro general de sistema
     * Post: Traspone el String legible usando configuración idiomática simple formato Locale Español
     */
    public String getFechaFormateada() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "ES"));
        return sdf.format(new Date(fechaMs));
    }

    /*
     * Pre: Requiere filtrar listas anticuadas comparando con hoy
     * Post: Devuelve True si la fecha del recordatorio es hoy o en el futuro (comparando solo por día, ignorando la hora).
     */
    public boolean esFuturoOHoy() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(fechaMs);
        c.set(java.util.Calendar.HOUR_OF_DAY, horaMinutos / 60);
        c.set(java.util.Calendar.MINUTE, horaMinutos % 60);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);

        return c.getTimeInMillis() > System.currentTimeMillis();
    }
}
