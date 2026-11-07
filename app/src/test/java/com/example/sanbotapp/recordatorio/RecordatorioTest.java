package com.example.sanbotapp.recordatorio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class RecordatorioTest {

    private static long ms(int anio, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, hora, minuto);
        return c.getTimeInMillis();
    }

    @Test
    public void nuevoEmpiezaEnLaProximaHoraEnPunto() {
        Recordatorio r = Recordatorio.nuevoPorDefecto(ms(2026, 11, 7, 16, 17));
        assertEquals(17 * 60, r.getHoraMinutos());
        assertEquals(ms(2026, 11, 7, 0, 0), r.getFechaMs());
        assertFalse(r.yaHaPasado(ms(2026, 11, 7, 16, 17)));
    }

    @Test
    public void nuevoPasadaLasOnceDeLaNocheCaeMañana() {
        Recordatorio r = Recordatorio.nuevoPorDefecto(ms(2026, 11, 7, 23, 30));
        assertEquals(0, r.getHoraMinutos());
        assertEquals(ms(2026, 11, 8, 0, 0), r.getFechaMs());
    }

    @Test
    public void yaHaPasadoUsaDiaYHora() {
        Recordatorio r = new Recordatorio(1, "Médico", 9 * 60, ms(2026, 11, 7, 16, 11), "", 10);
        assertTrue(r.yaHaPasado(ms(2026, 11, 7, 16, 17)));
        assertFalse(r.yaHaPasado(ms(2026, 11, 7, 8, 59)));
    }

    @Test
    public void unRecordatorioDeHoySigueEnLaLista() {
        Recordatorio hoyTemprano = new Recordatorio(1, "Test", 0, System.currentTimeMillis(), "", 10);
        assertTrue(hoyTemprano.esFuturoOHoy());
        Recordatorio ayer = new Recordatorio(2, "Ayer", 23 * 60, System.currentTimeMillis() - 24L * 60 * 60 * 1000, "", 10);
        assertFalse(ayer.esFuturoOHoy());
    }
}
