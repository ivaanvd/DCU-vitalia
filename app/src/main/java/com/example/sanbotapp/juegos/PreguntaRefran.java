package com.example.sanbotapp.juegos;

public class PreguntaRefran {
    private String textoCuestion;
    private String opcion1;
    private String opcion2;
    private int indiceCorrecto; // 1 o 2

    public PreguntaRefran(String texto, String opc1, String opc2, int indCorrecto) {
        this.textoCuestion = texto;
        this.opcion1 = opc1;
        this.opcion2 = opc2;
        this.indiceCorrecto = indCorrecto;
    }

    public String getTextoCuestion() { return textoCuestion; }
    public String getOpcion1() { return opcion1; }
    public String getOpcion2() { return opcion2; }
    public int getIndiceCorrecto() { return indiceCorrecto; }
}
