package com.example.sanbotapp.ajustes;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.MainActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.WelcomeActivity;

public class AjustesActivity extends BaseActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_VOLUMEN = "ajuste_volumen";
    private static final String KEY_BRILLO = "ajuste_brillo";
    private static final String KEY_NOMBRE = "nombre_usuario";
    private static final String KEY_FOTO_URI = "foto_uri";
    private static final String KEY_FIRST_RUN = "first_run";

    private EditText etVolumen;
    private EditText etBrillo;

    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        // Botón para borrar cuenta
        Button btnBorrarCuenta = findViewById(R.id.btnBorrarCuenta);
        btnBorrarCuenta.setOnClickListener(v -> mostrarDialogoConfirmacion());
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

    /**
     * Muestra un diálogo de confirmación antes de borrar la cuenta
     */
    private void mostrarDialogoConfirmacion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Borrar Cuenta");
        builder.setMessage("¿Estás seguro de que quieres borrar tu cuenta y todos tus datos?\n\nEsta acción no se puede deshacer.");
        
        builder.setPositiveButton("Borrar", (dialog, which) -> borrarCuenta());
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Borra todos los datos del usuario y vuelve a WelcomeActivity
     */
    private void borrarCuenta() {
        // Borrar todos los datos de SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_NOMBRE);
        editor.remove(KEY_FOTO_URI);
        editor.remove(KEY_VOLUMEN);
        editor.remove(KEY_BRILLO);
        editor.putBoolean(KEY_FIRST_RUN, true); // Marcar como primera vez de nuevo
        editor.apply();

        // Volver a WelcomeActivity
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}