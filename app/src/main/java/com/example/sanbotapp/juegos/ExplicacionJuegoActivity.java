package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

/**
 * Activity que muestra una pantalla de explicación antes de iniciar un juego.
 * Para Mini-Rosco, permite seleccionar la categoría antes de empezar.
 * El robot lee la explicación en voz alta.
 */
public class ExplicacionJuegoActivity extends BaseActivity {

    private String tipoJuego;
    private String tema;
    private String explicacion;
    private Button btnEmpezarJuego;
    private Button btnVolverExplicar;
    private TextView tvExplicacion;
    private LinearLayout llCategorias;
    private boolean temaSeleccionado = false;

    // Mapeo de explicaciones por tipo de juego
    private static final String EXPLICACION_ROSCO =
            "En el Mini-Rosco debes hacer un recorrido circular respondiendo preguntas. " +
                    "Cada letra tiene una pregunta, ¡pero solo versión simplificada! " +
                    "Responde correctamente todas las preguntas para completar la ronda. " +
                    "Presiona el botón de la respuesta correcta y ¡verás cómo avanzas por el círculo!";

    private static final String EXPLICACION_BINGO =
            "¡Es hora de jugar al Bingo! Se mostrarán números uno a uno, " +
                    "y debes buscarlos en tu cartilla. Marca los números que coincidan. " +
                    "El primer jugador en completar una línea, columna o diagonal... " +
                    "¡grita BINGO y gana la partida! ¿Serás tú el ganador?";

    private static final String EXPLICACION_BUSCA_ENCUENTRA =
            "¡Prepárate para buscar! En este juego verás imágenes y tendrás que encontrar " +
                    "lo que se te pide. Mira cuidadosamente y toca la parte de la imagen donde creas " +
                    "que está lo que buscas. Tienes que ser rápido y atinado. ¿Puedes encontrarlo todo?";

    private static final String EXPLICACION_REFRANES =
            "En el juego de Refranes te diremos la primera parte de un dicho popular, " +
                    "y tu deberás completarla con la segunda parte correcta. " +
                    "Elige entre dos opciones. Algunos refranes son muy conocidos, " +
                    "¡pero otros pueden sorprenderte! ¿Cuántos refranes completos lograrás acertar?";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explicacion_juego);

        // Obtener los parámetros de intent
        tipoJuego = getIntent().getStringExtra("TIPO_JUEGO");
        tema = getIntent().getStringExtra("TEMA");

        if (tipoJuego == null) tipoJuego = "ROSCO";

        // Configurar el banner
        setupTopBackBanner("Explicación del Juego");

        // Obtener referencias a los views
        tvExplicacion = findViewById(R.id.tvExplicacion);
        TextView tvTituloJuego = findViewById(R.id.tvTituloJuego);
        btnEmpezarJuego = findViewById(R.id.btnEmpezarJuego);
        btnVolverExplicar = findViewById(R.id.btnVolverExplicar);
        llCategorias = findViewById(R.id.llCategorias);

        // Seleccionar la explicación según el tipo de juego
        seleccionarExplicacion();

        // Mostrar el título del juego
        if (tvTituloJuego != null) {
            switch (tipoJuego) {
                case "BINGO":
                    tvTituloJuego.setText("Bingo");
                    break;
                case "BUSCA_ENCUENTRA":
                    tvTituloJuego.setText("Busca & Encuentra");
                    break;
                case "REFRANES":
                    tvTituloJuego.setText("Refranes");
                    break;
                case "ROSCO":
                default:
                    tvTituloJuego.setText("Mini-Rosco");
                    break;
            }
        }

        // Mostrar la explicación en el TextView
        if (tvExplicacion != null) {
            tvExplicacion.setText(explicacion);
        }

        // Si es Mini-Rosco y no hay tema seleccionado, mostrar categorías
        if ("ROSCO".equals(tipoJuego) && tema == null) {
            mostrarCategorias();
            temaSeleccionado = false;
        } else {
            temaSeleccionado = true;
        }

        // Configurar listeners de botones
        if (btnEmpezarJuego != null) {
            btnEmpezarJuego.setOnClickListener(v -> empezarJuego());
        }

        if (btnVolverExplicar != null) {
            btnVolverExplicar.setOnClickListener(v -> volverExplicar());
        }
    }

    /**
     * Se llama cuando el servicio del robot está listo.
     * Aquí hacemos que el robot diga la explicación del juego.
     */
    @Override
    protected void onRobotServiceReady() {
        super.onRobotServiceReady();
        // El robot dice la explicación después de que el servicio esté listo
        hablarOSimular(explicacion);
    }

    /**
     * Selecciona la explicación según el tipo de juego.
     */
    private void seleccionarExplicacion() {
        switch (tipoJuego) {
            case "BINGO":
                explicacion = EXPLICACION_BINGO;
                break;
            case "BUSCA_ENCUENTRA":
                explicacion = EXPLICACION_BUSCA_ENCUENTRA;
                break;
            case "REFRANES":
                explicacion = EXPLICACION_REFRANES;
                break;
            case "ROSCO":
            default:
                explicacion = EXPLICACION_ROSCO;
                break;
        }
    }

    /**
     * Muestra las categorías del Mini-Rosco para que el usuario seleccione una.
     */
    private void mostrarCategorias() {
        if (llCategorias == null) return;

        // Obtener las categorías desde strings.xml
        String[] categorias = getResources().getStringArray(R.array.rosco_categorias);

        // Mostrar el contenedor
        llCategorias.setVisibility(LinearLayout.VISIBLE);

        // Generar botones para cada categoría
        for (String categoria : categorias) {
            Button btnCategoria = new Button(this);
            btnCategoria.setText(categoria);
            btnCategoria.setBackground(getDrawable(R.drawable.bg_tipo_normal));
            btnCategoria.setTextColor(getColor(android.R.color.black));
            btnCategoria.setTextSize(28);
            btnCategoria.setAllCaps(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (80 * getResources().getDisplayMetrics().density)
            );
            params.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
            btnCategoria.setLayoutParams(params);

            btnCategoria.setOnClickListener(v -> seleccionarCategoria(categoria));
            llCategorias.addView(btnCategoria);
        }
    }

    /**
     * Se llama cuando el usuario selecciona una categoría.
     */
    private void seleccionarCategoria(String categoria) {
        tema = categoria;
        temaSeleccionado = true;
        // Iniciar el juego directamente
        empezarJuego();
    }

    /**
     * El robot repite la explicación del juego.
     */
    private void volverExplicar() {
        hablarOSimular(explicacion);
    }

    /**
     * Inicia el juego correspondiente.
     */
    private void empezarJuego() {
        // Si es Mini-Rosco y no hay tema seleccionado, no hacer nada
        if ("ROSCO".equals(tipoJuego) && !temaSeleccionado) {
            return;
        }

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
            case "ROSCO":
            default:
                intent = new Intent(this, ElegirTematicaRoscoActivity.class);
                if (tema != null) {
                    intent.putExtra("TEMA", tema);
                }
                break;
        }

        startActivity(intent);
        finish();
    }
}
