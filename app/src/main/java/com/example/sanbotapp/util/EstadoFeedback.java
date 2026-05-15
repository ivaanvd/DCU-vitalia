package com.example.sanbotapp.util;

/**
 * Enumeración que centraliza los estados de feedback del hardware del robot.
 * Evita el uso de "magic strings" y reduce errores tipográficos.
 */
public enum EstadoFeedback {
    ESCUCHANDO,
    HABLANDO,
    ACIERTO,
    FALLO,
    SUMMARY_START,
    THINKING_START,
    MOURN,
    EXITO,
    CANCELADO,
    ERROR,
    ALARMA,
    CELEBRACION,
    SALUDO,
    IDLE
}
