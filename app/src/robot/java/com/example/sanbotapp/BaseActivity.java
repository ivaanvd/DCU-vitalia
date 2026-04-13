package com.example.sanbotapp;

import android.util.Log;
import android.widget.TextView;
import android.view.View;

import com.example.sanbotapp.robotControl.SpeechControl;
import com.example.sanbotapp.robotControl.WheelControl;
import com.example.sanbotapp.robotControl.HeadControl;
import com.example.sanbotapp.robotControl.HandsControl;
import com.example.sanbotapp.robotControl.SystemControl;
import com.example.sanbotapp.robotControl.HardwareControl;
import com.example.sanbotapp.robotControl.AudioControl;
import com.example.sanbotapp.moduloReactivo.MovementControl;

import com.qihancloud.opensdk.base.TopBaseActivity;
import com.qihancloud.opensdk.beans.FuncConstant;
import com.qihancloud.opensdk.function.unit.SpeechManager;
import com.qihancloud.opensdk.function.unit.WheelMotionManager;
import com.qihancloud.opensdk.function.unit.HeadMotionManager;
import com.qihancloud.opensdk.function.unit.HandMotionManager;
import com.qihancloud.opensdk.function.unit.SystemManager;
import com.qihancloud.opensdk.function.unit.HardWareManager;
import com.qihancloud.opensdk.function.unit.ModularMotionManager;
import com.qihancloud.opensdk.function.beans.EmotionsType;
import android.media.AudioManager;
import android.content.Context;

/**
 * BaseActivity para el flavor ROBOT.
 *
 * Clase base abstracta que extiende TopBaseActivity del SDK de Qihan/Sanbot.
 * Centraliza la inicialización y el control de todos los subsistemas del robot
 * (voz, ruedas, cabeza, brazos, sistema, hardware, audio y movimiento).
 *
 * Todas las Activities del flavor robot deben heredar de esta clase.
 * MainActivity llama a sus métodos sin conocer los detalles del flavor.
 */
public abstract class BaseActivity extends TopBaseActivity {

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

    // ── Controladores de cada subsistema del robot ──────────────────────────

    /** Controla el sistema de síntesis de voz (TTS) del robot. */
    private SpeechControl speechControl;

    /** Controla las ruedas del robot: avance, retroceso, giros y desplazamientos laterales. */
    private WheelControl wheelControl;

    /** Controla los movimientos de la cabeza del robot: arriba, abajo, rotación. */
    private HeadControl headControl;

    /** Controla los brazos del robot: subir, bajar, extender, recoger. */
    private HandsControl handsControl;

    /** Controla el sistema general del robot: emociones en pantalla y estado del sistema. */
    private SystemControl systemControl;

    /** Controla el hardware físico del robot: LEDs de colores en distintas partes del cuerpo. */
    private HardwareControl hardwareControl;

    /** Controla el volumen del sistema de audio del robot. */
    private AudioControl audioControl;

    /**
     * Controla comportamientos de movimiento compuestos: movimiento aleatorio
     * y seguimiento de personas combinando ruedas, cabeza y brazos.
     */
    private MovementControl movementControl;


