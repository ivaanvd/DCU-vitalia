package com.example.sanbotapp.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.alarmas.ActividadPopupActivity;
public class ActividadAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int actividadId = intent.getIntExtra("actividad_id", -1);
        if (actividadId == -1) return;

        ActividadRepository repo = new ActividadRepository(context);
        Actividad a = repo.getById(actividadId);
        if (a == null || !a.getEstado().equals(Actividad.ESTADO_PENDIENTE)) return;

        // Lanzar la Activity directamente — sin notificación
        Intent popupIntent = new Intent(context, ActividadPopupActivity.class);
        popupIntent.putExtra(ActividadPopupActivity.EXTRA_ACTIVIDAD_ID, actividadId);
        popupIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        context.startActivity(popupIntent);
    }
}