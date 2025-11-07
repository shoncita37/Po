package com.example.po;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa una empresa que gestiona listas de entidades por tipo.
 * - clientes: Entidad con tipo "cliente"
 * - encargados: Entidad con tipo "encargado"
 * - trabajadores: Entidad con tipo "trabajador"
 */
public class Empresa {

    private final List<Entidad> clientes = new ArrayList<>();
    private final List<Entidad> encargados = new ArrayList<>();
    private final List<Entidad> trabajadores = new ArrayList<>();

    public Empresa() {}

    // Clientes
    public void addCliente(Entidad entidad) {
        if (entidad == null) return;
        entidad.setTipo("cliente");
        clientes.add(entidad);
    }

    public void removeCliente(Entidad entidad) {
        clientes.remove(entidad);
    }

    public List<Entidad> getClientes() {
        return Collections.unmodifiableList(clientes);
    }

    // Encargados
    public void addEncargado(Entidad entidad) {
        if (entidad == null) return;
        entidad.setTipo("encargado");
        encargados.add(entidad);
    }

    public void removeEncargado(Entidad entidad) {
        encargados.remove(entidad);
    }

    public List<Entidad> getEncargados() {
        return Collections.unmodifiableList(encargados);
    }

    // Trabajadores
    public void addTrabajador(Entidad entidad) {
        if (entidad == null) return;
        entidad.setTipo("trabajador");
        trabajadores.add(entidad);
    }

    public void removeTrabajador(Entidad entidad) {
        trabajadores.remove(entidad);
    }

    public List<Entidad> getTrabajadores() {
        return Collections.unmodifiableList(trabajadores);
    }
}