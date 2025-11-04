package com.example.po;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SelectedTagAdapter extends RecyclerView.Adapter<SelectedTagAdapter.TagViewHolder> {
    
    private List<String> selectedTags;
    private Context context;
    private OnTagRemovedListener listener;
    
    public interface OnTagRemovedListener {
        void onTagRemoved(String tag, int position);
    }
    
    public SelectedTagAdapter(Context context, List<String> selectedTags, OnTagRemovedListener listener) {
        this.context = context;
        this.selectedTags = selectedTags;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = selectedTags.get(position);
        holder.textViewTag.setText(tag);
        
        // Ocultar botón de edición y mostrar solo el de eliminar
        holder.buttonEditTag.setVisibility(View.GONE);
        
        // Deshabilitar eliminación para el tag "todos"
        if ("todos".equals(tag)) {
            holder.buttonDeleteTag.setVisibility(View.INVISIBLE);
        } else {
            holder.buttonDeleteTag.setVisibility(View.VISIBLE);
            holder.buttonDeleteTag.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagRemoved(tag, position);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return selectedTags.size();
    }
    
    public void updateTags(List<String> newTags) {
        this.selectedTags = newTags;
        notifyDataSetChanged();
    }
    
    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTag;
        ImageButton buttonEditTag;
        ImageButton buttonDeleteTag;
        
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTag = itemView.findViewById(R.id.textViewTag);
            buttonEditTag = itemView.findViewById(R.id.buttonEditTag);
            buttonDeleteTag = itemView.findViewById(R.id.buttonDeleteTag);
        }
    }
}