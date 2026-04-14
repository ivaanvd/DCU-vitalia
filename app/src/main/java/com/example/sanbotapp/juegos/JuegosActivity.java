package com.example.sanbotapp.juegos;
import android.content.Intent;
import android.os.Bundle;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class JuegosActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juegos);
        setupTopBackBanner("Juegos");
        
        findViewById(R.id.btnRefranes).setOnClickListener(v -> startActivity(new Intent(this, JuegoRefranesActivity.class)));
        findViewById(R.id.btnBuscaEncuentra).setOnClickListener(v -> startActivity(new Intent(this, JuegoBuscaEncuentraActivity.class)));
        findViewById(R.id.btnMiniRosco).setOnClickListener(v -> startActivity(new Intent(this, ElegirTematicaRoscoActivity.class)));
        findViewById(R.id.btnBingo).setOnClickListener(v -> startActivity(new Intent(this, JuegoBingoActivity.class)));
    }
}
