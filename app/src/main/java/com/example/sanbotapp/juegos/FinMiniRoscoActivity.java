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
        setupTopBackBanner("Mini-Rosco");

        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int total = getIntent().getIntExtra("TOTAL", 1); // evitar div/0
        String mensaje = getIntent().getStringExtra("MENSAJE");
        
        if (aciertos >= (total / 2)) {
            mostrarEmocion("PRISE");
            encenderLed(com.qihancloud.opensdk.function.beans.LED.PART_ALL, com.qihancloud.opensdk.function.beans.LED.MODE_GREEN);
            if (mensaje == null) mensaje = "¡Misión cumplida! ¡Qué bien se te da esto!";
        } else {
            mostrarEmocion("CRY");
            encenderLed(com.qihancloud.opensdk.function.beans.LED.PART_ALL, com.qihancloud.opensdk.function.beans.LED.MODE_RED);
            if (mensaje == null) mensaje = "¡Buen intento! Seguro que la próxima vez lo haces mejor.";
        }
        
        hablarOSimular(mensaje);

        TextView tvContador = findViewById(R.id.tvAciertosRosco);
        if (tvContador != null) tvContador.setText(aciertos + "/" + total + " palabras");
        
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
