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
    private int    anticipacionMinutos = 10; // minutos de antelación

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
    public Recordatorio(int id, String titulo, int horaMinutos, long fechaMs, String descripcion, int anticipacionMinutos) {
        this.id          = id;
        this.titulo      = titulo;
        this.horaMinutos = horaMinutos;
        this.fechaMs     = fechaMs;
        this.descripcion = descripcion;
        this.anticipacionMinutos = anticipacionMinutos;
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
    public void setAnticipacionMinutos(int a){ this.anticipacionMinutos = a; }

    public int getAnticipacionMinutos()      { return anticipacionMinutos; }

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
        return inicioDelDia(fechaMs) >= inicioDelDia(System.currentTimeMillis());
    }

    /** True si el día y la hora del recordatorio ya han pasado (su alarma no sonaría). */
    public boolean yaHaPasado(long ahoraMs) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(inicioDelDia(fechaMs));
        c.set(java.util.Calendar.HOUR_OF_DAY, horaMinutos / 60);
        c.set(java.util.Calendar.MINUTE, horaMinutos % 60);
        return c.getTimeInMillis() <= ahoraMs;
    }

    /**
     * Valores iniciales de un recordatorio nuevo: la próxima hora en punto
     * (pasada la medianoche cae en el día siguiente), para que nunca nazca en el pasado.
     */
    public static Recordatorio nuevoPorDefecto(long ahoraMs) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ahoraMs);
        c.add(java.util.Calendar.HOUR_OF_DAY, 1);
        Recordatorio r = new Recordatorio();
        r.setHoraMinutos(c.get(java.util.Calendar.HOUR_OF_DAY) * 60);
        r.setFechaMs(inicioDelDia(c.getTimeInMillis()));
        return r;
    }

    private static long inicioDelDia(long ms) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
