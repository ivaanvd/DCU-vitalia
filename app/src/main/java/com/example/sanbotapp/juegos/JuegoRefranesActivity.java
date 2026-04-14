package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
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
    
    // Colores para el feedback visual
    private static final String COLOR_CORRECTO = "#00CC00";  // Verde
    private static final String COLOR_INCORRECTO = "#FF0000"; // Rojo
    private static final String COLOR_NORMAL = "#FFFFFF";     // Azul original

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_refranes);
        setupTopBackBanner("Juego de Refranes");

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
        listaPreguntas = new ArrayList<>();
        listaPreguntas.add(new PreguntaRefran("A quien madruga, Dios le...", "Ayuda", "Mira", 1));
        listaPreguntas.add(new PreguntaRefran("Más vale pájaro en mano que...", "Ciento volando", "Dos saltando", 1));
        listaPreguntas.add(new PreguntaRefran("En boca cerrada no entran...", "Sombras", "Moscas", 2));
        listaPreguntas.add(new PreguntaRefran("A caballo regalado no le mires el...", "Diente", "Pelo", 1));
    }

    private void mostrarPregunta() {
        if (indiceActual < listaPreguntas.size()) {
            PreguntaRefran actual = listaPreguntas.get(indiceActual);

            if (tvEstadoPregunta != null) {
                tvEstadoPregunta.setText("PREGUNTA " + (indiceActual + 1) + " DE " + listaPreguntas.size());
            }

            if (tvRefran != null) {
                tvRefran.setText(actual.getTextoCuestion());
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
        // Cambiar color del botón a verde
        Button boton = (seleccion == 1) ? btnOpcion1 : btnOpcion2;
        boton.setBackgroundResource(R.drawable.bg_tipo_correcto);
        boton.setTextColor(Color.WHITE);
        
        // El robot habla felicidad
        hablarOSimular("¡Excelente! ¡Acertaste!");
        
        // Después de 2 segundos, mostrar siguiente pregunta
        handler.postDelayed(() -> {
            restaurarBotones();
            indiceActual++;
            mostrarPregunta();
            juegoBloqueado = false;
        }, 2000);
    }

    private void mostrarFeedbackIncorrecto(int seleccionIncorrecta, int correcta) {
        Button botonIncorrecto = (seleccionIncorrecta == 1) ? btnOpcion1 : btnOpcion2;
        botonIncorrecto.setBackgroundResource(R.drawable.bg_tipo_incorrecto);
        botonIncorrecto.setTextColor(Color.WHITE);

        Button botonCorrecto = (correcta == 1) ? btnOpcion1 : btnOpcion2;
        botonCorrecto.setBackgroundResource(R.drawable.bg_tipo_correcto);
        botonCorrecto.setTextColor(Color.WHITE);

        hablarOSimular("¡No! Esa no era la respuesta. La correcta es la otra.");

        handler.postDelayed(() -> {
            restaurarBotones();
            indiceActual++;
            mostrarPregunta();
            juegoBloqueado = false;
        }, 2500);
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
            mensaje = "¡Excelente! Te sabes todos los refranes perfectamente.";
        } else if (aciertos >= listaPreguntas.size() / 2) {
            mensaje = "¡Muy bien! Te acuerdas de casi todo.";
        } else {
            mensaje = "Buen intento, seguro que en la próxima aciertas más.";
        }
        intent.putExtra("MENSAJE", mensaje);

        startActivity(intent);
        finish();
    }
}