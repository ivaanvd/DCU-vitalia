package com.example.sanbotapp.juegos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class FinRefranesActivity extends BaseActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fin_refranes);
        setupTopBackBanner("Juego de Refranes");

        // Recuperar envíos
        int aciertos = getIntent().getIntExtra("ACIERTOS", 0);
        int total = getIntent().getIntExtra("TOTAL", 0);
        String mensaje = getIntent().getStringExtra("MENSAJE");
        if (mensaje == null) mensaje = "¡Muy bien! ¡Tú sí que sabes de refranes!";

        // Actualizar UI
        TextView tvContador = findViewById(R.id.tvContadorAciertos);
        if (tvContador != null) {
            tvContador.setText(aciertos + "/" + total + " refranes acertados");
        }
        
        TextView tvRefran = findViewById(R.id.tvBocadilloTexto);
        if (tvRefran != null) {
            tvRefran.setText(mensaje);
        }

        // Eventos
        findViewById(R.id.btnJugarOtraVez).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegoRefranesActivity.class));
            finish();
        });
        
        findViewById(R.id.btnVolverMenu).setOnClickListener(v -> {
            startActivity(new Intent(this, JuegosActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}
