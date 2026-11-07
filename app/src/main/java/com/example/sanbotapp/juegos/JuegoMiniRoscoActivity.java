package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.RobotLed;
import java.util.ArrayList;
import java.util.List;

public class JuegoMiniRoscoActivity extends BaseActivity {

    private List<PreguntaRosco> todasLasPreguntas;
    private List<PreguntaRosco> preguntasDelTema;
    private int indiceActual = 0;
    private int aciertos = 0;
    private String temaSeleccionado;
    private boolean juegoBloqueado = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvLetra;
    private TextView tvPistaRobot;
    private Button btnOpcion1;
    private Button btnOpcion2;
    private LinearLayout llContenedorLetras;
    private List<TextView> vistasLetras;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_mini_rosco);
        setupTopBackBanner(getString(R.string.juego_rosco_titulo));
        setVoiceEnabled(false); // Deshabilitar escucha en el rosco

        temaSeleccionado = getIntent().getStringExtra("TEMA");
        if (temaSeleccionado == null) {
            List<String> temas = ContenidoJuegos.temasRosco(this);
            temaSeleccionado = temas.isEmpty() ? "" : temas.get(0);
        }

        TextView tvCabecera = findViewById(R.id.tvTemaRosco);
        if (tvCabecera != null) {
            tvCabecera.setText(getString(R.string.rosco_tematica, temaSeleccionado).toUpperCase());
        }

        tvLetra = findViewById(R.id.tvProgresoLetra);
        tvPistaRobot = findViewById(R.id.tvBocadilloTexto);
        btnOpcion1 = findViewById(R.id.btnRoscoOpcion1);
        btnOpcion2 = findViewById(R.id.btnRoscoOpcion2);
        llContenedorLetras = findViewById(R.id.llContenedorLetras);

        if (tvPistaRobot != null) tvPistaRobot.setTextSize(32);
        if (btnOpcion1 != null) btnOpcion1.setTextSize(20);
        if (btnOpcion2 != null) btnOpcion2.setTextSize(20);

        cargarBBDDPreguntas();
        preguntasDelTema = new ArrayList<>();
        for (PreguntaRosco p : todasLasPreguntas) {
            if (p.getTematica().equalsIgnoreCase(temaSeleccionado)) {
                preguntasDelTema.add(p);
            }
        }

        if (preguntasDelTema.isEmpty()) {
            preguntasDelTema.addAll(todasLasPreguntas);
        }

        inicializarFilaLetras();
        // mostrarPregunta() se llama ahora en onRobotServiceReady para asegurar que hable

        if (btnOpcion1 != null) btnOpcion1.setOnClickListener(v -> procesarRespuesta(1));
        if (btnOpcion2 != null) btnOpcion2.setOnClickListener(v -> procesarRespuesta(2));
    }

    private void inicializarFilaLetras() {
        vistasLetras = new ArrayList<>();
        float scale = getResources().getDisplayMetrics().density;
        int sizePx = (int) (82 * scale + 0.5f);
        int marginHorizontalPx = (int) (12 * scale + 0.5f);
        int marginVerticalPx = (int) (6 * scale + 0.5f);

        for (PreguntaRosco p : preguntasDelTema) {
            TextView tv = new TextView(this);
            tv.setText(p.getLetra());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(30);
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundResource(R.drawable.bg_rosco_letra_pendiente);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
            params.setMargins(marginHorizontalPx, marginVerticalPx, marginHorizontalPx, marginVerticalPx);
            tv.setLayoutParams(params);

            if (llContenedorLetras != null) llContenedorLetras.addView(tv);
            vistasLetras.add(tv);
        }
    }

    private void cargarBBDDPreguntas() {
        todasLasPreguntas = ContenidoJuegos.preguntasRosco(this);
    }

    private void mostrarPregunta() {
        if (indiceActual < preguntasDelTema.size()) {
            vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_actual);

            PreguntaRosco p = preguntasDelTema.get(indiceActual);
            if (tvLetra != null) tvLetra.setVisibility(TextView.GONE);
            if (tvPistaRobot != null) {
                String textoCompleto = getString(R.string.rosco_pista, p.getLetra(), p.getPista());
                tvPistaRobot.setText(textoCompleto);
                hablarOSimular(textoCompleto);
            }
            if (btnOpcion1 != null) btnOpcion1.setText(p.getOpcion1());
            if (btnOpcion2 != null) btnOpcion2.setText(p.getOpcion2());
        } else {
            finalizarJuego();
        }
    }

    private void procesarRespuesta(int seleccion) {
        if (juegoBloqueado) return;

        juegoBloqueado = true;
        boolean esCorrecta = seleccion == preguntasDelTema.get(indiceActual).getIndiceCorrecto();

        if (esCorrecta) {
            aciertos++;
            mostrarFeedbackCorrecto(seleccion);
        } else {
            mostrarFeedbackIncorrecto(seleccion, preguntasDelTema.get(indiceActual).getIndiceCorrecto());
        }
    }

    private void mostrarFeedbackCorrecto(int seleccion) {
        // Círculo de la letra en verde
        vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_correcta);

        // Botón pulsado en verde
        Button boton = (seleccion == 1) ? btnOpcion1 : btnOpcion2;
        boton.setBackgroundResource(R.drawable.bg_tipo_correcto);
        boton.setTextColor(Color.WHITE);

        // Feedback robot
        gestionarFeedbackHardware("ACIERTO");
        hablarOSimular(getString(R.string.juego_correcto));

        handler.postDelayed(() -> {
            apagarLed(RobotLed.PART_ALL);
            restaurarBotones();
            indiceActual++;
            mostrarPregunta();
            juegoBloqueado = false;
        }, 2000);
    }

    private void mostrarFeedbackIncorrecto(int seleccionIncorrecta, int correcta) {
        // Círculo de la letra en rojo
        vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_incorrecta);

        // Botón incorrecto en rojo, botón correcto en verde
        Button botonIncorrecto = (seleccionIncorrecta == 1) ? btnOpcion1 : btnOpcion2;
        botonIncorrecto.setBackgroundResource(R.drawable.bg_tipo_incorrecto);
        botonIncorrecto.setTextColor(Color.WHITE);

        Button botonCorrecto = (correcta == 1) ? btnOpcion1 : btnOpcion2;
        botonCorrecto.setBackgroundResource(R.drawable.bg_tipo_correcto);
        botonCorrecto.setTextColor(Color.WHITE);

        // Feedback robot
        gestionarFeedbackHardware("FALLO");
        hablarOSimular(getString(R.string.juego_lastima));

        handler.postDelayed(() -> {
            apagarLed(RobotLed.PART_ALL);
            restaurarBotones();
            indiceActual++;
            mostrarPregunta();
            juegoBloqueado = false;
        }, 2500);
    }

    private void restaurarBotones() {
        if (btnOpcion1 != null) {
            btnOpcion1.setBackgroundResource(R.drawable.bg_tipo_normal);
            btnOpcion1.setTextColor(Color.parseColor("#111111"));
        }
        if (btnOpcion2 != null) {
            btnOpcion2.setBackgroundResource(R.drawable.bg_tipo_normal);
            btnOpcion2.setTextColor(Color.parseColor("#111111"));
        }
    }

    private void finalizarJuego() {
        Intent intent = new Intent(this, FinMiniRoscoActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("TOTAL", preguntasDelTema.size());
        intent.putExtra("TEMA", temaSeleccionado);

        String sms = (aciertos == preguntasDelTema.size())
                ? getString(R.string.rosco_fin_pleno)
                : getString(R.string.rosco_fin_esfuerzo);
        intent.putExtra("MENSAJE", sms);

        gestionarFeedbackHardware("CELEBRACION");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onRobotServiceReady() {
        mostrarPregunta();
    }
}