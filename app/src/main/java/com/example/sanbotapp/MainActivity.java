package com.example.sanbotapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.actividad.ActividadesActivity;
import com.example.sanbotapp.actividad.ActivitySchedulerHelper;
import com.example.sanbotapp.recordatorio.RecordatoriosActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * MainActivity extiende BaseActivity para obtener los métodos que usa el robot.
 * 
 */
public class MainActivity extends BaseActivity {

    // Constantes para guardar datos en SharedPreferences (nombre y foto del usuario)
    private static final String PREFS_NAME   = "AppPrefs";
    private static final String KEY_NOMBRE   = "nombre_usuario";
    private static final String KEY_FOTO_URI = "foto_uri";
    
    // Botones Principales
    private LinearLayout btnActividades, btnRecordatorios, btnJuegos, btnAjustes;
    // Rutinas de hoy
    private LinearLayout containerRutinasHoy;
    // Mensaje predeterminado ('Nada planificado')   
    private TextView     tvVacioHoy;
    // Saludo y fecha
    private TextView     tvSaludo, tvFechaHoy;
    // Nombre del usuario
    private SharedPreferences prefs;
    private String nombreUsuario = "amigo/a";
    // Comprobador de si hay actividades para activar   
    private ActivitySchedulerHelper scheduler;
    
    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se inicializan las vistas y componentes de la pantalla principal, 
     *       se cargan los datos guardados, se muestra la fecha actual 
     *       y se configuran los botones   
     */
    public void onCreate(Bundle savedInstanceState) {
        // Constructor para crear la pantalla principal
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Se inicializan las preferencias compartidas para leer datos como el nombre de usuario
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Se preparan las vistas y componentes de la pantalla principal
        inicializarVistas();
        cargarDatosGuardados();
        mostrarFechaActual();
        configurarBotones();
        
        // Se inicializa el vigilante que comprobará si es hora de ejecutar una actividad programada
        scheduler = new ActivitySchedulerHelper(this, new ActivitySchedulerHelper.RobotActionCallback() {
            @Override
            public void onTriggerRobotAction(String frase, String tipoEmocion) {
                // Cuando es la hora, manda orden al robot de hablar y mostrar emoción
                hablarOSimular(frase);
                // Recarga la lista de rutinas de hoy para reflejar cambios (ej. si una tarea desaparece o pospone)
                cargarRutinaHoy(); // Refresh list to show popup/postponed changes
            }
        });
    }

