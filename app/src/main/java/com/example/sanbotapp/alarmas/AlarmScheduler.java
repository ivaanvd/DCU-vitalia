package com.example.sanbotapp.alarmas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.sanbotapp.actividad.Actividad;

import java.util.Calendar;

public class AlarmScheduler {

    public static void programarActividad(Context context, Actividad a) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ActividadAlarmReceiver.class);
        intent.putExtra("actividad_id", a.getId());
        intent.putExtra("actividad_tipo", a.getTipo());
        intent.putExtra("actividad_label", a.getTipoLabel());

        // Cada actividad tiene su propio PendingIntent por ID
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                a.getId(), // requestCode único por actividad
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calcular milisegundos para hoy a la hora programada
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, a.getHoraMinutos() / 60);
        calendar.set(Calendar.MINUTE, a.getHoraMinutos() % 60);
        calendar.set(Calendar.SECOND, 0);

        // setExactAndAllowWhileIdle: funciona aunque el teléfono esté en Doze
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    public static void cancelarActividad(Context context, int actividadId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ActividadAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, actividadId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}