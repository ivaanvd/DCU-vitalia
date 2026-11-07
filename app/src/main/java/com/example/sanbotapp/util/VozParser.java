package com.example.sanbotapp.util;

import android.content.Context;

import androidx.core.os.ConfigurationCompat;

import java.util.List;
import java.util.Locale;

/**
 * Convierte texto dictado por voz en datos estructurados (hora, días, fecha, número, tipo).
 * Cada idioma tiene su implementación; se elige según el idioma de los recursos de la app.
 */
public interface VozParser {

    /** @return Minutos desde medianoche, o -1 si no se reconoce. */
    int parsearHoraVoz(String texto);

    /** @return Días mencionados como constantes de Calendar (MONDAY=2 … SUNDAY=1). */
    List<Integer> parsearDiasVoz(String texto);

    /** @return Timestamp en ms de la fecha a las 00:00, o null si no se reconoce. */
    Long parsearFechaVoz(String texto);

    /** @return Número hablado (0-60), o null si no se reconoce. */
    Integer parsearNumeroVoz(String texto);

    /** @return Constante Actividad.TIPO_*, o null si no se reconoce. */
    String parsearTipoVoz(String texto);

    static VozParser para(Context context) {
        Locale locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
        return para(locale);
    }

    static VozParser para(Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage())) {
            return VozParserEn.INSTANCE;
        }
        return VozParserEs.INSTANCE;
    }
}
