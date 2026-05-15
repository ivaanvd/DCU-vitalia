package com.example.sanbotapp.ajustes;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.WelcomeActivity;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.recordatorio.RecordatorioRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import com.example.sanbotapp.util.AppConstants;
import com.example.sanbotapp.util.PhotoHelper;

public class AjustesActivity extends BaseActivity {

    private EditText etNombre;
    private ImageView ivAvatar;
    private String rutaFoto;

    private EditText etVolumen;
    private EditText etBrillo;
    private SharedPreferences prefs;
    private PhotoHelper photoHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        setupTopBackBanner("Ajustes");

        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        etVolumen = findViewById(R.id.etVolumen);
        etBrillo  = findViewById(R.id.etBrillo);
        etNombre  = findViewById(R.id.etNombreAjustes);
        ivAvatar  = findViewById(R.id.ivAvatarAjustes);

        TextView btnMenosVolumen = findViewById(R.id.btnMenosVolumen);
        TextView btnMasVolumen   = findViewById(R.id.btnMasVolumen);
        TextView btnMenosBrillo  = findViewById(R.id.btnMenosBrillo);
        TextView btnMasBrillo    = findViewById(R.id.btnMasBrillo);

        Button btnCamara  = findViewById(R.id.btnCambiarFotoCamara);
        Button btnGaleria = findViewById(R.id.btnCambiarFotoGaleria);

        photoHelper = new PhotoHelper(this, new PhotoHelper.PhotoCallback() {
            @Override
            public void onPhotoReady(String ruta) {
                rutaFoto = ruta;
                PhotoHelper.mostrarFotoDesdeRuta(ruta, ivAvatar);
                prefs.edit().putString(AppConstants.KEY_FOTO_PATH, ruta).apply();
                hablarEnMain("He actualizado tu foto de perfil.");
            }
            @Override
            public void onPermissionDenied() {
                Toast.makeText(AjustesActivity.this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Cargar valores reales del sistema (sin aplicarlos) ───────────────
        // Si nunca se han guardado, leemos del sistema; si ya había un valor
        // guardado por el usuario, lo respetamos.
        int volumenInicial = prefs.contains(AppConstants.KEY_VOLUMEN)
                ? prefs.getInt(AppConstants.KEY_VOLUMEN, 70)
                : getVolumenSistema();

        int brilloInicial = prefs.contains(AppConstants.KEY_BRILLO)
                ? prefs.getInt(AppConstants.KEY_BRILLO, 60)
                : getBrilloSistema();

        // Solo mostramos los valores en pantalla, NO los aplicamos al hardware
        setValorVolumen(volumenInicial);
        setValorBrillo(brilloInicial);

        // ── Listeners de botones ─────────────────────────────────────────────
        btnMenosVolumen.setOnClickListener(v -> cambiarVolumen(-1));
        btnMasVolumen  .setOnClickListener(v -> cambiarVolumen(+1));
        btnMenosBrillo .setOnClickListener(v -> cambiarBrillo(-1));
        btnMasBrillo   .setOnClickListener(v -> cambiarBrillo(+1));

        btnCamara.setOnClickListener(v -> photoHelper.checkCameraPermissionAndOpen());
        btnGaleria.setOnClickListener(v -> photoHelper.checkStoragePermissionAndOpen());

        // ── Cargar datos de perfil ───────────────────────────────────────────
        String nombre = prefs.getString(AppConstants.KEY_NOMBRE, "Usuario");
        etNombre.setText(nombre);
        etNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) guardarNombre();
        });

        rutaFoto = prefs.getString(AppConstants.KEY_FOTO_PATH, null);
        if (rutaFoto != null) {
            PhotoHelper.mostrarFotoDesdeRuta(rutaFoto, ivAvatar);
        }

        etVolumen.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoVolumen();
        });
        etBrillo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) aplicarTextoBrillo();
        });

        Button btnBorrarCuenta = findViewById(R.id.btnBorrarCuenta);
        btnBorrarCuenta.setOnClickListener(v -> mostrarDialogoConfirmacion());

        // Botón GUARDAR CAMBIOS
        Button btnGuardar = findViewById(R.id.btnGuardarAjustes);
        btnGuardar.setOnClickListener(v -> consolidarGuardarAjustes());

        // Verificar permiso de escritura de ajustes del sistema al entrar
        verificarPermisosBrillo();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Opcional: Podríamos no guardar aquí si queremos que sea 100% manual,
        // pero por seguridad guardaremos el nombre si ha cambiado.
        guardarNombre();
    }

    private void consolidarGuardarAjustes() {
        // 1. Nombre
        guardarNombre();

        // 2. Volumen
        int vol = limitar(leerEditTextSeguro(etVolumen, 70));
        guardarVolumen(vol);
        aplicarVolumenReal(vol);

        // 3. Brillo
        int bri = limitar(leerEditTextSeguro(etBrillo, 60));
        guardarBrillo(bri);
        aplicarBrilloReal(bri);

        // 4. Feedback
        Toast.makeText(this, "Ajustes guardados correctamente", Toast.LENGTH_SHORT).show();
        hablarEnMain("He guardado todos tus cambios.");
        
        // Volver a inicio
        Intent intent = new Intent(this, com.example.sanbotapp.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
        prefs.edit().putInt(AppConstants.KEY_VOLUMEN, valor).apply();
    }

    private void guardarBrillo(int valor) {
        prefs.edit().putInt(AppConstants.KEY_BRILLO, valor).apply();
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
                .remove(AppConstants.KEY_NOMBRE)
                .remove(AppConstants.KEY_FOTO_PATH)
                .remove(AppConstants.KEY_VOLUMEN)
                .remove(AppConstants.KEY_BRILLO)
                .putBoolean(AppConstants.KEY_FIRST_RUN, true)
                .apply();

        // Eliminar actividades y recordatorios de la base de datos (SharedPreferences)
        new ActividadRepository(this).deleteAll();
        new RecordatorioRepository(this).deleteAll();

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── Perfil (Nombre y Foto) ───────────────────────────────────────────────

    private void guardarNombre() {
        String nombre = etNombre.getText().toString().trim();
        if (!nombre.isEmpty()) {
            prefs.edit().putString(AppConstants.KEY_NOMBRE, nombre).apply();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        photoHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        photoHelper.handlePermissionsResult(requestCode, grantResults);
    }
}
