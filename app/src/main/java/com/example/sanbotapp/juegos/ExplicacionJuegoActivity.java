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
    private boolean juegoIniciado = false;

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explicacion_juego);

        tipoJuego = getIntent().getStringExtra("TIPO_JUEGO");
        if (tipoJuego == null) tipoJuego = "ROSCO";
        tema = getIntent().getStringExtra("TEMA");

        setupTopBackBanner(getString(R.string.juego_explicacion_titulo));
        tvExplicacion     = findViewById(R.id.tvExplicacion);
        TextView tvTituloJuego = findViewById(R.id.tvTituloJuego);
        btnEmpezarJuego   = findViewById(R.id.btnEmpezarJuego);
        btnVolverExplicar = findViewById(R.id.btnVolverExplicar);

        configurarInterfaz(tvTituloJuego);

        if (btnEmpezarJuego != null) {
            btnEmpezarJuego.setOnClickListener(v -> {
                juegoIniciado = true;
                pararVoz();
                empezarJuego();
            });
        }

        if (btnVolverExplicar != null) {
            btnVolverExplicar.setOnClickListener(v -> hablarOSimular(explicacion));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        pararVoz();
    }

    // ── Configuración por tipo de juego ───────────────────────────────────────

    /*
     * Pre: tipoJuego inicializado
     * Post: Asigna el texto de explicación correcto y el título visible en pantalla
     */
    private void configurarInterfaz(TextView tvTituloJuego) {
        switch (tipoJuego) {
            case "BINGO":
                explicacion = getString(R.string.explicacion_bingo);
                if (tvTituloJuego != null) tvTituloJuego.setText(R.string.ui_bingo);
                break;
            case "BUSCA_ENCUENTRA":
                explicacion = getString(R.string.explicacion_busca);
                if (tvTituloJuego != null) tvTituloJuego.setText(R.string.ui_busca_y_encuentra_2);
                break;
            case "REFRANES":
                explicacion = getString(R.string.explicacion_refranes);
                if (tvTituloJuego != null) tvTituloJuego.setText(R.string.ui_refranes);
                break;
            default: // ROSCO
                explicacion = getString(R.string.explicacion_rosco);
                if (tvTituloJuego != null) tvTituloJuego.setText(R.string.juego_rosco_titulo);
                break;
        }
        if (tvExplicacion != null) tvExplicacion.setText(explicacion);
    }

    // ── Navegación al juego ───────────────────────────────────────────────────

    /*
     * Pre: tipoJuego inicializado
     * Post: Lanza la Activity correspondiente al juego seleccionado
     */
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
                    intent = new Intent(this, JuegoMiniRoscoActivity.class);
                    intent.putExtra("TEMA", tema);
                } else {
                    intent = new Intent(this, ElegirTematicaRoscoActivity.class);
                }
                break;
        }
        startActivity(intent);
        finish();
    }

    // ── Robot habla la explicación al arrancar ────────────────────────────────

    @Override
    protected void onRobotServiceReady() {
        super.onRobotServiceReady();
        if (!juegoIniciado) {
            // Añadimos un pequeño delay para que la transición de pantalla termine
            // y no se corte la primera frase.
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!juegoIniciado) hablarOSimular(explicacion);
            }, 800);
        }
    }
}