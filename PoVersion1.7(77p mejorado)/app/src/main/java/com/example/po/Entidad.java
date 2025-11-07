package com.example.po;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una entidad con nombre, tipo, tags y email.
 */
public class Entidad {

    private String nombre;
    private String tipo; // "cliente", "encargado", "trabajador" u otro
    private List<String> tags;
    private String email;

    public Entidad() {
        this.tags = new ArrayList<>();
    }

    public Entidad(String nombre, String tipo, List<String> tags, String email) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) return;
        if (this.tags == null) this.tags = new ArrayList<>();
        this.tags.add(tag);
    }

    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}