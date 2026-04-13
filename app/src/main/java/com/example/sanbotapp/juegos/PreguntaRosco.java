package com.example.sanbotapp.juegos;

public class PreguntaRosco {
    private String tematica;
    private String letra;
    private String pista;
    private String opcion1;
    private String opcion2;
    private int indiceCorrecto;

    public PreguntaRosco(String tematica, String letra, String pista, String opc1, String opc2, int indCorrecto) {
        this.tematica = tematica;
        this.letra = letra;
        this.pista = pista;
        this.opcion1 = opc1;
        this.opcion2 = opc2;
        this.indiceCorrecto = indCorrecto;
    }

    public String getTematica() { return tematica; }
    public String getLetra() { return letra; }
    public String getPista() { return pista; }
    public String getOpcion1() { return opcion1; }
    public String getOpcion2() { return opcion2; }
    public int getIndiceCorrecto() { return indiceCorrecto; }
}
