package com.example.sanbotapp.util;

import com.example.sanbotapp.actividad.Actividad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * English implementation of VozParser
 * (e.g. "half past ten", "quarter to five", "5 pm", "Mondays and Thursdays", "October 12").
 */
public class VozParserEn implements VozParser {

    static final VozParserEn INSTANCE = new VozParserEn();

    private static final Map<String, Integer> UNIDADES = new HashMap<>();
    private static final Map<String, Integer> DECENAS = new HashMap<>();
    private static final Map<String, Integer> ORDINALES = new HashMap<>();
    private static final Map<String, Integer> DECENAS_ORDINALES = new HashMap<>();

    static {
        String[] unidades = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
                "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                "eighteen", "nineteen" };
        for (int i = 0; i < unidades.length; i++) UNIDADES.put(unidades[i], i);
        DECENAS.put("twenty", 20);
        DECENAS.put("thirty", 30);
        DECENAS.put("forty", 40);
        DECENAS.put("fifty", 50);
        DECENAS.put("sixty", 60);

        String[] ordinales = { "", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth",
                "ninth", "tenth", "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth",
                "seventeenth", "eighteenth", "nineteenth" };
        for (int i = 1; i < ordinales.length; i++) ORDINALES.put(ordinales[i], i);
        DECENAS_ORDINALES.put("twentieth", 20);
        DECENAS_ORDINALES.put("thirtieth", 30);
    }

    private static final String[] MESES = { "january", "february", "march", "april", "may", "june", "july",
            "august", "september", "october", "november", "december" };

    private static final String[] DIAS = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
    private static final int[] VALORES_DIA = { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };

    // ── Hora ─────────────────────────────────────────────────────────────────

