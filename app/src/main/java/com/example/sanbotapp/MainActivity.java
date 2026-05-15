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

import com.example.sanbotapp.actividad.ActividadUIUtils;
import com.example.sanbotapp.util.AppConstants;
import com.example.sanbotapp.util.PhotoHelper;
import com.example.sanbotapp.actividad.Actividad;
import com.example.sanbotapp.actividad.ActividadRepository;
import com.example.sanbotapp.actividad.ActividadesActivity;
import com.example.sanbotapp.recordatorio.RecordatoriosActivity;
import com.example.sanbotapp.alarmas.AlarmScheduler;


import java.io.File;
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

    // Botones Principales
    private LinearLayout btnActividades, btnRecordatorios, btnJuegos, btnAjustes;
    // Rutinas de hoy
    private LinearLayout containerRutinasHoy;
    // Mensaje predeterminado ('Nada planificado')   
    private TextView     tvVacioHoy;
    // Saludo y fecha
    private TextView     tvSaludo, tvFechaHoy;
    private TextView     tvNombreAvatar;
    // Nombre del usuario
    private SharedPreferences prefs;
    private String nombreUsuario = "amigo/a";
    // Comprobador de si hay actividades para activar
    private boolean yaHeSaludado = false;

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
        prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);

        // Verificar si es la primera vez que se abre la app
        if (prefs.getBoolean(AppConstants.KEY_FIRST_RUN, true)) {
            // Es la primera vez: abrir WelcomeActivity
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Se preparan las vistas y componentes de la pantalla principal
        inicializarVistas();
        mostrarFechaActual();
        configurarBotones();
        reiniciarBrazos();
        
        // Inicializar canal de notificaciones para alarmas
        AlarmScheduler.createNotificationChannel(this);
    }

    // Se ejecuta al volver a visualizar la pantalla (ej. tras cerrar la pantalla de rutinas)
    /*
     * Pre: Se ejecuta al volver a visualizar la pantalla (ej. tras cerrar la pantalla de rutinas)
     * Post: Se recarga la lista de rutinas de hoy para reflejar cambios (ej. si una tarea desaparece o pospone) y se reinicia el reloj vigilante
     */
    @Override
    public void onResume() {
        super.onResume();
        cargarDatosGuardados(); // Recargar nombre y foto por si cambiaron en Ajustes
        cargarRutinaHoy(); // Recargamos por si se añadió alguna actividad en otra ventana
        activarSeguimiento(); // MEJORA ÁREA 5: El robot mira al usuario al inicio
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Cuando la aplicación pasa a segundo plano o abrimos otra pantalla, se pausa el vigilante para ahorrar batería/recursos
     */
    @Override
    public void onPause() {
        super.onPause();
        desactivarSeguimiento(); // Desactivar seguimiento al salir para ahorrar recursos
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Cuando el robot está listo, se ejecuta este método y saluda al usuario
     */
    @Override
    protected void onRobotServiceReady() {
        nombreUsuario = prefs.getString(AppConstants.KEY_NOMBRE, "amigo/a");
        
        // Saludo solo la primera vez que se entra en la app para no ser repetitivo
        if (!yaHeSaludado) {
            yaHeSaludado = true;
            boolean isNewUser = getIntent().getBooleanExtra("is_new_user", false);
            
            if (isNewUser) {
                // Mensaje específico tras registro
                hablarOSimular("Bienvenido " + nombreUsuario + " a Vitalia, aquí puedes gestionar toda tu aplicación.");
            } else {
                // Saludo estándar contextual
                gestionarFeedbackHardware("SALUDO");
                hablarOSimular(generarSaludoContextual(nombreUsuario));
            }
            mostrarEmocion(getEmocionPorHora());
        }
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
     * No van las emociones
     */
    private String getEmocionPorHora() {
        int hora = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);

        if (hora >= 6 && hora < 12) {
            return "SMILE";     // Mañana → cara alegre
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
        tvNombreAvatar      = findViewById(R.id.tvNombreAvatar);
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
        nombreUsuario = prefs.getString(AppConstants.KEY_NOMBRE, "amigo/a");
        tvSaludo.setText("¡Hola, " + nombreUsuario + "!");
        tvNombreAvatar.setText(nombreUsuario);

        String fotoPa = prefs.getString(AppConstants.KEY_FOTO_PATH, null);
        PhotoHelper.mostrarFotoDesdeRuta(fotoPa, findViewById(R.id.ivAvatar));
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
                .setImageResource(a.getIconoRes());
        ((TextView) card.findViewById(R.id.tvLabelCard)).setText(a.getTipoLabel());

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(22));

        if (Actividad.ESTADO_COMPLETADA.equals(a.getEstado())) {
            // Completada → fondo transparente con borde del color del tipo
            int colorBase = Color.parseColor(a.getColorHex());
            bg.setColor(Color.TRANSPARENT);
            bg.setStroke((int) dpToPx(2), colorBase);

            // Texto e icono en el color del tipo (no blanco)
            TextView tvLabel = card.findViewById(R.id.tvLabelCard);
            tvLabel.setTextColor(colorBase);
            TextView tvHora = card.findViewById(R.id.tvHoraCard);
            tvHora.setTextColor(colorBase);
        } else {
            // Pendiente / pospuesta → fondo sólido con color del tipo
            bg.setColor(Color.parseColor(a.getColorHex()));
        }

        card.setBackground(bg);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> ActividadUIUtils.mostrarDialogoDetalle(this, a));

        return card;
    }

    /*
     * Pre: Se ejecuta al crear la pantalla principal
     * Post: Se muestra el detalle de la actividad
     */



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
            startActivity(new Intent(this, ActividadesActivity.class));
        });

        btnRecordatorios.setOnClickListener(v -> {
            hablarOSimular("Vamos a ver tus recordatorios.");
            startActivity(new Intent(this, RecordatoriosActivity.class));
        });

        btnJuegos.setOnClickListener(v -> {
            hablarOSimular("¡Hora de jugar!");
            startActivity(new Intent(this, com.example.sanbotapp.juegos.JuegosActivity.class));
        });

        btnAjustes.setOnClickListener(v -> {
            hablarOSimular("Abriendo ajustes.");
            startActivity(new Intent(this, com.example.sanbotapp.ajustes.AjustesActivity.class));
        });
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}