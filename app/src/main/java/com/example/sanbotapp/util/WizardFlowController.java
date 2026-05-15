package com.example.sanbotapp.util;

import com.example.sanbotapp.CampoVozEspera;

/**
 * Controlador central para el flujo de conversación del asistente (Wizard).
 * Define el orden de los campos y las instrucciones que el robot debe decir.
 */
public class WizardFlowController {

    /**
     * Devuelve el siguiente campo lógico en el flujo de creación.
     */
    public static CampoVozEspera getSiguienteCampo(CampoVozEspera actual, boolean isEditingFromSummary, boolean isActividad) {
        if (isEditingFromSummary) return CampoVozEspera.RESUMEN_FINAL;

        if (isActividad) {
            switch (actual) {
                case TIPO: return CampoVozEspera.DESCRIPCION;
                case DESCRIPCION: return CampoVozEspera.HORA;
                case HORA: return CampoVozEspera.DIA_SEMANA;
                default: return CampoVozEspera.RESUMEN_FINAL;
            }
        } else {
            // Recordatorios
            switch (actual) {
                case TITULO: return CampoVozEspera.HORA;
                case HORA: return CampoVozEspera.FECHA;
                case FECHA: return CampoVozEspera.ANTICIPACION;
                default: return CampoVozEspera.RESUMEN_FINAL;
            }
        }
    }

    /**
     * Devuelve el campo anterior en el flujo (para el botón 'Atrás').
     */
    public static CampoVozEspera getCampoAnterior(CampoVozEspera actual, boolean isActividad) {
        if (isActividad) {
            switch (actual) {
                case DESCRIPCION: return CampoVozEspera.TIPO;
                case HORA: return CampoVozEspera.DESCRIPCION;
                case DIA_SEMANA: return CampoVozEspera.HORA;
                default: return CampoVozEspera.NINGUNO;
            }
        } else {
            switch (actual) {
                case HORA: return CampoVozEspera.TITULO;
                case FECHA: return CampoVozEspera.HORA;
                case ANTICIPACION: return CampoVozEspera.FECHA;
                default: return CampoVozEspera.NINGUNO;
            }
        }
    }

    /**
     * Devuelve la instrucción que el robot debe decir al entrar en un campo.
     */
    public static String getInstruccionCampo(CampoVozEspera campo, String contextoDinamico) {
        switch (campo) {
            case TIPO: return "¿Qué tipo de actividad es? Por ejemplo, medicación o paseo.";
            case DESCRIPCION: return contextoDinamico != null ? contextoDinamico : "¿Puedes darme más detalles?";
            case TITULO: return "¿Qué título le ponemos al recordatorio?";
            case HORA: return "¿A qué hora?";
            case FECHA: return "¿Para qué fecha?";
            case DIA_SEMANA: return "¿Qué días de la semana?";
            case ANTICIPACION: return "¿Con cuántos minutos de antelación quieres que te avise?";
            case CONFIRMACION_CAMPO: return "He anotado '" + contextoDinamico + "'. ¿Es correcto? Pulsa el botón CONFIRMAR para guardar o REPETIR para volver a intentarlo.";
            case RESUMEN_FINAL: return "He terminado. " + contextoDinamico + ". ¿Guardamos o quieres cambiar algo?";
            case ELECCION_EDICION: return "¿Qué campo quieres cambiar?";
            default: return "Toca mi cabeza cuando estés listo.";
        }
    }
}
