package com.example.sanbotapp.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.alarmas.ActividadPopupActivity;
import com.example.sanbotapp.alarmas.AlarmScheduler;

public class ActividadAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int actividadId = intent.getIntExtra("actividad_id", -1);
        if (actividadId == -1) return;

        ActividadRepository repo = new ActividadRepository(context);
        Actividad a = repo.getById(actividadId);
        
        // Solo disparar si está pendiente. 
        // Nota: Si es recurrente, el scheduler ya la habrá movido a la próxima fecha, 
        // pero aquí comprobamos el estado actual guardado.
        if (a == null || !a.getEstado().equals(Actividad.ESTADO_PENDIENTE)) return;

        // Intent para abrir el Popup directamente
        Intent popupIntent = new Intent(context, ActividadPopupActivity.class);
        popupIntent.putExtra(ActividadPopupActivity.EXTRA_ACTIVIDAD_ID, actividadId);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            context.startActivity(popupIntent);
        } catch (Exception e) {
            // En Android 10+, si la app está en background, esto puede fallar.
            // Pero como el usuario pidió quitar la notificación, intentamos lanzarlo.
            // Nota: Para que funcione en background sin notificación, se requiere el permiso SYSTEM_ALERT_WINDOW
            // o que la app esté en foreground.
            android.util.Log.e("ActividadAlarmReceiver", "No se pudo lanzar el popup desde background: " + e.getMessage());
        }

        // Reprogramar para la próxima ocurrencia (si es recurrente)
        AlarmScheduler.programarActividad(context, a);
    }
}