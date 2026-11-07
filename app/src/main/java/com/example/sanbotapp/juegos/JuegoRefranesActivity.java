package com.example.sanbotapp.juegos;

import static java.lang.Thread.sleep;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.RobotLed;

import java.util.ArrayList;
import java.util.List;

public class JuegoRefranesActivity extends BaseActivity {

    private List<PreguntaRefran> listaPreguntas;
    private int indiceActual = 0;
    private int aciertos = 0;
    private boolean juegoBloqueado = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvRefran;
    private TextView tvEstadoPregunta;
    private Button btnOpcion1;
    private Button btnOpcion2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_refranes);
        setupTopBackBanner(getString(R.string.juego_refranes_titulo));
        setVoiceEnabled(false); // Deshabilitar escucha en los refranes

        tvRefran = findViewById(R.id.tvBocadilloTexto);
        tvEstadoPregunta = findViewById(R.id.tvEstadoPregunta);
        btnOpcion1 = findViewById(R.id.btnOpcion1);
        btnOpcion2 = findViewById(R.id.btnOpcion2);

        cargarPreguntas();
        mostrarPregunta();

        if (btnOpcion1 != null) {
            btnOpcion1.setOnClickListener(v -> procesarRespuesta(1));
        }
        if (btnOpcion2 != null) {
            btnOpcion2.setOnClickListener(v -> procesarRespuesta(2));
        }
    }

    private void cargarPreguntas() {
        listaPreguntas = ContenidoJuegos.preguntasRefranes(this);
    }

    private void mostrarPregunta() {
        if (indiceActual < listaPreguntas.size()) {
            PreguntaRefran actual = listaPreguntas.get(indiceActual);

            if (tvEstadoPregunta != null) {
                tvEstadoPregunta.setText(getString(R.string.refranes_contador, indiceActual + 1, listaPreguntas.size()));
            }

            if (tvRefran != null) {
                tvRefran.setText(actual.getTextoCuestion());

                // ← Solo la primera pregunta necesita el delay
                if (indiceActual == 0) {
                    handler.postDelayed(() -> hablarOSimular(actual.getTextoCuestion()), 1500);
                } else {
                    hablarOSimular(actual.getTextoCuestion());
                }
            }

            if (btnOpcion1 != null && btnOpcion2 != null) {
                btnOpcion1.setText(actual.getOpcion1());
                btnOpcion2.setText(actual.getOpcion2());
            }
        } else {
            irAResultado();
        }
    }

    private void procesarRespuesta(int seleccion) {
        if (juegoBloqueado) return; // Evitar clicks mientras se muestra el feedback

        juegoBloqueado = true;
        PreguntaRefran actual = listaPreguntas.get(indiceActual);

        if (seleccion == actual.getIndiceCorrecto()) {
            // ¡ACERTÓ!
            aciertos++;
            mostrarFeedbackCorrecto(seleccion);
        } else {
            // FALLÓ
            mostrarFeedbackIncorrecto(seleccion, actual.getIndiceCorrecto());
        }
    }

    private void mostrarFeedbackCorrecto(int seleccion) {
        encenderLed(RobotLed.PART_ALL, RobotLed.MODE_GREEN);
        Button boton = (seleccion == 1) ? btnOpcion1 : btnOpcion2;
        boton.setBackgroundResource(R.drawable.bg_tipo_correcto);
        boton.setTextColor(Color.WHITE);

        gestionarFeedbackHardware("ACIERTO");
        hablarOSimular(getString(R.string.refranes_correcto));

        handler.postDelayed(() -> {
            apagarLed(RobotLed.PART_ALL);

            handler.postDelayed(() -> {
                restaurarBotones();
                indiceActual++;
                mostrarPregunta();
                juegoBloqueado = false;
            }, 1500);
        }, 1800);
    }

    private void mostrarFeedbackIncorrecto(int seleccionIncorrecta, int correcta) {
        encenderLed(RobotLed.PART_ALL, RobotLed.MODE_RED);

        Button botonIncorrecto = (seleccionIncorrecta == 1) ? btnOpcion1 : btnOpcion2;
        botonIncorrecto.setBackgroundResource(R.drawable.bg_tipo_incorrecto);
        botonIncorrecto.setTextColor(Color.WHITE);

        Button botonCorrecto = (correcta == 1) ? btnOpcion1 : btnOpcion2;
        botonCorrecto.setBackgroundResource(R.drawable.bg_tipo_correcto);
        botonCorrecto.setTextColor(Color.WHITE);

        gestionarFeedbackHardware("FALLO");
        hablarOSimular(getString(R.string.juego_lastima));
//        moverCabezaBasico("ABAJO");
//        reiniciarCabeza();

        // Esperar a que termine la emoción y el TTS antes de mostrar feedback visual
        handler.postDelayed(() -> {
            apagarLed(RobotLed.PART_ALL);

            handler.postDelayed(() -> {
                restaurarBotones();
                indiceActual++;
                mostrarPregunta();
                juegoBloqueado = false;
            }, 1500);
        }, 2200); // tiempo estimado emoción CRY + TTS "Lástima, te acercabas"
    }


    private void restaurarBotones() {
        btnOpcion1.setBackgroundResource(R.drawable.bg_tipo_normal);
        btnOpcion1.setTextColor(Color.parseColor("#111111"));

        btnOpcion2.setBackgroundResource(R.drawable.bg_tipo_normal);
        btnOpcion2.setTextColor(Color.parseColor("#111111"));
    }

    private void irAResultado() {
        Intent intent = new Intent(this, FinRefranesActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("TOTAL", listaPreguntas.size());

        String mensaje;
        if (aciertos == listaPreguntas.size()) {
            mensaje = getString(R.string.refranes_fin_todos);
        } else if (aciertos >= listaPreguntas.size() / 2) {
            mensaje = getString(R.string.refranes_fin_casi);
        } else {
            mensaje = getString(R.string.refranes_fin_intento);
        }
        intent.putExtra("MENSAJE", mensaje);

        gestionarFeedbackHardware("CELEBRACION");
        startActivity(intent);
        finish();
    }
}