    // Se ejecuta al volver a visualizar la pantalla (ej. tras cerrar la pantalla de rutinas)
    /*
     * Pre: Se ejecuta al volver a visualizar la pantalla (ej. tras cerrar la pantalla de rutinas)
     * Post: Se recarga la lista de rutinas de hoy para reflejar cambios (ej. si una tarea desaparece o pospone) y se reinicia el reloj vigilante
     */ 
    @Override
    public void onResume() {
        super.onResume();
        cargarRutinaHoy(); // Recargamos por si se añadió alguna actividad en otra ventana
        if (scheduler != null) scheduler.start(); // Reinicia el reloj vigilante
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Cuando la aplicación pasa a segundo plano o abrimos otra pantalla, se pausa el vigilante para ahorrar batería/recursos
     */
    @Override
    public void onPause() {
        super.onPause();
        if (scheduler != null) scheduler.stop(); // Se pausa el vigilante para ahorrar batería/recursos
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Cuando el robot está listo, se ejecuta este método y saluda al usuario   
     */ 
    @Override
    protected void onRobotServiceReady() { // Obligatorio por extender BaseActivity porque es una clase abstracta: Se ejecuta cuando el hardware del robot despierta y está listo
        nombreUsuario = prefs.getString(KEY_NOMBRE, "amigo/a");
        hablarOSimular(generarSaludoContextual(nombreUsuario));
        mostrarEmocion(getEmocionPorHora()); // opcional, ver abajo
    }

    /**
     * Devuelve la frase de saludo adecuada según la hora del día.
     *
     * @param nombre Nombre del usuario leído de SharedPreferences.
     * @return Frase completa lista para que el robot la pronuncie.
     */
    private String generarSaludoContextual(String nombre) {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (hora >= 6 && hora < 12) {
            return "Buenos días, " + nombre + ". Espero que hayas descansado bien.";
        } else if (hora >= 12 && hora < 20) {
            return "Buenas tardes, " + nombre + ". Espero que hayas tenido un buen dia";
        } else {
            return "Buenas noches, " + nombre + ". Pronto tocará ir a la cama.";
        }
    }

    /**
     * Devuelve la emoción apropiada para mostrar en pantalla según la hora.
     * Se combina con mostrarEmocion() si quieres acompañar el saludo visualmente.
     *
     * @return Nombre de la emoción compatible con EmotionsType del SDK.
     */
    private String getEmocionPorHora() {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (hora >= 6 && hora < 12) {
            return "HAPPY";     // Mañana → cara alegre
        } else if (hora >= 12 && hora < 20) {
            return "NEUTRAL";   // Tarde → cara tranquila
        } else {
            return "SLEEPY";    // Noche → cara de sueño
        }
    }
    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se inicializan las vistas y componentes de la pantalla principal   
     */
    private void inicializarVistas() {
        tvSaludo            = findViewById(R.id.tvSaludo);
        tvFechaHoy          = findViewById(R.id.tvFechaHoy);
        containerRutinasHoy = findViewById(R.id.containerRutinasHoy);
        tvVacioHoy          = findViewById(R.id.tvVacioHoy);
        btnActividades      = findViewById(R.id.btnActividades);
        btnRecordatorios    = findViewById(R.id.btnRecordatorios);
        btnJuegos           = findViewById(R.id.btnJuegos);
        btnAjustes          = findViewById(R.id.btnAjustes);
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se cargan los datos guardados (nombre y foto del usuario)
     */
    private void cargarDatosGuardados() {
        nombreUsuario = prefs.getString(KEY_NOMBRE, "amigo/a");
        tvSaludo.setText("¡Hola, " + nombreUsuario + "!");

        String fotoUri = prefs.getString(KEY_FOTO_URI, null);
        if (fotoUri != null) {
            try {
                Uri uri = Uri.parse(fotoUri);
                getContentResolver().openInputStream(uri).close();
                ((ImageView) findViewById(R.id.ivAvatar)).setImageURI(uri);
            } catch (Exception e) {
                prefs.edit().remove(KEY_FOTO_URI).apply();
            }
        }
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se muestra la fecha actual en el formato "EEEE, d 'de' MMMM 'de' yyyy" (ej. LUNES, 23 DE MARZO DE 2026)
     */
    private void mostrarFechaActual() {
        SimpleDateFormat sdfDia   = new SimpleDateFormat("EEEE", new Locale("es", "ES"));
        SimpleDateFormat sdfFecha = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        Date hoy = new Date();
        tvFechaHoy.setText(sdfDia.format(hoy).toUpperCase() + ", " + sdfFecha.format(hoy));
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se cargan las actividades del día de hoy y se muestran en la pantalla principal
     */
    private void cargarRutinaHoy() {
        containerRutinasHoy.removeAllViews(); // Limpia la lista actual en pantalla
        ActividadRepository repo = new ActividadRepository(this);
        List<Actividad> hoy = repo.getDeHoy(); // Filtra la BD por el día de la semana actual

        if (hoy.isEmpty()) {
            // Si no hay tareas de hoy, muestra el mensaje predeterminado ('Nada planificado')
            tvVacioHoy.setVisibility(View.VISIBLE);
            containerRutinasHoy.setVisibility(View.GONE);
        } else {
            // Si hay tareas, oculta el mensaje de vacío y dibuja las tarjetas visuales
            tvVacioHoy.setVisibility(View.GONE);
            containerRutinasHoy.setVisibility(View.VISIBLE);
            for (Actividad a : hoy) {
                containerRutinasHoy.addView(crearTarjetaActividad(a));
            }
        }
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se crea una tarjeta visual para cada actividad
     */
    private View crearTarjetaActividad(final Actividad a) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_actividad_card, containerRutinasHoy, false);

        ((TextView) card.findViewById(R.id.tvHoraCard)).setText(a.getHoraFormateada());
        ((ImageView) card.findViewById(R.id.ivIconoCard))
                .setImageResource(getIconoParaTipo(a.getTipo()));
        ((TextView) card.findViewById(R.id.tvLabelCard)).setText(a.getTipoLabel());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor(a.getColorHex()));
        bg.setCornerRadius(dpToPx(22));
        card.setBackground(bg);

        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> mostrarDetalleActividad(a));

        return card;
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se muestra el detalle de la actividad
     */
    private void mostrarDetalleActividad(final Actividad a) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_actividad, null);

        // Círculo de color con icono drawable (no emoji)
        View frameCirculo = dv.findViewById(R.id.frameEmojiDetAct);
        GradientDrawable bgCirculo = new GradientDrawable();
        bgCirculo.setShape(GradientDrawable.OVAL);
        bgCirculo.setColor(Color.parseColor(a.getColorHex()));
        frameCirculo.setBackground(bgCirculo);

        // El ImageView dentro del FrameLayout recibe el icono
        ((ImageView) dv.findViewById(R.id.tvEmojiDetAct))
                .setImageResource(getIconoParaTipo(a.getTipo()));

        // Hora en negro (sin colorear)
        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());

        // Nombre del tipo en mayúsculas
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());

