package com.example.sanbotapp;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.sanbotapp.moduloReactivo.MovementControl;
import com.example.sanbotapp.robotControl.AudioControl;
import com.example.sanbotapp.robotControl.HandsControl;
import com.example.sanbotapp.robotControl.HeadControl;
import com.example.sanbotapp.robotControl.HardwareControl;
import com.example.sanbotapp.robotControl.SpeechControl;
import com.example.sanbotapp.robotControl.SystemControl;
import com.example.sanbotapp.robotControl.WheelControl;



import com.qihancloud.opensdk.base.TopBaseActivity;
import com.qihancloud.opensdk.beans.FuncConstant;
import com.qihancloud.opensdk.function.beans.EmotionsType;
import com.qihancloud.opensdk.function.beans.speech.Grammar;
import com.qihancloud.opensdk.function.unit.interfaces.hardware.TouchSensorListener;
import com.qihancloud.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.qihancloud.opensdk.function.unit.HandMotionManager;
import com.qihancloud.opensdk.function.unit.HardWareManager;
import com.qihancloud.opensdk.function.unit.HeadMotionManager;
import com.qihancloud.opensdk.function.unit.ModularMotionManager;
import com.qihancloud.opensdk.function.unit.SpeechManager;
import com.qihancloud.opensdk.function.unit.SystemManager;
import com.qihancloud.opensdk.function.unit.WheelMotionManager;

/**
 * BaseActivity para el flavor ROBOT.
 *
 * Clase base abstracta que extiende TopBaseActivity del SDK de Qihan/Sanbot.
 * Centraliza la inicialización y el control de todos los subsistemas del robot
 * (voz, ruedas, cabeza, brazos, sistema, hardware, audio y movimiento).
 *
 * Todas las Activities del flavor robot deben heredar de esta clase.
 */
public abstract class BaseActivity extends TopBaseActivity {

    // ── Controladores de cada subsistema del robot ──────────────────────────

    /** Controla el sistema de síntesis de voz (TTS) del robot. */
    private SpeechControl speechControl;

    /** Referencia directa al SpeechManager del SDK para el reconocimiento de voz. */
    private SpeechManager speechManager;

    /** Controla las ruedas del robot. */
    private WheelControl wheelControl;

    /** Controla los movimientos de la cabeza del robot. */
    private HeadControl headControl;

    /** Controla los brazos del robot. */
    private HandsControl handsControl;

    /** Controla el sistema general del robot: emociones en pantalla. */
    private SystemControl systemControl;

    /** Controla el hardware físico del robot: LEDs. */
    private HardwareControl hardwareControl;

    /** Controla el volumen del sistema de audio del robot. */
    private AudioControl audioControl;

    /** Controla comportamientos de movimiento compuestos. */
    private MovementControl movementControl;


    // ══════════════════════════════════════════════════════════════════════════
    // CICLO DE VIDA DEL SERVICIO DEL ROBOT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onMainServiceConnected() {

        // ── Voz ──────────────────────────────────────────────────────────────
        // CORRECCIÓN: se asigna al campo this.speechManager, no a variable local
        this.speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        if (this.speechManager != null) {
            speechControl = new SpeechControl(this.speechManager);
            Log.d("BaseActivity[Robot]", "SpeechControl inicializado correctamente.");
        } else {
            Log.e("BaseActivity[Robot]", "SpeechManager es null — ¿el servicio está disponible?");
        }

        // ── Ruedas ───────────────────────────────────────────────────────────
        WheelMotionManager wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        if (wheelMotionManager != null) {
            wheelControl = new WheelControl(wheelMotionManager);
        }

        // ── Cabeza ───────────────────────────────────────────────────────────
        HeadMotionManager headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        if (headMotionManager != null) {
            headControl = new HeadControl(headMotionManager);
        }

        // ── Brazos ───────────────────────────────────────────────────────────
        HandMotionManager handMotionManager = (HandMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        if (handMotionManager != null) {
            handsControl = new HandsControl(handMotionManager);
        }

        // ── Sistema ──────────────────────────────────────────────────────────
        SystemManager symManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        if (symManager != null) {
            systemControl = new SystemControl(symManager);
        }
        // ── Sensor táctil ────────────────────────────────────────────────────────
        HardWareManager hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        if (hardWareManager != null) {
            hardwareControl = new HardwareControl(hardWareManager);

            // Touch listener — la API usa "HareWare" (errata del SDK, es correcto así)
            hardWareManager.setOnHareWareListener(new TouchSensorListener() {
                @Override
                public void onTouch(int part) {
                    // Part 11 = centro cabeza, 12 = lado derecho, 13 = lado izquierdo
                    if (part == 11 || part == 12 || part == 13) {
                        onCabezaTocada();
                    }
                }
            });
        }

        // ── Audio ────────────────────────────────────────────────────────────
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioControl = new AudioControl(audioManager);
        }

        // ── Movimiento compuesto ─────────────────────────────────────────────
        ModularMotionManager modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        if (modularMotionManager != null) {
            movementControl = new MovementControl(modularMotionManager, this, wheelControl, headControl, handsControl);
        }

