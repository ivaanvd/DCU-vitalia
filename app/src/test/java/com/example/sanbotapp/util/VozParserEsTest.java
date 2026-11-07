package com.example.sanbotapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.sanbotapp.actividad.Actividad;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/** Fija el comportamiento existente del parser en español. */
public class VozParserEsTest {

    private final VozParser parser = VozParser.para(new Locale("es", "ES"));

    private static int hm(int h, int m) { return h * 60 + m; }

    @Test
    public void horas() {
        assertEquals(hm(10, 30), parser.parsearHoraVoz("las diez y media"));
        assertEquals(hm(17, 0), parser.parsearHoraVoz("las cinco de la tarde"));
        assertEquals(hm(10, 45), parser.parsearHoraVoz("las once menos cuarto"));
        assertEquals(hm(9, 30), parser.parsearHoraVoz("09:30"));
        assertEquals(-1, parser.parsearHoraVoz("cuando quieras"));
    }

    @Test
    public void dias() {
        assertEquals(Arrays.asList(Calendar.MONDAY, Calendar.THURSDAY), parser.parsearDiasVoz("los lunes y jueves"));
        assertEquals(Arrays.asList(Calendar.WEDNESDAY, Calendar.SATURDAY), parser.parsearDiasVoz("miércoles y sabado"));
    }

    @Test
    public void fechas() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(parser.parsearFechaVoz("doce de octubre"));
        assertEquals(Calendar.OCTOBER, c.get(Calendar.MONTH));
        assertEquals(12, c.get(Calendar.DAY_OF_MONTH));
        assertNull(parser.parsearFechaVoz("la semana que viene"));
    }

    @Test
    public void numeros() {
        assertEquals(Integer.valueOf(10), parser.parsearNumeroVoz("diez minutos"));
        assertEquals(Integer.valueOf(0), parser.parsearNumeroVoz("ninguno"));
        assertEquals(Integer.valueOf(15), parser.parsearNumeroVoz("15 minutos"));
    }

    @Test
    public void tipos() {
        assertEquals(Actividad.TIPO_MEDICACION, parser.parsearTipoVoz("tomar la medicación"));
        assertEquals(Actividad.TIPO_PASEO_EJERCICIO, parser.parsearTipoVoz("dar un paseo"));
        assertEquals(Actividad.TIPO_LLAMADA_FAMILIAR, parser.parsearTipoVoz("llamar a la familia"));
        assertNull(parser.parsearTipoVoz("otra cosa"));
    }
}
