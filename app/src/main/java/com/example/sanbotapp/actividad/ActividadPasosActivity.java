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
import com.example.sanbotapp.util.EstadoFeedback;

import java.util.List;

public class ActividadPasosActivity extends BaseActivity {

    public static final String EXTRA_TIPO_ACTIVIDAD  = "extra_tipo_actividad";
    public static final String EXTRA_ACTIVIDAD_ID    = "extra_actividad_id";

    private List<TipoActividad.Paso> pasos;
    private int indicePasoActual = 0;
    private TipoActividad tipoEnum;
    private int actividadId = -1;
    private Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvContador;
    private TextView tvTextoPaso;
    private ImageView ivIconoPaso;
    private Button btnAccion;
    private View cardPaso;


    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_pasos);

        String tipoStr = getIntent().getStringExtra(EXTRA_TIPO_ACTIVIDAD);
        actividadId = getIntent().getIntExtra(EXTRA_ACTIVIDAD_ID, -1);
        tipoEnum = TipoActividad.fromString(tipoStr);

        setupTopBackBanner(tipoEnum.getLabel());

        tvContador  = findViewById(R.id.tvPasoContador);
        tvTextoPaso = findViewById(R.id.tvPasoTexto);
        ivIconoPaso = findViewById(R.id.ivPasoIcono);
        btnAccion   = findViewById(R.id.btnPasoAccion);
        cardPaso    = findViewById(R.id.cardPaso);

        pasos = tipoEnum.getPasos();

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

        TipoActividad.Paso paso = pasos.get(indicePasoActual);
        int           total = pasos.size();

        tvContador.setText("PASO " + (indicePasoActual + 1) + " DE " + total);
        tvTextoPaso.setText((indicePasoActual + 1) + ". " + paso.texto.toUpperCase());
        ivIconoPaso.setImageResource(paso.iconoRes);

        boolean esUltimo = (indicePasoActual == total - 1);
        btnAccion.setText(esUltimo ? "FIN ACTIVIDAD" : "SIGUIENTE PASO");
    }

    // avanzarPaso() ya usa mostrarPaso() que incluye voz, sin cambios
    private void avanzarPaso() {
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
        gestionarFeedbackHardware(EstadoFeedback.CELEBRACION);
        hablarOSimular("¡Muy bien! Has completado la actividad. ¡Excelente trabajo!");
        
        // Wait 3 seconds before finishing to let the robot celebrate
        handler.postDelayed(() -> {
            gestionarFeedbackHardware(EstadoFeedback.IDLE);
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
            int colorBase = Color.parseColor(tipoEnum.getColorHex());
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

}