package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class FinBingoActivity extends BaseActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_bingo);
        setupTopBackBanner("Juego de Bingo");

        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int total = getIntent().getIntExtra("TOTAL", 0);
        String mensaje = getIntent().getStringExtra("MENSAJE");
        if (mensaje == null) mensaje = "¡Línea y bingo! ¡Es usted un hacha!";

        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) tvRefran.setText(mensaje);

        findViewById(R.id.btnJugarOtraVez).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegoBingoActivity.class));
            finish();
        });
        
        findViewById(R.id.btnVolverMenu).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegosActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}
