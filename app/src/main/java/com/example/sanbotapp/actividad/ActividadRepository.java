package com.example.sanbotapp.actividad;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sanbotapp.alarmas.AlarmScheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Repositorio de Actividades (rutinas semanales).
 * Persistencia con SharedPreferences + JSON.
 */
public class ActividadRepository {

    private static final String PREFS_NAME       = "ActividadesPrefs";
    private static final String KEY_LISTA        = "actividades";
    private static final String KEY_NEXT_ID      = "next_id";
    private static final String KEY_LAST_RESET   = "last_reset_date";

    private final SharedPreferences prefs;
    private final Context context;

    public ActividadRepository(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Leer todo ─────────────────────────────────────────────────────────────

    public List<Actividad> getAll() {
        List<Actividad> lista = new ArrayList<>();
        String json = prefs.getString(KEY_LISTA, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lista.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Devuelve las actividades de hoy, realizando un reset de estados si es un nuevo día.
     */
    public List<Actividad> getDeHoy() {
        verificarYReiniciarEstados();

        List<Actividad> todas = getAll();
        List<Actividad> hoy   = new ArrayList<>();
        for (Actividad a : todas) {
            if (a.coincideHoy()) hoy.add(a);
        }
        Collections.sort(hoy, new Comparator<Actividad>() {
            @Override public int compare(Actividad a, Actividad b) {
                return Integer.compare(a.getHoraMinutos(), b.getHoraMinutos());
            }
        });
        return hoy;
    }

    /**
     * Si la fecha actual es distinta a la del último reset, vuelve todas a PENDIENTE.
     */
    private void verificarYReiniciarEstados() {
        String hoyStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastReset = prefs.getString(KEY_LAST_RESET, "");

        if (!hoyStr.equals(lastReset)) {
            List<Actividad> lista = getAll();
            boolean modificado = false;
            for (Actividad a : lista) {
                if (!Actividad.ESTADO_PENDIENTE.equals(a.getEstado())) {
                    a.setEstado(Actividad.ESTADO_PENDIENTE);
                    modificado = true;
                }
            }
            if (modificado) {
                prefs.edit()
                        .putString(KEY_LISTA, toJsonArray(lista))
                        .putString(KEY_LAST_RESET, hoyStr)
                        .apply();
            } else {
                prefs.edit().putString(KEY_LAST_RESET, hoyStr).apply();
            }
        }
    }

    public Actividad getById(int id) {
        for (Actividad a : getAll()) {
            if (a.getId() == id) return a;
        }
        return null;
    }

    // ── Añadir ────────────────────────────────────────────────────────────────

    public Actividad add(Actividad a) {
        insert(a);
        return a;
    }

    public int insert(Actividad a) {
        int nextId = prefs.getInt(KEY_NEXT_ID, 1);
        a.setId(nextId);
        List<Actividad> lista = getAll();
        lista.add(a);
        prefs.edit()
                .putString(KEY_LISTA, toJsonArray(lista))
                .putInt(KEY_NEXT_ID, nextId + 1)
                .apply();
        AlarmScheduler.programarActividad(context, a);
        return nextId;
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    public void update(Actividad updated) {
        List<Actividad> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == updated.getId()) {
                lista.set(i, updated);
                break;
            }
        }
        prefs.edit().putString(KEY_LISTA, toJsonArray(lista)).apply();
        AlarmScheduler.programarActividad(context, updated);
    }

    // ── Lógica de posponer ───────────────────────────────────────────────────

    public Actividad posponerActividad(int actividadId, int minutosDelay) {
        Actividad original = getById(actividadId);
        if (original == null) return null;

        AlarmScheduler.cancelarActividad(context, actividadId);
        delete(actividadId);

        int nuevaHora = original.getHoraMinutos() + minutosDelay;

        Actividad nueva = new Actividad(0, original.getTipo(), nuevaHora, original.getDescripcion());
        nueva.setDiasSemana(new ArrayList<>(original.getDiasSemana()));
        nueva.setIdActividadOriginal(actividadId);
        nueva.setCreadaPorSistema(true);
        nueva.setEstado(Actividad.ESTADO_PENDIENTE);

        int nuevoId = insert(nueva);
        nueva.setId(nuevoId);
        return nueva;
    }

    public void completarPospuesta(int idCopia) {
        Actividad copia = getById(idCopia);
        if (copia == null || !copia.isCreadaPorSistema()) return;

        int originalId = copia.getIdActividadOriginal();
        AlarmScheduler.cancelarActividad(context, idCopia);
        delete(idCopia);

        Actividad original = getById(originalId);
        if (original != null) {
            original.setEstado(Actividad.ESTADO_COMPLETADA);
            update(original);
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public void delete(int id) {
        AlarmScheduler.cancelarActividad(context, id);
        List<Actividad> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == id) {
                lista.remove(i);
                break;
            }
        }
        prefs.edit().putString(KEY_LISTA, toJsonArray(lista)).apply();
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────

    private Actividad fromJson(JSONObject obj) throws JSONException {
        Actividad a = new Actividad();
        a.setId(obj.getInt("id"));
        a.setTipo(obj.getString("tipo"));
        a.setEstado(obj.optString("estado", Actividad.ESTADO_PENDIENTE));
        a.setHoraMinutos(obj.getInt("horaMinutos"));
        a.setDescripcion(obj.optString("descripcion", ""));
        a.setIdActividadOriginal(obj.optInt("idActividadOriginal", 0));
        a.setCreadaPorSistema(obj.optBoolean("creadaPorSistema", false));

        List<Integer> dias = new ArrayList<>();
        JSONArray diasArr  = obj.optJSONArray("diasSemana");
        if (diasArr != null) {
            for (int i = 0; i < diasArr.length(); i++) dias.add(diasArr.getInt(i));
        }
        a.setDiasSemana(dias);

        return a;
    }

    private JSONObject toJson(Actividad a) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",                  a.getId());
            obj.put("tipo",                a.getTipo());
            obj.put("estado",              a.getEstado());
            obj.put("horaMinutos",         a.getHoraMinutos());
            obj.put("descripcion",         a.getDescripcion());
            obj.put("idActividadOriginal", a.getIdActividadOriginal());
            obj.put("creadaPorSistema",    a.isCreadaPorSistema());

            JSONArray diasArr = new JSONArray();
            if (a.getDiasSemana() != null) {
                for (int dia : a.getDiasSemana()) diasArr.put(dia);
            }
            obj.put("diasSemana", diasArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private String toJsonArray(List<Actividad> lista) {
        JSONArray arr = new JSONArray();
        for (Actividad a : lista) arr.put(toJson(a));
        return arr.toString();
    }
}