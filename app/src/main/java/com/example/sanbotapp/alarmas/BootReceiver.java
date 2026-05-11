package com.example.sanbotapp.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.alarmas.AlarmScheduler;

import java.util.List;

// Las alarmas se pierden si el teléfono se apaga; este receiver las reprograma
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        ActividadRepository repo = new ActividadRepository(context);
        List<Actividad> todas = repo.getAll();

        for (Actividad a : todas) {
            AlarmScheduler.programarActividad(context, a);
        }
    }
}