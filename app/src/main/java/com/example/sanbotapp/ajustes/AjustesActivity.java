package com.example.sanbotapp.ajustes;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.WelcomeActivity;

public class AjustesActivity extends BaseActivity {

    private static final String PREFS_NAME    = "AppPrefs";
    private static final String KEY_VOLUMEN   = "ajuste_volumen";
    private static final String KEY_BRILLO    = "ajuste_brillo";
    private static final String KEY_NOMBRE    = "nombre_usuario";
    private static final String KEY_FOTO_URI  = "foto_uri";
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

        // ── Cargar valores reales del sistema (sin aplicarlos) ───────────────
        // Si nunca se han guardado, leemos del sistema; si ya había un valor
        // guardado por el usuario, lo respetamos.
        int volumenInicial = prefs.contains(KEY_VOLUMEN)
                ? prefs.getInt(KEY_VOLUMEN, 70)
                : getVolumenSistema();

        int brilloInicial = prefs.contains(KEY_BRILLO)
                ? prefs.getInt(KEY_BRILLO, 60)
                : getBrilloSistema();

        // Solo mostramos los valores en pantalla, NO los aplicamos al hardware
        setValorVolumen(volumenInicial);
        setValorBrillo(brilloInicial);

        // ── Listeners de botones ─────────────────────────────────────────────
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

        // Verificar permiso de escritura de ajustes del sistema al entrar
        verificarPermisosBrillo();
    }


    // ── Volumen ───────────────────────────────────────────────────────────────

    /**
     * Lee el volumen actual del AudioManager y lo convierte a porcentaje 0-100.
     * Se usa solo cuando no hay ningún valor guardado en SharedPreferences.
     */
    private int getVolumenSistema() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null) return 70;
        int max    = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int actual = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return Math.round(actual * 100f / max);
    }

    private void cambiarVolumen(int delta) {
        int nuevo = limitar(leerEditTextSeguro(etVolumen, 70) + delta);
        setValorVolumen(nuevo);
        guardarVolumen(nuevo);
        aplicarVolumenReal(nuevo);
    }

    private void aplicarTextoVolumen() {
        int valor = limitar(leerEditTextSeguro(etVolumen, 70));
        setValorVolumen(valor);
        guardarVolumen(valor);
        aplicarVolumenReal(valor);
    }

    /**
     * Aplica el volumen al robot usando el método de BaseActivity,
     * que internamente usa AudioControl del SDK de Sanbot.
     */
    private void aplicarVolumenReal(int porcentaje) {
        setVolumenRobot(porcentaje);
    }


    // ── Brillo ────────────────────────────────────────────────────────────────

    /**
     * Lee el brillo actual de la pantalla del sistema y lo convierte a porcentaje 0-100.
     * Se usa solo cuando no hay ningún valor guardado en SharedPreferences.
     */
    private int getBrilloSistema() {
        try {
            int brillo255 = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
            );
            return Math.round(brillo255 * 100f / 255f);
        } catch (Settings.SettingNotFoundException e) {
            return 60; // fallback solo si el ajuste no existe
        }
    }

    /**
     * Verifica que la app tenga permiso para modificar los ajustes del sistema.
     * Si no lo tiene, muestra un diálogo para que el usuario lo conceda.
     */
    private void verificarPermisosBrillo() {
        if (!Settings.System.canWrite(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permiso necesario")
                    .setMessage("Para cambiar el brillo de la pantalla necesitamos permiso de modificar ajustes del sistema.")
                    .setPositiveButton("Conceder", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                    .show();
        }
    }

    private void cambiarBrillo(int delta) {
        int nuevo = limitar(leerEditTextSeguro(etBrillo, 60) + delta);
        setValorBrillo(nuevo);
        guardarBrillo(nuevo);
        aplicarBrilloReal(nuevo);
    }

    private void aplicarTextoBrillo() {
        int valor = limitar(leerEditTextSeguro(etBrillo, 60));
        setValorBrillo(valor);
        guardarBrillo(valor);
        aplicarBrilloReal(valor);
    }

    /**
     * Convierte el porcentaje (0-100) a la escala de brillo del sistema (0-255)
     * y lo aplica mediante Settings.System.
     *
     * Primero desactiva el brillo automático para que el cambio manual tenga efecto.
     * Requiere permiso WRITE_SETTINGS en el Manifest y que el usuario lo haya concedido.
     */
    private void aplicarBrilloReal(int porcentaje) {
        if (!Settings.System.canWrite(this)) {
            verificarPermisosBrillo();
            return;
        }
        try {
            // Desactivar brillo automático; si sigue en auto, el cambio manual no surte efecto
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );

            int brillo255 = Math.round(porcentaje * 255f / 100f);
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