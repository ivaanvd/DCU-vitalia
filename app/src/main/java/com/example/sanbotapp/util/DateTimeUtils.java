package com.example.sanbotapp.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TextView;
import java.util.Calendar;

public class DateTimeUtils {

    public interface OnTimeSelectedListener {
        void onTimeSelected(int hourOfDay, int minute, int totalMinutes);
    }

    public interface OnDateSelectedListener {
        void onDateSelected(long timestampMs);
    }

    /**
     * Muestra un TimePickerDialog y devuelve el resultado mediante un listener.
     */
    public static void mostrarTimePicker(Context context, int minutosIniciales, OnTimeSelectedListener listener) {
        int h = minutosIniciales / 60;
        int m = minutosIniciales % 60;
        
        new TimePickerDialog(context, android.R.style.Theme_Material_Dialog, (view, hourOfDay, minute) -> {
            if (listener != null) {
                listener.onTimeSelected(hourOfDay, minute, hourOfDay * 60 + minute);
            }
        }, h, m, true).show();
    }

    /**
     * Muestra un DatePickerDialog y devuelve el resultado mediante un listener.
     */
    public static void mostrarDatePicker(Context context, long fechaInicialMs, OnDateSelectedListener listener) {
        Calendar cal = Calendar.getInstance();
        if (fechaInicialMs > 0) {
            cal.setTimeInMillis(fechaInicialMs);
        }
        
        new DatePickerDialog(context, (view, year, month, day) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            if (listener != null) {
                listener.onDateSelected(c.getTimeInMillis());
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Formatea minutos a HH:mm
     */
    public static String formatearHora(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }

    /**
     * Devuelve el timestamp (ms) del inicio del día de hoy (00:00:00.000).
     */
    public static long fechaHoyInicio() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
