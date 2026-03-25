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
import com.example.sanbotapp.R;

import java.util.Calendar;
import java.util.List;

public class RecordatoriosActivity extends BaseActivity {

    private LinearLayout          containerRecordatorios;
    private TextView              tvVacio;
    private RecordatorioRepository repo;

    // ── Estado del diálogo ────────────────────────────────────────────────────
    private int  horaSeleccionada = 9 * 60;
    private long fechaSeleccionadaMs;

    /*
     * Pre: Inicia la actividad visual para la gestión de recordatorios únicos
     * Post: Configura las vistas iniciales y variables, y pinta los registros futuros
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordatorios);

        containerRecordatorios = findViewById(R.id.containerRecordatorios);
        tvVacio                = findViewById(R.id.tvVacioRecordatorios);
        repo                   = new RecordatorioRepository(this);

        // Fecha por defecto = hoy
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fechaSeleccionadaMs = cal.getTimeInMillis();

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirRecordatorio);
        btnAnadir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoAnadir(null); }
        });

        renderizarLista();
    }

    // ── Renderizado ───────────────────────────────────────────────────────────

    /*
     * Pre: Se ha cargado o modificado un recordatorio
     * Post: Vacía la lista gráfica e inserta exclusivamente los recordatorios marcados en calendario como 'Futuros'
     */
    private void renderizarLista() {
        containerRecordatorios.removeAllViews();
        List<Recordatorio> lista = repo.getFuturos();

        if (lista.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
            for (final Recordatorio r : lista) {
                View item = crearItemRecordatorio(r);
                containerRecordatorios.addView(item);
            }
        }
    }

