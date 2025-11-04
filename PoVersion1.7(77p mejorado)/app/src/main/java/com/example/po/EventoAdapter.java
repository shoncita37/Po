package com.example.po;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class EventoAdapter extends RecyclerView.Adapter<EventoAdapter.EventoViewHolder> {

    private List<Evento> listaEventos;
    // 1. Variable para el listener
    private OnItemClickListener listener;

    // 2. Interfaz para manejar el click
    public interface OnItemClickListener {
        void onItemClick(Evento evento);
    }

    // 3. Método para establecer el listener desde HomeActivity
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public EventoAdapter(List<Evento> listaEventos) {
        this.listaEventos = listaEventos;
    }

    @NonNull
    @Override
    public EventoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evento, parent, false);
        return new EventoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventoViewHolder holder, int position) {
        Evento evento = listaEventos.get(position);
        
        // Asegurarse de que los tags no sean nulos
        if (evento.getTags() == null) {
            evento.setTags(new ArrayList<>());
            evento.addTag("todos");
        }
        
        holder.nombreEvento.setText(evento.getNombre());
        holder.fechaEvento.setText(evento.getFecha());
    }

    @Override
    public int getItemCount() {
        return listaEventos.size();
    }

    public class EventoViewHolder extends RecyclerView.ViewHolder {
        TextView nombreEvento;
        TextView fechaEvento;

        public EventoViewHolder(@NonNull View itemView) {
            super(itemView);
            nombreEvento = itemView.findViewById(R.id.textViewNombreEvento);
            fechaEvento = itemView.findViewById(R.id.textViewFechaEvento);

            // 4. Configurar el click en el ViewHolder
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(listaEventos.get(position));
                    }
                }
            });
        }
    }
}