package com.example.sanbotapp.actividad;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;
import java.util.ArrayList;
import java.util.List;

public class ActividadPasosActivity extends BaseActivity {

    public static final String EXTRA_TIPO_ACTIVIDAD  = "extra_tipo_actividad";
    public static final String EXTRA_ACTIVIDAD_ID    = "extra_actividad_id";

    private List<PasoActividad> pasos;
    private int indicePasoActual = 0;
    private boolean bloqueado = false;
    private String tipoActividad;
    private int actividadId = -1;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvContador;
    private TextView tvTextoPaso;
    private ImageView ivIconoPaso;
    private Button btnAccion;
    private View cardPaso;

    // ── Clase interna de paso ─────────────────────────────────────────────────

    public static class PasoActividad {
        public final String texto;
        public final int    iconoRes;

        public PasoActividad(String texto, int iconoRes) {
            this.texto    = texto;
            this.iconoRes = iconoRes;
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_pasos);

        tipoActividad = getIntent().getStringExtra(EXTRA_TIPO_ACTIVIDAD);
        actividadId   = getIntent().getIntExtra(EXTRA_ACTIVIDAD_ID, -1);
        if (tipoActividad == null) tipoActividad = Actividad.TIPO_MEDICACION;

        setupTopBackBanner(getLabelParaTipo(tipoActividad));

        tvContador  = findViewById(R.id.tvPasoContador);
        tvTextoPaso = findViewById(R.id.tvPasoTexto);
        ivIconoPaso = findViewById(R.id.ivPasoIcono);
        btnAccion   = findViewById(R.id.btnPasoAccion);
        cardPaso    = findViewById(R.id.cardPaso);

        pasos = getPasosParaTipo(tipoActividad);

        aplicarColorFondo();
        mostrarPasoSinVoz(); // pinta la UI inmediatamente

