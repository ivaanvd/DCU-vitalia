package com.example.sanbotapp.actividad;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.sanbotapp.BaseActivity;
import com.example.sanbotapp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * Pantalla de gestión de actividades recurrentes del robot.
 * Extiende BaseActivity para tener acceso a hablarOSimular()
 * y poder guiar al usuario mayor con voz en cada paso.
 */
public class ActividadesActivity extends BaseActivity {

    private LinearLayout       containerActividades;
    private TextView           tvVacio;
    private ActividadRepository repo;

    // ── Estado del diálogo de añadir/editar ──────────────────────────────────
    private int           horaSeleccionada  = 9 * 60; // 09:00 por defecto
    private List<Integer> diasSeleccionados = new ArrayList<>();

    // Tipos del spinner (por orden de aparición)
    private static final String[] TIPOS = {
            Actividad.TIPO_MEDICACION,
            Actividad.TIPO_BEBER_AGUA,
            Actividad.TIPO_COMER,
            Actividad.TIPO_PASEO_EJERCICIO,
            Actividad.TIPO_JUEGOS,
            Actividad.TIPO_ASEO,
            Actividad.TIPO_LLAMADA_FAMILIAR,
            Actividad.TIPO_IR_DORMIR
    };
    private static final String[] ETIQUETAS_TIPOS = {
            "MEDICACIÓN",
            "BEBER AGUA",
            "COMER",
            "PASEO/EJERCICIO",
            "JUEGOS",
            "ASEO",
            "LLAMADA FAMILIAR",
            "IR A DORMIR"
    };
    private static final int[] VALORES_DIA = { 2, 3, 4, 5, 6, 7, 1 }; // LUN…DOM (Calendar)

    /*
     * Pre: Se lanza la pantalla de gestión de actividades
     * Post: Inicializa la interfaz, el repositorio, y renderiza la lista guardada
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actividades);
        setupTopBackBanner("Actividades");

        containerActividades = findViewById(R.id.containerActividades);
        tvVacio              = findViewById(R.id.tvVacioActividades);
        repo                 = new ActividadRepository(this);

        LinearLayout btnAnadir = findViewById(R.id.btnAnadirActividad);
        btnAnadir.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoAnadir(null); }
        });

        renderizarLista();
    }

    // ── Renderizado ───────────────────────────────────────────────────────────

    /*
     * Pre: La base de datos puede haber cambiado (inserción, actualización o borrado)
     * Post: Limpia la vista, obtiene todas las actividades, las ordena
     *       cronológicamente y las dibuja en pantalla
     */
    private void renderizarLista() {
        containerActividades.removeAllViews();
        List<Actividad> lista = repo.getAll();

        Collections.sort(lista, new Comparator<Actividad>() {
            @Override public int compare(Actividad a, Actividad b) {
                return Integer.compare(a.getHoraMinutos(), b.getHoraMinutos());
            }
        });

        if (lista.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
            for (final Actividad a : lista) {
                View item = crearItemActividad(a);
                containerActividades.addView(item);
            }
        }
    }

    /*
     * Pre: Recibe dos listas de enteros representando días de la semana
     * Post: Devuelve true si existe intersección entre ambas listas, false si no
     */
    private boolean tienenDiaComun(List<Integer> dias1, List<Integer> dias2) {
        if (dias1 == null || dias1.isEmpty()) return true;
        if (dias2 == null || dias2.isEmpty()) return true;
        for (Integer d : dias1) {
            if (dias2.contains(d)) return true;
        }
        return false;
    }

