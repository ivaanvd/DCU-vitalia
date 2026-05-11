package com.example.sanbotapp.actividad;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.sanbotapp.alarmas.AlarmScheduler;

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
    private final Context context;

    /*
     * Pre: Recibe el contexto de la aplicación
     * Post: Inicializa el acceso a SharedPreferences con la clave del repositorio
     */
    public ActividadRepository(Context context) {
        this.context = context.getApplicationContext();
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
     * Post: Devuelve una lista ordenada cronológicamente de las actividades del día actual
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

    /*
     * Pre: Recibe un ID numérico de actividad
     * Post: Devuelve el objeto Actividad con ese ID, o null si no existe
     */
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

    /*
     * Pre: Recibe una actividad nueva sin ID asignado
     * Post: Asigna ID único, persiste en SharedPreferences y programa su alarma
     */
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
     * Pre: Recibe el ID de la actividad a posponer y los minutos de delay
     * Post: Cancela la alarma original, la borra, y crea una nueva con la hora desplazada
     */
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

        int nuevoId = insert(nueva); // insert ya programa la nueva alarma
        nueva.setId(nuevoId);
        return nueva;
    }

    /*
     * Pre: El usuario completa una actividad que fue pospuesta (creadaPorSistema)
     * Post: Cancela su alarma, borra la copia y marca la original como completada
     */
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

    /*
     * Pre: Se demanda eliminar un registro por su ID
     * Post: Cancela la alarma asociada y elimina el objeto de SharedPreferences
     */
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