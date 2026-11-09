package com.example.sanbotapp.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class DateTimeUtilsTest {

    private Locale localeOriginal;

    @Before
    public void fijarLocale() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void restaurarLocale() {
        Locale.setDefault(localeOriginal);
    }

    @Test
    public void formato24h_rellenaConCeros() {
        assertEquals("15:30", DateTimeUtils.formatearHora(15 * 60 + 30, true));
        assertEquals("09:05", DateTimeUtils.formatearHora(9 * 60 + 5, true));
        assertEquals("00:00", DateTimeUtils.formatearHora(0, true));
    }

    @Test
    public void formato12h_usaAmPm() {
        assertEquals("3:30 PM", DateTimeUtils.formatearHora(15 * 60 + 30, false));
        assertEquals("9:05 AM", DateTimeUtils.formatearHora(9 * 60 + 5, false));
    }

    @Test
    public void formato12h_mediodiaYMedianoche() {
        assertEquals("12:00 PM", DateTimeUtils.formatearHora(12 * 60, false));
        assertEquals("12:00 AM", DateTimeUtils.formatearHora(0, false));
    }
}
