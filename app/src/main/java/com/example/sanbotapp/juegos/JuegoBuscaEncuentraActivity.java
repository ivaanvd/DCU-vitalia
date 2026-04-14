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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JuegoBuscaEncuentraActivity extends BaseActivity {

    private TextView tvIndicacionRobot;
    private TextView tvMensajeAuxiliar;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean juegoActivo = false;
    private int botonCorrectoId;

    private ImageButton btnBuscaMedicacion;
    private ImageButton btnBuscaComida;
    private ImageButton btnBuscaAjustes;
    private ImageButton btnBuscaCalendario;
    private ImageButton btnBuscaDormir;
    private ImageButton btnBuscaOtros;

    // Guarda qué icono real tiene cada botón
    private final Map<Integer, Integer> mapaIconos = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_busca_encuentra);
        setupTopBackBanner("Busca y Encuentra");

        tvIndicacionRobot = findViewById(R.id.tvBocadilloTexto);
        tvMensajeAuxiliar = findViewById(R.id.tvMensajeAuxiliarBusca);

        btnBuscaMedicacion = findViewById(R.id.btnBuscaMedicacion);
        btnBuscaComida = findViewById(R.id.btnBuscaComida);
        btnBuscaAjustes = findViewById(R.id.btnBuscaAjustes);
        btnBuscaCalendario = findViewById(R.id.btnBuscaCalendario);
        btnBuscaDormir = findViewById(R.id.btnBuscaDormir);
        btnBuscaOtros = findViewById(R.id.btnBuscaOtros);

        prepararTablero();
        configurarBotonesCentrales();
        mostrarDialogoInicio();
    }

    private void mostrarDialogoInicio() {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_inicio_busca_encuentra, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dv.findViewById(R.id.btnEntendidoBusca).setOnClickListener(v -> {
            dialog.dismiss();
            iniciarMemorizacion();
        });

        dialog.show();
    }

    private void prepararTablero() {
        tvIndicacionRobot.setText("Uy, qué cabeza de hojalata... ¡He perdido la rueda de configuración! Fíjate bien dónde la he dejado...");
        tvMensajeAuxiliar.setText("MEMORIZA LA POSICIÓN DEL ICONO");
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
            tvMensajeAuxiliar.setText("AHORA RECUERDA DÓNDE ESTABA EL ICONO");
            tvMensajeAuxiliar.setTextColor(Color.parseColor("#5A5A5A"));
            habilitarBotonesNoFallados();
            juegoActivo = true;
        }, 5000);
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
            if (icono != null) {
                boton.setImageResource(icono);
            }
        }
    }

    private void configurarBotonesCentrales() {
        View.OnClickListener escuchador = v -> {
            if (!juegoActivo) return;

            ImageButton botonPulsado = (ImageButton) v;

            if (v.getId() == botonCorrectoId) {
                juegoActivo = false;
                deshabilitarTodosLosBotones();

                tvMensajeAuxiliar.setText("¡MUY BIEN! ESTE ERA EL ICONO CORRECTO");
                tvMensajeAuxiliar.setTextColor(Color.parseColor("#198754"));

                // Mostrar todas las cartas reales antes de terminar
                mostrarTodosLosIconosReales();

                handler.postDelayed(() -> {
                    startActivity(new Intent(this, FinBuscaEncuentraActivity.class));
                    finish();
                }, 4000);

            } else {
                botonPulsado.setEnabled(false);
                botonPulsado.setAlpha(0.55f);
                botonPulsado.setImageResource(mapaIconos.get(botonPulsado.getId()));
                botonPulsado.setForeground(getDrawable(R.drawable.fg_card_error_x));

                tvMensajeAuxiliar.setText("ESA NO ES. SIGUE PROBANDO");
                tvMensajeAuxiliar.setTextColor(Color.parseColor("#C0392B"));
            }
        };

        for (ImageButton boton : obtenerBotones()) {
            boton.setOnClickListener(escuchador);
        }
    }

    private ImageButton[] obtenerBotones() {
        return new ImageButton[]{
                btnBuscaMedicacion,
                btnBuscaComida,
                btnBuscaAjustes,
                btnBuscaCalendario,
                btnBuscaDormir,
                btnBuscaOtros
        };
    }
}