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
        setupTopBackBanner("Elegir Temática");
        
        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) {
            tvRefran.setText("¿De qué hablamos hoy? Selecciona algo que te guste.");
        }
        
        // Obtener las categorías desde strings.xml
        String[] categorias = getResources().getStringArray(R.array.rosco_categorias);
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
}
