package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class ElegirTematicaRoscoActivity extends BaseActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegir_tematica_rosco);
        setupTopBackBanner(getString(R.string.rosco_elegir_tematica_titulo));
        
        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) {
            tvRefran.setText(R.string.rosco_elegir_tematica);
        }
        
        // Las temáticas salen del mismo fichero que las preguntas (res/raw/juego_rosco.json)
        java.util.List<String> categorias = ContenidoJuegos.temasRosco(this);
        LinearLayout llContenedorBotones = findViewById(R.id.llContenedorBotones);
        
        // Generar los botones dinámicamente
        if (llContenedorBotones != null) {
            for (String categoria : categorias) {
                Button btnTema = crearBotonTema(categoria);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (int)(100 * getResources().getDisplayMetrics().density)
                );
                params.bottomMargin = (int)(40 * getResources().getDisplayMetrics().density);
                btnTema.setLayoutParams(params);
                llContenedorBotones.addView(btnTema);
            }
        }
    }
    
    private Button crearBotonTema(String nombre) {
        Button btn = new Button(this);
        btn.setText(nombre);
        btn.setBackground(getDrawable(R.drawable.bg_tipo_normal));
        btn.setTextColor(getColor(android.R.color.black));
        btn.setTextSize(40);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> lanzarJuego(nombre));
        return btn;
    }

    private void lanzarJuego(String tema) {
        // Va directo al juego, ya explicamos antes
        Intent i = new Intent(this, JuegoMiniRoscoActivity.class);
        i.putExtra("TEMA", tema);
        startActivity(i);
        finish();
    }

    @Override
    protected void onRobotServiceReady() {
        hablarOSimular(getString(R.string.rosco_elegir_tematica));
    }
}