    @Override
    public int parsearHoraVoz(String texto) {
        if (texto == null) return -1;
        String t = normalizar(texto);

        if (contienePalabra(t, "noon") || contienePalabra(t, "midday")) return 12 * 60;
        if (contienePalabra(t, "midnight")) return 0;

        t = palabrasANumeros(t, false);
        t = t.replaceAll("(\\d{1,2}) oh (\\d)\\b", "$1:0$2");

        boolean esTarde = Pattern.compile("\\d\\s*p\\.?\\s?m\\b").matcher(t).find()
                || contieneAlguna(t, "afternoon", "evening", "night", "tonight");
        boolean esManana = Pattern.compile("\\d\\s*a\\.?\\s?m\\b").matcher(t).find()
                || contienePalabra(t, "morning");

        Matcher m;
        if ((m = Pattern.compile("half past (\\d{1,2})").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(1)), 30, esTarde, esManana);
        }
        if ((m = Pattern.compile("quarter past (\\d{1,2})").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(1)), 15, esTarde, esManana);
        }
        if ((m = Pattern.compile("quarter to (\\d{1,2})").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(1)) - 1, 45, esTarde, esManana);
        }
        if ((m = Pattern.compile("(\\d{1,2}) (?:minutes? )?past (\\d{1,2})").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)), esTarde, esManana);
        }
        if ((m = Pattern.compile("(\\d{1,2}) (?:minutes? )?(?:to|till|before) (\\d{1,2})").matcher(t)).find()) {
            int min = Integer.parseInt(m.group(1));
            if (min >= 1 && min <= 59) {
                return componer(Integer.parseInt(m.group(2)) - 1, 60 - min, esTarde, esManana);
            }
        }
        if ((m = Pattern.compile("(\\d{1,2})\\s*[:.]\\s*(\\d{2})").matcher(t)).find()
                || (m = Pattern.compile("(\\d{1,2}) (\\d{1,2})\\b").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), esTarde, esManana);
        }
        if ((m = Pattern.compile("(\\d{1,2})").matcher(t)).find()) {
            return componer(Integer.parseInt(m.group(1)), 0, esTarde, esManana);
        }
        return -1;
    }

    private static int componer(int h, int m, boolean esTarde, boolean esManana) {
        if (h < 0) h = 23;
        if (h > 23 || m < 0 || m > 59) return -1;
        if (h < 13) h = ajustarTarde(h, esTarde, esManana);
        return h * 60 + m;
    }

    private static int ajustarTarde(int h, boolean esTarde, boolean esManana) {
        int res = (h == 12) ? 0 : h;
        if (esTarde) return (h == 12) ? 12 : res + 12;
        if (esManana) return res;
        if (res >= 1 && res <= 6) return res + 12; // Same heuristic as the Spanish parser
        return h;
    }

    // ── Días ─────────────────────────────────────────────────────────────────

    @Override
    public List<Integer> parsearDiasVoz(String texto) {
        List<Integer> dias = new ArrayList<>();
        if (texto == null) return dias;
        String t = normalizar(texto);

        if (t.contains("every day") || contienePalabra(t, "everyday") || contienePalabra(t, "daily")) {
            for (int v : VALORES_DIA) dias.add(v);
            return dias;
        }
        if (contienePalabra(t, "weekdays")) {
            dias.addAll(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY));
        }
        if (contienePalabra(t, "weekend") || contienePalabra(t, "weekends")) {
            dias.add(Calendar.SATURDAY);
            dias.add(Calendar.SUNDAY);
        }
        for (int i = 0; i < DIAS.length; i++) {
            if ((contienePalabra(t, DIAS[i]) || contienePalabra(t, DIAS[i] + "s")) && !dias.contains(VALORES_DIA[i])) {
                dias.add(VALORES_DIA[i]);
            }
        }
        return dias;
    }

    // ── Fecha ────────────────────────────────────────────────────────────────

    @Override
    public Long parsearFechaVoz(String texto) {
        if (texto == null) return null;
        String t = normalizar(texto);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (contienePalabra(t, "today")) return cal.getTimeInMillis();
        if (contienePalabra(t, "tomorrow")) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            return cal.getTimeInMillis();
        }

        int mes = -1;
        for (int i = 0; i < MESES.length; i++) {
            if (contienePalabra(t, MESES[i])) { mes = i; break; }
        }
        if (mes == -1) return null;

        int anio = cal.get(Calendar.YEAR);
        Matcher mAnio = Pattern.compile("\\b(20\\d{2})\\b").matcher(t);
        if (mAnio.find()) {
            anio = Integer.parseInt(mAnio.group(1));
            t = t.substring(0, mAnio.start()) + t.substring(mAnio.end());
        } else {
            Matcher mAnioPalabras = Pattern.compile("twenty (twenty[- ]?\\w+|thirty)").matcher(t);
            if (mAnioPalabras.find()) {
                String resto = palabrasANumeros(mAnioPalabras.group(1), false).trim();
                if (resto.matches("\\d{2}")) anio = 2000 + Integer.parseInt(resto);
                t = t.substring(0, mAnioPalabras.start()) + t.substring(mAnioPalabras.end());
            }
        }

        t = palabrasANumeros(t, true);
        int dia = -1;
        Matcher mDia = Pattern.compile("\\b(\\d{1,2})(?:st|nd|rd|th)?\\b").matcher(t);
        if (mDia.find()) {
            int d = Integer.parseInt(mDia.group(1));
            if (d >= 1 && d <= 31) dia = d;
        }
        if (dia == -1) return null;

        cal.set(anio, mes, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ── Número ───────────────────────────────────────────────────────────────

    @Override
    public Integer parsearNumeroVoz(String texto) {
        if (texto == null) return null;
        String t = normalizar(texto);
        if (contienePalabra(t, "none") || contienePalabra(t, "no warning") || contienePalabra(t, "nothing")
                || contienePalabra(t, "zero")) {
            return 0;
        }
        if (t.contains("half an hour") || t.contains("half hour")) return 30;
        if (t.contains("an hour") || t.contains("one hour")) return 60;

        t = palabrasANumeros(t, false);
        Matcher m = Pattern.compile("(\\d+)").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    // ── Tipo de actividad ────────────────────────────────────────────────────

    @Override
    public String parsearTipoVoz(String texto) {
        if (texto == null) return null;
        String t = normalizar(texto);

        if (contieneAlguna(t, "medication", "medicine", "medicines", "pill", "pills", "tablet", "tablets"))
            return Actividad.TIPO_MEDICACION;
        if (contieneAlguna(t, "water", "drink", "hydrate", "hydration"))
            return Actividad.TIPO_BEBER_AGUA;
        if (contieneAlguna(t, "eat", "eating", "food", "meal", "lunch", "breakfast", "dinner", "supper"))
            return Actividad.TIPO_COMER;
        if (contieneAlguna(t, "walk", "walking", "exercise", "exercises", "stroll", "sport", "stretch", "stretching"))
            return Actividad.TIPO_PASEO_EJERCICIO;
        if (contieneAlguna(t, "game", "games", "play", "playing", "puzzle", "puzzles"))
            return Actividad.TIPO_JUEGOS;
        if (contieneAlguna(t, "wash", "washing", "shower", "bath", "hygiene", "toilet", "grooming"))
            return Actividad.TIPO_ASEO;
        if (contieneAlguna(t, "call", "phone", "family", "video call", "ring"))
            return Actividad.TIPO_LLAMADA_FAMILIAR;
        if (contieneAlguna(t, "sleep", "bed", "bedtime", "rest", "nap"))
            return Actividad.TIPO_IR_DORMIR;
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String normalizar(String texto) {
        return texto.toLowerCase(java.util.Locale.ROOT).replace('-', ' ').replace(",", " ")
                .replace("o'clock", " ").replaceAll("\\s+", " ").trim();
    }

    private static boolean contieneAlguna(String texto, String... palabras) {
        for (String p : palabras) {
            if (contienePalabra(texto, p)) return true;
        }
        return false;
    }

    private static boolean contienePalabra(String texto, String palabra) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(palabra) + "(?![\\p{L}\\p{N}])")
                .matcher(texto).find();
    }

    /**
     * Replaces spoken numbers ("twenty five", "eleven") with digits.
     * With ordinales=true, also converts "first", "twenty third", etc.
     */
    static String palabrasANumeros(String texto, boolean ordinales) {
        String[] tokens = texto.split(" ");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String tok = tokens[i];
            Integer valor = null;
            if (DECENAS.containsKey(tok)) {
                valor = DECENAS.get(tok);
                if (i + 1 < tokens.length) {
                    String sig = tokens[i + 1];
                    Integer unidad = UNIDADES.get(sig);
                    if (unidad == null && ordinales) unidad = ORDINALES.get(sig);
                    if (unidad != null && unidad >= 1 && unidad <= 9) {
                        valor += unidad;
                        i++;
                    }
                }
            } else if (UNIDADES.containsKey(tok)) {
                valor = UNIDADES.get(tok);
            } else if (ordinales && ORDINALES.containsKey(tok)) {
                valor = ORDINALES.get(tok);
            } else if (ordinales && DECENAS_ORDINALES.containsKey(tok)) {
                valor = DECENAS_ORDINALES.get(tok);
            }
            if (out.length() > 0) out.append(' ');
            out.append(valor != null ? String.valueOf(valor) : tok);
        }
        return out.toString();
    }
}
