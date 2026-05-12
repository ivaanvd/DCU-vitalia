package com.example.sanbotapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * WelcomeActivity
 *
 * Pantalla de bienvenida guiada por el robot.
 * - El robot saluda y explica los pasos al usuario al arrancar.
 * - Tocar la cabeza del robot (sensores 11, 12 o 13) activa el
 *   reconocimiento de voz para rellenar el campo nombre.
 * - El botón de escucha por voz ha sido eliminado (flujo más natural).
 * - El robot guía al usuario en cada paso con feedback de voz.
 */
public class WelcomeActivity extends BaseActivity {

    private static final String PREFS_NAME     = "AppPrefs";
    private static final String KEY_NOMBRE     = "nombre_usuario";
    private static final String KEY_FOTO_PATH  = "foto_path";
    private static final String KEY_FIRST_RUN  = "first_run";

    private static final int REQUEST_CAMERA             = 100;
    private static final int REQUEST_GALLERY            = 101;
    private static final int CAMERA_PERMISSION_REQUEST  = 102;
    private static final int STORAGE_PERMISSION_REQUEST = 103;

    private EditText  etNombre;
    private ImageView ivFoto;
    private Button    btnCapturarFoto, btnSeleccionarGaleria, btnGuardar;
    private TextView  tvEstado;

    private SharedPreferences prefs;
    private Uri    fotoUri;
    private String rutaFoto;

    /** Evita activar el reconocimiento dos veces a la vez. */
    private boolean esperandoNombre = false;
    private boolean saludoRealizado = false;

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        etNombre              = findViewById(R.id.etNombre);
        ivFoto                = findViewById(R.id.ivFoto);
        btnCapturarFoto       = findViewById(R.id.btnCapturarFoto);
        btnSeleccionarGaleria = findViewById(R.id.btnSeleccionarGaleria);
        btnGuardar            = findViewById(R.id.btnGuardar);

        // Hacer la foto redonda
        ivFoto.setClipToOutline(true);
        ivFoto.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        // Ocultar teclado al inicio
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Si btnCapturarVoz sigue en el layout y no quieres tocar el XML aún,
        // descomenta la siguiente línea para ocultarlo:
        // findViewById(R.id.btnCapturarVoz).setVisibility(View.GONE);

        btnCapturarFoto.setOnClickListener(v -> abrirCamara());
        btnSeleccionarGaleria.setOnClickListener(v -> abrirGaleria());
        btnGuardar.setOnClickListener(v -> guardarDatos());

