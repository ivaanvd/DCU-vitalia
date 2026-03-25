package com.example.sanbotapp.actividad;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

public class ActividadEnCursoActivity extends BaseActivity {

    public static final String EXTRA_ACTIVIDAD_ID = "extra_actividad_id";
    private ActividadRepository repo;
    private Actividad actividadActual;
    private CountDownTimer timer;

    /*
     * Pre: Se inicia la actividad desde el planificador al coincidir la hora
     * Post: Carga la vista de progreso en curso, el timer y los botones de acción principal
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividad_en_curso);

        int actividadId = getIntent().getIntExtra(EXTRA_ACTIVIDAD_ID, -1);
        repo = new ActividadRepository(this);

        actividadActual = buscarActividad(actividadId);
        if (actividadActual == null) {
            Toast.makeText(this, "Actividad no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderizarActividad();
        iniciarTemporizador();

        Button btnCompletar = findViewById(R.id.btnEnCursoCompletar);
        btnCompletar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completarYSalir();
            }
        });
    }

    /*
     * Pre: Se cargan los datos de duración de la actividad actual
     * Post: Comienza el descuento visual regresivo en pantalla y genera el evento de término
     */
    private void iniciarTemporizador() {
        int duracionMs = actividadActual.getDuracionMinutos() * 60 * 1000;
        final TextView tvTemporizador = findViewById(R.id.tvEnCursoTemporizador);

        timer = new CountDownTimer(duracionMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutos = (int) (millisUntilFinished / 1000) / 60;
                int segundos = (int) (millisUntilFinished / 1000) % 60;
                tvTemporizador.setText(String.format("%02d:%02d", minutos, segundos));
            }

            @Override
            public void onFinish() {
                tvTemporizador.setText("00:00");
                hablarOSimular("¡Tiempo agotado!");
            }
        }.start();
    }

    /*
     * Pre: Se destruye la ventana de progreso (fin de ciclo celular o pulsar Atrás)
     * Post: Cancela explícitamente el hilo del temporizador para prevenir fugas de memoria
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }

    /*
     * Pre: Se extrae el ID numérico de los Extras del Intent de apertura
     * Post: Devuelve el objeto instanciado correspondiente haciendo match de ID en la BD
     */
    private Actividad buscarActividad(int id) {
        for (Actividad a : repo.getAll()) {
            if (a.getId() == id) return a;
        }
        return null;
    }

    /*
     * Pre: Objeto actividadActual instanciado exitosamente
     * Post: Asigna los textos, colores descriptivos y gráficos al layout
     */
    private void renderizarActividad() {
        TextView tvTitulo = findViewById(R.id.tvEnCursoTitulo);
        tvTitulo.setText(actividadActual.getTipoLabel());

        TextView tvDesc = findViewById(R.id.tvEnCursoDesc);
        if (actividadActual.getDescripcion() != null && !actividadActual.getDescripcion().isEmpty()) {
            tvDesc.setText(actividadActual.getDescripcion());
        } else {
            tvDesc.setVisibility(View.GONE);
        }

        View frameIcon = findViewById(R.id.frameEnCursoIcon);
        GradientDrawable bgCirculo = new GradientDrawable();
        bgCirculo.setShape(GradientDrawable.OVAL);
        bgCirculo.setColor(Color.parseColor(actividadActual.getColorHex()));
        frameIcon.setBackground(bgCirculo);

        ImageView ivIcon = findViewById(R.id.ivEnCursoIcon);
        ivIcon.setImageResource(getIconoParaTipo(actividadActual.getTipo()));
    }

    /*
     * Pre: El usuario pulsa el botón verde "COMPLETAR" o forzado por finalización temporal
     * Post: Acaba internamente la tarea en BD y dispara emoción FELIZ clausurando Activity
     */
    private void completarYSalir() {
        if (actividadActual.isCreadaPorSistema()) {
            repo.completarPospuesta(actividadActual.getId());
        } else {
            actividadActual.setEstado(Actividad.ESTADO_COMPLETADA);
            repo.update(actividadActual);
        }
        hablarOSimular("¡Muy bien hecho!");
        finish();
    }

    private int getIconoParaTipo(String tipo) {
        switch (tipo) {
            case Actividad.TIPO_MEDICACION:       return R.drawable.ic_medicacion;
            case Actividad.TIPO_BEBER_AGUA:       return R.drawable.ic_agua;
            case Actividad.TIPO_COMER:            return R.drawable.ic_comida;
            case Actividad.TIPO_PASEO_EJERCICIO:  return R.drawable.ic_ejercicio;
            case Actividad.TIPO_JUEGOS:           return R.drawable.ic_puzzle;
            case Actividad.TIPO_ASEO:             return R.drawable.ic_aseo;
            case Actividad.TIPO_LLAMADA_FAMILIAR: return R.drawable.ic_llamada;
            case Actividad.TIPO_IR_DORMIR:        return R.drawable.ic_dormir;
            default:                              return R.drawable.ic_calendario;
        }
    }
}
