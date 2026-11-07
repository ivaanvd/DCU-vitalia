package com.example.sanbotapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sanbotapp.util.EstadoFeedback;

/**
 * BaseActivity para el flavor TABLET.
 * Extiende AppCompatActivity normal (sin SDK de Sanbot) y expone la misma API
 * que la BaseActivity del robot. Las funciones del robot se simulan con logs.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity[Tablet]";

    private boolean isVoiceEnabled = true;
    private View btnMicUI;
    private TextView tvEstadoMicUI;
    private String textoOriginalMic;
    private String lastSpokenText = "";

    protected final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // En tablet no hay MainService: simulamos la conexión tras crear la Activity
        onMainServiceConnected();
    }

    protected void onMainServiceConnected() {
        onRobotServiceReady();
    }

    protected void onRobotServiceReady() {
        // Gancho opcional para subclases
    }

    @Override
    public void onPause() {
        super.onPause();
        updateMicUI(false);
        gestionarFeedbackHardware(EstadoFeedback.IDLE);
        if (!(this instanceof MainActivity)) {
            pararVoz();
        }
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

    public void hablarEnMain(String texto) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            hablarOSimular(texto);
        } else {
            runOnUiThread(() -> hablarOSimular(texto));
        }
    }

    public void hablarOSimular(String texto) {
        if (TextUtils.isEmpty(texto)) return;
        this.lastSpokenText = texto;
        actualizarBocadillo(texto);
        Log.d(TAG, "ROBOT DIRÍA: " + texto);
    }

    protected void actualizarBocadillo(String texto) {
        runOnUiThread(() -> {
            TextView tvBocadillo = findViewById(R.id.tvBocadilloTexto);
            if (tvBocadillo != null) {
                tvBocadillo.setText(texto);
            }
            View btnRepetir = findViewById(R.id.btnRepetirBocadillo);
            if (btnRepetir != null) {
                btnRepetir.setOnClickListener(v -> repetirUltimaFrase());
            }
        });
    }

    public void repetirUltimaFrase() {
        if (!TextUtils.isEmpty(lastSpokenText)) {
            hablarOSimular(lastSpokenText);
        }
    }

    protected void pararVoz() {
        Log.d(TAG, "pararVoz() sin efecto");
    }

    public void escuchar() {
        Log.d(TAG, "Simulando escucha (esperando 3s)...");
        gestionarFeedbackHardware(EstadoFeedback.ESCUCHANDO);
        updateMicUI(true);
        mainHandler.postDelayed(() -> {
            updateMicUI(false);
            gestionarFeedbackHardware(EstadoFeedback.IDLE);
            onTextoEscuchado("simulación");
        }, 3000);
    }

    protected void onTextoEscuchado(String texto) {}

    protected void setMicUI(View btn, TextView statusText) {
        this.btnMicUI = btn;
        this.tvEstadoMicUI = statusText;
        if (statusText != null) {
            this.textoOriginalMic = statusText.getText().toString();
        }
    }

    protected void updateMicUI(boolean listening) {
        runOnUiThread(() -> {
            if (btnMicUI != null) {
                btnMicUI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(listening ? R.color.mic_listening : R.color.mic_normal)
                ));
            }
            if (tvEstadoMicUI != null) {
                tvEstadoMicUI.setText(listening ? getString(R.string.mic_escuchando)
                        : (textoOriginalMic != null ? textoOriginalMic : getString(R.string.ui_pulsa_para_hablar_2)));
                tvEstadoMicUI.setTextColor(getResources().getColor(listening ? R.color.mic_listening : R.color.mic_normal));
            }
        });
    }

    // ── FEEDBACK ─────────────────────────────────────────────────────────────

    /** @deprecated Usar gestionarFeedbackHardware(EstadoFeedback) */
    @Deprecated
    public void gestionarFeedbackHardware(String estado) {
        try {
            gestionarFeedbackHardware(EstadoFeedback.valueOf(estado.toUpperCase()));
        } catch (Exception e) {
            Log.e(TAG, "Estado de feedback no válido: " + estado);
        }
    }

    public void gestionarFeedbackHardware(EstadoFeedback estado) {
        Log.d(TAG, "gestionarFeedbackHardware: " + estado);
    }

    protected void realizarSaludoHumanizado() {
        Log.d(TAG, "realizarSaludoHumanizado");
    }

    protected void asentirConCabeza() {
        Log.d(TAG, "asentirConCabeza");
    }

    // ── SENSOR TÁCTIL ────────────────────────────────────────────────────────

    protected void onCabezaTocada() {
        if (!isVoiceEnabled) {
            hablarOSimular(getString(R.string.voz_no_puedo_escuchar_aqui));
        }
    }

    protected void setVoiceEnabled(boolean enabled) {
        this.isVoiceEnabled = enabled;
    }

    // ── RUEDAS (sin efecto en tablet) ────────────────────────────────────────
    public void moverRuedasBasico(String accion, Integer angulo) {
        Log.d(TAG, "moverRuedasBasico: " + accion); }
    public void moverRuedasBasicoLento(String accion, Integer angulo) {
        Log.d(TAG, "moverRuedasBasicoLento: " + accion); }
    public void avanzarRobot(Integer distancia, Integer velocidad) {
        Log.d(TAG, "avanzarRobot"); }
    public void retrocederRobot(Integer distancia, Integer velocidad) {
        Log.d(TAG, "retrocederRobot"); }
    public void avanzarIzquierdaRobot(Integer distancia, Integer velocidad) {
        Log.d(TAG, "avanzarIzquierdaRobot"); }
    public void avanzarDerechaRobot(Integer distancia, Integer velocidad) {
        Log.d(TAG, "avanzarDerechaRobot"); }

    // ── CABEZA (sin efecto en tablet) ────────────────────────────────────────
    public void moverCabezaBasico(String accion) {
        Log.d(TAG, "moverCabezaBasico: " + accion); }
    public void girarCabeza(int angulo) {
        Log.d(TAG, "girarCabeza: " + angulo); }
    public void reiniciarCabeza() {
        Log.d(TAG, "reiniciarCabeza"); }

    // ── BRAZOS (sin efecto en tablet) ────────────────────────────────────────
    public void moverBrazos(String accion, String brazo) {
        Log.d(TAG, "moverBrazos: " + accion + " " + brazo); }
    public void reiniciarBrazos() {
        Log.d(TAG, "reiniciarBrazos"); }

    // ── EMOCIONES (sin efecto en tablet) ─────────────────────────────────────
    protected void mostrarEmocion(String emocion) {
        Log.d(TAG, "mostrarEmocion: " + emocion); }

    // ── LEDS (sin efecto en tablet) ──────────────────────────────────────────
    public void encenderLed(byte parte, byte modo) {
        Log.d(TAG, "encenderLed"); }
    public void apagarLed(byte parte) {
        Log.d(TAG, "apagarLed"); }

    // ── AUDIO (sin efecto en tablet) ─────────────────────────────────────────
    public void setVolumenRobot(int volumen) {
        Log.d(TAG, "setVolumen: " + volumen); }
    public int getVolumenRobot() { return 5; }

    // ── MOVIMIENTO COMPUESTO (sin efecto en tablet) ──────────────────────────
    public void activarMovimientoAleatorio() {
        Log.d(TAG, "activarMovimientoAleatorio"); }
    public void desactivarMovimientoAleatorio() {
        Log.d(TAG, "desactivarMovimientoAleatorio"); }
    public void activarSeguimiento() {
        Log.d(TAG, "activarSeguimiento"); }
    public void desactivarSeguimiento() {
        Log.d(TAG, "desactivarSeguimiento"); }
}
