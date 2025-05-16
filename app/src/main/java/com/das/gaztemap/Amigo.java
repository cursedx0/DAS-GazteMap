package com.das.gaztemap;

public class Amigo {
    private String nombre;
    private String fotoUrl;
    private int id;

    public Amigo(String nombre, String fotoUrl, int id) {
        this.nombre = nombre;
        this.fotoUrl = fotoUrl;
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFoto() {
        return fotoUrl;
    }
    public int getId() {
        return id;
    }
}