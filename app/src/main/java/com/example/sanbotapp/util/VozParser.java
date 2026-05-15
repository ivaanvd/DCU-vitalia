package com.example.sanbotapp.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase de utilidad para procesar texto dictado por voz y convertirlo en datos estructurados.
 * Extraído de las Activities para cumplir con el principio de Responsabilidad Única.
 */
public class VozParser {

    /**
     * Parsea una hora dictada por voz (ej. "las diez y media", "las cinco de la tarde").
     * @return Minutos totales desde el inicio del día, o -1 si no se reconoce.
     */
    public static int parsearHoraVoz(String texto) {
        if (texto == null) return -1;
        texto = texto.toLowerCase().trim();
        boolean esTarde = texto.contains("tarde") || texto.contains("noche") || texto.contains("pm");
        boolean esMañana = texto.contains("mañana") || texto.contains("am");

        // 1. Reemplazo de palabras comunes por números
        texto = texto.replace("media", "30").replace("cuarto", "15");

        // Mapa de palabras a números
        String[] palabras = { "cero", "una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
                "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis", 
                "veintisiete", "veintiocho", "veintinueve", "treinta", "cuarenta", "cincuenta" };
        int[] valores = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                26, 27, 28, 29, 30, 40, 50 };

        // Reemplazar palabras por números en el texto para facilitar regex
        for (int i = palabras.length - 1; i >= 0; i--) {
            texto = texto.replace(palabras[i], String.valueOf(valores[i]));
        }

        int h = -1, m = -1;

        // Regex para "H menos M"
        Matcher matMenos = Pattern.compile("(\\d{1,2})\\s*menos\\s*(\\d{1,2})").matcher(texto);
        if (matMenos.find()) {
            h = Integer.parseInt(matMenos.group(1));
            m = Integer.parseInt(matMenos.group(2));
            h--;
            if (h < 0) h = 23;
            m = 60 - m;
            return ajustarTarde(h, esTarde, esMañana) * 60 + m;
        }

        // Regex para "H y M" o "H M" o "H:M"
        Matcher matY = Pattern.compile("(\\d{1,2})\\s*(?:y|:|\\s)\\s*(\\d{1,2})").matcher(texto);
        if (matY.find()) {
            h = Integer.parseInt(matY.group(1));
            m = Integer.parseInt(matY.group(2));
        } else {
            // Solo hora
            Matcher matH = Pattern.compile("(\\d{1,2})").matcher(texto);
            if (matH.find()) {
                h = Integer.parseInt(matH.group(1));
                m = 0;
            }
        }

        if (h != -1 && h <= 23 && m <= 59) {
            if (h < 13) h = ajustarTarde(h, esTarde, esMañana);
            return h * 60 + m;
        }

