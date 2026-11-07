package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class FinMiniRoscoActivity extends BaseActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_mini_rosco);
        setupTopBackBanner(getString(R.string.juego_rosco_titulo));

        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int total = getIntent().getIntExtra("TOTAL", 1); // evitar div/0
        String mensaje = getIntent().getStringExtra("MENSAJE");
        
        if (aciertos >= (total / 2)) {
            gestionarFeedbackHardware("CELEBRACION");
            if (mensaje == null) mensaje = getString(R.string.rosco_fin_bien);
        } else {
            gestionarFeedbackHardware("MOURN");
            if (mensaje == null) mensaje = getString(R.string.rosco_fin_intento);
        }
        
        hablarOSimular(mensaje);

        TextView tvContador = findViewById(R.id.tvAciertosRosco);
        if (tvContador != null) tvContador.setText(getString(R.string.rosco_fin_contador, aciertos, total));
        
        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) tvRefran.setText(mensaje);

        findViewById(R.id.btnJugarOtraVez).setOnClickListener(v -> {
            startActivity(new Intent(this, ElegirTematicaRoscoActivity.class));
            finish();
        });
        
        findViewById(R.id.btnVolverMenu).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegosActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}
