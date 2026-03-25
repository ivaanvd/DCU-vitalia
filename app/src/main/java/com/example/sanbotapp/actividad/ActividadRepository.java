package com.example.sanbotapp.actividad;

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
 * Repositorio de Actividades (rutinas semanales).
 * Persistencia con SharedPreferences + JSON.
 */
public class ActividadRepository {

    private static final String PREFS_NAME  = "ActividadesPrefs";
    private static final String KEY_LISTA   = "actividades";
    private static final String KEY_NEXT_ID = "next_id";

    private final SharedPreferences prefs;

    /*
     * Pre: Recibe el contexto de la aplicación
     * Post: Inicializa el acceso a SharedPreferences con la clave del repositorio
     */
    public ActividadRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ── Leer todo ─────────────────────────────────────────────────────────────

    /*
     * Pre: Existen datos almacenados (o vacío) en formato JSON en SharedPreferences
     * Post: Devuelve una lista de objetos Actividad parseados
     */
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

    /*
     * Pre: Existen actividades almacenadas en la base de datos
     * Post: Devuelve una lista ordenada cronológicamente de las actividades asignadas para el día de la semana actual
     */
    public List<Actividad> getDeHoy() {
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

    // ── Añadir ────────────────────────────────────────────────────────────────

    /*
     * Pre: Recibe una actividad nueva sin ID
     * Post: Asigna ID incremental, guarda en JSON y devuelve la actividad resultante
     */
    public Actividad add(Actividad a) {
        int nextId = prefs.getInt(KEY_NEXT_ID, 1);
        a.setId(nextId);
        List<Actividad> lista = getAll();
        lista.add(a);
        prefs.edit()
                .putString(KEY_LISTA, toJsonArray(lista))
                .putInt(KEY_NEXT_ID, nextId + 1)
                .apply();
        return a;
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    /*
     * Pre: Recibe una actividad modificada que ya existe en BD mediante su ID
     * Post: Actualiza el objeto en la lista y guarda sobreescribiendo el JSON
     */
    public void update(Actividad updated) {
        List<Actividad> lista = getAll();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).getId() == updated.getId()) {
                lista.set(i, updated);
                break;
            }
        }
        prefs.edit().putString(KEY_LISTA, toJsonArray(lista)).apply();
    }

    // ── Lógica de posponer ───────────────────────────────────────────────────

    /*
     * Pre: El usuario decide posponer la alerta minutos después
     * Post: Cambia el estado del original a pospuesta y crea una nueva tarea programada por el sistema
     */
    public Actividad addCopiaPostpuesta(Actividad original, int minutosDelay) {
        // Marcamos la original como POSPUESTA
        original.setEstado(Actividad.ESTADO_POSPUESTA);
        update(original);

        // Creamos la nueva
        Actividad copia = new Actividad(0, original.getTipo(), original.getHoraMinutos() + minutosDelay, original.getDescripcion());
        copia.setEstado(Actividad.ESTADO_PENDIENTE);
        copia.setDiasSemana(new ArrayList<>(original.getDiasSemana()));
        copia.setIdActividadOriginal(original.getId());
        copia.setCreadaPorSistema(true);
        
        return add(copia);
    }

    /*
     * Pre: El usuario presiona el botón completar en una actividad clonada como 'pospuesta'
     * Post: Elimina la actividad temporal y marca el registro original subyacente como completado
     */
    public void completarPospuesta(int idCopia) {
        List<Actividad> lista = getAll();
        Actividad copia = null;
        for (Actividad a : lista) {
            if (a.getId() == idCopia) {
                copia = a;
                break;
            }
        }

        if (copia != null && copia.isCreadaPorSistema()) {
            int originalId = copia.getIdActividadOriginal();
            // Borramos la copia
            delete(idCopia);
            
            // Buscamos y completamos la original
            List<Actividad> updatedLista = getAll(); // refetch in case it changed
            for (Actividad a : updatedLista) {
                if (a.getId() == originalId) {
                    a.setEstado(Actividad.ESTADO_COMPLETADA);
                    update(a);
                    break;
                }
            }
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    /*
     * Pre: Se demanda eliminar un registro por su ID
     * Post: Busca y elimina el objeto de la lista serializada de SharedPreferences
     */
    public void delete(int id) {
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
