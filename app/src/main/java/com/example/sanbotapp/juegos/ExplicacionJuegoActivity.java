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
            "Vamos a jugar al Mini-Rosco. " +
            "Te haré preguntas sobre una temática que hayas elegido. " +
            "Cada pregunta tiene dos opciones, y solo una es correcta. " +
            "Te daré una descripción y tú deberás elegir la respuesta pulsando uno de los dos botones. " +
            "Si aciertas, la letra se pondrá en verde. Si fallas, se pondrá en rojo y pasaremos a la siguiente. " +
            "¡No te preocupes si fallas, lo importante es participar! " +
            "Cuando estés listo o lista, pulsa el botón de empezar.";

    private static final String EXPLICACION_BINGO =
            "Vamos a jugar al Bingo. " +
            "Verás varias tarjetas con imágenes en la pantalla. " +
            "Iré leyendo enunciados uno a uno, " +
            "y tú deberás pulsar la tarjeta que corresponda a lo que describe. " +
            "Si aciertas, la tarjeta se marcará en verde y pasaremos al siguiente enunciado. " +
            "Si te equivocas, te avisaré y podrás intentarlo de nuevo. " +
            "¡El objetivo es completar todas! " +
            "Cuando estés listo o lista, pulsa el botón de empezar.";

    private static final String EXPLICACION_BUSCA_ENCUENTRA =
            "Vamos a jugar a Busca y Encuentra. " +
            "Primero te mostraré varios iconos en la pantalla durante unos segundos. " +
            "Fíjate bien en dónde está cada uno, porque después los voy a ocultar. " +
            "Entonces te pediré que encuentres uno de ellos. " +
            "Tendrás que recordar dónde estaba y pulsar el sitio correcto. " +
            "¡Cuanto más rápido lo encuentres, mejor! " +
            "El cronómetro empezará en cuanto ocultemos los iconos. " +
            "Cuando estés listo o lista, pulsa el botón de empezar.";

    private static final String EXPLICACION_REFRANES =
            "Vamos a jugar a los Refranes. " +
            "Te diré la primera parte de un refrán muy conocido " +
            "y tú deberás elegir cómo termina entre dos opciones. " +
            "Solo una respuesta es la correcta. " +
            "Si aciertas, me alegraré mucho. " +
            "Si fallas, te diré cuál era la respuesta correcta y seguiremos adelante. " +
            "¡Son refranes que seguro que has escuchado muchas veces! " +
            "Cuando estés listo o lista, pulsa el botón de empezar.";

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
            hablarOSimular(explicacion);
        }
    }
}