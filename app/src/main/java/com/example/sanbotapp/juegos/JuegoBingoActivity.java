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

import java.util.ArrayList;
import java.util.List;

public class JuegoBingoActivity extends BaseActivity {

    private List<RondaBingo> rondasFuturas;
    private int indiceRonda = 0;
    private int aciertos = 0;

    private TextView tvRobot;
    private TextView tvFeedback;

    private LinearLayout panelAvisoError;
    private TextView tvMensajeErrorBingo;
    private Button btnEntendidoBingo;

    private View overlayAvisoBingo;

    private boolean juegoBloqueado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_bingo);
        setupTopBackBanner("Bingo");

        tvRobot = findViewById(R.id.tvBocadilloTexto);
        tvFeedback = findViewById(R.id.tvFeedbackBingo);

        panelAvisoError = findViewById(R.id.panelAvisoErrorBingo);
        tvMensajeErrorBingo = findViewById(R.id.tvMensajeErrorBingo);
        btnEntendidoBingo = findViewById(R.id.btnEntendidoBingo);
        overlayAvisoBingo = findViewById(R.id.overlayAvisoBingo);

        cargarBolasDeBingo();
        mostrarRonda();
        configurarBotoneraCartones();

        if (btnEntendidoBingo != null) {
            btnEntendidoBingo.setOnClickListener(v -> ocultarAvisoError());
        }
    }

    private void cargarBolasDeBingo() {
        rondasFuturas = new ArrayList<>();
        rondasFuturas.add(new RondaBingo(
                "¡\"Llegó la hora de nutrir el cuerpo!\"",
                R.id.btnBingoComida
        ));
        rondasFuturas.add(new RondaBingo(
                "\"Ha caído la noche. ¿Qué tarjeta corresponde a descansar?\"",
                R.id.btnBingoDormir
        ));
        rondasFuturas.add(new RondaBingo(
                "\"El médico nos recetó una ayuda.\"",
                R.id.btnBingoMedicacion
        ));
        rondasFuturas.add(new RondaBingo(
                "\"Marca la tarjeta de la cita para no olvidarla.\"",
                R.id.btnBingoCalendario
        ));
    }

    private void mostrarRonda() {
        if (indiceRonda < rondasFuturas.size()) {
            RondaBingo r = rondasFuturas.get(indiceRonda);
            if (tvRobot != null) {
                tvRobot.setText(r.getConsigna());
            }
            if (tvFeedback != null) {
                tvFeedback.setText("MARCA LA TARJETA CORRECTA");
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

                if (indiceRonda < rondasFuturas.size()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        juegoBloqueado = false;
                        mostrarRonda();
                    }, 1200);
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(this::finalizarJuego, 1800);
                }
            } else {
                mostrarAvisoError("Esa no es la tarjeta correcta.\nLee de nuevo la indicación y vuelve a intentarlo.");
            }
        };

        findViewById(R.id.btnBingoMedicacion).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoComida).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoCalendario).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoDormir).setOnClickListener(clickHandler);
    }

    private void marcarTarjetaCorrecta(View tarjeta) {
        tarjeta.setEnabled(false);
        tarjeta.setAlpha(0.45f);
        tarjeta.setForeground(getDrawable(R.drawable.ic_sello_correcto));
    }

    private void mostrarAvisoError(String mensaje) {
        juegoBloqueado = true;

        if (overlayAvisoBingo != null) {
            overlayAvisoBingo.setVisibility(View.VISIBLE);
        }
        if (panelAvisoError != null) {
            panelAvisoError.setVisibility(View.VISIBLE);
        }
        if (tvMensajeErrorBingo != null) {
            tvMensajeErrorBingo.setText(mensaje);
        }
    }

    private void ocultarAvisoError() {
        if (overlayAvisoBingo != null) {
            overlayAvisoBingo.setVisibility(View.GONE);
        }
        if (panelAvisoError != null) {
            panelAvisoError.setVisibility(View.GONE);
        }

        juegoBloqueado = false;
    }

    private void finalizarJuego() {
        Intent intent = new Intent(this, FinBingoActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("TOTAL", rondasFuturas.size());
        intent.putExtra("MENSAJE", "¡Espectacular Bingo! Has completado toda la tarjeta.");
        startActivity(intent);
        finish();
    }
}