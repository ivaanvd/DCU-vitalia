package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class FinBuscaEncuentraActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_busca_encuentra);
        setupTopBackBanner("Busca y Encuentra");

        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) {
            tvRefran.setText("¡Con tu ayuda lo he encontrado! ¡Eres increíble!");
        }

        findViewById(R.id.btnJugarOtraVez).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegoBuscaEncuentraActivity.class));
            finish();
        });

        findViewById(R.id.btnVolverMenu).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegosActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}