        // Click en el avatar → Diálogo de selección
        ivFoto.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("¿Cómo quieres añadir la foto?");
            builder.setItems(new CharSequence[]{"Usar Cámara", "Elegir de Galería"}, (dialog, which) -> {
                if (which == 0) abrirCamara();
                else abrirGaleria();
            });
            builder.show();
        });

        // Botón repetir saludo
        findViewById(R.id.btnRepetirSaludo).setOnClickListener(v -> {
            saludoRealizado = false;
            onRobotServiceReady();
        });
    }

    @Override
    protected void onRobotServiceReady() {
        if (saludoRealizado) return;
        saludoRealizado = true;

        // El robot guía al usuario desde el principio con frases secuenciales.
        new Thread(() -> {
            hablarYEsperar("¡Hola! Soy Sanbot. Vamos a configurar tu perfil.");
            hablarYEsperar("Dime tu nombre. Toca mi cabeza cuando estés listo.");
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        esperandoNombre = false;
    }

    // =========================================================================
    // Toque de cabeza → activa escucha de nombre
    // =========================================================================

    /**
     * Llamado automáticamente desde BaseActivity cuando el usuario toca
     * la cabeza del robot (sensores 11, 12 o 13 según la doc del SDK).
     */
    @Override
    protected void onCabezaTocada() {
        if (esperandoNombre) return;
        esperandoNombre = true;

        // No hablamos, solo escuchamos directamente (ya se dieron instrucciones antes)
        new Thread(this::escuchar).start();
    }

    // =========================================================================
    // Voz — resultado del reconocimiento
    // =========================================================================

    @Override
    protected void onTextoEscuchado(String texto) {
        if (!esperandoNombre || TextUtils.isEmpty(texto)) return;

        esperandoNombre = false;

        runOnUiThread(() -> {
            etNombre.setText(texto);
        });

        // El robot confirma y guía al siguiente paso
        new Thread(() -> {
            sleep(300);
            hablarOSimular("Perfecto, " + texto
                    + ". Ahora necesito una foto tuya. Toca el botón de cámara o galería.");
        }).start();
    }

    // =========================================================================
    // Cámara
    // =========================================================================

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
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No se encontró una aplicación de cámara",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File archivoFoto = crearArchivoFoto();
            rutaFoto = archivoFoto.getAbsolutePath();
            fotoUri  = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    archivoFoto);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            startActivityForResult(intent, REQUEST_CAMERA);
        } catch (IOException e) {
            Toast.makeText(this, "Error al preparar el archivo de foto: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // Galería
    // =========================================================================

    private void abrirGaleria() {
        String permiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permiso)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permiso},
                    STORAGE_PERMISSION_REQUEST);
            return;
        }
        lanzarSelectorGaleria();
    }

    private void lanzarSelectorGaleria() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
                Intent.createChooser(intent, "Seleccionar imagen"),
                REQUEST_GALLERY);
    }

    // =========================================================================
    // Resultado de intents (cámara y galería)
    // =========================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA) {
            if (rutaFoto != null) {
                mostrarFotoDesdeRuta(rutaFoto);
                hablarOSimular("Foto guardada. Cuando quieras, toca el botón comenzar para terminar.");
            }

        } else if (requestCode == REQUEST_GALLERY && data != null) {
            Uri imageUri = data.getData();
            if (imageUri == null) return;

            try {
                rutaFoto = copiarFotoAAlmacenamiento(imageUri);
                if (rutaFoto != null) {
                    mostrarFotoDesdeRuta(rutaFoto);
                    hablarOSimular("Foto seleccionada. Cuando quieras, toca el botón comenzar para terminar.");
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // =========================================================================
    // Helpers de imagen
    // =========================================================================

    private void mostrarFotoDesdeRuta(String ruta) {
        Bitmap bitmap = BitmapFactory.decodeFile(ruta);
        if (bitmap != null) {
            ivFoto.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "No se pudo decodificar la imagen",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoFoto() throws IOException {
        File storageDir = getExternalFilesDir("fotos");
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                throw new IOException("No se pudo crear el directorio: " + storageDir);
            }
        }
        return new File(storageDir, "avatar_usuario.jpg");
    }

    private String copiarFotoAAlmacenamiento(Uri sourceUri) throws IOException {
        File destFile = crearArchivoFoto();
        try (InputStream input  = getContentResolver().openInputStream(sourceUri);
             FileOutputStream output = new FileOutputStream(destFile)) {

            if (input == null) {
                throw new IOException("No se pudo abrir el stream de la imagen seleccionada");
            }
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) > 0) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        }
        return destFile.getAbsolutePath();
    }

    // =========================================================================
    // Permisos
    // =========================================================================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (granted) {
                iniciarCamara();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (granted) {
                lanzarSelectorGaleria();
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =========================================================================
    // Guardar y navegar a MainActivity
    // =========================================================================

    private void guardarDatos() {
        String nombre = etNombre.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "Por favor, ingresa o captura tu nombre",
                    Toast.LENGTH_SHORT).show();
            hablarOSimular("Aún no tengo tu nombre. Toca mi cabeza y dímelo.");
            return;
        }
        if (TextUtils.isEmpty(rutaFoto)) {
            Toast.makeText(this, "Por favor, captura o selecciona una foto",
                    Toast.LENGTH_SHORT).show();
            hablarOSimular("Todavía necesito tu foto. Toca el botón de cámara o galería.");
            return;
        }

        prefs.edit()
                .putString(KEY_NOMBRE, nombre)
                .putString(KEY_FOTO_PATH, rutaFoto)
                .putBoolean(KEY_FIRST_RUN, false)
                .apply();

        hablarOSimular("¡Todo listo, " + nombre + "! Bienvenido a la aplicación.");

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    /** Sleep sin checked exception para usar en Threads/lambdas. */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Habla la frase y espera una estimación de su duración antes de continuar.
     * Útil para encadenar frases en un hilo sin que se solapen.
     * (~80 ms por carácter es una aproximación conservadora).
     */
    private void hablarYEsperar(String frase) {
        hablarOSimular(frase);
        sleep(frase.length() * 80L);
    }
}