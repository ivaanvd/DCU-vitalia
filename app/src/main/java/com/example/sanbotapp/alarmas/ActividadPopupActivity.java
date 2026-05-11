package com.example.sanbotapp.alarmas;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadPasosActivity;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.alarmas.AlarmScheduler;

public class ActividadPopupActivity extends BaseActivity {

    public static final String EXTRA_ACTIVIDAD_ID = "extra_actividad_id";

    private ActividadRepository repo;
    private Actividad actividad;
    private android.media.Ringtone alarmSound;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Encender pantalla aunque esté bloqueada
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON    |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON    |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED  |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_actividad_popup);

        repo = new ActividadRepository(this);

        int id = getIntent().getIntExtra(EXTRA_ACTIVIDAD_ID, -1);
        actividad = repo.getById(id);

        if (actividad == null) {
            finish();
            return;
        }

        renderizar();
        configurarBotones();
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

    // El robot habla cuando el SDK esté listo — igual que en tus otras Activities
    @Override
    protected void onMainServiceConnected() {
        super.onMainServiceConnected();
        if (actividad != null) {
            hablarOSimular("¡Atención! Es hora de " + actividad.getTipoLabel().toLowerCase());
        }
    }

    private void renderizar() {
        ImageView ivRobot = findViewById(R.id.ivPopupIcono);
        ivRobot.setImageResource(actividad.getIconoRes());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor(actividad.getColorHex()));
        findViewById(R.id.framePopupIcono).setBackground(bg);

        ((TextView) findViewById(R.id.tvPopupTitulo))
                .setText("¡ES HORA DE " + actividad.getTipoLabel().toUpperCase() + "!");

        TextView tvDesc = findViewById(R.id.tvPopupDesc);
        if (actividad.getDescripcion() != null && !actividad.getDescripcion().isEmpty()) {
            tvDesc.setText(actividad.getDescripcion());
        } else {
            tvDesc.setVisibility(android.view.View.GONE);
        }

        ((TextView) findViewById(R.id.tvPopupDuracion))
                .setText("Duración estimada: " + actividad.getDuracionMinutos() + " min");
    }

    private void configurarBotones() {
        Button btnHacerAhora = findViewById(R.id.btnPopupHacerAhora);
        Button btnPosponer   = findViewById(R.id.btnPopupPosponer);

        // Si es creada por sistema (ya pospuesta una vez) no se puede posponer más
        if (actividad.isCreadaPorSistema()) {
            btnPosponer.setVisibility(android.view.View.GONE);
        }

        btnHacerAhora.setOnClickListener(v -> {
            // Ir a la pantalla de pasos guiados
            android.content.Intent intent = new android.content.Intent(
                    this, ActividadPasosActivity.class
            );
            intent.putExtra(ActividadPasosActivity.EXTRA_TIPO_ACTIVIDAD, actividad.getTipo());
            intent.putExtra(ActividadPasosActivity.EXTRA_ACTIVIDAD_ID, actividad.getId());
            startActivity(intent);
            finish();
        });

        btnPosponer.setOnClickListener(v -> {
            repo.posponerActividad(actividad.getId(), 30);
            hablarOSimular("De acuerdo, te recuerdo en 30 minutos.");
            Toast.makeText(this, "Actividad pospuesta 30 minutos", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}