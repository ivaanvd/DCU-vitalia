package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.RobotLed;

import java.util.ArrayList;
import java.util.List;

public class JuegoBingoActivity extends BaseActivity {

    private List<RondaBingo> rondasFuturas;
    private int indiceRonda = 0;
    private int aciertos = 0;
    private int fallos = 0;

    private TextView tvRobot;
    private TextView tvFeedback;
    private boolean juegoBloqueado = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_bingo);
        setupTopBackBanner(getString(R.string.ui_bingo));
        setVoiceEnabled(false); // Deshabilitar escucha en el bingo

        tvRobot = findViewById(R.id.tvBocadilloTexto);
        tvFeedback = findViewById(R.id.tvFeedbackBingo);

        cargarBolasDeBingo();
        mostrarRonda();
        configurarBotoneraCartones();
    }

    private void cargarBolasDeBingo() {
        rondasFuturas = new ArrayList<>();
        rondasFuturas.add(new RondaBingo(
                getString(R.string.bingo_ronda_comida),
                R.id.btnBingoComida
        ));
        rondasFuturas.add(new RondaBingo(
                getString(R.string.bingo_ronda_dormir),
                R.id.btnBingoDormir
        ));
        rondasFuturas.add(new RondaBingo(
                getString(R.string.bingo_ronda_medicacion),
                R.id.btnBingoMedicacion
        ));
        rondasFuturas.add(new RondaBingo(
                getString(R.string.bingo_ronda_calendario),
                R.id.btnBingoCalendario
        ));
    }

    private void mostrarRonda() {
        if (indiceRonda < rondasFuturas.size()) {
            RondaBingo r = rondasFuturas.get(indiceRonda);
            if (tvRobot != null) {
                tvRobot.setText(r.getConsigna());
                hablarOSimular(r.getConsigna());
            }
            if (tvFeedback != null) {
                tvFeedback.setText(R.string.ui_marca_la_tarjeta_correcta);
                tvFeedback.setTextColor(Color.parseColor("#5A5A5A"));
            }
        }
    }

    private void configurarBotoneraCartones() {
        View.OnClickListener clickHandler = v -> {
            if (juegoBloqueado) return;
            if (indiceRonda >= rondasFuturas.size()) return;

            RondaBingo ronda = rondasFuturas.get(indiceRonda);

            if (v.getId() == ronda.getIdBotonCorrecto()) {
                juegoBloqueado = true;

                marcarTarjetaCorrecta(v);
                aciertos++;
                indiceRonda++;
                gestionarFeedbackHardware("ACIERTO");

                if (indiceRonda < rondasFuturas.size()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        juegoBloqueado = false;
                        mostrarRonda();
                    }, 1200);
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(this::finalizarJuego, 1800);
                }
            } else {
                mostrarFeedbackError();
            }
        };

        findViewById(R.id.btnBingoMedicacion).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoComida).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoCalendario).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoDormir).setOnClickListener(clickHandler);
    }

    private void marcarTarjetaCorrecta(View tarjeta) {
        tarjeta.setEnabled(false);
        tarjeta.setAlpha(0.85f);
        tarjeta.setBackgroundResource(R.drawable.bg_tipo_correcto);
    }

    private void mostrarFeedbackError() {
        juegoBloqueado = true;
        fallos++;
        
        // Guardar el mensaje original para restaurarlo después
        final String consignaOriginal = rondasFuturas.get(indiceRonda).getConsigna();
        
        gestionarFeedbackHardware("FALLO");
        hablarOSimular(getString(R.string.bingo_error));
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            juegoBloqueado = false;
            // Restaurar el texto de la consigna en el bocadillo
            if (tvRobot != null) {
                tvRobot.setText(consignaOriginal);
            }
        }, 3500); // Un poco más de tiempo para que termine de hablar
    }

    private void finalizarJuego() {
        Intent intent = new Intent(this, FinBingoActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("FALLOS", fallos);
        intent.putExtra("TOTAL", rondasFuturas.size());
        intent.putExtra("MENSAJE", getString(R.string.bingo_fin));
        gestionarFeedbackHardware("CELEBRACION");
        startActivity(intent);
        finish();
    }
}