    /*
     * Pre: El usuario selecciona la fecha y hora final de un recordatorio
     * Post: Controla de forma preventiva que no choque en un lapso de 30m con otra alerta en la misma fecha general
     */
    private boolean haySolapamiento(long fechaMs, int horaMinutos, int idAExcluir) {
        List<Recordatorio> lista = repo.getFuturos();
        for (Recordatorio r : lista) {
            if (r.getId() == idAExcluir) continue;
            if (r.getFechaMs() == fechaMs) {
                int start1 = horaMinutos;
                int end1 = start1 + 30; // Suponemos 30 min para recordatorios
                int start2 = r.getHoraMinutos();
                int end2 = start2 + 30;
                if (start1 < end2 && start2 < end1) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Pre: El proceso renderizarLista recorre los objetos recordatorios
     * Post: Infla el XML creando una etiqueta de recordatorio con hora, fecha y título y los asocia a sus escuchadores
     */
    private View crearItemRecordatorio(final Recordatorio r) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_recordatorio, containerRecordatorios, false);

        ((TextView) item.findViewById(R.id.tvHoraItemRec)).setText(r.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvFechaItemRec)).setText(r.getFechaFormateada());
        ((TextView) item.findViewById(R.id.tvTituloItemRec)).setText(
                r.getTitulo() != null && !r.getTitulo().isEmpty()
                        ? r.getTitulo().toUpperCase() : "SIN TÍTULO");

        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoDetalle(r); }
        });

        View btnEditar   = item.findViewById(R.id.btnEditarItemRec);
        View btnEliminar = item.findViewById(R.id.btnEliminarItemRec);

        btnEditar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoAnadir(r); }
        });
        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmarEliminar(r); }
        });

        return item;
    }

    // ── Diálogo AÑADIR / EDITAR ───────────────────────────────────────────────

    /*
     * Pre: Clic en el botón verde inferior o en editar ítem
     * Post: Abre modal para configuración de Título, Fecha Completa, y Horarios.
     */
    private void mostrarDialogoAnadir(final Recordatorio existente) {
        horaSeleccionada    = (existente != null) ? existente.getHoraMinutos() : 9 * 60;
        fechaSeleccionadaMs = (existente != null) ? existente.getFechaMs() : fechaHoyInicio();

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_recordatorio, null);

        final EditText etTitulo = dv.findViewById(R.id.etTituloRecordatorio);
        final EditText etDesc   = dv.findViewById(R.id.etDescripcionRecordatorio);
        final TextView tvHora   = dv.findViewById(R.id.tvHoraDialogRec);
        final android.view.View tvFechaContainer = dv.findViewById(R.id.tvFechaDialogRec);
        final TextView tvFechaTexto = dv.findViewById(R.id.tvFechaTexto);

        if (existente != null) {
            etTitulo.setText(existente.getTitulo());
            etDesc.setText(existente.getDescripcion());
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);
        actualizarDisplayFecha(tvFechaTexto, fechaSeleccionadaMs);

        tvHora.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { abrirTimePicker(tvHora); }
        });

        tvFechaContainer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { abrirDatePicker(tvFechaTexto); }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCancelarDialogRec).setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { dialog.dismiss(); }
                });

        // X esquina superior derecha también cierra
        dv.findViewById(R.id.btnCerrarDialogRec).setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { dialog.dismiss(); }
                });

        dv.findViewById(R.id.btnGuardarDialogRec).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String titulo = etTitulo.getText().toString().trim();
                String desc   = etDesc.getText().toString().trim();
                if (titulo.isEmpty()) titulo = "Sin título";
                
                final int idExistente = (existente != null) ? existente.getId() : -1;
                final String finalTitulo = titulo;
                final String finalDesc = desc;

                if (haySolapamiento(fechaSeleccionadaMs, horaSeleccionada, idExistente)) {
                    new AlertDialog.Builder(RecordatoriosActivity.this)
                            .setTitle("Solapamiento")
                            .setMessage("Ya hay un recordatorio programado a esa hora el mismo día. ¿Deseas guardarlo de todos modos?")
                            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) {
                                    guardarRecordatorio(existente, finalTitulo, finalDesc);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    guardarRecordatorio(existente, finalTitulo, finalDesc);
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    /*
     * Pre: Supera filtros de solapamiento y validación visual
     * Post: Compila los parámetros y crea/actualiza registro en BD refrescando la UI
     */
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

    // ── Diálogo DETALLE ───────────────────────────────────────────────────────

    /*
     * Pre: Pulsación en un panel recordatorio visual
     * Post: Despliega descripción ampliada, fecha y título en modo sólo lectura
     */
    private void mostrarDialogoDetalle(final Recordatorio r) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_recordatorio, null);

        ((TextView) dv.findViewById(R.id.tvTituloDetRec)).setText(
                r.getTitulo() != null ? r.getTitulo().toUpperCase() : "SIN TÍTULO");
        ((TextView) dv.findViewById(R.id.tvHoraDetRec)).setText(r.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvFechaDetRec)).setText(r.getFechaFormateada());
        String desc = (r.getDescripcion() != null && !r.getDescripcion().isEmpty())
                ? r.getDescripcion().toUpperCase() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetRec)).setText(desc);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCerrarDetRec).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });

        dialog.show();
    }

    // ── Confirmar eliminar ────────────────────────────────────────────────────

    /*
     * Pre: Botón de papelera en recordatorio pulsado
     * Post: Pregunta seguridad de borrado mostrando el título dinámico y procede si afirma
     */
    private void confirmarEliminar(final Recordatorio r) {
        String titulo = (r.getTitulo() != null && !r.getTitulo().isEmpty())
                ? r.getTitulo() : "este recordatorio";
        new AlertDialog.Builder(this)
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Seguro que quieres eliminar \"" + titulo + "\"?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        repo.delete(r.getId());
                        renderizarLista();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void abrirTimePicker(final TextView tv) {
        int h = horaSeleccionada / 60, m = horaSeleccionada % 60;
        new TimePickerDialog(this, android.R.style.Theme_Material_Dialog,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker view, int hh, int mm) {
                        horaSeleccionada = hh * 60 + mm;
                        actualizarDisplayHora(tv, horaSeleccionada);
                    }
                }, h, m, true).show();
    }

    private void abrirDatePicker(final TextView tv) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(fechaSeleccionadaMs);
        new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int year, int month, int day) {
                        Calendar c = Calendar.getInstance();
                        c.set(year, month, day, 0, 0, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        fechaSeleccionadaMs = c.getTimeInMillis();
                        actualizarDisplayFecha(tv, fechaSeleccionadaMs);
                    }
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
}