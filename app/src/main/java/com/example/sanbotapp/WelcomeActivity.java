package com.example.sanbotapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.sanbotapp.util.AppConstants;
import com.example.sanbotapp.util.PhotoHelper;

import java.io.File;

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

    private EditText  etNombre;
    private ImageView ivFoto;
    private Button    btnCapturarFoto, btnSeleccionarGaleria, btnGuardar;

    private SharedPreferences prefs;
    private String rutaFoto;
    private PhotoHelper photoHelper;

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

        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        etNombre              = findViewById(R.id.etNombre);
        ivFoto                = findViewById(R.id.ivFoto);
        btnCapturarFoto       = findViewById(R.id.btnCapturarFoto);
        btnSeleccionarGaleria = findViewById(R.id.btnSeleccionarGaleria);
        btnGuardar            = findViewById(R.id.btnGuardar);

        photoHelper = new PhotoHelper(this, new PhotoHelper.PhotoCallback() {
            @Override
            public void onPhotoReady(String ruta) {
                rutaFoto = ruta;
                PhotoHelper.mostrarFotoDesdeRuta(ruta, ivFoto);
                hablarOSimular("Foto añadida. Cuando quieras, toca el botón comenzar para terminar.");
            }
            @Override
            public void onPermissionDenied() {
                Toast.makeText(WelcomeActivity.this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        });

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

        btnCapturarFoto.setOnClickListener(v -> photoHelper.checkCameraPermissionAndOpen());
        btnSeleccionarGaleria.setOnClickListener(v -> photoHelper.checkStoragePermissionAndOpen());
        btnGuardar.setOnClickListener(v -> guardarDatos());

        // Click en el avatar → Diálogo de selección
        ivFoto.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("¿Cómo quieres añadir la foto?");
            builder.setItems(new CharSequence[]{"Usar Cámara", "Elegir de Galería"}, (dialog, which) -> {
                if (which == 0) photoHelper.checkCameraPermissionAndOpen();
                else photoHelper.checkStoragePermissionAndOpen();
            });
            builder.show();
        });

        // MEJORA P0: Botón de micrófono en pantalla
        android.view.View btnMic = findViewById(R.id.btnMicWelcome);
        android.widget.TextView tvEstadoMic = findViewById(R.id.tvEstadoMicWelcome);
        if (btnMic != null && tvEstadoMic != null) {
            setMicUI(btnMic, tvEstadoMic);
            btnMic.setOnClickListener(v -> onCabezaTocada());
        }

        // Botón repetir saludo
        findViewById(R.id.btnRepetirBocadillo).setOnClickListener(v -> {
            saludoRealizado = false;
            onRobotServiceReady();
        });
    }

    @Override
    protected void onRobotServiceReady() {
        if (saludoRealizado) return;
        saludoRealizado = true;

        // El robot guía al usuario desde el principio con frases secuenciales.
        realizarSaludoHumanizado(); // MEJORA ÁREA 5: Saludo con brazos y cabeza
        new Thread(() -> {
            hablarYEsperar("¡Hola! Soy Sanbot. Vamos a configurar tu perfil.");
            hablarYEsperar("Pulsa el botón del micrófono y dime tu nombre.");
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
        runOnUiThread(() -> etNombre.setBackgroundResource(R.drawable.bg_field_active));
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
            etNombre.setBackgroundResource(R.drawable.bg_campo_descripcion); // Quitar resaltado
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


    // =========================================================================
    // Resultado de intents (cámara y galería)
    // =========================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        photoHelper.handleActivityResult(requestCode, resultCode, data);
    }

    // =========================================================================
    // Helpers de imagen
    // =========================================================================


    // =========================================================================
    // Permisos
    // =========================================================================

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        photoHelper.handlePermissionsResult(requestCode, grantResults);
    }

    // =========================================================================
    // Guardar y navegar a MainActivity
    // =========================================================================

    private void guardarDatos() {
        String nombre = etNombre.getText().toString().trim();

        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "Por favor, ingresa o captura tu nombre",
                    Toast.LENGTH_SHORT).show();
            hablarOSimular("Aún no tengo tu nombre. Pulsa el micrófono y dímelo.");
            return;
        }
        if (TextUtils.isEmpty(rutaFoto)) {
            Toast.makeText(this, "Por favor, captura o selecciona una foto",
                    Toast.LENGTH_SHORT).show();
            hablarOSimular("Todavía necesito tu foto. Toca el botón de cámara o galería.");
            return;
        }

        prefs.edit()
                .putString(AppConstants.KEY_NOMBRE, nombre)
                .putString(AppConstants.KEY_FOTO_PATH, rutaFoto)
                .putBoolean(AppConstants.KEY_FIRST_RUN, false)
                .apply();

        hablarOSimular("¡Todo listo, " + nombre + "! Bienvenido a la aplicación.");

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("is_new_user", true);
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