        // Espera a que la Activity esté visible antes de hablar
        handler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                hablarOSimular(pasos.get(0).texto);
            }
        }, 800);

        btnAccion.setOnClickListener(v -> avanzarPaso());
    }

    // ── Lógica de pasos ───────────────────────────────────────────────────────

    /*
     * Pre: indicePasoActual apunta al paso que se desea mostrar
     * Post: Actualiza todas las vistas con el contenido del paso actual
     *       y el robot lo lee en voz alta
     */
    private void mostrarPaso() {
        if (indicePasoActual >= pasos.size()) {
            finalizarActividad();
            return;
        }
        mostrarPasoSinVoz();
        hablarOSimular(pasos.get(indicePasoActual).texto);
    }

    private void mostrarPasoSinVoz() {
        if (indicePasoActual >= pasos.size()) return;

        PasoActividad paso  = pasos.get(indicePasoActual);
        int           total = pasos.size();

        tvContador.setText("PASO " + (indicePasoActual + 1) + " DE " + total);
        tvTextoPaso.setText((indicePasoActual + 1) + ". " + paso.texto.toUpperCase());
        ivIconoPaso.setImageResource(paso.iconoRes);

        boolean esUltimo = (indicePasoActual == total - 1);
        btnAccion.setText(esUltimo ? "FIN ACTIVIDAD" : "SIGUIENTE PASO");
    }

    // avanzarPaso() ya usa mostrarPaso() que incluye voz, sin cambios
    private void avanzarPaso() {
        if (bloqueado) return;
        bloqueado = true;

        // Quitamos el delay artificial de 1000ms para una respuesta inmediata
        bloqueado = false;
        indicePasoActual++;
        mostrarPaso();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    /*
     * Pre: Se ha llegado al último paso y el usuario pulsa "FIN ACTIVIDAD"
     * Post: Marca la actividad como completada en BD (si viene con ID),
     *       el robot felicita al usuario y cierra la pantalla
     */
    private void finalizarActividad() {
        if (actividadId >= 0) {
            ActividadRepository repo = new ActividadRepository(this);
            for (Actividad act : repo.getAll()) {
                if (act.getId() == actividadId) {
                    act.setEstado(Actividad.ESTADO_COMPLETADA);
                    repo.update(act);
                    break;
                }
            }
        }
        
        // Emotional feedback — MEJORA ÁREA 5: Celebración
        gestionarFeedbackHardware("CELEBRACION");
        hablarOSimular("¡Muy bien! Has completado la actividad. ¡Excelente trabajo!");
        
        // Wait 3 seconds before finishing to let the robot celebrate
        handler.postDelayed(() -> {
            gestionarFeedbackHardware("IDLE");
            finish();
        }, 3000);
    }

    // ── Color de fondo ────────────────────────────────────────────────────────

    /*
     * Pre: tipoActividad inicializado y cardPaso referenciado
     * Post: Aplica al fondo de la tarjeta el color de la actividad con opacidad al 35%
     */
    private void aplicarColorFondo() {
        try {
            int colorBase = Color.parseColor(getColorParaTipo(tipoActividad));
            int colorConAlpha = Color.argb(90,
                    Color.red(colorBase),
                    Color.green(colorBase),
                    Color.blue(colorBase));

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(colorConAlpha);
            bg.setCornerRadius(32 * getResources().getDisplayMetrics().density);
            cardPaso.setBackground(bg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Datos de pasos por tipo ───────────────────────────────────────────────

    private List<PasoActividad> getPasosParaTipo(String tipo) {
        List<PasoActividad> lista = new ArrayList<>();
        switch (tipo) {
            case Actividad.TIPO_MEDICACION:
                lista.add(new PasoActividad("Lávate las manos",       R.drawable.ic_aseo));
                lista.add(new PasoActividad("Ve a por la medicación",  R.drawable.ic_medicacion));
                lista.add(new PasoActividad("Toma tu medicación",      R.drawable.ic_medicacion));
                break;
            case Actividad.TIPO_BEBER_AGUA:
                lista.add(new PasoActividad("Ve a por un vaso de agua",              R.drawable.ic_agua));
                lista.add(new PasoActividad("Bebe despacio, sin prisa",              R.drawable.ic_agua));
                lista.add(new PasoActividad("Recuerda hidratarte cada hora",         R.drawable.ic_agua));
                break;
            case Actividad.TIPO_COMER:
                lista.add(new PasoActividad("Prepara la mesa con calma",             R.drawable.ic_comida));
                lista.add(new PasoActividad("Sirve tu comida",                       R.drawable.ic_comida));
                lista.add(new PasoActividad("Come despacio y mastica bien",          R.drawable.ic_comida));
                lista.add(new PasoActividad("Bebe agua durante la comida",           R.drawable.ic_agua));
                break;
            case Actividad.TIPO_PASEO_EJERCICIO:
                lista.add(new PasoActividad("Siéntate en una silla",                 R.drawable.ic_ejercicio));
                lista.add(new PasoActividad("Respira profundo",                      R.drawable.ic_ejercicio));
                lista.add(new PasoActividad("Gira el cuello suavemente",             R.drawable.ic_ejercicio));
                lista.add(new PasoActividad("Mueve los codos",                       R.drawable.ic_ejercicio));
                lista.add(new PasoActividad("Levanta los brazos",                    R.drawable.ic_ejercicio));
                break;
            case Actividad.TIPO_JUEGOS:
                lista.add(new PasoActividad("Busca un lugar tranquilo",              R.drawable.ic_puzzle));
                lista.add(new PasoActividad("Realiza el ejercicio propuesto",        R.drawable.ic_puzzle));
                lista.add(new PasoActividad("Descansa un momento",                   R.drawable.ic_puzzle));
                break;
            case Actividad.TIPO_ASEO:
                lista.add(new PasoActividad("Prepara lo que necesitas",              R.drawable.ic_aseo));
                lista.add(new PasoActividad("Realiza tu higiene personal",           R.drawable.ic_aseo));
                lista.add(new PasoActividad("Recoge y deja todo ordenado",           R.drawable.ic_aseo));
                break;
            case Actividad.TIPO_LLAMADA_FAMILIAR:
                lista.add(new PasoActividad("Piensa en alguien a quien llamar",      R.drawable.ic_llamada));
                lista.add(new PasoActividad("Marca el número",                       R.drawable.ic_llamada));
                lista.add(new PasoActividad("Saluda y pregunta cómo están",          R.drawable.ic_llamada));
                break;
            case Actividad.TIPO_IR_DORMIR:
                lista.add(new PasoActividad("Apaga las luces de la casa",            R.drawable.ic_dormir));
                lista.add(new PasoActividad("Prepara tu ropa para mañana",           R.drawable.ic_dormir));
                lista.add(new PasoActividad("Ponte cómodo en la cama",               R.drawable.ic_dormir));
                lista.add(new PasoActividad("Respira profundo y descansa",           R.drawable.ic_dormir));
                break;
            default:
                lista.add(new PasoActividad("Realiza la actividad con calma",        R.drawable.ic_calendario));
                break;
        }
        return lista;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getColorParaTipo(String tipo) {
        Actividad temp = new Actividad(0, tipo, 0, "");
        return temp.getColorHex();
    }

    private String getLabelParaTipo(String tipo) {
        Actividad temp = new Actividad(0, tipo, 0, "");
        return temp.getTipoLabel();
    }
}