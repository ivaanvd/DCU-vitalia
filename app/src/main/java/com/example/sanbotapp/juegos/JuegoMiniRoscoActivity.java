package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import java.util.ArrayList;
import java.util.List;

public class JuegoMiniRoscoActivity extends BaseActivity {

    private List<PreguntaRosco> todasLasPreguntas;
    private List<PreguntaRosco> preguntasDelTema;
    private int indiceActual = 0;
    private int aciertos = 0;
    private String temaSeleccionado;

    private TextView tvLetra;
    private TextView tvPistaRobot;
    private Button btnOpcion1;
    private Button btnOpcion2;
    private LinearLayout llContenedorLetras;
    private List<TextView> vistasLetras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juego_mini_rosco);
        setupTopBackBanner("Mini-Rosco");

        temaSeleccionado = getIntent().getStringExtra("TEMA");
        if (temaSeleccionado == null) temaSeleccionado = "Varios";

        TextView tvCabecera = findViewById(R.id.tvTemaRosco);
        if (tvCabecera != null) {
            tvCabecera.setText("TEMÁTICA: " + temaSeleccionado.toUpperCase());
        }

        tvLetra = findViewById(R.id.tvProgresoLetra);
        tvPistaRobot = findViewById(R.id.tvBocadilloTexto);
        btnOpcion1 = findViewById(R.id.btnRoscoOpcion1);
        btnOpcion2 = findViewById(R.id.btnRoscoOpcion2);
        llContenedorLetras = findViewById(R.id.llContenedorLetras);

        cargarBBDDPreguntas();
        preguntasDelTema = new ArrayList<>();
        for (PreguntaRosco p : todasLasPreguntas) {
            if (p.getTematica().equalsIgnoreCase(temaSeleccionado)) {
                preguntasDelTema.add(p);
            }
        }
        
        if (preguntasDelTema.isEmpty()) {
            preguntasDelTema.add(new PreguntaRosco(temaSeleccionado, "A", "Empieza por A: Opción falsa por defecto", "Amarillo", "Azul", 1));
        }

        inicializarFilaLetras();
        mostrarPregunta();

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

            if (llContenedorLetras != null) {
                llContenedorLetras.addView(tv);
            }
            vistasLetras.add(tv);
        }
    }

    private void cargarBBDDPreguntas() {
        todasLasPreguntas = new ArrayList<>();
        // Plantas
        todasLasPreguntas.add(new PreguntaRosco("Flora y Plantas", "A", "Árbol caducifolio muy común que da de fruto la bellota.", "Arce", "Alcornoque", 2));
        todasLasPreguntas.add(new PreguntaRosco("Flora y Plantas", "C", "Flor espinosa de muchos colores.", "Clavel", "Cactus", 1));
        todasLasPreguntas.add(new PreguntaRosco("Flora y Plantas", "O", "Árbol que da pequeñas frutas para extraer aceite.", "Olmo", "Olivo", 2));
        // Frutas
        todasLasPreguntas.add(new PreguntaRosco("Frutas", "M", "Fruta jugosa y dulce, puede ser roja o verde.", "Manzana", "Mora", 1));
        todasLasPreguntas.add(new PreguntaRosco("Frutas", "P", "Fruta amarilla de Canarias.", "Pera", "Plátano", 2));
        todasLasPreguntas.add(new PreguntaRosco("Frutas", "K", "Fruta ovalada de carne verde.", "Kiwi", "Kaki", 1));
        // Animales
        todasLasPreguntas.add(new PreguntaRosco("Animales", "P", "Animal doméstico considerado el mejor amigo del hombre.", "Perro", "Pato", 1));
        todasLasPreguntas.add(new PreguntaRosco("Animales", "G", "Felino que hace miau.", "Gato", "Gorila", 1));
        todasLasPreguntas.add(new PreguntaRosco("Animales", "L", "El llamado 'rey de la selva'.", "León", "Lince", 1));
    }

    private void mostrarPregunta() {
        if (indiceActual < preguntasDelTema.size()) {
            // Teñir círculo actual de Azul
            vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_actual);

            PreguntaRosco p = preguntasDelTema.get(indiceActual);
            if (tvLetra != null) tvLetra.setVisibility(TextView.GONE);
            if (tvPistaRobot != null) {
                tvPistaRobot.setText("CON LA " + p.getLetra() + ": \"" + p.getPista() + "\"");
            }
            if (btnOpcion1 != null) btnOpcion1.setText(p.getOpcion1());
            if (btnOpcion2 != null) btnOpcion2.setText(p.getOpcion2());
        } else {
            finalizarJuego();
        }
    }

    private void procesarRespuesta(int seleccion) {
        if (seleccion == preguntasDelTema.get(indiceActual).getIndiceCorrecto()) {
            aciertos++;
            vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_correcta);
        } else {
            vistasLetras.get(indiceActual).setBackgroundResource(R.drawable.bg_rosco_letra_incorrecta);
        }
        indiceActual++;
        mostrarPregunta();
    }

    private void finalizarJuego() {
        Intent intent = new Intent(this, FinMiniRoscoActivity.class);
        intent.putExtra("ACIERTOS", aciertos);
        intent.putExtra("TOTAL", preguntasDelTema.size());
        intent.putExtra("TEMA", temaSeleccionado);
        
        String sms = (aciertos == preguntasDelTema.size()) 
                     ? "¡Pleno! Un rosco impoluto." 
                     : "Ese rosco se nos ha resistido un poco, ¡pero muy buen esfuerzo!";
        intent.putExtra("MENSAJE", sms);
        
        startActivity(intent);
        finish();
    }
}
