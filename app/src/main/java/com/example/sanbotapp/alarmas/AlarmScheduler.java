package com.example.sanbotapp.alarmas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.recordatorio.Recordatorio;

import java.util.Calendar;

public class AlarmScheduler {

    private static final int RECORDATORIO_ID_OFFSET = 10000;

    public static void programarActividad(Context context, Actividad a) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ActividadAlarmReceiver.class);
        intent.putExtra("actividad_id", a.getId());
        intent.putExtra("actividad_tipo", a.getTipo());
        intent.putExtra("actividad_label", a.getTipoLabel());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                a.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, a.getHoraMinutos() / 60);
        calendar.set(Calendar.MINUTE, a.getHoraMinutos() % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

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

    public static void programarRecordatorio(Context context, Recordatorio r) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, RecordatorioAlarmReceiver.class);
        intent.putExtra("recordatorio_id", r.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                RECORDATORIO_ID_OFFSET + r.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(r.getFechaMs());
        calendar.set(Calendar.HOUR_OF_DAY, r.getHoraMinutos() / 60);
        calendar.set(Calendar.MINUTE, r.getHoraMinutos() % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) return;

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

    public static void cancelarRecordatorio(Context context, int recordatorioId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RecordatorioAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, RECORDATORIO_ID_OFFSET + recordatorioId, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}