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
        setupTopBackBanner("Juego de Bingo");

        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int fallos = getIntent().getIntExtra("FALLOS", 0); // ← AÑADIR
        int total = getIntent().getIntExtra("TOTAL", 0);
        String mensaje = getIntent().getStringExtra("MENSAJE");
        if (mensaje == null) mensaje = "¡Línea y bingo! ¡Es usted un hacha!";

        TextView tvRobot = findViewById(R.id.tvBocadilloTexto);
        if (tvRobot != null) tvRobot.setText(mensaje);


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
