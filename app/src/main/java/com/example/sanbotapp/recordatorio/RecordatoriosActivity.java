package com.example.sanbotapp.recordatorio;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.CampoVozEspera;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordatoriosActivity extends BaseActivity {

    private LinearLayout           containerRecordatorios;
    private TextView               tvVacio;
    private RecordatorioRepository repo;

    // ── Estado del diálogo ────────────────────────────────────────────────────
    private int  horaSeleccionada   = 9 * 60;
    private long fechaSeleccionadaMs;

    // ── Estado de voz ─────────────────────────────────────────────────────────
    /** Campo del diálogo actualmente esperando voz. NINGUNO si no hay diálogo abierto. */
    private CampoVozEspera campoEspera = CampoVozEspera.NINGUNO;

    /** Referencias vivas al diálogo abierto para actualizar sus vistas desde onCabezaTocada(). */
    private EditText  dialogEtTitulo;
    private EditText  dialogEtDesc;
    private TextView  dialogTvHora;
    private AlertDialog dialogActivo;

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

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fechaSeleccionadaMs = cal.getTimeInMillis();

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirRecordatorio);
        btnAnadir.setOnClickListener(v -> mostrarDialogoAnadir(null));

        renderizarLista();
    }

    // =========================================================================
    // Sensor táctil de cabeza → entrada por voz en el diálogo
    // =========================================================================

    /**
     * Llamado automáticamente desde BaseActivity cuando el usuario toca
     * la cabeza del robot (sensores 11, 12 o 13).
     *
     * Avanza al siguiente campo del diálogo en orden:
     *   TITULO → DESCRIPCION → HORA → NINGUNO (completo)
     */
    @Override
    protected void onCabezaTocada() {
        if (dialogActivo == null || !dialogActivo.isShowing()) return;

        switch (campoEspera) {

            case TITULO:
                campoEspera = CampoVozEspera.DESCRIPCION; // siguiente tras escuchar
                hablarOSimular("Dime el título del recordatorio.");
                new Thread(() -> { sleep(1800); escuchar(); }).start();
                break;

            case DESCRIPCION:
                campoEspera = CampoVozEspera.HORA;
                hablarOSimular("Ahora dime una descripción, o di 'ninguna' para dejarlo vacío.");
                new Thread(() -> { sleep(2200); escuchar(); }).start();
                break;

            case HORA:
                campoEspera = CampoVozEspera.NINGUNO;
                hablarOSimular("Dime la hora. Por ejemplo: nueve y media.");
                new Thread(() -> { sleep(2000); escuchar(); }).start();
                break;

            case NINGUNO:
            default:
                hablarOSimular("Todos los campos están listos. Pulsa Guardar cuando quieras.");
                break;
        }
    }

    // =========================================================================
    // Resultado del reconocimiento de voz
    // =========================================================================

    /**
     * Recibe el texto reconocido y lo aplica al campo correspondiente.
     *
     * NOTA: campoEspera ya avanzó al campo SIGUIENTE antes de llamar a escuchar(),
     * por eso procesamos el campo ANTERIOR al valor actual de campoEspera.
     */
    @Override
    protected void onTextoEscuchado(String texto) {
        if (texto == null || texto.trim().isEmpty()) return;
        if (dialogActivo == null || !dialogActivo.isShowing()) return;

        switch (campoEspera) {

            case DESCRIPCION:
                // El campo anterior era TITULO
                runOnUiThread(() -> {
                    if (dialogEtTitulo != null) dialogEtTitulo.setText(texto);
                });
                hablarOSimular("Título guardado: " + texto
                        + ". Toca mi cabeza para decirme la descripción.");
                break;

            case HORA:
                // El campo anterior era DESCRIPCION
                runOnUiThread(() -> {
                    if (dialogEtDesc != null) {
                        dialogEtDesc.setText(
                                texto.trim().equalsIgnoreCase("ninguna") ? "" : texto);
                    }
                });
                hablarOSimular("Descripción guardada. Toca mi cabeza para decirme la hora.");
                break;

            case NINGUNO:
                // El campo anterior era HORA
                runOnUiThread(() -> {
                    Integer minutosVoz = parsearHoraVoz(texto);
                    if (minutosVoz != null) {
                        horaSeleccionada = minutosVoz;
                        if (dialogTvHora != null)
                            actualizarDisplayHora(dialogTvHora, horaSeleccionada);
                        hablarOSimular("Hora guardada. Ya puedes pulsar Guardar cuando estés listo.");
                    } else {
                        hablarOSimular("No entendí la hora. Toca mi cabeza de nuevo para repetirla.");
                        campoEspera = CampoVozEspera.HORA; // reintento
                    }
                });
                break;

            default:
                break;
        }
    }

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
        List<Recordatorio> lista = repo.getFuturos();
        for (Recordatorio r : lista) {
            if (r.getId() == idAExcluir) continue;
            if (r.getFechaMs() == fechaMs) {
                int start1 = horaMinutos,    end1 = start1 + 30;
                int start2 = r.getHoraMinutos(), end2 = start2 + 30;
                if (start1 < end2 && start2 < end1) return true;
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
        horaSeleccionada    = (existente != null) ? existente.getHoraMinutos() : 9 * 60;
        fechaSeleccionadaMs = (existente != null) ? existente.getFechaMs() : fechaHoyInicio();

        // Resetear estado de voz
        campoEspera = CampoVozEspera.NINGUNO;

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_recordatorio, null);

        final EditText etTitulo        = dv.findViewById(R.id.etTituloRecordatorio);
        final EditText etDesc          = dv.findViewById(R.id.etDescripcionRecordatorio);
        final TextView tvHora          = dv.findViewById(R.id.tvHoraDialogRec);
        final View     tvFechaContainer = dv.findViewById(R.id.tvFechaDialogRec);
        final TextView tvFechaTexto    = dv.findViewById(R.id.tvFechaTexto);

        if (existente != null) {
            etTitulo.setText(existente.getTitulo());
            etDesc.setText(existente.getDescripcion());
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);
        actualizarDisplayFecha(tvFechaTexto, fechaSeleccionadaMs);

        tvHora.setOnClickListener(v -> abrirTimePicker(tvHora));
        tvFechaContainer.setOnClickListener(v -> abrirDatePicker(tvFechaTexto));

        // ── Guardar referencias vivas para acceso desde onCabezaTocada() ─────
        dialogEtTitulo = etTitulo;
        dialogEtDesc   = etDesc;
        dialogTvHora   = tvHora;

        dialogActivo = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialogActivo.getWindow() != null)
            dialogActivo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Al cerrar el diálogo, limpiar estado de voz
        dialogActivo.setOnDismissListener(d -> {
            campoEspera    = CampoVozEspera.NINGUNO;
            dialogActivo   = null;
            dialogEtTitulo = null;
            dialogEtDesc   = null;
            dialogTvHora   = null;
        });

        dv.findViewById(R.id.btnCancelarDialogRec).setOnClickListener(v -> dialogActivo.dismiss());
        dv.findViewById(R.id.btnCerrarDialogRec).setOnClickListener(v -> dialogActivo.dismiss());

        dv.findViewById(R.id.btnGuardarDialogRec).setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String desc   = etDesc.getText().toString().trim();
            if (titulo.isEmpty()) titulo = "Sin título";

            final int    idExistente  = (existente != null) ? existente.getId() : -1;
            final String finalTitulo  = titulo;
            final String finalDesc    = desc;

            if (!esRecordatorioFuturo(fechaSeleccionadaMs, horaSeleccionada)) {
                new AlertDialog.Builder(RecordatoriosActivity.this)
                        .setTitle("Fecha no válida")
                        .setMessage("No puedes guardar un recordatorio en una fecha u hora anterior a la actual.")
                        .setPositiveButton("Aceptar", null)
                        .show();
                return;
            }

            if (haySolapamiento(fechaSeleccionadaMs, horaSeleccionada, idExistente)) {
                new AlertDialog.Builder(RecordatoriosActivity.this)
                        .setTitle("Solapamiento")
                        .setMessage("Ya hay un recordatorio a esa hora el mismo día. ¿Deseas guardarlo de todos modos?")
                        .setPositiveButton("Sí", (d, w) -> {
                            guardarRecordatorio(existente, finalTitulo, finalDesc);
                            dialogActivo.dismiss();
                        })
                        .setNegativeButton("No", null)
                        .show();
            } else {
                guardarRecordatorio(existente, finalTitulo, finalDesc);
                dialogActivo.dismiss();
            }
        });

        dialogActivo.show();

        // ── VOZ: el robot explica el flujo al abrir el diálogo ───────────────
        new Thread(() -> {
            sleep(400);
            if (existente == null) {
                hablarOSimular("Vamos a añadir un recordatorio. Toca mi cabeza para dictarme el título.");
            } else {
                hablarOSimular("Puedes tocar mi cabeza para cambiar el título, la descripción o la hora.");
            }
            runOnUiThread(() -> campoEspera = CampoVozEspera.TITULO);
        }).start();
    }

    private void guardarRecordatorio(Recordatorio existente, String titulo, String desc) {
        if (existente == null) {
            Recordatorio nuevo = new Recordatorio(0, titulo, horaSeleccionada,
                    fechaSeleccionadaMs, desc);
            repo.add(nuevo);
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

        final AlertDialog dialog = new AlertDialog.Builder(this)
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
    // Parser de voz
    // =========================================================================

    /**
     * Convierte texto hablado en minutos totales desde medianoche.
     * Ejemplos: "nueve y media" → 570, "las diez" → 600, "ocho y cuarto" → 495
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

        // Solo hora con "las": "a las 9", "las 10"
        m = java.util.regex.Pattern.compile("(?:las?|a las?)\\s*(\\d{1,2})").matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1)) * 60;

        // Solo dígito suelto: "9", "10"
        m = java.util.regex.Pattern.compile("^(\\d{1,2})$").matcher(texto.trim());
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            if (h >= 0 && h <= 23) return h * 60;
        }

        // Palabras de hora
        String[] nombresHora = {"una","dos","tres","cuatro","cinco","seis",
                "siete","ocho","nueve","diez","once","doce"};
        int horaBase = -1;
        for (int i = 0; i < nombresHora.length; i++) {
            if (texto.contains(nombresHora[i])) { horaBase = i + 1; break; }
        }
        if (horaBase == -1) return null;

        int minutos = 0;
        if (texto.contains("y media"))             minutos = 30;
        else if (texto.contains("y cuarto"))       minutos = 15;
        else if (texto.contains("menos cuarto")) { horaBase++; minutos = 45; }

        return horaBase * 60 + minutos;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void abrirTimePicker(final TextView tv) {
        int h = horaSeleccionada / 60, m = horaSeleccionada % 60;
        new TimePickerDialog(this, android.R.style.Theme_Material_Dialog,
                (view, hh, mm) -> {
                    horaSeleccionada = hh * 60 + mm;
                    actualizarDisplayHora(tv, horaSeleccionada);
                }, h, m, true).show();
    }

    private void abrirDatePicker(final TextView tv) {
        final Calendar cal = Calendar.getInstance();
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}