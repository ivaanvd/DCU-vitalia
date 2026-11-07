package com.example.sanbotapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.sanbotapp.actividad.Actividad;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VozParserEnTest {

    private final VozParser parser = VozParser.para(Locale.ENGLISH);

    private static int hm(int h, int m) { return h * 60 + m; }

    @Test
    public void seleccionaParserPorIdioma() {
        assertEquals(VozParserEn.class, VozParser.para(new Locale("en", "AU")).getClass());
        assertEquals(VozParserEs.class, VozParser.para(new Locale("es", "ES")).getClass());
        assertEquals(VozParserEs.class, VozParser.para(Locale.FRENCH).getClass());
    }

    @Test
    public void horas() {
        assertEquals(hm(10, 30), parser.parsearHoraVoz("half past ten"));
        assertEquals(hm(10, 30), parser.parsearHoraVoz("ten thirty"));
        assertEquals(hm(10, 30), parser.parsearHoraVoz("10:30 am"));
        assertEquals(hm(8, 15), parser.parsearHoraVoz("quarter past eight"));
        assertEquals(hm(8, 20), parser.parsearHoraVoz("twenty past eight"));
        assertEquals(hm(10, 5), parser.parsearHoraVoz("ten oh five"));
        assertEquals(hm(17, 0), parser.parsearHoraVoz("5 pm"));
        assertEquals(hm(17, 0), parser.parsearHoraVoz("five p.m."));
        assertEquals(hm(19, 15), parser.parsearHoraVoz("seven fifteen in the evening"));
        assertEquals(hm(4, 45), parser.parsearHoraVoz("quarter to five in the morning"));
        assertEquals(hm(12, 0), parser.parsearHoraVoz("at noon"));
        assertEquals(0, parser.parsearHoraVoz("midnight"));
        assertEquals(hm(9, 0), parser.parsearHoraVoz("nine o'clock"));
        assertEquals(-1, parser.parsearHoraVoz("whenever you like"));
    }

    @Test
    public void horasSinAmPmUsanLaMismaHeuristicaQueEspanol() {
        // 1-6 sin indicar am/pm se interpreta como tarde, igual que VozParserEs
        assertEquals(hm(16, 45), parser.parsearHoraVoz("quarter to five"));
        assertEquals(hm(15, 0), parser.parsearHoraVoz("three"));
    }

    @Test
    public void horaConfirmadaSeVuelveAParsear() {
        // RecordatoriosActivity guarda "HH:mm" y lo re-parsea al confirmar
        assertEquals(hm(17, 0), parser.parsearHoraVoz("17:00"));
        assertEquals(hm(9, 30), parser.parsearHoraVoz("09:30"));
    }

    @Test
    public void dias() {
        assertEquals(Arrays.asList(Calendar.MONDAY, Calendar.THURSDAY),
                parser.parsearDiasVoz("Mondays and Thursdays"));
        assertEquals(7, parser.parsearDiasVoz("every day").size());
        assertEquals(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY), parser.parsearDiasVoz("on weekdays"));
        assertEquals(Arrays.asList(Calendar.SATURDAY, Calendar.SUNDAY), parser.parsearDiasVoz("at the weekend"));
        assertEquals(0, parser.parsearDiasVoz("sometime").size());
    }

    @Test
    public void fechas() {
        assertFecha(Calendar.getInstance().get(Calendar.YEAR), Calendar.OCTOBER, 12,
                parser.parsearFechaVoz("October twelfth"));
        assertFecha(Calendar.getInstance().get(Calendar.YEAR), Calendar.OCTOBER, 12,
                parser.parsearFechaVoz("the 12th of October"));
        assertFecha(2027, Calendar.MARCH, 3, parser.parsearFechaVoz("the 3rd of March 2027"));
        assertFecha(2027, Calendar.JUNE, 21, parser.parsearFechaVoz("twenty first of June twenty twenty seven"));
        assertNull(parser.parsearFechaVoz("next week maybe"));
    }

    @Test
    public void fechaConfirmadaSeVuelveAParsear() {
        // RecordatoriosActivity muestra la fecha como "October 12" (patrón dMMMM en inglés)
        assertFecha(Calendar.getInstance().get(Calendar.YEAR), Calendar.OCTOBER, 12,
                parser.parsearFechaVoz("October 12"));
    }

    @Test
    public void fechasRelativas() {
        Calendar manana = Calendar.getInstance();
        manana.add(Calendar.DAY_OF_MONTH, 1);
        assertFecha(manana.get(Calendar.YEAR), manana.get(Calendar.MONTH), manana.get(Calendar.DAY_OF_MONTH),
                parser.parsearFechaVoz("tomorrow"));
    }

    @Test
    public void numeros() {
        assertEquals(Integer.valueOf(10), parser.parsearNumeroVoz("ten minutes"));
        assertEquals(Integer.valueOf(25), parser.parsearNumeroVoz("twenty five"));
        assertEquals(Integer.valueOf(15), parser.parsearNumeroVoz("15 minutes"));
        assertEquals(Integer.valueOf(30), parser.parsearNumeroVoz("half an hour"));
        assertEquals(Integer.valueOf(60), parser.parsearNumeroVoz("an hour"));
        assertEquals(Integer.valueOf(0), parser.parsearNumeroVoz("none"));
        assertNull(parser.parsearNumeroVoz("a while"));
    }

    @Test
    public void tipos() {
        assertEquals(Actividad.TIPO_MEDICACION, parser.parsearTipoVoz("take my pills"));
        assertEquals(Actividad.TIPO_BEBER_AGUA, parser.parsearTipoVoz("drink some water"));
        assertEquals(Actividad.TIPO_COMER, parser.parsearTipoVoz("lunch"));
        assertEquals(Actividad.TIPO_PASEO_EJERCICIO, parser.parsearTipoVoz("go for a walk"));
        assertEquals(Actividad.TIPO_JUEGOS, parser.parsearTipoVoz("play a game"));
        assertEquals(Actividad.TIPO_ASEO, parser.parsearTipoVoz("have a shower"));
        assertEquals(Actividad.TIPO_LLAMADA_FAMILIAR, parser.parsearTipoVoz("call my daughter"));
        assertEquals(Actividad.TIPO_IR_DORMIR, parser.parsearTipoVoz("time for bed"));
        assertNull(parser.parsearTipoVoz("something else"));
    }

    private static void assertFecha(int anio, int mes, int dia, Long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        List<Integer> esperado = Arrays.asList(anio, mes, dia, 0, 0);
        List<Integer> real = Arrays.asList(c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        assertEquals(esperado, real);
    }
}
