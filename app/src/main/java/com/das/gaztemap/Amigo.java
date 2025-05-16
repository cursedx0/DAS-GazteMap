package com.das.gaztemap;

public class Amigo {
    private String nombre;
    private String fotoUrl;

    public Amigo(String nombre, String fotoUrl) {
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFoto() {
        return fotoUrl;
    }
}