package com.example.sanbotapp.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sanbotapp.recordatorio.Recordatorio;
import com.example.sanbotapp.recordatorio.RecordatorioRepository;
import com.example.sanbotapp.alarmas.RecordatorioPopupActivity;

public class RecordatorioAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int recordatorioId = intent.getIntExtra("recordatorio_id", -1);
        if (recordatorioId == -1) return;

        RecordatorioRepository repo = new RecordatorioRepository(context);
        Recordatorio r = repo.getById(recordatorioId);
        if (r == null) return;

        Intent popupIntent = new Intent(context, RecordatorioPopupActivity.class);
        popupIntent.putExtra(RecordatorioPopupActivity.EXTRA_RECORDATORIO_ID, recordatorioId);
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            context.startActivity(popupIntent);
        } catch (Exception e) {
            android.util.Log.e("RecordatorioReceiver", "Error lanzando popup: " + e.getMessage());
        }
    }
}
