package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class ExplicacionJuegoActivity extends BaseActivity {

    private String tipoJuego;
    private String tema;
    private String explicacion;
    private Button btnEmpezarJuego;
    private Button btnVolverExplicar;
    private TextView tvExplicacion;

    // Constantes de explicación
    private static final String EXPLICACION_ROSCO = "En el Mini-Rosco debes hacer un recorrido circular respondiendo preguntas...";
    private static final String EXPLICACION_BINGO = "¡Es hora de jugar al Bingo! Se mostrarán números uno a uno...";
    private static final String EXPLICACION_BUSCA_ENCUENTRA = "¡Prepárate para buscar! En este juego verás imágenes...";
    private static final String EXPLICACION_REFRANES = "En el juego de Refranes te diremos la primera parte de un dicho...";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explicacion_juego);

        // 1. Recuperar datos del Intent
        tipoJuego = getIntent().getStringExtra("TIPO_JUEGO");
        if (tipoJuego == null) tipoJuego = "ROSCO";
        tema = getIntent().getStringExtra("TEMA");

        // 2. Inicializar Vistas
        setupTopBackBanner("Explicación del Juego");
        tvExplicacion = findViewById(R.id.tvExplicacion);
        TextView tvTituloJuego = findViewById(R.id.tvTituloJuego);
        btnEmpezarJuego = findViewById(R.id.btnEmpezarJuego);
        btnVolverExplicar = findViewById(R.id.btnVolverExplicar);

        // 3. Configurar Contenido
        configurarInterfaz(tvTituloJuego);

        // 4. Listeners — botón siempre visible, sin lógica de categorías aquí
        if (btnEmpezarJuego != null) btnEmpezarJuego.setOnClickListener(v -> empezarJuego());
        if (btnVolverExplicar != null) btnVolverExplicar.setOnClickListener(v -> hablarOSimular(explicacion));
    }

    private void configurarInterfaz(TextView tvTituloJuego) {
        switch (tipoJuego) {
            case "BINGO":
                explicacion = EXPLICACION_BINGO;
                if (tvTituloJuego != null) tvTituloJuego.setText("Bingo");
                break;
            case "BUSCA_ENCUENTRA":
                explicacion = EXPLICACION_BUSCA_ENCUENTRA;
                if (tvTituloJuego != null) tvTituloJuego.setText("Busca & Encuentra");
                break;
            case "REFRANES":
                explicacion = EXPLICACION_REFRANES;
                if (tvTituloJuego != null) tvTituloJuego.setText("Refranes");
                break;
            default: // ROSCO
                explicacion = EXPLICACION_ROSCO;
                if (tvTituloJuego != null) tvTituloJuego.setText("Mini-Rosco");
                break;
        }
        if (tvExplicacion != null) tvExplicacion.setText(explicacion);
    }

    private void empezarJuego() {
        Intent intent;
        switch (tipoJuego) {
            case "BINGO":
                intent = new Intent(this, JuegoBingoActivity.class);
                break;
            case "BUSCA_ENCUENTRA":
                intent = new Intent(this, JuegoBuscaEncuentraActivity.class);
                break;
            case "REFRANES":
                intent = new Intent(this, JuegoRefranesActivity.class);
                break;
            default: // ROSCO
                if (tema != null) {
                    // Llega con tema ya elegido (no debería pasar en el flujo normal, pero por si acaso)
                    intent = new Intent(this, JuegoMiniRoscoActivity.class);
                    intent.putExtra("TEMA", tema);
                } else {
                    // Flujo normal: ir a elegir temática
                    intent = new Intent(this, ElegirTematicaRoscoActivity.class);
                }
                break;
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onRobotServiceReady() {
        super.onRobotServiceReady();
        hablarOSimular(explicacion);
    }
}