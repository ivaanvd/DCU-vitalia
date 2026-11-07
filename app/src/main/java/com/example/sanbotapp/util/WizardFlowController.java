package com.example.sanbotapp.util;

import android.content.Context;

import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

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
    public static String getInstruccionCampo(Context ctx, CampoVozEspera campo, String contextoDinamico) {
        switch (campo) {
            case TIPO: return ctx.getString(R.string.wiz_instr_tipo);
            case DESCRIPCION: return contextoDinamico != null ? contextoDinamico : ctx.getString(R.string.wiz_instr_descripcion);
            case TITULO: return ctx.getString(R.string.wiz_instr_titulo);
            case HORA: return ctx.getString(R.string.wiz_instr_hora);
            case FECHA: return ctx.getString(R.string.wiz_instr_fecha);
            case DIA_SEMANA: return ctx.getString(R.string.wiz_instr_dias);
            case ANTICIPACION: return ctx.getString(R.string.wiz_instr_anticipacion);
            case CONFIRMACION_CAMPO: return ctx.getString(R.string.wiz_instr_confirmacion, contextoDinamico);
            case RESUMEN_FINAL: return ctx.getString(R.string.wiz_instr_resumen, contextoDinamico);
            case ELECCION_EDICION: return ctx.getString(R.string.wiz_instr_eleccion);
            default: return ctx.getString(R.string.wiz_instr_toca_cabeza);
        }
    }
}
