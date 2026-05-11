package com.example.sanbotapp.alarmas;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.recordatorio.Recordatorio;
import com.example.sanbotapp.recordatorio.RecordatorioRepository;

public class RecordatorioPopupActivity extends BaseActivity {

    public static final String EXTRA_RECORDATORIO_ID = "recordatorio_id";

    private Recordatorio recordatorio;
    private android.media.Ringtone alarmSound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Encender pantalla
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON    |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED  |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_recordatorio_popup);

        int id = getIntent().getIntExtra(EXTRA_RECORDATORIO_ID, -1);
        RecordatorioRepository repo = new RecordatorioRepository(this);
        recordatorio = repo.getById(id);

        if (recordatorio == null) {
            finish();
            return;
        }

        setupTopBackBanner("Recordatorio");
        renderizar();
        iniciarAlarma();
    }

    private void iniciarAlarma() {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
            alarmSound = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (alarmSound != null) {
                alarmSound.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alarmSound != null && alarmSound.isPlaying()) {
            alarmSound.stop();
        }
    }

    @Override
    protected void onMainServiceConnected() {
        super.onMainServiceConnected(); // CRÍTICO: inicializa los controladores del robot
        if (recordatorio != null) {
            hablarOSimular("Tienes un recordatorio: " + recordatorio.getTitulo());
        }
    }

    private void renderizar() {
        ((TextView) findViewById(R.id.tvPopupTituloRec)).setText(recordatorio.getTitulo().toUpperCase());
        TextView tvDesc = findViewById(R.id.tvPopupDescRec);
        if (recordatorio.getDescripcion() != null && !recordatorio.getDescripcion().isEmpty()) {
            tvDesc.setText(recordatorio.getDescripcion());
        } else {
            tvDesc.setText("Sin descripción adicional.");
        }

        Button btnCerrar = findViewById(R.id.btnPopupCerrarRec);
        btnCerrar.setOnClickListener(v -> finish());
    }
}
