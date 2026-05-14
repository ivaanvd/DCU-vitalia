package com.example.sanbotapp;

import android.content.Context;
import android.content.SharedPreferences;
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

    /** Indica si esta pantalla soporta interacción por voz activa (tocar cabeza). */
    private boolean isVoiceEnabled = true;

    /** Referencias a la UI del micrófono para feedback visual (Mejora P0). */
    private View btnMicUI;
    private android.widget.TextView tvEstadoMicUI;
    private String textoOriginalMic = "PULSA PARA HABLAR";
    private String lastSpokenText = "";

    /** Handler para tareas programadas en el hilo principal. */
    protected final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());


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
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Aplicar volumen guardado (Comentado para evitar que cambie solo al entrar en actividades)
        // int volumen = prefs.getInt("ajuste_volumen", 70);
        // setVolumenRobot(volumen);

        // Aplicar brillo guardado (requiere permisos de sistema)
        if (android.provider.Settings.System.canWrite(this)) {
            int brillo = prefs.getInt("ajuste_brillo", 60);
            int brillo255 = Math.round(brillo * 255f / 100f);
            try {
                android.provider.Settings.System.putInt(
                        getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                );
                android.provider.Settings.System.putInt(
                        getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS,
                        brillo255
                );
            } catch (Exception e) {
                Log.e("BaseActivity", "No se pudo aplicar brillo: " + e.getMessage());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Resetear UI de micrófono por si acaso se quedó pillado
        updateMicUI(false);
        gestionarFeedbackHardware("IDLE");

        // No callar si es el Home para que no se corten frases tipo "Abriendo recordatorios..."
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


    // ══════════════════════════════════════════════════════════════════════════
    // VOZ
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Hace que el robot pronuncie la frase dada en voz alta mediante TTS.
     */
    public void hablarOSimular(String texto) {
        // No forzamos emoción por defecto para permitir que persistan las de error/duda
        hablarOSimular(texto, null);
    }

    /**
     * Versión extendida de hablarOSimular que permite especificar la emoción.
     * Adaptado para evitar hardcodear SMILE siempre.
     */
    public void hablarOSimular(String texto, EmotionsType emocion) {
        if (TextUtils.isEmpty(texto)) return;
        this.lastSpokenText = texto;

        actualizarBocadillo(texto);
        
        // Solo cambiamos la emoción si se especifica una nueva, para no tapar emociones anteriores
        if (emocion != null) {
            gestionarFeedbackHardware("HABLANDO", emocion);
        } else {
            // Si no hay emoción, solo encendemos LEDs sin tocar la pantalla
            if (hardwareControl != null) {
                hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x01);
            }
        }

        if (speechControl != null) {
            speechControl.hablar(texto);
            // Reducimos la latencia del hilo de finalización usando un pool o simplificando el post-habla
            new Thread(() -> {
                speechControl.heAcabado2();
                // Al terminar de hablar, volvemos a IDLE solo si no hay una emoción crítica
                runOnUiThread(() -> {
                     if (hardwareControl != null) hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
                });
            }).start();
        } else {
            Log.d("BaseActivity[Sim]", "Simulando habla: " + texto);
            mainHandler.postDelayed(() -> {
                if (hardwareControl != null) hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
            }, 200);
        }
    }

    /**
     * Actualiza el TextView del bocadillo (tvBocadilloTexto) si existe en el layout actual.
     * Mejora Área 2: Feedback visual de lo que dice el robot.
     */
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

    /**
     * Repite la última frase pronunciada por el robot (Cambio solicitado).
     */
    public void repetirUltimaFrase() {
        if (!TextUtils.isEmpty(lastSpokenText)) {
            hablarOSimular(lastSpokenText);
        }
    }

    public void escuchar() {
        Log.d("BaseActivity", "Iniciando escucha...");
        gestionarFeedbackHardware("ESCUCHANDO");
        updateMicUI(true);

        if (speechControl != null) {
            speechControl.iniciarUnaVez(text -> {
                runOnUiThread(() -> {
                    updateMicUI(false);
                    gestionarFeedbackHardware("IDLE");
                    onTextoEscuchado(text);
                });
            });
        } else {
            Log.d("BaseActivity[Sim]", "Simulando escucha (esperando 3s)...");
            mainHandler.postDelayed(() -> {
                updateMicUI(false);
                gestionarFeedbackHardware("IDLE");
                onTextoEscuchado("simulación");
            }, 3000);
        }
    }

    public void gestionarFeedbackHardware(String estado) {
        gestionarFeedbackHardware(estado, EmotionsType.SMILE);
    }

    public void gestionarFeedbackHardware(String estado, EmotionsType emocionDefault) {
        if (hardwareControl == null || systemControl == null || movementControl == null) return;

        // Ejecutamos en segundo plano para no bloquear el hilo de UI con llamadas IPC
        new Thread(() -> {
            switch (estado) {
                case "ESCUCHANDO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x04);
                    systemControl.cambiarEmocion(EmotionsType.QUESTION);
                    movementControl.activarSeguimiento();
                    if (headControl != null) headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ARRIBA);
                    break;

                case "HABLANDO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x01);
                    if (emocionDefault != null) {
                        systemControl.cambiarEmocion(emocionDefault);
                    }
                    movementControl.activarSeguimiento();
                    break;

                case "ACIERTO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x02); // Verde
                    systemControl.cambiarEmocion(EmotionsType.SURPRISE);
                    if (handsControl != null) {
                        handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.LEVANTAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
                        mainHandler.postDelayed(() -> new Thread(() -> {
                            handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.BAJAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
                            hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
                            systemControl.cambiarEmocion(EmotionsType.SMILE);
                        }).start(), 2000);
                    }
                    break;

                case "FALLO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x03); // Rojo
                    systemControl.cambiarEmocion(EmotionsType.CRY);
                    if (handsControl != null) {
                        mainHandler.postDelayed(() -> new Thread(() -> {
                            hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
                            systemControl.cambiarEmocion(EmotionsType.NORMAL);
                        }).start(), 2000);
                    }
                    break;

                case "SUMMARY_START":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x05); // Púrpura/Morado
                    systemControl.cambiarEmocion(EmotionsType.GRIEVANCE);
                    break;

                case "THINKING_START":
                    systemControl.cambiarEmocion(EmotionsType.QUESTION);
                    break;

                case "MOURN":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x03); // Rojo
                    systemControl.cambiarEmocion(EmotionsType.CRY);
                    if (headControl != null) {
                        new Thread(() -> headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ABAJO)).start();
                    }
                    if (handsControl != null) {
                        handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.BAJAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
                    }
                    mainHandler.postDelayed(() -> new Thread(() -> {
                        hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
                        systemControl.cambiarEmocion(EmotionsType.NORMAL);
                        if (headControl != null) {
                            headControl.reiniciar(); // Reset cabeza
                        }
                    }).start(), 5000);
                    break;

                case "EXITO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x02);
                    systemControl.cambiarEmocion(EmotionsType.SMILE);
                    asentirConCabeza();
                    break;

                case "CANCELADO":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x06);
                    systemControl.cambiarEmocion(EmotionsType.GRIEVANCE);
                    break;

                case "ERROR":
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x03);
                    systemControl.cambiarEmocion(EmotionsType.QUESTION);
                    break;

                case "ALARMA":
                    systemControl.cambiarEmocion(EmotionsType.SURPRISE);
                    if (handsControl != null) handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.LEVANTAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
                    if (headControl != null) headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ARRIBA);
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x05);
                    break;

                case "CELEBRACION":
                    systemControl.cambiarEmocion(EmotionsType.SMILE);
                    hardwareControl.encenderLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL, (byte) 0x02);
                    asentirConCabeza();
                    if (handsControl != null) {
                        handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.LEVANTAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
                        mainHandler.postDelayed(() -> new Thread(() -> handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.BAJAR_BRAZO, HandsControl.TipoBrazo.AMBOS)).start(), 2000);
                    }
                    break;

                case "SALUDO":
                    systemControl.cambiarEmocion(EmotionsType.SMILE);
                    if (handsControl != null) {
                        handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.LEVANTAR_BRAZO, HandsControl.TipoBrazo.DERECHO);
                        mainHandler.postDelayed(() -> new Thread(() -> handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.BAJAR_BRAZO, HandsControl.TipoBrazo.DERECHO)).start(), 1500);
                    }
                    break;

                case "IDLE":
                    hardwareControl.apagarLED(com.qihancloud.opensdk.function.beans.LED.PART_ALL);
                    systemControl.cambiarEmocion(EmotionsType.NORMAL);
                    movementControl.desactivarSeguimiento();
                    if (headControl != null) headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.CENTRO);
                    break;
            }
        }).start();
    }

    /**
     * Gesto de saludo inicial (Área 5).
     */
    protected void realizarSaludoHumanizado() {
        if (handsControl == null || headControl == null) return;
        new Thread(() -> {
            handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.LEVANTAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ARRIBA);
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            handsControl.controlBasicoBrazos(HandsControl.AccionesBrazos.BAJAR_BRAZO, HandsControl.TipoBrazo.AMBOS);
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.CENTRO);
        }).start();
    }

    /**
     * Activa el seguimiento de personas (Mejora Área 5 - Cambio 2).
     */
    public void activarSeguimiento() {
        if (movementControl != null) movementControl.activarSeguimiento();
    }

    /**
     * Desactiva el seguimiento de personas.
     */
    public void desactivarSeguimiento() {
        if (movementControl != null) movementControl.desactivarSeguimiento();
    }

    /**
     * Gesto de asentir con la cabeza (Cambio 1).
     */
    protected void asentirConCabeza() {
        if (headControl == null) return;
        new Thread(() -> {
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ABAJO);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.ARRIBA);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            headControl.controlBasicoCabeza(HeadControl.AccionesCabeza.CENTRO);
        }).start();
    }

    /**
     * Detiene inmediatamente la síntesis de voz en curso.
     */
    protected void pararVoz() {
        if (speechControl != null) {
            new Thread(() -> speechControl.pararHabla()).start();
        }
    }


    /**
     * Registra los elementos de UI que representan el micrófono para actualizar su estado.
     * Mejora P0: Eliminar barrera de "tocar cabeza".
     */
    protected void setMicUI(View btn, TextView statusText) {
        this.btnMicUI = btn;
        this.tvEstadoMicUI = statusText;
        if (statusText != null) {
            this.textoOriginalMic = statusText.getText().toString();
        }
    }

    /**
     * Actualiza la UI del micrófono según si el robot está escuchando o no.
     * Mejora P0: Feedback visual claro.
     */
    protected void updateMicUI(boolean listening) {
        runOnUiThread(() -> {
            if (btnMicUI != null) {
                // Cambiar color de fondo (usando tint para no machacar el ripple)
                btnMicUI.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(listening ? R.color.mic_listening : R.color.mic_normal)
                ));
            }
            if (tvEstadoMicUI != null) {
                tvEstadoMicUI.setText(listening ? "TE ESTOY ESCUCHANDO..." : textoOriginalMic);
                tvEstadoMicUI.setTextColor(getResources().getColor(listening ? R.color.mic_listening : R.color.mic_normal));
            }
        });
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
        if (!isVoiceEnabled) {
            hablarOSimular("Lo siento, en esta pantalla no puedo escucharte. Por favor, usa los botones de la pantalla.");
        }
    }

    /**
     * Permite a las subclases habilitar o deshabilitar la escucha por voz al tocar la cabeza.
     */
    protected void setVoiceEnabled(boolean enabled) {
        this.isVoiceEnabled = enabled;
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

}