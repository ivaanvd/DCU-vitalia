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

    // ── Explicaciones completas por juego ─────────────────────────────────────

    private static final String EXPLICACION_ROSCO =
            "¡Vamos a jugar al Mini-Rosco! Elige la opción correcta para cada definición. " +
            "Si aciertas, se pondrá en verde, y si fallas, en rojo. ¡Ánimo!";

    private static final String EXPLICACION_BINGO =
            "¡Es la hora del Bingo! Pulsa la imagen que corresponda a lo que yo diga. " +
            "Si te equivocas, puedes intentarlo de nuevo hasta completar todas.";

    private static final String EXPLICACION_BUSCA_ENCUENTRA =
            "En este juego debes memorizar los iconos antes de que se oculten. " +
            "Después, tendrás que recordar dónde estaba el que yo te pida. ¡Sé rápido!";

    private static final String EXPLICACION_REFRANES =
            "¡Vamos con los Refranes! Yo digo la primera parte y tú eliges cómo termina. " +
            "Seguro que te los sabes todos.";

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explicacion_juego);

        tipoJuego = getIntent().getStringExtra("TIPO_JUEGO");
        if (tipoJuego == null) tipoJuego = "ROSCO";
        tema = getIntent().getStringExtra("TEMA");

        setupTopBackBanner("Explicación del Juego");
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