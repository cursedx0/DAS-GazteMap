package com.example.gaztemap;

public class Amigo {
    private String nombre;
    private String fotoUrl;

    public Amigo(String nombre, String email) {
        this.nombre = nombre;
        this.fotoUrl = email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFoto() {
        return fotoUrl;
    }
}