    /*
     * Pre: Recibe los días y la hora planeada para una nueva actividad o modificación
     * Post: Devuelve true si choca con el horario de otra actividad existente en el mismo día
     */
    private boolean haySolapamiento(List<Integer> dias, int horaMinutos, int duracion, int idAExcluir) {
        List<Actividad> lista = repo.getAll();
        for (Actividad a : lista) {
            if (a.getId() == idAExcluir) continue;

            if (tienenDiaComun(dias, a.getDiasSemana())) {
                int start1 = horaMinutos;
                int end1   = start1 + duracion;
                int start2 = a.getHoraMinutos();
                int end2   = start2 + a.getDuracionMinutos();

                if (start1 < end2 && start2 < end1) return true;
            }
        }
        return false;
    }

    /*
     * Pre: Se iteran las actividades desde la base de datos
     * Post: Devuelve un elemento visual View (tarjeta) relleno con la info
     *       de la actividad y sus listeners de clics
     */
    private View crearItemActividad(final Actividad a) {
        View item = LayoutInflater.from(this)
                .inflate(R.layout.item_actividad, containerActividades, false);

        ((TextView) item.findViewById(R.id.tvHoraItem)).setText(a.getHoraFormateada());
        ((TextView) item.findViewById(R.id.tvTipoItem)).setText(a.getTipoLabel());

        // Fondo con color del tipo
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor(a.getColorHex()));
        bg.setCornerRadius(dpToPx(14));
        item.setBackground(bg);

        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoDetalle(a); }
        });

        item.findViewById(R.id.btnEditarItem).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { mostrarDialogoAnadir(a); }
        });

        item.findViewById(R.id.btnEliminarItem).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { confirmarEliminar(a); }
        });

        return item;
    }

    // ── Diálogo AÑADIR / EDITAR ───────────────────────────────────────────────

    /*
     * Pre: El usuario pulsa el botón de añadir genérico, o editar en una tarjeta
     * Post: Muestra un diálogo flotante configurado en modo Creación o Edición
     *       temporal cargando los datos. El robot guía al usuario por voz
     *       explicando qué tiene que hacer en este primer paso.
     */
    private void mostrarDialogoAnadir(final Actividad existente) {
        horaSeleccionada  = (existente != null) ? existente.getHoraMinutos() : 9 * 60;
        diasSeleccionados = (existente != null && existente.getDiasSemana() != null)
                ? new ArrayList<>(existente.getDiasSemana())
                : new ArrayList<>();

        // ── VOZ: el robot explica el paso 1 al abrir el diálogo ──────────────
        if (existente == null) {
            hablarOSimular("Vamos a añadir una actividad. Elige el tipo y la hora, y pulsa Añadir.");
        } else {
            hablarOSimular("Aquí puedes modificar la actividad. Cambia lo que necesites y pulsa Guardar.");
        }

        final View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_anadir_actividad, null);

        final TextView tvHora   = dv.findViewById(R.id.tvHoraDialogActividad);
        final EditText etDesc   = dv.findViewById(R.id.etDescripcionActividad);
        final Spinner  spinner  = dv.findViewById(R.id.spinnerTipoActividad);
        final TextView tvTitulo = dv.findViewById(R.id.tvTituloDialogActividad);

        tvTitulo.setText(existente != null ? "EDITAR ACTIVIDAD" : "AÑADIR ACTIVIDAD");

        Button btnGuardar = dv.findViewById(R.id.btnGuardarDialogActividad);
        btnGuardar.setText(existente != null ? "✓  GUARDAR" : "✓  AÑADIR");

        // Spinner de tipos
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ETIQUETAS_TIPOS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (existente != null) {
            etDesc.setText(existente.getDescripcion());
            for (int i = 0; i < TIPOS.length; i++) {
                if (TIPOS[i].equals(existente.getTipo())) { spinner.setSelection(i); break; }
            }
        }

        actualizarDisplayHora(tvHora, horaSeleccionada);

        // Botones de días de la semana
        final TextView[] btnsDia = {
                dv.findViewById(R.id.btnDiaLun),
                dv.findViewById(R.id.btnDiaMar),
                dv.findViewById(R.id.btnDiaMie),
                dv.findViewById(R.id.btnDiaJue),
                dv.findViewById(R.id.btnDiaVie),
                dv.findViewById(R.id.btnDiaSab),
                dv.findViewById(R.id.btnDiaDom)
        };
        actualizarBotonesDia(btnsDia, VALORES_DIA, diasSeleccionados);

        for (int i = 0; i < btnsDia.length; i++) {
            final int dia = VALORES_DIA[i];
            btnsDia[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (diasSeleccionados.contains(dia)) diasSeleccionados.remove(Integer.valueOf(dia));
                    else diasSeleccionados.add(dia);
                    actualizarBotonesDia(btnsDia, VALORES_DIA, diasSeleccionados);
                }
            });
        }

        // ── VOZ: el robot avisa al usuario cuando va a elegir la hora ────────
        tvHora.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hablarOSimular("Elige la hora a la que quieres programar la actividad.");
                abrirTimePicker(tvHora);
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCancelarDialogActividad).setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { dialog.dismiss(); }
                });

        dv.findViewById(R.id.btnCancelarDialogActividad2).setOnClickListener(
                new View.OnClickListener() {
                    @Override public void onClick(View v) { dialog.dismiss(); }
                });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String desc = etDesc.getText().toString().trim();
                String tipo = TIPOS[spinner.getSelectedItemPosition()];

                Actividad temp   = new Actividad(0, tipo, horaSeleccionada, "");
                int       duracion    = temp.getDuracionMinutos();
                final int idExistente = (existente != null) ? existente.getId() : -1;
                final String finalDesc = desc;
                final String finalTipo = tipo;

                if (haySolapamiento(diasSeleccionados, horaSeleccionada, duracion, idExistente)) {
                    // ── VOZ: avisa del solapamiento antes de mostrar el diálogo ──
                    hablarOSimular("Atención. Ya tienes algo programado a esa hora. ¿Quieres guardarlo igualmente?");

                    new AlertDialog.Builder(ActividadesActivity.this)
                            .setTitle("Solapamiento")
                            .setMessage("Ya hay una actividad programada que se solapa en el tiempo para alguno de los días seleccionados. ¿Deseas guardarla de todos modos?")
                            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) {
                                    guardarActividad(existente, finalTipo, finalDesc);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                } else {
                    guardarActividad(existente, finalTipo, finalDesc);
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    /*
     * Pre: El usuario confirma el diálogo de edición/creación validando los datos
     * Post: Construye o actualiza el objeto Actividad, impacta en BD,
     *       vuelve a renderizar la lista y el robot confirma por voz la acción realizada.
     */
    private void guardarActividad(Actividad existente, String tipo, String desc) {
        if (existente == null) {
            // ── VOZ: confirma que la nueva actividad ha sido añadida ──────────
            hablarOSimular("¡Listo! He añadido la actividad a tu agenda.");

            Actividad nueva = new Actividad(0, tipo, horaSeleccionada, desc);
            nueva.setDiasSemana(new ArrayList<>(diasSeleccionados));
            repo.add(nueva);
        } else {
            // ── VOZ: confirma que los cambios han sido guardados ──────────────
            hablarOSimular("Perfecto. He guardado los cambios.");

            existente.setTipo(tipo);
            existente.setHoraMinutos(horaSeleccionada);
            existente.setDescripcion(desc);
            existente.setDiasSemana(new ArrayList<>(diasSeleccionados));
            repo.update(existente);
        }
        renderizarLista();
    }

    // ── Diálogo DETALLE ───────────────────────────────────────────────────────

    /*
     * Pre: El usuario hace clic principal sobre la tarjeta de la actividad
     * Post: Abre un diálogo de sólo lectura resumiendo visualmente días
     *       y descripción de la tarea
     */
    private void mostrarDialogoDetalle(final Actividad a) {
        View dv = LayoutInflater.from(this)
                .inflate(R.layout.dialog_detalle_actividad, null);

        GradientDrawable bgCirculo = new GradientDrawable();
        bgCirculo.setShape(GradientDrawable.OVAL);
        bgCirculo.setColor(Color.parseColor(a.getColorHex()));
        dv.findViewById(R.id.frameEmojiDetAct).setBackground(bgCirculo);
        ((android.widget.ImageView) dv.findViewById(R.id.tvEmojiDetAct))
                .setImageResource(getIconoParaTipo(a.getTipo()));

        ((TextView) dv.findViewById(R.id.tvHoraDetAct)).setText(a.getHoraFormateada());
        ((TextView) dv.findViewById(R.id.tvTipoDetAct)).setText(a.getTipoLabel());

        String desc = (a.getDescripcion() != null && !a.getDescripcion().isEmpty())
                ? a.getDescripcion().toUpperCase() : "—";
        ((TextView) dv.findViewById(R.id.tvDescDetAct)).setText(desc);

        int[] idsDias = {
                R.id.detDiaLun, R.id.detDiaMar, R.id.detDiaMie, R.id.detDiaJue,
                R.id.detDiaVie, R.id.detDiaSab, R.id.detDiaDom
        };
        List<Integer> dias = a.getDiasSemana();
        for (int i = 0; i < idsDias.length; i++) {
            TextView tv = dv.findViewById(idsDias[i]);
            boolean activo = dias != null && dias.contains(VALORES_DIA[i]);
            if (activo) {
                GradientDrawable bgDia = new GradientDrawable();
                bgDia.setShape(GradientDrawable.OVAL);
                bgDia.setColor(Color.parseColor("#1C1C1E"));
                tv.setBackground(bgDia);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setBackgroundResource(R.drawable.bg_tipo_normal);
                tv.setTextColor(Color.parseColor("#1C1C1E"));
            }
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dv).setCancelable(true).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dv.findViewById(R.id.btnCerrarDetAct).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dialog.dismiss(); }
        });

        dialog.show();
    }

    // ── Eliminar con confirmación ─────────────────────────────────────────────

    /*
     * Pre: El usuario pulsa el icono de papelera sobre una actividad de la lista
     * Post: Lanza un diálogo de confirmación y, de ser aceptado, elimina el
     *       registro de BD y el robot confirma la eliminación por voz.
     */
    private void confirmarEliminar(final Actividad a) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Seguro que quieres eliminar \"" + a.getTipoLabel() + "\"?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        // ── VOZ: confirma la eliminación al usuario ───────────
                        hablarOSimular("He eliminado la actividad.");
                        repo.delete(a.getId());
                        renderizarLista();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /*
     * Pre: Se recibe un TextView que muestra la hora y los valores actuales
     * Post: Abre el selector de hora nativo de Android y actualiza horaSeleccionada
     *       y el TextView al confirmar
     */
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

    /*
     * Pre: Recibe un TextView y un valor en minutos totales
     * Post: Actualiza el texto del TextView con formato HH:MM
     */
    private void actualizarDisplayHora(TextView tv, int minutos) {
        tv.setText(String.format("%02d:%02d", minutos / 60, minutos % 60));
    }

    /*
     * Pre: Recibe los botones de día, sus valores y la lista de días seleccionados
     * Post: Actualiza el fondo de cada botón para reflejar si está activo o no
     */
    private void actualizarBotonesDia(TextView[] botones, int[] valores, List<Integer> sel) {
        for (int i = 0; i < botones.length; i++) {
            botones[i].setBackgroundResource(sel.contains(valores[i])
                    ? R.drawable.bg_tipo_selected : R.drawable.bg_tipo_normal);
        }
    }

    /*
     * Pre: Recibe un string con el tipo de actividad
     * Post: Devuelve el recurso drawable correspondiente al icono del tipo
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

    /*
     * Pre: Recibe un valor en dp
     * Post: Devuelve el equivalente en píxeles según la densidad de pantalla
     */
    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
