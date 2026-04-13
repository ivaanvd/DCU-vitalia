package com.example.sanbotapp.juegos;

public class RondaBusca {
    private String nombreObjeto;
    private int botonIdCorrecto;

    public RondaBusca(String nombreObjeto, int botonIdCorrecto) {
        this.nombreObjeto = nombreObjeto;
        this.botonIdCorrecto = botonIdCorrecto;
    }

    public String getNombreObjeto() { return nombreObjeto; }
    public int getBotonIdCorrecto() { return botonIdCorrecto; }
}