    // ══════════════════════════════════════════════════════════════════════════
    // CICLO DE VIDA DEL SERVICIO DEL ROBOT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Llamado automáticamente por TopBaseActivity cuando el servicio interno
     * del robot ya está vinculado y listo para recibir órdenes.
     *
     * Aquí se obtienen todos los managers del SDK mediante getUnitManager()
     * usando las constantes de FuncConstant, y se instancian los controladores
     * propios de la aplicación que envuelven cada manager.
     *
     * Si algún manager devuelve null (servicio no disponible), se omite
     * silenciosamente para que el resto siga funcionando.
     *
     * Al final llama a onRobotServiceReady() para notificar a las subclases.
     */
    protected void onMainServiceConnected() {

        // ── Voz ──────────────────────────────────────────────────────────────
        // Obtiene el manager de síntesis de voz del SDK y crea SpeechControl.
        SpeechManager speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        if (speechManager != null) {
            speechControl = new SpeechControl(speechManager);
            Log.d("BaseActivity[Robot]", "SpeechControl inicializado correctamente.");
        } else {
            Log.e("BaseActivity[Robot]", "SpeechManager es null — ¿el servicio está disponible?");
        }

        // ── Ruedas ───────────────────────────────────────────────────────────
        // Obtiene el manager de movimiento de ruedas y crea WheelControl.
        WheelMotionManager wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        if (wheelMotionManager != null) {
            wheelControl = new WheelControl(wheelMotionManager);
        }

        // ── Cabeza ───────────────────────────────────────────────────────────
        // Obtiene el manager de movimiento de cabeza y crea HeadControl.
        HeadMotionManager headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        if (headMotionManager != null) {
            headControl = new HeadControl(headMotionManager);
        }

        // ── Brazos ───────────────────────────────────────────────────────────
        // Obtiene el manager de movimiento de manos/brazos y crea HandsControl.
        HandMotionManager handMotionManager = (HandMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        if (handMotionManager != null) {
            handsControl = new HandsControl(handMotionManager);
        }

        // ── Sistema ──────────────────────────────────────────────────────────
        // Obtiene el manager del sistema (emociones, estado) y crea SystemControl.
        SystemManager symManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        if (symManager != null) {
            systemControl = new SystemControl(symManager);
        }

        // ── Hardware / LEDs ──────────────────────────────────────────────────
        // Obtiene el manager de hardware (LEDs, sensores físicos) y crea HardwareControl.
        HardWareManager hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        if (hardWareManager != null) {
            hardwareControl = new HardwareControl(hardWareManager);
        }

        // ── Audio ────────────────────────────────────────────────────────────
        // Obtiene el AudioManager del sistema Android (no del SDK) y crea AudioControl
        // para ajustar el volumen del robot desde la capa de aplicación.
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioControl = new AudioControl(audioManager);
        }

        // ── Movimiento compuesto ─────────────────────────────────────────────
        // Obtiene el manager de movimiento modular y crea MovementControl,
        // que coordina ruedas + cabeza + brazos para comportamientos complejos.
        ModularMotionManager modularMotionManager = (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        if (modularMotionManager != null) {
            movementControl = new MovementControl(modularMotionManager, this, wheelControl, headControl, handsControl);
        }

        // Notifica a la subclase (p.ej. MainActivity) que todo está listo.
        onRobotServiceReady();
    }

    /**
     * Gancho (hook) que se llama al final de onMainServiceConnected(),
     * una vez que todos los controladores han sido inicializados.
     *
     * Las subclases pueden sobreescribirlo para ejecutar acciones de bienvenida
     * (p.ej. que el robot diga "Hola") en cuanto el servicio esté operativo.
     * La implementación por defecto no hace nada.
     */
    protected void onRobotServiceReady() {
        // Gancho opcional para subclases
    }


    // ══════════════════════════════════════════════════════════════════════════
    // VOZ
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Hace que el robot pronuncie la frase dada en voz alta mediante TTS.
     *
     * La llamada a speechControl.hablar() se ejecuta en un hilo secundario
     * para no bloquear el hilo principal de la UI mientras el robot habla.
     *
     * Si SpeechControl no está listo aún (servicio no conectado), registra
     * una advertencia en el log y descarta la frase sin lanzar excepción.
     *
     * @param frase Texto que el robot debe decir en voz alta.
     */
    protected void hablarOSimular(final String frase) {
        if (speechControl != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    speechControl.hablar(frase);
                }
            }).start();
        } else {
            Log.w("BaseActivity[Robot]", "SpeechControl aún no listo. Frase no pronunciada: " + frase);
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    // RUEDAS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta una acción básica de ruedas a velocidad estándar.
     *
     * @param accion Nombre de la acción (p.ej. "ADELANTE", "ATRAS", "IZQUIERDA", "DERECHA").
     *               Se convierte a mayúsculas y se mapea al enum AccionesRuedas.
     * @param angulo Ángulo de giro en grados (usado en acciones de rotación).
     */
    public void moverRuedasBasico(String accion, Integer angulo) {
        if (wheelControl != null)
            wheelControl.controlBasicoRuedas(WheelControl.AccionesRuedas.valueOf(accion.toUpperCase()), angulo);
    }

    /**
     * Ejecuta una acción básica de ruedas a velocidad reducida (modo lento).
     * Útil para maniobras de precisión o entornos con personas cerca.
     *
     * @param accion Nombre de la acción (mismo enum que moverRuedasBasico).
     * @param angulo Ángulo de giro en grados.
     */
    public void moverRuedasBasicoLento(String accion, Integer angulo) {
        if (wheelControl != null)
            wheelControl.controlBasicoRuedasLento(WheelControl.AccionesRuedas.valueOf(accion.toUpperCase()), angulo);
    }

    /**
     * Mueve el robot hacia adelante una distancia determinada a una velocidad dada.
     *
     * @param distancia Distancia a recorrer in centímetros.
     * @param velocidad Velocidad de desplazamiento (unidades definidas por el SDK).
     */
    public void avanzarRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzar(distancia, velocidad);
    }

    /**
     * Mueve el robot hacia atrás una distancia determinada a una velocidad dada.
     *
     * @param distancia Distancia a recorrer en centímetros.
     * @param velocidad Velocidad de desplazamiento.
     */
    public void retrocederRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.atras(distancia, velocidad);
    }

    /**
     * Desplaza el robot en diagonal hacia adelante-izquierda.
     *
     * @param distancia Distancia a recorrer.
     * @param velocidad Velocidad de desplazamiento.
     */
    public void avanzarIzquierdaRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzaLeft(distancia, velocidad);
    }

    /**
     * Desplaza el robot en diagonal hacia adelante-derecha.
     *
     * @param distancia Distancia a recorrer.
     * @param velocidad Velocidad de desplazamiento.
     */
    public void avanzarDerechaRobot(Integer distancia, Integer velocidad) {
        if (wheelControl != null) wheelControl.avanzaRight(distancia, velocidad);
    }


    // ══════════════════════════════════════════════════════════════════════════
    // CABEZA
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta un movimiento básico de cabeza (p.ej. ARRIBA, ABAJO, IZQUIERDA, DERECHA).
     *
     * @param accion Nombre de la acción de cabeza. Se convierte al enum AccionesCabeza.
     */
    public void moverCabezaBasico(String accion) {
        if (headControl != null)
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.valueOf(accion.toUpperCase()));
    }

    /**
     * Gira la cabeza del robot hasta el ángulo absoluto indicado.
     *
     * @param angulo Ángulo destino en grados (0 = frente, positivo = izquierda,
     *               negativo = derecha, según la convención del SDK).
     */
    public void girarCabeza(int angulo) {
        if (headControl != null) headControl.girarCabeza(angulo);
    }

    /**
     * Devuelve la cabeza del robot a su posición neutral (frente, centro).
     * Útil al terminar una secuencia de movimientos de seguimiento.
     */
    public void reiniciarCabeza() {
        if (headControl != null) headControl.reiniciar();
    }


    // ══════════════════════════════════════════════════════════════════════════
    // BRAZOS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta una acción sobre uno o ambos brazos del robot.
     *
     * @param accion Nombre de la acción (p.ej. "SUBIR", "BAJAR", "EXTENDER").
     *               Se mapea al enum AccionesBrazos.
     * @param brazo  Brazo a mover: "IZQUIERDO", "DERECHO" o "AMBOS".
     *               Se mapea al enum TipoBrazo.
     */
    public void moverBrazos(String accion, String brazo) {
        if (handsControl != null)
            handsControl.controlBasicoBrazos(
                    HandsControl.AccionesBrazos.valueOf(accion.toUpperCase()),
                    HandsControl.TipoBrazo.valueOf(brazo.toUpperCase())
            );
    }

    /**
     * Devuelve ambos brazos a su posición de reposo neutral.
     * Se llama al final de animaciones o secuencias gestuales.
     */
    public void reiniciarBrazos() {
        if (handsControl != null) handsControl.reiniciar();
    }


    // ══════════════════════════════════════════════════════════════════════════
    // SISTEMA / EMOCIONES
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Muestra una emoción en la pantalla facial del robot (ojos animados).
     *
     * Convierte el string al enum EmotionsType del SDK. Si el nombre no
     * corresponde a ninguna emoción válida, captura la excepción y registra
     * el error sin detener la aplicación.
     *
     * @param emocion Nombre de la emoción (p.ej. "HAPPY", "SAD", "ANGRY", "NEUTRAL").
     */
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

    /**
     * Enciende un LED de una parte concreta del cuerpo del robot en un modo dado.
     *
     * @param parte Identificador de la zona LED (byte definido por el SDK,
     *              p.ej. pecho, cabeza, base).
     * @param modo  Modo de iluminación (p.ej. fijo, parpadeo, pulso).
     */
    public void encenderLed(byte parte, byte modo) {
        if (hardwareControl != null) hardwareControl.encenderLED(parte, modo);
    }

    /**
     * Apaga el LED de una parte concreta del cuerpo del robot.
     *
     * @param parte Identificador de la zona LED a apagar.
     */
    public void apagarLed(byte parte) {
        if (hardwareControl != null) hardwareControl.apagarLED(parte);
    }


    // ══════════════════════════════════════════════════════════════════════════
    // AUDIO / VOLUMEN
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Establece el volumen del sistema de audio del robot.
     *
     * @param volumen Nivel de volumen deseado (rango definido por AudioControl,
     *                habitualmente 0–15 siguiendo la escala de Android).
     */
    public void setVolumenRobot(int volumen) {
        if (audioControl != null) audioControl.setVolumen(volumen);
    }

    /**
     * Obtiene el volumen actual del sistema de audio del robot.
     *
     * @return Nivel de volumen actual, o 0 si AudioControl no está disponible.
     */
    public int getVolumenRobot() {
        return audioControl != null ? audioControl.getVolumen() : 0;
    }


    // ══════════════════════════════════════════════════════════════════════════
    // MOVIMIENTO COMPUESTO (aleatorio + seguimiento)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Activa el modo de movimiento aleatorio del robot.
     * En este modo el robot se mueve, gira la cabeza y gesticula de forma
     * autónoma sin objetivo fijo, para parecer "vivo" en estado de espera.
     */
    public void activarMovimientoAleatorio() {
        if (movementControl != null) movementControl.activarMovimientoAleatorio();
    }

    /**
     * Detiene el modo de movimiento aleatorio y deja el robot estático.
     */
    public void desactivarMovimientoAleatorio() {
        if (movementControl != null) movementControl.desactivarMovimientoAleatorio();
    }

    /**
     * Activa el modo de seguimiento de personas.
     * El robot usa sus sensores para detectar a una persona y orienta
     * la cabeza (y opcionalmente las ruedas) hacia ella mientras se mueve.
     */
    public void activarSeguimiento() {
        if (movementControl != null) movementControl.activarSeguimiento();
    }

    /**
     * Detiene el modo de seguimiento y devuelve el robot a reposo.
     */
    public void desactivarSeguimiento() {
        if (movementControl != null) movementControl.desactivarSeguimiento();
    }
}
