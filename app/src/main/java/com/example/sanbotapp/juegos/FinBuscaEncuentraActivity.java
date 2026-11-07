package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.util.RobotLed;

public class FinBuscaEncuentraActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_busca_encuentra);
        setupTopBackBanner(getString(R.string.ui_busca_y_encuentra_2));

        long segundos = getIntent().getLongExtra("SEGUNDOS", 0);

        TextView tvRobot = findViewById(R.id.tvBocadilloTexto);
        if (tvRobot != null) {
            if (segundos < 3) {
                mostrarEmocion("SURPRISE");
                encenderLed(RobotLed.PART_ALL, RobotLed.MODE_GREEN);
                tvRobot.setText(R.string.busca_fin_rapido);
                hablarOSimular(getString(R.string.busca_fin_rapido));
            } else if (segundos < 10) {
                mostrarEmocion("PRISE");
                encenderLed(RobotLed.PART_ALL, RobotLed.MODE_GREEN);
                tvRobot.setText(R.string.busca_fin_medio);
                hablarOSimular(getString(R.string.busca_fin_medio));
            } else {
                mostrarEmocion("SMILE");
                encenderLed(RobotLed.PART_ALL, RobotLed.MODE_GREEN);
                tvRobot.setText(R.string.busca_fin_lento);
                hablarOSimular(getString(R.string.busca_fin_lento));
            }
        }

        TextView tvTiempo = findViewById(R.id.tvTiempoBusca);
        if (tvTiempo != null) {
            tvTiempo.setText(segundos + " s");
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