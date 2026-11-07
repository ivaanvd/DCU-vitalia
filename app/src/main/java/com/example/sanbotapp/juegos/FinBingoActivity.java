package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class FinBingoActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_bingo);
        setupTopBackBanner(getString(R.string.juego_bingo_titulo));

        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int fallos = getIntent().getIntExtra("FALLOS", 0); // ← AÑADIR
        int total = getIntent().getIntExtra("TOTAL", 0);
        String mensaje = getIntent().getStringExtra("MENSAJE");
        if (mensaje == null) mensaje = getString(R.string.bingo_fin_defecto);

        if (aciertos >= total / 2) {
            gestionarFeedbackHardware("CELEBRACION");
            hablarOSimular(getString(R.string.bingo_fin_enhorabuena));
        } else {
            gestionarFeedbackHardware("MOURN");
            hablarOSimular(getString(R.string.bingo_fin_animo));
        }


        TextView tvAciertos = findViewById(R.id.tvAciertosBingo);
        if (tvAciertos != null) tvAciertos.setText(String.valueOf(aciertos));

        TextView tvFallos = findViewById(R.id.tvFallosBingo);
        if (tvFallos != null) tvFallos.setText(String.valueOf(fallos));

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
