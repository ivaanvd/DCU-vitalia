package com.example.sanbotapp.recordatorio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Repositorio de Recordatorios (eventos puntuales con fecha y título).
 * Persistencia con SharedPreferences + JSON.
 */
public class RecordatorioRepository {

    private static final String PREFS_NAME  = "RecordatoriosV2Prefs";
    private static final String KEY_LISTA   = "recordatorios";
    private static final String KEY_NEXT_ID = "next_id";

    private final SharedPreferences prefs;

    public RecordatorioRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Leer todo ─────────────────────────────────────────────────────────────

    public List<Recordatorio> getAll() {
        List<Recordatorio> lista = new ArrayList<>();
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

    /** Devuelve solo recordatorios cuya fecha es hoy o en el futuro, ordenados por fecha+hora. */
    public List<Recordatorio> getFuturos() {
        List<Recordatorio> todos    = getAll();
        List<Recordatorio> futuros  = new ArrayList<>();
        for (Recordatorio r : todos) {
            if (r.esFuturoOHoy()) futuros.add(r);
        }
        Collections.sort(futuros, new Comparator<Recordatorio>() {
            @Override public int compare(Recordatorio a, Recordatorio b) {
                int cmpFecha = Long.compare(a.getFechaMs(), b.getFechaMs());
                if (cmpFecha != 0) return cmpFecha;
                return Integer.compare(a.getHoraMinutos(), b.getHoraMinutos());
            }
        });
        return futuros;
    }

    // ── Añadir ────────────────────────────────────────────────────────────────

    public Recordatorio add(Recordatorio r) {
        int nextId = prefs.getInt(KEY_NEXT_ID, 1);
        r.setId(nextId);
        List<Recordatorio> lista = getAll();
        lista.add(r);
        prefs.edit()
                .putString(KEY_LISTA, toJsonArray(lista))
                .putInt(KEY_NEXT_ID, nextId + 1)
                .apply();
        return r;
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    public void update(Recordatorio updated) {
        List<Recordatorio> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == updated.getId()) {
                lista.set(i, updated);
                break;
            }
        }
        prefs.edit().putString(KEY_LISTA, toJsonArray(lista)).apply();
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    public void delete(int id) {
        List<Recordatorio> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == id) {
                lista.remove(i);
                break;
            }
        }
        prefs.edit().putString(KEY_LISTA, toJsonArray(lista)).apply();
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────

    private Recordatorio fromJson(JSONObject obj) throws JSONException {
        Recordatorio r = new Recordatorio();
        r.setId(obj.getInt("id"));
        r.setTitulo(obj.optString("titulo", ""));
        r.setHoraMinutos(obj.getInt("horaMinutos"));
        r.setFechaMs(obj.getLong("fechaMs"));
        r.setDescripcion(obj.optString("descripcion", ""));
        return r;
    }

    private JSONObject toJson(Recordatorio r) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",          r.getId());
            obj.put("titulo",      r.getTitulo());
            obj.put("horaMinutos", r.getHoraMinutos());
            obj.put("fechaMs",     r.getFechaMs());
            obj.put("descripcion", r.getDescripcion());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private String toJsonArray(List<Recordatorio> lista) {
        JSONArray arr = new JSONArray();
        for (Recordatorio r : lista) arr.put(toJson(r));
        return arr.toString();
    }
}
