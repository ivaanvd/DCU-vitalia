package com.example.sanbotapp.ajustes;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class AjustesActivity extends BaseActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_VOLUMEN = "ajuste_volumen";
    private static final String KEY_BRILLO = "ajuste_brillo";

    private EditText etVolumen;
    private EditText etBrillo;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        setupTopBackBanner("Ajustes");

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etVolumen = findViewById(R.id.etVolumen);
        etBrillo = findViewById(R.id.etBrillo);

        TextView btnMenosVolumen = findViewById(R.id.btnMenosVolumen);
        TextView btnMasVolumen = findViewById(R.id.btnMasVolumen);
        TextView btnMenosBrillo = findViewById(R.id.btnMenosBrillo);
        TextView btnMasBrillo = findViewById(R.id.btnMasBrillo);

        int volumenInicial = prefs.getInt(KEY_VOLUMEN, 70);
        int brilloInicial = prefs.getInt(KEY_BRILLO, 60);

        setValorVolumen(volumenInicial);
        setValorBrillo(brilloInicial);

        btnMenosVolumen.setOnClickListener(v -> cambiarVolumen(-1));
        btnMasVolumen.setOnClickListener(v -> cambiarVolumen(1));
        btnMenosBrillo.setOnClickListener(v -> cambiarBrillo(-1));
        btnMasBrillo.setOnClickListener(v -> cambiarBrillo(1));

        etVolumen.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoVolumen();
        });

        etBrillo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoBrillo();
        });
    }

    private void cambiarVolumen(int delta) {
        int actual = leerEditTextSeguro(etVolumen, 70);
        int nuevo = limitar(actual + delta);
        setValorVolumen(nuevo);
        guardarVolumen(nuevo);
    }

    private void cambiarBrillo(int delta) {
        int actual = leerEditTextSeguro(etBrillo, 60);
        int nuevo = limitar(actual + delta);
        setValorBrillo(nuevo);
        guardarBrillo(nuevo);
    }

    private void aplicarTextoVolumen() {
        int valor = leerEditTextSeguro(etVolumen, 70);
        valor = limitar(valor);
        setValorVolumen(valor);
        guardarVolumen(valor);
    }

    private void aplicarTextoBrillo() {
        int valor = leerEditTextSeguro(etBrillo, 60);
        valor = limitar(valor);
        setValorBrillo(valor);
        guardarBrillo(valor);
    }

    private void setValorVolumen(int valor) {
        etVolumen.setText(String.valueOf(valor));
        etVolumen.setSelection(etVolumen.getText().length());
    }

    private void setValorBrillo(int valor) {
        etBrillo.setText(String.valueOf(valor));
        etBrillo.setSelection(etBrillo.getText().length());
    }

    private void guardarVolumen(int valor) {
        prefs.edit().putInt(KEY_VOLUMEN, valor).apply();
    }

    private void guardarBrillo(int valor) {
        prefs.edit().putInt(KEY_BRILLO, valor).apply();
    }

    private int leerEditTextSeguro(EditText editText, int valorPorDefecto) {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) return valorPorDefecto;
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }

    private int limitar(int valor) {
        if (valor < 0) return 0;
        if (valor > 100) return 100;
        return valor;
    }
}