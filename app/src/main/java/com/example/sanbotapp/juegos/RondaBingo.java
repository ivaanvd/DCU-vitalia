package com.example.sanbotapp.juegos;

public class RondaBingo {
    private String consigna;
    private int idBotonCorrecto;

    public RondaBingo(String consigna, int idBotonCorrecto) {
        this.consigna = consigna;
        this.idBotonCorrecto = idBotonCorrecto;
    }

    public String getConsigna() { return consigna; }
    public int getIdBotonCorrecto() { return idBotonCorrecto; }
}
