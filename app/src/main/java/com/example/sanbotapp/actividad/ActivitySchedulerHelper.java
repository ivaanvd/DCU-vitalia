package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sanbotapp.R;

import java.util.Calendar;
import java.util.List;

public class ActivitySchedulerHelper {

    public interface RobotActionCallback {
        void onTriggerRobotAction(String frase, String tipoEmocion);
    }

    private final Context context;
    private final ActividadRepository repo;
    private final RobotActionCallback robotCallback;
    
    private Handler schedulerHandler;
    private Runnable checkRunnable;
    private AlertDialog currentDialog;
    private Ringtone currentRingtone;
    
    // Para no mostrar repetidas veces la misma actividad en el mismo minuto
    private int lastActivityNotifiedId = -1;

    /*
     * Pre: Recibe el contexto de la aplicación y un callback para enviar información al robot
     * Post: Inicializa el repositorio y define la función callback a llamar cuando la alerta deba ser informada
     */
    public ActivitySchedulerHelper(Context context, RobotActionCallback callback) {
        this.context = context;
        this.repo = new ActividadRepository(context);
        this.robotCallback = callback;
    }

    /*
     * Pre: El scheduler no está corriendo
     * Post: Inicia el ciclo de verificación de actividades cada 5 segundos buscando eventos disponibles
     */
    public void start() {
        if (schedulerHandler == null) {
            schedulerHandler = new Handler(Looper.getMainLooper());
        }
        
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkUpcomingActivities();
                // Revisar cada 5 segundos
                schedulerHandler.postDelayed(this, 5000);
            }
        };
        schedulerHandler.post(checkRunnable);
    }

    /*
     * Pre: El scheduler está en ejecución 
     * Post: Detiene la verificación periódica de actividades (generalmente cuando la app se pausa)
     */
    public void stop() {
        if (schedulerHandler != null && checkRunnable != null) {
            schedulerHandler.removeCallbacks(checkRunnable);
        }
    }

    /*
     * Pre: Es invocado por el ciclo del scheduler internamente cada N segundos
     * Post: Verifica si hay alguna actividad cuya hora coincida con la hora de sistema actual y la muestra
     */
    private void checkUpcomingActivities() {
        if (currentDialog != null && currentDialog.isShowing()) {
            return; // Ya hay un popup mostrandose
        }

        Calendar now = Calendar.getInstance();
        int minutosActuales = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        
        List<Actividad> hoy = repo.getDeHoy();
        for (Actividad a : hoy) {
            boolean isTimeValid = minutosActuales >= a.getHoraMinutos() && minutosActuales <= (a.getHoraMinutos() + 5);
            
            if (a.getEstado().equals(Actividad.ESTADO_PENDIENTE) && isTimeValid) {
                if (a.getId() != lastActivityNotifiedId) {
                    lastActivityNotifiedId = a.getId();
                    showActivityPopup(a);
                    break; 
                }
            }
        }
    }

    /*
     * Pre: Se recibe en los argumentos una actividad que acaba de cumplir su horario de ejecución
     * Post: Construye un diálogo popup interrumpiendo al usuario, dispara la alerta y ejecuta el callback del robot
     */
    private void showActivityPopup(final Actividad a) {
        View dv = LayoutInflater.from(context).inflate(R.layout.dialog_actividad_programada, null);
        
        // Robot Emotion and Colors
        View frameRobot = dv.findViewById(R.id.frameRobotEmotion);
        GradientDrawable bgCirculo = new GradientDrawable();
        bgCirculo.setShape(GradientDrawable.OVAL);
        bgCirculo.setColor(Color.parseColor(a.getColorHex()));
        frameRobot.setBackground(bgCirculo);

        ImageView ivRobot = dv.findViewById(R.id.ivRobotEmotion);
        ivRobot.setImageResource(getIconoParaTipo(a.getTipo()));
        
        TextView tvTitulo = dv.findViewById(R.id.tvPopupTitulo);
        tvTitulo.setText("¡ES HORA DE " + a.getTipoLabel() + "!");

        TextView tvDesc = dv.findViewById(R.id.tvPopupDesc);
        if (a.getDescripcion() != null && !a.getDescripcion().isEmpty()) {
            tvDesc.setText(a.getDescripcion());
        } else {
            tvDesc.setVisibility(View.GONE);
        }

        TextView tvDuracion = dv.findViewById(R.id.tvPopupDuracion);
        tvDuracion.setText("Duración estimada: " + a.getDuracionMinutos() + " min");

        Button btnHacerAhora = dv.findViewById(R.id.btnPopupHacerAhora);
        Button btnPosponer = dv.findViewById(R.id.btnPopupPosponer);

        // Si es creada por sistema no se puede posponer más
        if (a.isCreadaPorSistema()) {
            btnPosponer.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dv);
        builder.setCancelable(false); // Forzar a que interactuen

        currentDialog = builder.create();
        if (currentDialog.getWindow() != null) {
            currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnHacerAhora.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Navegar a la pantalla de completado en el futuro, por ahora la marcamos completada y cerramos
                android.content.Intent intent = new android.content.Intent(context, ActividadEnCursoActivity.class);
                intent.putExtra(ActividadEnCursoActivity.EXTRA_ACTIVIDAD_ID, a.getId());
                context.startActivity(intent);
                cerrarDialogoYDetenerAlarma();
            }
        });

        btnPosponer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                repo.addCopiaPostpuesta(a, 30);
                Toast.makeText(context, "Actividad pospuesta 30 minutos", Toast.LENGTH_SHORT).show();
                cerrarDialogoYDetenerAlarma();
            }
        });

        dv.findViewById(R.id.btnPopupCerrar).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Asumimos que pospone si lo cierra
                if (!a.isCreadaPorSistema()) {
                    repo.addCopiaPostpuesta(a, 30);
                }
                cerrarDialogoYDetenerAlarma();
            }
        });

        reproducirAlarma();
        currentDialog.show();
        
        if (robotCallback != null) {
            String frase = "¡Atención! Es hora de " + a.getTipoLabel().toLowerCase();
            robotCallback.onTriggerRobotAction(frase, a.getTipo());
        }
    }

    /*
     * Pre: Se ha programado mostrar el popup de la actividad
     * Post: Reproduce la alarma o tono de notificación del sistema para alertar al paciente
     */
    private void reproducirAlarma() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            currentRingtone = RingtoneManager.getRingtone(context, alarmUri);
            if (currentRingtone != null) {
                currentRingtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Pre: El usuario interacciona con el popup (posponer, cancelar o iniciar)
     * Post: Silencia la alarma si está sonando y destruye el diálogo visual del popup
     */
    private void cerrarDialogoYDetenerAlarma() {
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    /*
     * Pre: El usuario pulsa Completar sobre una actividad
     * Post: Marca la actividad como completada definitivamente en la base de datos
     */
    private void marcarComoCompletada(Actividad a) {
        if (a.isCreadaPorSistema()) {
            repo.completarPospuesta(a.getId());
        } else {
            a.setEstado(Actividad.ESTADO_COMPLETADA);
            repo.update(a);
        }
        Toast.makeText(context, "Actividad completada", Toast.LENGTH_SHORT).show();
    }

    /*
     * Pre: Recibe en memoria el String identificador del tipo de actividad
     * Post: Devuelve el recurso visual gráfico (R.drawable.X) asociado a la categoría
     */
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
