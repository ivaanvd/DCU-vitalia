package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class ElegirTematicaRoscoActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegir_tematica_rosco);
        setupTopBackBanner("Elegir Temática");
        
        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) {
            tvRefran.setText("¿De qué hablamos hoy? Selecciona algo que te guste.");
        }
        
        findViewById(R.id.btnTema1).setOnClickListener(v -> lanzarJuego("Flora y Plantas"));
        findViewById(R.id.btnTema2).setOnClickListener(v -> lanzarJuego("Frutas"));
        findViewById(R.id.btnTema3).setOnClickListener(v -> lanzarJuego("Animales"));
    }
    
    private void lanzarJuego(String tema) {
        Intent i = new Intent(this, JuegoMiniRoscoActivity.class);
        i.putExtra("TEMA", tema);
        startActivity(i);
        finish();
    }
}
