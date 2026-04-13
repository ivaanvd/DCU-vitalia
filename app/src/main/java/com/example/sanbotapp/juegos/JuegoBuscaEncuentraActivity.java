package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import java.util.ArrayList;
import java.util.List;

public class JuegoBuscaEncuentraActivity extends BaseActivity {
    
    private List<RondaBusca> rondas;
    private int indiceRonda = 0;
    
    private TextView tvIndicacionRobot;
    private TextView tvMensajeAuxiliar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_busca_encuentra);
        setupTopBackBanner("Busca y Encuentra");
        
        tvIndicacionRobot = findViewById(R.id.tvBocadilloTexto);
        tvMensajeAuxiliar = findViewById(R.id.tvMensajeAuxiliarBusca);
        
        prepararRondas();
        mostrarRonda();
        configurarBotonesCentrales();
    }
    
    private void prepararRondas() {
        rondas = new ArrayList<>();
        rondas.add(new RondaBusca("la COMIDA", R.id.btnBuscaComida));
        rondas.add(new RondaBusca("dormir plácidamente", R.id.btnBuscaDormir));
        rondas.add(new RondaBusca("los AJUSTES de sistema", R.id.btnBuscaAjustes));
    }
    
    private void mostrarRonda() {
        if (indiceRonda < rondas.size()) {
            RondaBusca actual = rondas.get(indiceRonda);
            if(tvIndicacionRobot != null) {
                tvIndicacionRobot.setText("Encuentra y pulsa el icono de " + actual.getNombreObjeto() + ".");
            }
            if(tvMensajeAuxiliar != null) {
                tvMensajeAuxiliar.setText("Revisa la cuadrícula inferior atentamente:");
                tvMensajeAuxiliar.setTextColor(Color.parseColor("#888888"));
            }
        } else {
            // Se han superado todas las rondas
            startActivity(new Intent(this, FinBuscaEncuentraActivity.class));
            finish();
        }
    }
    
    private void configurarBotonesCentrales() {
        View.OnClickListener escuchador = v -> {
            if (indiceRonda >= rondas.size()) return;
            RondaBusca rondaActual = rondas.get(indiceRonda);
            
            if (v.getId() == rondaActual.getBotonIdCorrecto()) {
                indiceRonda++;
                mostrarRonda(); // Iniciar siguiente o terminar
            } else {
                if(tvMensajeAuxiliar != null) {
                    tvMensajeAuxiliar.setText("Ese no es. ¡Sigue intentándolo, tú puedes!");
                    tvMensajeAuxiliar.setTextColor(Color.parseColor("#DC3545"));
                }
            }
        };
        
        findViewById(R.id.btnBuscaMedicacion).setOnClickListener(escuchador);
        findViewById(R.id.btnBuscaComida).setOnClickListener(escuchador);
        findViewById(R.id.btnBuscaAjustes).setOnClickListener(escuchador);
        findViewById(R.id.btnBuscaCalendario).setOnClickListener(escuchador);
        findViewById(R.id.btnBuscaDormir).setOnClickListener(escuchador);
        findViewById(R.id.btnBuscaOtros).setOnClickListener(escuchador);
    }
}
