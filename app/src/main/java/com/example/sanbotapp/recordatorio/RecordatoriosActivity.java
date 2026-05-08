package com.example.sanbotapp.recordatorio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordatoriosActivity extends BaseActivity {

    // ── Constante configurable ────────────────────────────────────────────────
    /** Milisegundos de espera entre que el robot termina de hablar y se activa el micro.
     *  Auméntalo si el micro se corta antes de que el robot acabe. */
    private static final int DELAY_MICRO_MS = 3000;

    // ── Vistas principales ────────────────────────────────────────────────────
    private LinearLayout           containerRecordatorios;
    private TextView               tvVacio;
    private RecordatorioRepository repo;

    // ── Estado del diálogo ────────────────────────────────────────────────────
    private int  horaSeleccionada   = 9 * 60;
    private long fechaSeleccionadaMs;

    // ── Estado de voz ─────────────────────────────────────────────────────────
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;

    // Referencias débiles al diálogo activo
    private java.lang.ref.WeakReference<AlertDialog> dialogRef;
    private java.lang.ref.WeakReference<EditText>    etTituloRef;
    private java.lang.ref.WeakReference<EditText>    etDescRef;
    private java.lang.ref.WeakReference<TextView>    tvHoraRef;
    private java.lang.ref.WeakReference<TextView>    tvFechaRef;

    // Handler en hilo principal (sustituye new Thread + sleep)
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // =========================================================================
    // Ciclo de vida
    // =========================================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordatorios);
        setupTopBackBanner("Recordatorios");

        containerRecordatorios = findViewById(R.id.containerRecordatorios);
        tvVacio                = findViewById(R.id.tvVacioRecordatorios);
        repo                   = new RecordatorioRepository(this);
        fechaSeleccionadaMs    = fechaHoyInicio();

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirRecordatorio);
        btnAnadir.setOnClickListener(v -> mostrarDialogoAnadir(null));

        renderizarLista();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }

    // =========================================================================
    // Sensor táctil de cabeza → entrada por voz
    // =========================================================================

    @Override
    protected void onCabezaTocada() {
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        switch (campoEspera) {

            case CAMPO_EDITAR:
                hablarEnMain("¿Qué campo quieres cambiar? Puedes decir: "
                        + "título, descripción, hora, o fecha.");
                mainHandler.postDelayed(this::escuchar, DELAY_MICRO_MS);
                break;

            case TITULO:
                hablarEnMain("Dime el título del recordatorio.");
                mainHandler.postDelayed(this::escuchar, DELAY_MICRO_MS);
                break;

            case DESCRIPCION:
                hablarEnMain("Dime una descripción, o di 'ninguna' para dejarlo vacío.");
                mainHandler.postDelayed(this::escuchar, DELAY_MICRO_MS);
                break;

            case HORA:
                hablarEnMain("Dime la hora. Por ejemplo: nueve y media, o las doce.");
                mainHandler.postDelayed(this::escuchar, DELAY_MICRO_MS);
                break;

            case FECHA:
                hablarEnMain("Dime la fecha. Por ejemplo: quince de marzo de dos mil veinticinco.");
                mainHandler.postDelayed(this::escuchar, DELAY_MICRO_MS);
                break;

            case NINGUNO:
            default:
                hablarEnMain("Todos los campos están listos. Pulsa Guardar cuando quieras.");
                break;
        }
    }

    // =========================================================================
    // Resultado del reconocimiento de voz
    // =========================================================================

    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        AlertDialog dlg = dialogRef != null ? dialogRef.get() : null;
        if (dlg == null || !dlg.isShowing()) return;

        switch (campoEspera) {

            // ── El usuario dice qué campo quiere editar ───────────────────────
            case CAMPO_EDITAR: {
                String t = texto.toLowerCase().trim();
                if (t.contains("título") || t.contains("titulo") || t.contains("nombre")) {
                    campoEspera = CampoVozEspera.TITULO;
                    hablarEnMain("De acuerdo. Toca mi cabeza y dime el nuevo título.");
                } else if (t.contains("descripción") || t.contains("descripcion")) {
                    campoEspera = CampoVozEspera.DESCRIPCION;
                    hablarEnMain("Entendido. Toca mi cabeza y dime la nueva descripción.");
                } else if (t.contains("hora") || t.contains("tiempo")) {
                    campoEspera = CampoVozEspera.HORA;
                    hablarEnMain("Perfecto. Toca mi cabeza y dime la nueva hora.");
                } else if (t.contains("fecha") || t.contains("día") || t.contains("dia")) {
                    campoEspera = CampoVozEspera.FECHA;
                    hablarEnMain("Perfecto. Toca mi cabeza y dime la nueva fecha.");
                } else {
                    // No reconocido → sin avanzar, reintento
                    hablarEnMain("No entendí qué campo. Toca mi cabeza y di: "
                            + "título, descripción, hora, o fecha.");
                }
                break;
            }

            // ── Título ────────────────────────────────────────────────────────
            case TITULO: {
                String titulo = texto.trim();
                runOnUiThread(() -> {
                    EditText et = etTituloRef != null ? etTituloRef.get() : null;
                    if (et != null) et.setText(titulo);
                });
                // Flujo nuevo: avanza a descripción. Flujo edición: vuelve a CAMPO_EDITAR
                if (esDialogoEdicion) {
                    campoEspera = CampoVozEspera.CAMPO_EDITAR;
                    hablarEnMain("Título guardado: " + titulo
                            + ". Toca mi cabeza si quieres cambiar otro campo.");
                } else {
                    campoEspera = CampoVozEspera.DESCRIPCION;
                    hablarEnMain("Título guardado: " + titulo
                            + ". Toca mi cabeza para decirme la descripción.");
                }
                break;
            }

            // ── Descripción ───────────────────────────────────────────────────
            case DESCRIPCION: {
                String desc = texto.trim().equalsIgnoreCase("ninguna") ? "" : texto.trim();
                runOnUiThread(() -> {
                    EditText et = etDescRef != null ? etDescRef.get() : null;
                    if (et != null) et.setText(desc);
                });
                if (esDialogoEdicion) {
                    campoEspera = CampoVozEspera.CAMPO_EDITAR;
                    hablarEnMain("Descripción guardada. Toca mi cabeza si quieres cambiar otro campo.");
                } else {
                    campoEspera = CampoVozEspera.HORA;
                    hablarEnMain("Descripción guardada. Toca mi cabeza para decirme la hora.");
                }
                break;
            }

            // ── Hora ──────────────────────────────────────────────────────────
            case HORA: {
                Integer minutosVoz = parsearHoraVoz(texto);
                if (minutosVoz != null) {
                    horaSeleccionada = minutosVoz;
                    runOnUiThread(() -> {
                        TextView tv = tvHoraRef != null ? tvHoraRef.get() : null;
                        if (tv != null) actualizarDisplayHora(tv, horaSeleccionada);
                    });
                    if (esDialogoEdicion) {
                        campoEspera = CampoVozEspera.CAMPO_EDITAR;
                        hablarEnMain("Hora guardada. Toca mi cabeza si quieres cambiar otro campo.");
                    } else {
                        campoEspera = CampoVozEspera.FECHA;
                        hablarEnMain("Hora guardada. Toca mi cabeza para decirme la fecha.");
                    }
                } else {
                    // Reintento: campoEspera no avanza
                    hablarEnMain("No entendí la hora. Toca mi cabeza de nuevo para repetirla.");
                }
                break;
            }

            // ── Fecha ─────────────────────────────────────────────────────────
            case FECHA: {
                long[] fechaVoz = parsearFechaVoz(texto);
                if (fechaVoz != null) {
                    fechaSeleccionadaMs = fechaVoz[0];
                    runOnUiThread(() -> {
                        TextView tv = tvFechaRef != null ? tvFechaRef.get() : null;
                        if (tv != null) actualizarDisplayFecha(tv, fechaSeleccionadaMs);
                    });
                    if (esDialogoEdicion) {
                        campoEspera = CampoVozEspera.CAMPO_EDITAR;
                        hablarEnMain("Fecha guardada. Toca mi cabeza si quieres cambiar otro campo.");
                    } else {
                        campoEspera = CampoVozEspera.NINGUNO;
                        hablarEnMain("Fecha guardada. Ya puedes pulsar Guardar cuando estés listo.");
                    }
                } else {
                    // Reintento: campoEspera no avanza
                    hablarEnMain("No entendí la fecha. Toca mi cabeza de nuevo. "
                            + "Di, por ejemplo: quince de marzo de dos mil veinticinco.");
                }
                break;
            }

            default:
                break;
        }
    }

    // ── Flag para saber si el diálogo es de edición o de creación ────────────
    private boolean esDialogoEdicion = false;

    // =========================================================================
    // Renderizado
    // =========================================================================

    private void renderizarLista() {
        containerRecordatorios.removeAllViews();
        List<Recordatorio> lista = repo.getFuturos();

        if (lista.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
            for (final Recordatorio r : lista) {
                containerRecordatorios.addView(crearItemRecordatorio(r));
            }
        }
    }

    private boolean haySolapamiento(long fechaMs, int horaMinutos, int idAExcluir) {
        for (Recordatorio r : repo.getFuturos()) {
            if (r.getId() == idAExcluir) continue;
            if (r.getFechaMs() == fechaMs) {
                int s1 = horaMinutos,         e1 = s1 + 30;
                int s2 = r.getHoraMinutos(),  e2 = s2 + 30;
                if (s1 < e2 && s2 < e1) return true;
            }
        }
        return false;
    }

    private View crearItemRecordatorio(final Recordatorio r) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_recordatorio, containerRecordatorios, false);

        ((TextView) item.findViewById(R.id.tvHoraItemRec)).setText(r.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvFechaItemRec)).setText(r.getFechaFormateada());
        ((TextView) item.findViewById(R.id.tvTituloItemRec)).setText(
                r.getTitulo() != null && !r.getTitulo().isEmpty()
                        ? r.getTitulo().toUpperCase() : "SIN TÍTULO");

        item.setOnClickListener(v -> mostrarDialogoDetalle(r));
        item.findViewById(R.id.btnEditarItemRec).setOnClickListener(v -> mostrarDialogoAnadir(r));
        item.findViewById(R.id.btnEliminarItemRec).setOnClickListener(v -> confirmarEliminar(r));

        return item;
    }

    // =========================================================================
    // Diálogo AÑADIR / EDITAR
    // =========================================================================

    private void mostrarDialogoAnadir(final Recordatorio existente) {
        esDialogoEdicion    = (existente != null);
        horaSeleccionada    = esDialogoEdicion ? existente.getHoraMinutos() : 9 * 60;
        fechaSeleccionadaMs = esDialogoEdicion ? existente.getFechaMs() : fechaHoyInicio();
        campoEspera         = CampoVozEspera.NINGUNO;

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_recordatorio, null);

        final EditText etTitulo     = dv.findViewById(R.id.etTituloRecordatorio);
        final EditText etDesc       = dv.findViewById(R.id.etDescripcionRecordatorio);
        final TextView tvHora       = dv.findViewById(R.id.tvHoraDialogRec);
        final View     tvFechaCont  = dv.findViewById(R.id.tvFechaDialogRec);
        final TextView tvFechaTexto = dv.findViewById(R.id.tvFechaTexto);

        if (existente != null) {
            etTitulo.setText(existente.getTitulo());
            etDesc.setText(existente.getDescripcion());
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);
        actualizarDisplayFecha(tvFechaTexto, fechaSeleccionadaMs);

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));
        tvFechaCont.setOnClickListener(v -> abrirDatePicker(tvFechaTexto));

        // Referencias débiles para acceso desde callbacks de voz
        etTituloRef = new java.lang.ref.WeakReference<>(etTitulo);
        etDescRef   = new java.lang.ref.WeakReference<>(etDesc);
        tvHoraRef   = new java.lang.ref.WeakReference<>(tvHora);
        tvFechaRef  = new java.lang.ref.WeakReference<>(tvFechaTexto);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogRef = new java.lang.ref.WeakReference<>(dialog);

        dialog.setOnDismissListener(d -> {
            campoEspera = CampoVozEspera.NINGUNO;
            dialogRef   = null;
            etTituloRef = null;
            etDescRef   = null;
            tvHoraRef   = null;
            tvFechaRef  = null;
            mainHandler.removeCallbacksAndMessages(null);
        });

        dv.findViewById(R.id.btnCancelarDialogRec).setOnClickListener(v -> dialog.dismiss());
        dv.findViewById(R.id.btnCerrarDialogRec).setOnClickListener(v -> dialog.dismiss());

        dv.findViewById(R.id.btnGuardarDialogRec).setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String desc   = etDesc.getText().toString().trim();
            if (titulo.isEmpty()) titulo = "Sin título";

            final String finalTitulo = titulo;
            final String finalDesc   = desc;
            final int    idExistente = esDialogoEdicion ? existente.getId() : -1;

            if (!esRecordatorioFuturo(fechaSeleccionadaMs, horaSeleccionada)) {
                new AlertDialog.Builder(this)
                        .setTitle("Fecha no válida")
                        .setMessage("No puedes guardar un recordatorio en una fecha u hora anterior a la actual.")
                        .setPositiveButton("Aceptar", null)
                        .show();
                return;
            }

            if (haySolapamiento(fechaSeleccionadaMs, horaSeleccionada, idExistente)) {
                new AlertDialog.Builder(this)
                        .setTitle("Solapamiento")
                        .setMessage("Ya hay un recordatorio a esa hora. ¿Deseas guardarlo de todos modos?")
                        .setPositiveButton("Sí", (d, w) -> {
                            guardarRecordatorio(existente, finalTitulo, finalDesc);
                            dialog.dismiss();
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                guardarRecordatorio(existente, finalTitulo, finalDesc);
                dialog.dismiss();
            }
        });

        dialog.show();

        // El robot explica el flujo al abrirse
        mainHandler.postDelayed(() -> {
            if (!esDialogoEdicion) {
                // Creación: flujo lineal título → descripción → hora → fecha
                campoEspera = CampoVozEspera.TITULO;
                hablarEnMain("Vamos a añadir un recordatorio. "
                        + "Toca mi cabeza cuando estés listo y dime el título.");
            } else {
                // Edición: el robot pregunta qué campo
                campoEspera = CampoVozEspera.CAMPO_EDITAR;
                hablarEnMain("Toca mi cabeza y dime qué campo quieres cambiar: "
                        + "título, descripción, hora, o fecha.");
            }
        }, 400);
    }

    private void guardarRecordatorio(Recordatorio existente, String titulo, String desc) {
        if (existente == null) {
            repo.add(new Recordatorio(0, titulo, horaSeleccionada, fechaSeleccionadaMs, desc));
        } else {
            existente.setTitulo(titulo);
            existente.setHoraMinutos(horaSeleccionada);
            existente.setFechaMs(fechaSeleccionadaMs);
            existente.setDescripcion(desc);
            repo.update(existente);
        }
        renderizarLista();
    }

    // =========================================================================
    // Diálogo DETALLE
    // =========================================================================

    private void mostrarDialogoDetalle(final Recordatorio r) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_recordatorio, null);

        ((TextView) dv.findViewById(R.id.tvTituloDetRec)).setText(
                r.getTitulo() != null ? r.getTitulo().toUpperCase() : "SIN TÍTULO");
        ((TextView) dv.findViewById(R.id.tvHoraDetRec)).setText(r.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvFechaDetRec)).setText(r.getFechaFormateada());
        String desc = (r.getDescripcion() != null && !r.getDescripcion().isEmpty())
                ? r.getDescripcion() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetRec)).setText(desc);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCerrarDetRec).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================================
    // Confirmar eliminar
    // =========================================================================

    private void confirmarEliminar(final Recordatorio r) {
        String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty())
                ? r.getTitulo() : "este recordatorio";
        new AlertDialog.Builder(this)
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Seguro que quieres eliminar \"" + titulo + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    repo.delete(r.getId());
                    renderizarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =========================================================================
    // Parsers de voz
    // =========================================================================

    /**
     * Convierte texto hablado en minutos desde medianoche.
     * Ejemplos: "nueve y media" → 570, "las doce" → 720, "ocho menos cuarto" → 465
     * Devuelve null si no se reconoce el patrón.
     */
    private Integer parsearHoraVoz(String texto) {
        texto = texto.toLowerCase().trim();

        // Dígitos directos: "9:30", "10 00"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})[:\\s](\\d{2})").matcher(texto);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            if (h >= 0 && h <= 23 && min >= 0 && min <= 59) return h * 60 + min;
        }

        // "a las 9", "las 10"
        m = java.util.regex.Pattern
                .compile("(?:las?|a las?)\\s*(\\d{1,2})").matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1)) * 60;

        // Dígito suelto: "9", "10"
        m = java.util.regex.Pattern.compile("^(\\d{1,2})$").matcher(texto.trim());
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h >= 0 && h <= 23) return h * 60;
        }

        // Palabras
        final String[] nombresHora = {
                "una","dos","tres","cuatro","cinco","seis",
                "siete","ocho","nueve","diez","once","doce"
        };
        int horaBase = -1;
        for (int i = 0; i < nombresHora.length; i++) {
            if (texto.contains(nombresHora[i])) { horaBase = i + 1; break; }
        }
        if (horaBase == -1) return null;

        int minutos = 0;
        if (texto.contains("y media"))           { minutos = 30; }
        else if (texto.contains("y cuarto"))     { minutos = 15; }
        else if (texto.contains("menos cuarto")) { horaBase--; if (horaBase <= 0) horaBase += 12; minutos = 45; }

        return horaBase * 60 + minutos;
    }

    /**
     * Convierte texto hablado en timestamp Unix (ms) de la fecha indicada a las 00:00.
     *
     * Ejemplos:
     *   "quince de marzo de dos mil veinticinco"  → timestamp del 15/03/2025 00:00
     *   "el tres de enero"                        → timestamp del 03/01/año actual 00:00
     *   "22 de septiembre de 2026"                → timestamp del 22/09/2026 00:00
     *
     * @return long[]{timestampMs} o null si no se reconoce.
     */
    private long[] parsearFechaVoz(String texto) {
        texto = texto.toLowerCase().trim();

        // ── Detectar mes ──────────────────────────────────────────────────────
        final String[] MESES = {
                "enero","febrero","marzo","abril","mayo","junio",
                "julio","agosto","septiembre","octubre","noviembre","diciembre"
        };
        int mes = -1;
        for (int i = 0; i < MESES.length; i++) {
            if (texto.contains(MESES[i])) { mes = i + 1; break; }
        }
        if (mes == -1) return null;

        // ── Detectar día ──────────────────────────────────────────────────────
        final String[] NUMEROS = {
                "uno","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez",
                "once","doce","trece","catorce","quince","dieciséis","diecisiete","dieciocho",
                "diecinueve","veinte","veintiuno","veintidós","veintitrés","veinticuatro",
                "veinticinco","veintiséis","veintisiete","veintiocho","veintinueve","treinta",
                "treinta y uno"
        };
        final String[] NUMEROS_ALT = {
                "uno","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez",
                "once","doce","trece","catorce","quince","dieciseis","diecisiete","dieciocho",
                "diecinueve","veinte","veintiuno","veintidos","veintitres","veinticuatro",
                "veinticinco","veintiseis","veintisiete","veintiocho","veintinueve","treinta",
                "treinta y uno"
        };

        int dia = -1;
        // Primero intentar dígito
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(\\d{1,2})\\b").matcher(texto);
        while (m.find()) {
            int c = Integer.parseInt(m.group(1));
            if (c >= 1 && c <= 31) { dia = c; break; }
        }
        // Si no, palabras (de mayor a menor para evitar "tres" dentro de "treinta")
        if (dia == -1) {
            for (int i = NUMEROS.length - 1; i >= 0; i--) {
                if (texto.contains(NUMEROS[i]) || texto.contains(NUMEROS_ALT[i])) {
                    dia = i + 1; break;
                }
            }
        }
        if (dia == -1 || dia < 1 || dia > 31) return null;

        // ── Detectar año ──────────────────────────────────────────────────────
        int anio = Calendar.getInstance().get(Calendar.YEAR); // por defecto año actual

        // Dígitos: "2025", "2026"
        m = java.util.regex.Pattern.compile("\\b(20\\d{2})\\b").matcher(texto);
        if (m.find()) {
            anio = Integer.parseInt(m.group(1));
        } else {
            // Palabras: "dos mil veinticinco" → 2025, "dos mil veintiséis" → 2026
            final String[][] ANIOS_PALABRAS = {
                    {"dos mil veintiuno",      "2021"},
                    {"dos mil veintidós",      "2022"}, {"dos mil veintidos",   "2022"},
                    {"dos mil veintitrés",     "2023"}, {"dos mil veintitres",  "2023"},
                    {"dos mil veinticuatro",   "2024"},
                    {"dos mil veinticinco",    "2025"},
                    {"dos mil veintiséis",     "2026"}, {"dos mil veintiseis",  "2026"},
                    {"dos mil veintisiete",    "2027"},
                    {"dos mil veintiocho",     "2028"},
                    {"dos mil veintinueve",    "2029"},
                    {"dos mil treinta",        "2030"},
            };
            for (String[] par : ANIOS_PALABRAS) {
                if (texto.contains(par[0])) { anio = Integer.parseInt(par[1]); break; }
            }
        }

        // ── Construir timestamp ───────────────────────────────────────────────
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new long[]{ cal.getTimeInMillis() };
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Wrapper que garantiza que hablarOSimular se llama siempre en el hilo principal. */
    private void hablarEnMain(String texto) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hablarOSimular(texto);
        } else {
            runOnUiThread(() -> hablarOSimular(texto));
        }
    }

    private void abrirTimePicker(final TextView tv) {
        int h = horaSeleccionada / 60, m = horaSeleccionada % 60;
        new TimePickerDialog(this, android.R.style.Theme_Material_Dialog,
                (view, hh, mm) -> {
                    horaSeleccionada = hh * 60 + mm;
                    actualizarDisplayHora(tv, horaSeleccionada);
                }, h, m, true).show();
    }

    private void abrirDatePicker(final TextView tv) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(fechaSeleccionadaMs);
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, day, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    fechaSeleccionadaMs = c.getTimeInMillis();
                    actualizarDisplayFecha(tv, fechaSeleccionadaMs);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void actualizarDisplayHora(TextView tv, int minutos) {
        tv.setText(String.format("%02d:%02d", minutos / 60, minutos % 60));
    }

    private void actualizarDisplayFecha(TextView tv, long ms) {
        Recordatorio tmp = new Recordatorio();
        tmp.setFechaMs(ms);
        tv.setText(tmp.getFechaFormateada());
    }

    private long fechaHoyInicio() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long construirTimestampRecordatorio(long fechaMs, int horaMinutos) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fechaMs);
        c.set(Calendar.HOUR_OF_DAY, horaMinutos / 60);
        c.set(Calendar.MINUTE, horaMinutos % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private boolean esRecordatorioFuturo(long fechaMs, int horaMinutos) {
        return construirTimestampRecordatorio(fechaMs, horaMinutos) > System.currentTimeMillis();
    }
}