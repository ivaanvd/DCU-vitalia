package com.example.sanbotapp.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.recordatorio.Recordatorio;
import com.example.sanbotapp.recordatorio.RecordatorioRepository;

public class RecordatorioAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int recordatorioId = intent.getIntExtra("recordatorio_id", -1);
        if (recordatorioId == -1) return;

        RecordatorioRepository repo = new RecordatorioRepository(context);
        Recordatorio r = null;
        for (Recordatorio item : repo.getAll()) {
            if (item.getId() == recordatorioId) {
                r = item;
                break;
            }
        }
        if (r == null) return;

        // Lanzar la Activity del Popup de Recordatorio
        Intent popupIntent = new Intent(context, RecordatorioPopupActivity.class);
        popupIntent.putExtra("recordatorio_id", recordatorioId);
        popupIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        context.startActivity(popupIntent);
    }
}
