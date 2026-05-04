package com.example.sanbotapp.juegos;
import android.content.Intent;
import android.os.Bundle;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class JuegosActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_juegos);
        setupTopBackBanner("Juegos");
        
        findViewById(R.id.btnRefranes).setOnClickListener(v -> lanzarExplicacion("REFRANES"));
        findViewById(R.id.btnBuscaEncuentra).setOnClickListener(v -> lanzarExplicacion("BUSCA_ENCUENTRA"));
        findViewById(R.id.btnMiniRosco).setOnClickListener(v -> lanzarExplicacion("ROSCO"));
        findViewById(R.id.btnBingo).setOnClickListener(v -> lanzarExplicacion("BINGO"));
    }

    private void lanzarExplicacion(String tipoJuego) {
        Intent intent = new Intent(this, ExplicacionJuegoActivity.class);
        intent.putExtra("TIPO_JUEGO", tipoJuego);
        startActivity(intent);
    }
}
