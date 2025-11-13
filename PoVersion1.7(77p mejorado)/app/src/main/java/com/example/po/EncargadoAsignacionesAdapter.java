package com.example.po;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EncargadoAsignacionesAdapter extends RecyclerView.Adapter<EncargadoAsignacionesAdapter.ViewHolder> {

    public static class EncargadoAsignacion {
        public String encargadoNombre;
        public List<String> trabajadores;

        public EncargadoAsignacion(String encargadoNombre, List<String> trabajadores) {
            this.encargadoNombre = encargadoNombre;
            this.trabajadores = trabajadores != null ? trabajadores : new ArrayList<>();
        }
    }

    private final List<EncargadoAsignacion> data;

    public EncargadoAsignacionesAdapter(List<EncargadoAsignacion> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_encargado_trabajadores, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EncargadoAsignacion item = data.get(position);
        holder.textEncargado.setText(item.encargadoNombre);
        String trabajadoresText = item.trabajadores.isEmpty() ? "(sin trabajadores)" : String.join(", ", item.trabajadores);
        holder.textTrabajadores.setText(trabajadoresText);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textEncargado;
        TextView textTrabajadores;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textEncargado = itemView.findViewById(R.id.textEncargadoNombre);
            textTrabajadores = itemView.findViewById(R.id.textTrabajadoresList);
        }
    }
}