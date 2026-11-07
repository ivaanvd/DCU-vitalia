package com.example.sanbotapp.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VozComandosTest {

    @Test
    public void ignoraTildesYMayusculas() {
        assertTrue(VozComandos.contieneAlguna("Sí, correcto", "si"));
        assertTrue(VozComandos.contieneAlguna("cambia el titulo", "título"));
        assertTrue(VozComandos.contieneAlguna("AÑADIR", "añadir"));
    }

    @Test
    public void comparaPalabrasCompletas() {
        assertFalse(VozComandos.contieneAlguna("ahora", "hora"));
        assertFalse(VozComandos.contieneAlguna("now", "no"));
        assertFalse(VozComandos.contieneAlguna("siguiente", "si"));
        assertTrue(VozComandos.contieneAlguna("no, change it", "change"));
    }

    @Test
    public void frasesDeVariasPalabras() {
        assertTrue(VozComandos.contieneAlguna("vale, está bien", "está bien"));
        assertTrue(VozComandos.contieneAlguna("yes, save", "yes, save"));
        assertFalse(VozComandos.contieneAlguna("save", "yes, save"));
    }
}
