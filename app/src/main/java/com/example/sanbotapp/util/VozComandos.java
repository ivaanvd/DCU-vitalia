package com.example.sanbotapp.util;

import android.content.Context;

import androidx.annotation.ArrayRes;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detecta comandos de voz a partir de listas de palabras clave definidas en recursos
 * (res/values/arrays_voz.xml y sus traducciones), para que cada idioma tenga las suyas.
 * Compara palabras completas e ignora mayúsculas y tildes ("si" == "sí").
 */
public final class VozComandos {

    private VozComandos() {}

    public static boolean coincide(Context context, String texto, @ArrayRes int palabrasRes) {
        return contieneAlguna(texto, context.getResources().getStringArray(palabrasRes));
    }

    public static boolean contieneAlguna(String texto, String... palabras) {
        if (texto == null) return false;
        String t = normalizar(texto);
        for (String p : palabras) {
            String clave = normalizar(p);
            if (clave.isEmpty()) continue;
            if (Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(clave) + "(?![\\p{L}\\p{N}])")
                    .matcher(t).find()) {
                return true;
            }
        }
        return false;
    }

    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[¿?¡!.,;:\"]", " ").replaceAll("\\s+", " ").trim();
    }
}
