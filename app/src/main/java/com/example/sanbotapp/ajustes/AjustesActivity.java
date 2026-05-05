package com.example.sanbotapp.ajustes;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
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
        etBrillo  = findViewById(R.id.etBrillo);

        TextView btnMenosVolumen = findViewById(R.id.btnMenosVolumen);
        TextView btnMasVolumen   = findViewById(R.id.btnMasVolumen);
        TextView btnMenosBrillo  = findViewById(R.id.btnMenosBrillo);
        TextView btnMasBrillo    = findViewById(R.id.btnMasBrillo);

        // Cargar valores guardados y aplicarlos al arrancar la pantalla
        int volumenInicial = prefs.getInt(KEY_VOLUMEN, 70);
        int brilloInicial  = prefs.getInt(KEY_BRILLO, 60);

        setValorVolumen(volumenInicial);
        setValorBrillo(brilloInicial);

        // Aplicar en el robot/pantalla inmediatamente al entrar en Ajustes
        aplicarVolumenReal(volumenInicial);
        aplicarBrilloReal(brilloInicial);

        btnMenosVolumen.setOnClickListener(v -> cambiarVolumen(-1));
        btnMasVolumen  .setOnClickListener(v -> cambiarVolumen(+1));
        btnMenosBrillo .setOnClickListener(v -> cambiarBrillo(-1));
        btnMasBrillo   .setOnClickListener(v -> cambiarBrillo(+1));

        etVolumen.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoVolumen();
        });
        etBrillo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoBrillo();
        });

        Button btnBorrarCuenta = findViewById(R.id.btnBorrarCuenta);
        btnBorrarCuenta.setOnClickListener(v -> mostrarDialogoConfirmacion());
    }

    // ── Volumen ───────────────────────────────────────────────────────────────

    private void cambiarVolumen(int delta) {
        int nuevo = limitar(leerEditTextSeguro(etVolumen, 70) + delta);
        setValorVolumen(nuevo);
        guardarVolumen(nuevo);
        aplicarVolumenReal(nuevo);          // ← NUEVO: aplica al robot
    }

    private void aplicarTextoVolumen() {
        int valor = limitar(leerEditTextSeguro(etVolumen, 70));
        setValorVolumen(valor);
        guardarVolumen(valor);
        aplicarVolumenReal(valor);          // ← NUEVO: aplica al robot
    }

    /**
     * Convierte el porcentaje (0-100) al rango del AudioManager y lo aplica.
     * setVolumenRobot() vive en BaseActivity y usa AudioControl.
     */
    private void aplicarVolumenReal(int porcentaje) {
        setVolumenRobot(porcentaje);
    }

    // ── Brillo ────────────────────────────────────────────────────────────────

    private void cambiarBrillo(int delta) {
        int nuevo = limitar(leerEditTextSeguro(etBrillo, 60) + delta);
        setValorBrillo(nuevo);
        guardarBrillo(nuevo);
        aplicarBrilloReal(nuevo);           // ← NUEVO: aplica a la pantalla
    }

    private void aplicarTextoBrillo() {
        int valor = limitar(leerEditTextSeguro(etBrillo, 60));
        setValorBrillo(valor);
        guardarBrillo(valor);
        aplicarBrilloReal(valor);           // ← NUEVO: aplica a la pantalla
    }

    /**
     * Convierte el porcentaje (0-100) a la escala de brillo del sistema (0-255)
     * y lo aplica mediante Settings.System.
     *
     * Requiere permiso WRITE_SETTINGS en el Manifest y que el usuario lo haya
     * concedido (Settings.System.canWrite(context)).
     */
    private void aplicarBrilloReal(int porcentaje) {
        int brillo255 = Math.round(porcentaje * 255f / 100f);
        try {
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brillo255
            );
        } catch (Exception e) {
            android.util.Log.e("AjustesActivity", "No se pudo cambiar el brillo: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        return Math.max(0, Math.min(100, valor));
    }

    // ── Borrar cuenta ─────────────────────────────────────────────────────────

    private void mostrarDialogoConfirmacion() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Borrar Cuenta")
                .setMessage("¿Estás seguro de que quieres borrar tu cuenta y todos tus datos?\n\nEsta acción no se puede deshacer.")
                .setPositiveButton("Borrar",   (d, w) -> borrarCuenta())
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create()
                .show();
    }

    private void borrarCuenta() {
        prefs.edit()
                .remove(KEY_NOMBRE)
                .remove(KEY_FOTO_URI)
                .remove(KEY_VOLUMEN)
                .remove(KEY_BRILLO)
                .putBoolean(KEY_FIRST_RUN, true)
                .apply();

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}