        return -1;
    }

    private static int ajustarTarde(int h, boolean esTarde, boolean esMañana) {
        int res = (h == 12) ? 0 : h;
        if (esTarde) return (h == 12) ? 12 : res + 12;
        if (esMañana) return res;
        if (res >= 1 && res <= 6) return res + 12; // Heurística
        return h;
    }

    /**
     * Parsea días de la semana mencionados en un texto.
     * @return Lista de enteros (Calendar.MONDAY, etc.)
     */
    public static List<Integer> parsearDiasVoz(String t) {
        if (t == null) return new ArrayList<>();
        t = t.toLowerCase();
        List<Integer> d = new ArrayList<>();
        String[] n = { "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo" };
        String[] a = { "lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo" };
        int[] v = { 2, 3, 4, 5, 6, 7, 1 }; // VALORES_DIA standard
        for (int i = 0; i < n.length; i++)
            if (t.contains(n[i]) || t.contains(a[i]))
                d.add(v[i]);
        return d;
    }

    /**
     * Parsea una fecha dictada por voz (ej. "quince de marzo").
     * @return Timestamp en ms de la fecha a las 00:00, o null si falla.
     */
    public static Long parsearFechaVoz(String texto) {
        if (texto == null) return null;
        texto = texto.toLowerCase().trim();

        // 1. Mes
        final String[] MESES = { "enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre" };
        int mes = -1;
        for (int i = 0; i < MESES.length; i++) {
            if (texto.contains(MESES[i])) { mes = i + 1; break; }
        }
        if (mes == -1) return null;

        // 2. Día
        final String[] NUMEROS = { "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve", 
                "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis", 
                "veintisiete", "veintiocho", "veintinueve", "treinta", "treinta y uno" };
        final String[] NUMEROS_ALT = { "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve", 
                "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro", "veinticinco", "veintiseis", 
                "veintisiete", "veintiocho", "veintinueve", "treinta", "treinta y uno" };

        int dia = -1;
        Matcher m = Pattern.compile("\\b(\\d{1,2})\\b").matcher(texto);
        if (m.find()) {
            int c = Integer.parseInt(m.group(1));
            if (c >= 1 && c <= 31) dia = c;
        }
        if (dia == -1) {
            for (int i = NUMEROS.length - 1; i >= 0; i--) {
                if (texto.contains(NUMEROS[i]) || texto.contains(NUMEROS_ALT[i])) {
                    dia = i + 1; break;
                }
            }
        }
        if (dia == -1) return null;

        // 3. Año
        int anio = Calendar.getInstance().get(Calendar.YEAR);
        m = Pattern.compile("\\b(20\\d{2})\\b").matcher(texto);
        if (m.find()) {
            anio = Integer.parseInt(m.group(1));
        } else {
            // Simplificado para este ejemplo, se puede ampliar
            if (texto.contains("dos mil veinticinco")) anio = 2025;
            else if (texto.contains("dos mil veintiseis") || texto.contains("dos mil veintiséis")) anio = 2026;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes - 1, dia, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Parsea un número hablado (0-60).
     */
    public static Integer parsearNumeroVoz(String texto) {
        if (texto == null) return null;
        texto = texto.toLowerCase().trim();
        if (texto.contains("cero") || texto.contains("ningun")) return 0;

        String[] palabras = {"una", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta"};
        int[] valores = {1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 30, 40, 50, 60};

        for (int i = 0; i < palabras.length; i++) {
            if (texto.contains(palabras[i])) return valores[i];
        }

        Matcher m = Pattern.compile("(\\d+)").matcher(texto);
        if (m.find()) return Integer.parseInt(m.group(1));

        return null;
    }

    /**
     * Parsea un tipo de actividad desde voz.
     */
    public static String parsearTipoVoz(String texto) {
        if (texto == null) return null;
        texto = texto.toLowerCase();
        
        // Mapeo de palabras clave a constantes de Actividad
        if (contieneAlguna(texto, "medicación", "medicacion", "medicina", "pastilla"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_MEDICACION;
        if (contieneAlguna(texto, "agua", "beber", "hidratación"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_BEBER_AGUA;
        if (contieneAlguna(texto, "comer", "comida", "almuerzo", "desayuno", "cena"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_COMER;
        if (contieneAlguna(texto, "paseo", "ejercicio", "andar", "caminar", "deporte"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_PASEO_EJERCICIO;
        if (contieneAlguna(texto, "juego", "jugar", "juegos", "entretenimiento"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_JUEGOS;
        if (contieneAlguna(texto, "aseo", "ducha", "baño", "higiene", "lavarse"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_ASEO;
        if (contieneAlguna(texto, "llamada", "familiar", "teléfono", "telefono", "familia", "videollamada"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_LLAMADA_FAMILIAR;
        if (contieneAlguna(texto, "dormir", "cama", "descansar", "noche", "siesta"))
            return com.example.sanbotapp.actividad.Actividad.TIPO_IR_DORMIR;
            
        return null;
    }

    private static boolean contieneAlguna(String texto, String... palabras) {
        for (String p : palabras) {
            if (texto.contains(p)) return true;
        }
        return false;
    }

}
