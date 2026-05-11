package com.example.sanbotapp.juegos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.qihancloud.opensdk.function.beans.LED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegoBuscaEncuentraActivity extends BaseActivity {

    private TextView tvIndicacionRobot;
    private TextView tvMensajeAuxiliar;
    private TextView tvEstadoBusca; // opcional, ya existe en el XML

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView tvCronometro; // Nuevo
    private final Handler cronoHandler = new Handler(Looper.getMainLooper());
    private Runnable cronoRunnable;

    private boolean juegoActivo = false;
    private int botonCorrectoId;
    private long tiempoInicioMs; // ← NUEVO: marca cuándo empieza a buscar

    private ImageButton btnBuscaMedicacion;
    private ImageButton btnBuscaComida;
    private ImageButton btnBuscaAjustes;
    private ImageButton btnBuscaCalendario;
    private ImageButton btnBuscaDormir;
    private ImageButton btnBuscaOtros;

    private final Map<Integer, Integer> mapaIconos = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_busca_encuentra);

        tvCronometro = findViewById(R.id.tvCronometro); // Inicializar
        tvIndicacionRobot = findViewById(R.id.tvBocadilloTexto);
        tvMensajeAuxiliar = findViewById(R.id.tvMensajeAuxiliarBusca);

        btnBuscaMedicacion = findViewById(R.id.btnBuscaMedicacion);
        btnBuscaComida    = findViewById(R.id.btnBuscaComida);
        btnBuscaAjustes   = findViewById(R.id.btnBuscaAjustes);
        btnBuscaCalendario = findViewById(R.id.btnBuscaCalendario);
        btnBuscaDormir    = findViewById(R.id.btnBuscaDormir);
        btnBuscaOtros     = findViewById(R.id.btnBuscaOtros);

        configurarBotonesCentrales();
        prepararTablero();          // ← arranca directo, sin modal
        iniciarMemorizacion();
    }

    private void prepararTablero() {
        // Fase 1: memorización → el robot NO desvela qué buscar todavía
        tvIndicacionRobot.setText("¡Atento a los iconos! Fíjate bien dónde están...");
        tvMensajeAuxiliar.setText("MEMORIZA LA POSICIÓN DE LOS ICONOS");
        tvMensajeAuxiliar.setTextColor(Color.parseColor("#5A5A5A"));

        List<Integer> iconos = new ArrayList<>();
        iconos.add(R.drawable.ic_medicacion);
        iconos.add(R.drawable.ic_comida);
        iconos.add(R.drawable.ic_ajustes);
        iconos.add(R.drawable.ic_calendario);
        iconos.add(R.drawable.ic_dormir);
        iconos.add(R.drawable.ic_otros);

        Collections.shuffle(iconos);

        ImageButton[] botones = obtenerBotones();
        mapaIconos.clear();

        for (int i = 0; i < botones.length; i++) {
            botones[i].setImageResource(iconos.get(i));
            botones[i].setEnabled(false);
            botones[i].setAlpha(1f);
            botones[i].setForeground(null);
            mapaIconos.put(botones[i].getId(), iconos.get(i));

            if (iconos.get(i) == R.drawable.ic_ajustes) {
                botonCorrectoId = botones[i].getId();
            }
        }
    }

    private void iniciarMemorizacion() {
        juegoActivo = false;
        handler.postDelayed(() -> {
            ocultarIconos();
            tvIndicacionRobot.setText("Uy, qué cabeza de hojalata... ¡He perdido la rueda de configuración! ¿La encuentras?");
            hablarOSimular("Uy, qué cabeza de hojalata... ¡He perdido la rueda de configuración! ¿La encuentras?");

            habilitarBotonesNoFallados();

            // INICIAR CRONÓMETRO
            tiempoInicioMs = System.currentTimeMillis();
            iniciarActualizacionCrono();

            juegoActivo = true;
        }, 5000);
    }

    private void iniciarActualizacionCrono() {
        cronoRunnable = new Runnable() {
            @Override
            public void run() {
                if (juegoActivo) {
                    long segundos = (System.currentTimeMillis() - tiempoInicioMs) / 1000;
                    tvCronometro.setText("Tiempo: " + segundos + "s");
                    cronoHandler.postDelayed(this, 1000); // Se ejecuta cada segundo
                }
            }
        };
        cronoHandler.post(cronoRunnable);
    }

    private void ocultarIconos() {
        for (ImageButton boton : obtenerBotones()) {
            boton.setImageResource(R.drawable.ic_otros);
            boton.setForeground(null);
        }
    }

    private void habilitarBotonesNoFallados() {
        for (ImageButton boton : obtenerBotones()) {
            if (boton.getAlpha() == 1f) {
                boton.setEnabled(true);
            }
        }
    }

    private void deshabilitarTodosLosBotones() {
        for (ImageButton boton : obtenerBotones()) {
            boton.setEnabled(false);
        }
    }

    private void mostrarTodosLosIconosReales() {
        for (ImageButton boton : obtenerBotones()) {
            Integer icono = mapaIconos.get(boton.getId());
            if (icono != null) boton.setImageResource(icono);
        }
    }

    private void configurarBotonesCentrales() {
        View.OnClickListener escuchador = v -> {
            if (!juegoActivo) return;

            ImageButton botonPulsado = (ImageButton) v;

            if (v.getId() == botonCorrectoId) {
                juegoActivo = false;
                cronoHandler.removeCallbacks(cronoRunnable);
                long tiempoMs = System.currentTimeMillis() - tiempoInicioMs;
                long segundos = tiempoMs / 1000;

                deshabilitarTodosLosBotones();

                // ← Verde inmediato
                botonPulsado.setBackgroundResource(R.drawable.bg_tipo_correcto);
                botonPulsado.setAlpha(1f);
                botonPulsado.setForeground(null);

                mostrarEmocion("PRISE"); // Emoción "orgulloso/feliz"
                hablarOSimular("¡¡MUY BIEN! ESTE ERA EL ICONO CORRECTO");
                encenderLed(LED.PART_ALL, LED.MODE_GREEN);
//                moverBrazos("LEVANTAR_BRAZO", "AMBOS"); // Celebración: brazos arriba
//                moverBrazos("BAJAR_BRAZO", "AMBOS");
                tvMensajeAuxiliar.setText("¡MUY BIEN! ESTE ERA EL ICONO CORRECTO");
                tvMensajeAuxiliar.setTextColor(Color.parseColor("#198754"));
                mostrarTodosLosIconosReales();

                handler.postDelayed(() -> {
                    apagarLed(LED.PART_ALL);
                    Intent intent = new Intent(this, FinBuscaEncuentraActivity.class);
                    intent.putExtra("SEGUNDOS", segundos);
                    startActivity(intent);
                    finish();
                }, 4000);

            } else {
                botonPulsado.setEnabled(false);
                botonPulsado.setAlpha(1f);
                botonPulsado.setImageResource(mapaIconos.get(botonPulsado.getId()));
                botonPulsado.setBackgroundResource(R.drawable.bg_tipo_incorrecto);
                botonPulsado.setForeground(null);

                encenderLed(LED.PART_ALL, LED.MODE_RED);
                handler.postDelayed(() -> apagarLed(LED.PART_ALL), 1500);
            }
        };

        for (ImageButton boton : obtenerBotones()) {
            boton.setOnClickListener(escuchador);
        }
    }

    private ImageButton[] obtenerBotones() {
        return new ImageButton[]{
                btnBuscaMedicacion, btnBuscaComida, btnBuscaAjustes,
                btnBuscaCalendario, btnBuscaDormir, btnBuscaOtros
        };
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        cronoHandler.removeCallbacks(cronoRunnable); // Evitar fugas de memoria
    }
}