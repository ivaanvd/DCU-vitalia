package com.example.sanbotapp.util;

/**
 * Constantes globales de la aplicación.
 * Centraliza claves de SharedPreferences y códigos de solicitud para evitar duplicidad.
 */
public class AppConstants {

    // ── SharedPreferences ─────────────────────────────────────────────────────
    public static final String PREFS_NAME    = "AppPrefs";
    public static final String KEY_NOMBRE    = "nombre_usuario";
    public static final String KEY_FOTO_PATH  = "foto_path";
    public static final String KEY_FIRST_RUN = "first_run";
    public static final String KEY_VOLUMEN   = "ajuste_volumen";
    public static final String KEY_BRILLO    = "ajuste_brillo";

    // ── Request Codes (Cámara, Galería, Permisos) ─────────────────────────────
    public static final int REQUEST_CAMERA             = 100;
    public static final int REQUEST_GALLERY            = 101;
    public static final int CAMERA_PERMISSION_REQUEST  = 102;
    public static final int STORAGE_PERMISSION_REQUEST = 103;

    // ── Otros ─────────────────────────────────────────────────────────────────
    public static final int DELAY_MICRO_MS = 3000;
}
