package com.example.pruebadefirebase;

public class Archivo {
    private String nombre;
    private String urlDescarga;

    public Archivo() {
        // Constructor vacío requerido por Firebase Database
    }

    public Archivo(String nombre, String urlDescarga) {
        this.nombre = nombre;
        this.urlDescarga = urlDescarga;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUrlDescarga() {
        return urlDescarga;
    }

    public void setUrlDescarga(String urlDescarga) {
        this.urlDescarga = urlDescarga;
    }
}
