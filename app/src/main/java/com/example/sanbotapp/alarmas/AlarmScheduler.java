package com.example.sanbotapp.alarmas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.recordatorio.Recordatorio;

import java.util.Calendar;
import java.util.List;

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

        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, a.getHoraMinutos() / 60);
        target.set(Calendar.MINUTE, a.getHoraMinutos() % 60);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // Si la lista de días está vacía, se asume que es para hoy (o mañana si ya pasó)
        List<Integer> dias = a.getDiasSemana();
        if (dias == null || dias.isEmpty()) {
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
        } else {
            // Buscar el próximo día válido (incluyendo hoy si aún no ha pasado la hora)
            int diasSumados = 0;
            while (diasSumados < 8) {
                int diaActual = target.get(Calendar.DAY_OF_WEEK);
                if (dias.contains(diaActual) && target.after(now)) {
                    // Encontrado
                    break;
                }
                target.add(Calendar.DAY_OF_YEAR, 1);
                diasSumados++;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    target.getTimeInMillis(),
                    pendingIntent
            );
        }

        android.util.Log.d("AlarmScheduler", "Programada actividad " + a.getId() + " para: " + target.getTime().toString());
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

        // Aplicar antelación
        if (r.getAnticipacionMinutos() > 0) {
            calendar.add(Calendar.MINUTE, -r.getAnticipacionMinutos());
        }

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

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "alarmas_channel",
                    "Alertas de Actividades",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Canal para las alarmas del robot");
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            android.app.NotificationManager manager = context.getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}