        // Notifica a la subclase que todo está listo
        onRobotServiceReady();
    }

    /**
     * Gancho que se llama cuando todos los controladores están listos.
     * Las subclases pueden sobreescribirlo para ejecutar acciones de bienvenida.
     */
    protected void onRobotServiceReady() {
        // Gancho opcional para subclases
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


    // ══════════════════════════════════════════════════════════════════════════
    // VOZ
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Hace que el robot pronuncie la frase dada en voz alta mediante TTS.
     */
    protected void hablarOSimular(final String frase) {
        if (speechControl != null) {
            new Thread(() -> speechControl.hablar(frase)).start();
        } else {
            Log.w("BaseActivity[Robot]", "SpeechControl aún no listo. Frase: " + frase);
        }
    }

    /**
     * Activa el reconocimiento de voz del robot.
     * Cuando se detecta texto, llama a onTextoEscuchado() en la subclase.
     *
     * CORRECCIÓN: usa SpeechManager.RecognizeListener (clase interna del SDK)
     * en lugar de la ruta de paquete incorrecta que causaba el error.
     */
    public void escuchar() {
        if (speechManager != null) {
            speechManager.setOnSpeechListener(new RecognizeListener() {
                @Override
                public boolean onRecognizeResult(Grammar grammar) {
                    if (grammar != null && !TextUtils.isEmpty(grammar.getText())) {
                        onTextoEscuchado(grammar.getText());
                        return true;
                    }
                    return false;
                }

                @Override
                public void onRecognizeVolume(int volume) {
                    // opcional: puedes ignorarlo o sobreescribir onVolumenDetectado()
                }
            });
        } else {
            Log.w("BaseActivity[Robot]", "speechManager null — escuchar() ignorado");
        }
    }

    /**
     * Hook que recibe el texto reconocido por el robot.
     * Las subclases lo sobreescriben para procesar el resultado.
     *
     * NOTA: puede llamarse desde un hilo secundario, usar runOnUiThread()
     * si se necesita actualizar la UI.
     *
     * @param texto Texto reconocido por el robot.
     */
    protected void onTextoEscuchado(String texto) {
        // Gancho opcional para subclases
    }


    // ══════════════════════════════════════════════════════════════════════════
    // RUEDAS
    // ══════════════════════════════════════════════════════════════════════════

    public void moverRuedasBasico(String accion, Integer angulo) {
        if (wheelControl != null)
            wheelControl.controlBasicoRuedas(WheelControl.AccionesRuedas.valueOf(accion.toUpperCase()), angulo);
    }

    public void moverRuedasBasicoLento(String accion, Integer angulo) {
        if (wheelControl != null)
            wheelControl.controlBasicoRuedasLento(WheelControl.AccionesRuedas.valueOf(accion.toUpperCase()), angulo);
    }

    public void avanzarRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzar(distancia, velocidad);
    }

    public void retrocederRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.atras(distancia, velocidad);
    }

    public void avanzarIzquierdaRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzaLeft(distancia, velocidad);
    }

    public void avanzarDerechaRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzaRight(distancia, velocidad);
    }


    // ══════════════════════════════════════════════════════════════════════════
    // CABEZA
    // ══════════════════════════════════════════════════════════════════════════

    public void moverCabezaBasico(String accion) {
        if (headControl != null)
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.valueOf(accion.toUpperCase()));
    }

    public void girarCabeza(int angulo) {
        if (headControl != null) headControl.girarCabeza(angulo);
    }

    public void reiniciarCabeza() {
        if (headControl != null) headControl.reiniciar();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SENSOR TÁCTIL DE CABEZA
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Gancho llamado cuando el usuario toca la cabeza del robot (sensores 11, 12 o 13).
     * Las subclases lo sobreescriben para reaccionar.
     * Puede llamarse desde un hilo secundario — usar runOnUiThread() si se toca la UI.
     */
    protected void onCabezaTocada() {
        // Gancho opcional para subclases
    }
    // ══════════════════════════════════════════════════════════════════════════
    // BRAZOS
    // ══════════════════════════════════════════════════════════════════════════

    public void moverBrazos(String accion, String brazo) {
        if (handsControl != null)
            handsControl.controlBasicoBrazos(
                    HandsControl.AccionesBrazos.valueOf(accion.toUpperCase()),
                    HandsControl.TipoBrazo.valueOf(brazo.toUpperCase())
            );
    }

    public void reiniciarBrazos() {
        if (handsControl != null) handsControl.reiniciar();
    }


    // ══════════════════════════════════════════════════════════════════════════
    // SISTEMA / EMOCIONES
    // ══════════════════════════════════════════════════════════════════════════

    protected void mostrarEmocion(String emocion) {
        if (systemControl != null) {
            try {
                systemControl.cambiarEmocion(EmotionsType.valueOf(emocion.toUpperCase()));
            } catch (Exception e) {
                Log.e("BaseActivity", "Emoción no válida: " + emocion);
            }
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    // HARDWARE / LEDs
    // ══════════════════════════════════════════════════════════════════════════

    public void encenderLed(byte parte, byte modo) {
        if (hardwareControl != null) hardwareControl.encenderLED(parte, modo);
    }

    public void apagarLed(byte parte) {
        if (hardwareControl != null) hardwareControl.apagarLED(parte);
    }


    // ══════════════════════════════════════════════════════════════════════════
    // AUDIO / VOLUMEN
    // ══════════════════════════════════════════════════════════════════════════

    public void setVolumenRobot(int volumen) {
        if (audioControl != null) audioControl.setVolumen(volumen);
    }

    public int getVolumenRobot() {
        return audioControl != null ? audioControl.getVolumen() : 0;
    }


    // ══════════════════════════════════════════════════════════════════════════
    // MOVIMIENTO COMPUESTO (aleatorio + seguimiento)
    // ══════════════════════════════════════════════════════════════════════════

    public void activarMovimientoAleatorio() {
        if (movementControl != null) movementControl.activarMovimientoAleatorio();
    }

    public void desactivarMovimientoAleatorio() {
        if (movementControl != null) movementControl.desactivarMovimientoAleatorio();
    }

    public void activarSeguimiento() {
        if (movementControl != null) movementControl.activarSeguimiento();
    }

    public void desactivarSeguimiento() {
        if (movementControl != null) movementControl.desactivarSeguimiento();
    }
}