package com.example.sanbotapp;

import android.util.Log;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

/**
 * BaseActivity para el flavor TABLET.
 * Extiende AppCompatActivity normal (sin SDK de Sanbot).
 * Todas las funciones del robot se simulan con logs,
 * para que MainActivity compile y funcione sin el hardware.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // En tablet, simulamos que el "servicio" está listo después de que la Activity se haya creado
        onRobotServiceReady();
    }

    protected void setupTopBackBanner(String titulo) {
        TextView tvBannerTitulo = findViewById(R.id.tvBannerTitulo);
        if (tvBannerTitulo != null) {
            tvBannerTitulo.setText(titulo);
        }

        View btnBannerBack = findViewById(R.id.btnBannerBack);
        if (btnBannerBack != null) {
            btnBannerBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    // ── VOZ ──────────────────────────────────────────────────────────────────

    /**
     * Simula el habla del robot imprimiendo la frase en el log.
     * En tablet no hay TTS real, pero el código de MainActivity
     * funciona exactamente igual sin cambiar nada.
     */
    protected void hablarOSimular(String frase) {
        Log.d("BaseActivity[Tablet]", "ROBOT DIRÍA: " + frase);
    }

    public void escuchar() {
        Log.d("BaseActivity[Tablet]", "escuchar() sin efecto");
    }

    protected void onTextoEscuchado(String texto) {}

    /**
     * En tablet el servicio del robot no existe,
     * así que onRobotServiceReady() se llama desde onCreate()
     * para que MainActivity reciba el arranque igualmente.
     */
    protected void onRobotServiceReady() {
        // Gancho opcional para subclases
    }

    // ── RUEDAS (sin efecto en tablet) ────────────────────────────────────────
    public void moverRuedasBasico(String accion, Integer angulo) {
        Log.d("BaseActivity[Tablet]", "moverRuedasBasico: " + accion); }
    public void moverRuedasBasicoLento(String accion, Integer angulo) {
        Log.d("BaseActivity[Tablet]", "moverRuedasBasicoLento: " + accion); }
    public void avanzarRobot(Integer distancia, Integer velocidad) {
        Log.d("BaseActivity[Tablet]", "avanzarRobot"); }
    public void retrocederRobot(Integer distancia, Integer velocidad) {
        Log.d("BaseActivity[Tablet]", "retrocederRobot"); }
    public void avanzarIzquierdaRobot(Integer distancia, Integer velocidad) {
        Log.d("BaseActivity[Tablet]", "avanzarIzquierdaRobot"); }
    public void avanzarDerechaRobot(Integer distancia, Integer velocidad) {
        Log.d("BaseActivity[Tablet]", "avanzarDerechaRobot"); }

    // ── CABEZA (sin efecto en tablet) ────────────────────────────────────────
    public void moverCabezaBasico(String accion) {
        Log.d("BaseActivity[Tablet]", "moverCabezaBasico: " + accion); }
    public void girarCabeza(int angulo) {
        Log.d("BaseActivity[Tablet]", "girarCabeza: " + angulo); }
    public void reiniciarCabeza() {
        Log.d("BaseActivity[Tablet]", "reiniciarCabeza"); }
    /**
     * Gancho llamado cuando el usuario toca la cabeza del robot (sensores 11, 12 o 13).
     * Las subclases lo sobreescriben para reaccionar.
     * Puede llamarse desde un hilo secundario — usar runOnUiThread() si se toca la UI.
     */
    protected void onCabezaTocada() {
        // Gancho opcional para subclases
    }
    // ── BRAZOS (sin efecto en tablet) ────────────────────────────────────────
    public void moverBrazos(String accion, String brazo) {
        Log.d("BaseActivity[Tablet]", "moverBrazos: " + accion + " " + brazo); }
    public void reiniciarBrazos() {
        Log.d("BaseActivity[Tablet]", "reiniciarBrazos"); }

    // ── EMOCIONES (sin efecto en tablet) ─────────────────────────────────────
    protected void mostrarEmocion(String emocion) {
        Log.d("BaseActivity[Tablet]", "mostrarEmocion: " + emocion); }

    // ── LEDS (sin efecto en tablet) ──────────────────────────────────────────
    public void encenderLed(byte parte, byte modo) {
        Log.d("BaseActivity[Tablet]", "encenderLed"); }
    public void apagarLed(byte parte) {
        Log.d("BaseActivity[Tablet]", "apagarLed"); }

    // ── AUDIO (sin efecto en tablet) ─────────────────────────────────────────
    public void setVolumenRobot(int volumen) {
        Log.d("BaseActivity[Tablet]", "setVolumen: " + volumen); }
    public int getVolumenRobot() { return 5; }

    // ── MOVIMIENTO COMPUESTO (sin efecto en tablet) ──────────────────────────
    public void activarMovimientoAleatorio() {
        Log.d("BaseActivity[Tablet]", "activarMovimientoAleatorio"); }
    public void desactivarMovimientoAleatorio() {
        Log.d("BaseActivity[Tablet]", "desactivarMovimientoAleatorio"); }
    public void activarSeguimiento() {
        Log.d("BaseActivity[Tablet]", "activarSeguimiento"); }
    public void desactivarSeguimiento() {
        Log.d("BaseActivity[Tablet]", "desactivarSeguimiento"); }
}
