package com.example.po;

public class Usuario {
    private String nombre;
    private String email;
    private String tipoUsuario; // "Común" o "Empresa"

    // Constructor vacío es requerido por Firestore
    public Usuario() {
    }

    public Usuario(String nombre, String email, String tipoUsuario) {
        this.nombre = nombre;
        this.email = email;
        this.tipoUsuario = tipoUsuario;
    }

    // Getters (los setters no son estrictamente necesarios si no los modificas)
    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }
}
