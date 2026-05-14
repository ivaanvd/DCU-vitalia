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
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import com.example.sanbotapp.WelcomeActivity;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.recordatorio.RecordatorioRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class AjustesActivity extends BaseActivity {

    private static final String PREFS_NAME    = "AppPrefs";
    private static final String KEY_VOLUMEN   = "ajuste_volumen";
    private static final String KEY_BRILLO    = "ajuste_brillo";
    private static final String KEY_NOMBRE    = "nombre_usuario";
    private static final String KEY_FOTO_PATH  = "foto_path"; // Coincide con WelcomeActivity
    private static final String KEY_FIRST_RUN = "first_run";

    private static final int REQUEST_CAMERA             = 100;
    private static final int REQUEST_GALLERY            = 101;
    private static final int CAMERA_PERMISSION_REQUEST  = 102;
    private static final int STORAGE_PERMISSION_REQUEST = 103;

    private EditText etNombre;
    private ImageView ivAvatar;
    private String rutaFoto;

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
        etNombre  = findViewById(R.id.etNombreAjustes);
        ivAvatar  = findViewById(R.id.ivAvatarAjustes);

        TextView btnMenosVolumen = findViewById(R.id.btnMenosVolumen);
        TextView btnMasVolumen   = findViewById(R.id.btnMasVolumen);
        TextView btnMenosBrillo  = findViewById(R.id.btnMenosBrillo);
        TextView btnMasBrillo    = findViewById(R.id.btnMasBrillo);

        Button btnCamara  = findViewById(R.id.btnCambiarFotoCamara);
        Button btnGaleria = findViewById(R.id.btnCambiarFotoGaleria);

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

        btnCamara.setOnClickListener(v -> abrirCamara());
        btnGaleria.setOnClickListener(v -> abrirGaleria());

        // ── Cargar datos de perfil ───────────────────────────────────────────
        String nombre = prefs.getString(KEY_NOMBRE, "Usuario");
        etNombre.setText(nombre);
        etNombre.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) guardarNombre();
        });

        rutaFoto = prefs.getString(KEY_FOTO_PATH, null);
        if (rutaFoto != null) {
            mostrarFotoDesdeRuta(rutaFoto);
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
    }

    private void hablarEnMain(String t) {
        hablarEnMain(t, null);
    }

    private void hablarEnMain(String t, com.qihancloud.opensdk.function.beans.EmotionsType emotion) {
        runOnUiThread(() -> hablarOSimular(t, emotion));
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
                .remove(KEY_FOTO_PATH)
                .remove(KEY_VOLUMEN)
                .remove(KEY_BRILLO)
                .putBoolean(KEY_FIRST_RUN, true)
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
            prefs.edit().putString(KEY_NOMBRE, nombre).apply();
        }
    }

    private void abrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            iniciarCamara();
        }
    }

    private void iniciarCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File archivoFoto = crearArchivoFoto();
            rutaFoto = archivoFoto.getAbsolutePath();
            Uri fotoUri  = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    archivoFoto);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Error al preparar cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permiso)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permiso}, STORAGE_PERMISSION_REQUEST);
            return;
        }
        lanzarSelectorGaleria();
    }

    private void lanzarSelectorGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Seleccionar imagen"), REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA) {
            if (rutaFoto != null) {
                mostrarFotoDesdeRuta(rutaFoto);
                prefs.edit().putString(KEY_FOTO_PATH, rutaFoto).apply();
            }
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    rutaFoto = copiarFotoAAlmacenamiento(imageUri);
                    mostrarFotoDesdeRuta(rutaFoto);
                    prefs.edit().putString(KEY_FOTO_PATH, rutaFoto).apply();
                } catch (IOException e) {
                    Toast.makeText(this, "Error al copiar imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void mostrarFotoDesdeRuta(String ruta) {
        Bitmap bitmap = BitmapFactory.decodeFile(ruta);
        if (bitmap != null) {
            ivAvatar.setImageBitmap(bitmap);
        }
    }

    private File crearArchivoFoto() throws IOException {
        File storageDir = getExternalFilesDir("fotos");
        if (storageDir != null && !storageDir.exists()) storageDir.mkdirs();
        return new File(storageDir, "avatar_usuario.jpg");
    }

    private String copiarFotoAAlmacenamiento(Uri sourceUri) throws IOException {
        File destFile = crearArchivoFoto();
        try (InputStream input  = getContentResolver().openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) > 0) output.write(buffer, 0, bytesRead);
        }
        return destFile.getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == CAMERA_PERMISSION_REQUEST && granted) iniciarCamara();
        else if (requestCode == STORAGE_PERMISSION_REQUEST && granted) lanzarSelectorGaleria();
    }
}
