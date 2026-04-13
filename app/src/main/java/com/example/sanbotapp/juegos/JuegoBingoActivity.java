package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private Button btnGritarBingo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_bingo);
        setupTopBackBanner("Juego de Bingo");

        tvRobot = findViewById(R.id.tvBocadilloTexto);
        tvFeedback = findViewById(R.id.tvFeedbackBingo);
        btnGritarBingo = findViewById(R.id.btnGritarBingo);

        cargarBolasDeBingo();
        mostrarRonda();
        configurarBotoneraCartones();

        if (btnGritarBingo != null) {
            btnGritarBingo.setOnClickListener(v -> finalizarJuego());
        }
    }

    private void cargarBolasDeBingo() {
        rondasFuturas = new ArrayList<>();
        rondasFuturas.add(new RondaBingo("¡Llegó la hora de nutrir el cuerpo! Tacha la tarjeta...", R.id.btnBingoComida));
        rondasFuturas.add(new RondaBingo("Ha caído la noche. ¿Qué tarjeta corresponde a cerrar los ojos?", R.id.btnBingoDormir));
        rondasFuturas.add(new RondaBingo("El médico nos recetó una ayuda, ¿Qué tarjeta es?", R.id.btnBingoMedicacion));
        rondasFuturas.add(new RondaBingo("Marca la cita de mañana para no olvidarla.", R.id.btnBingoCalendario));
    }

    private void mostrarRonda() {
        if (indiceRonda < rondasFuturas.size()) {
            RondaBingo r = rondasFuturas.get(indiceRonda);
            if (tvRobot != null) {
                tvRobot.setText(r.getConsigna());
            }
            if(tvFeedback != null) {
                tvFeedback.setText("¡Marca la tarjeta de abajo!");
                tvFeedback.setTextColor(Color.parseColor("#888888"));
            }
        } else {
            // Juego completado
            if (tvRobot != null) {
                tvRobot.setText("¡Todas tachadas! Pulsa el botón inferior para terminar.");
            }
            if(tvFeedback != null) {
                tvFeedback.setText("Rondas finalizadas");
            }
            if (btnGritarBingo != null) {
                btnGritarBingo.setEnabled(true);
                btnGritarBingo.setBackgroundColor(Color.parseColor("#198754"));
            }
        }
    }

    private void configurarBotoneraCartones() {
        View.OnClickListener clickHandler = v -> {
            if (indiceRonda >= rondasFuturas.size()) return;
            RondaBingo ronda = rondasFuturas.get(indiceRonda);

            if (v.getId() == ronda.getIdBotonCorrecto()) {
                v.setAlpha(0.4f);  // Disminuimos el alfa emulando haber tachado el cartón
                v.setEnabled(false); // Anula repeticiones
                aciertos++;
                indiceRonda++;
                mostrarRonda();
            } else {
                if (tvFeedback != null) {
                    tvFeedback.setText("Ups, esa no es la tarjeta correcta. ¡Sigue probando!");
                    tvFeedback.setTextColor(Color.parseColor("#DC3545"));
                }
            }
        };

        findViewById(R.id.btnBingoMedicacion).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoComida).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoCalendario).setOnClickListener(clickHandler);
        findViewById(R.id.btnBingoDormir).setOnClickListener(clickHandler);
    }

    private void finalizarJuego() {
        Intent intent = new Intent(this, FinBingoActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("TOTAL", rondasFuturas.size());
        intent.putExtra("MENSAJE", "¡Espectacular Bingo! Cantaste las " + aciertos + " tarjetas.");
        startActivity(intent);
        finish();
    }
}
