package com.example.sanbotapp.juegos;

import android.content.Context;
import android.util.Log;

import androidx.annotation.RawRes;

import com.example.sanbotapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga el contenido de los juegos desde res/raw (con variantes por idioma, ej. res/raw-en).
 */
public final class ContenidoJuegos {

    private static final String TAG = "ContenidoJuegos";

    private ContenidoJuegos() {}

    public static List<String> temasRosco(Context context) {
        List<String> temas = new ArrayList<>();
        try {
            JSONArray arr = leer(context, R.raw.juego_rosco).getJSONArray("temas");
            for (int i = 0; i < arr.length(); i++) {
                temas.add(arr.getJSONObject(i).getString("nombre"));
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "No se pudieron leer las temáticas del rosco", e);
        }
        return temas;
    }

    public static List<PreguntaRosco> preguntasRosco(Context context) {
        List<PreguntaRosco> preguntas = new ArrayList<>();
        try {
            JSONArray temas = leer(context, R.raw.juego_rosco).getJSONArray("temas");
            for (int i = 0; i < temas.length(); i++) {
                JSONObject tema = temas.getJSONObject(i);
                String nombre = tema.getString("nombre");
                JSONArray arr = tema.getJSONArray("preguntas");
                for (int j = 0; j < arr.length(); j++) {
                    JSONObject p = arr.getJSONObject(j);
                    JSONArray opciones = p.getJSONArray("opciones");
                    preguntas.add(new PreguntaRosco(nombre, p.getString("letra"), p.getString("pista"),
                            opciones.getString(0), opciones.getString(1), p.getInt("correcta")));
                }
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "No se pudieron leer las preguntas del rosco", e);
        }
        return preguntas;
    }

    public static List<PreguntaRefran> preguntasRefranes(Context context) {
        List<PreguntaRefran> preguntas = new ArrayList<>();
        try {
            JSONArray arr = leer(context, R.raw.juego_refranes).getJSONArray("preguntas");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                JSONArray opciones = p.getJSONArray("opciones");
                preguntas.add(new PreguntaRefran(p.getString("texto"),
                        opciones.getString(0), opciones.getString(1), p.getInt("correcta")));
            }
        } catch (JSONException | IOException e) {
            Log.e(TAG, "No se pudieron leer los refranes", e);
        }
        return preguntas;
    }

    private static JSONObject leer(Context context, @RawRes int res) throws IOException, JSONException {
        try (InputStream in = context.getResources().openRawResource(res);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
