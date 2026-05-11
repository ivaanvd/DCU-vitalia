package com.example.sanbotapp;

/**
 * Representa el campo del diálogo que está esperando entrada por voz.
 * NINGUNO significa que no hay ningún diálogo abierto o no se espera voz.
 */
public enum CampoVozEspera {
    NINGUNO,
    DESCRIPCION,   // campo texto libre (actividades y recordatorios)
    TITULO,        // campo título (solo recordatorios)
    HORA,          // "nueve y media", "las diez", etc.
    DIA_SEMANA,
    FECHA,
    TIPO,          // "lunes y miércoles", etc.
    CAMPO_EDITAR,
    ANTICIPACION
}