        // Descripción
        String desc = (a.getDescripcion() != null && !a.getDescripcion().isEmpty())
                ? a.getDescripcion().toUpperCase() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText(desc);

        // Días de la semana
        final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 };
        int[] idsDias = {
                R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue,
                R.id.detDiaVie, R.id.detDiaSab, R.id.detDiaDom
        };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < idsDias.length; i++) {
            TextView tv = dv.findViewById(idsDias[i]);
            boolean activo = dias != null && dias.contains(VALORES_DIA[i]);
            if (activo) {
                // Círculo relleno negro para días activos
                GradientDrawable bgDia = new GradientDrawable();
                bgDia.setShape(GradientDrawable.OVAL);
                bgDia.setColor(Color.parseColor("#1C1C1E"));
                tv.setBackground(bgDia);
                tv.setTextColor(Color.WHITE);
            } else {
                // Círculo con borde fino gris para días inactivos
                tv.setBackgroundResource(R.drawable.bg_tipo_normal);
                tv.setTextColor(Color.parseColor("#1C1C1E"));
            }
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // X para cerrar
        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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

    /**
     * Configura los listeners de los botones principales de la pantalla de inicio.
     *
     * Cada botón:
     *  1. Hace que el robot confirme en voz alta la acción que va a ocurrir.
     *  2. Espera un momento breve para que el usuario oiga la confirmación
     *     antes de que la pantalla cambie (evita desorientación).
     *  3. Abre la pantalla correspondiente.
     */
    private void configurarBotones() {

        btnActividades.setOnClickListener(v -> {
            hablarOSimular("Abriendo tus actividades.");
            v.postDelayed(() ->
                    startActivity(new Intent(this, ActividadesActivity.class)), 1200);
        });

        btnRecordatorios.setOnClickListener(v -> {
            hablarOSimular("Vamos a ver tus recordatorios.");
            v.postDelayed(() ->
                    startActivity(new Intent(this, RecordatoriosActivity.class)), 1200);
        });

        btnJuegos.setOnClickListener(v -> {
            hablarOSimular("¡Hora de jugar!");
            v.postDelayed(() ->
                    startActivity(new Intent(this, com.example.sanbotapp.juegos.JuegosActivity.class)), 1200);
        });

        btnAjustes.setOnClickListener(v -> {
            hablarOSimular("Abriendo ajustes.");
            v.postDelayed(() ->
                    startActivity(new Intent(this, com.example.sanbotapp.ajustes.AjustesActivity.class)), 1200);
        });
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}