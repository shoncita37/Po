package com.example.po;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
    
    private List<String> tags;
    private Context context;
    private TagManager tagManager;
    private String userId;
    private OnTagActionListener listener;
    
    public interface OnTagActionListener {
        void onTagEdit(String oldTag, String newTag);
        void onTagDelete(String tag);
    }
    
    public TagAdapter(Context context, List<String> tags, TagManager tagManager, String userId, OnTagActionListener listener) {
        this.context = context;
        this.tags = tags;
        this.tagManager = tagManager;
        this.userId = userId;
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
        String tag = tags.get(position);
        holder.textViewTag.setText(tag);
        
        // Deshabilitar edición y eliminación para el tag "todos"
        if ("todos".equals(tag)) {
            holder.buttonEditTag.setVisibility(View.INVISIBLE);
            holder.buttonDeleteTag.setVisibility(View.INVISIBLE);
        } else {
            holder.buttonEditTag.setVisibility(View.VISIBLE);
            holder.buttonDeleteTag.setVisibility(View.VISIBLE);
            
            holder.buttonEditTag.setOnClickListener(v -> showEditDialog(tag, position));
            holder.buttonDeleteTag.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagDelete(tag);
                }
            });
        }
    }
    
    @Override
    public int getItemCount() {
        return tags.size();
    }
    
    private void showEditDialog(String tag, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Editar Tag");
        
        final EditText input = new EditText(context);
        input.setText(tag);
        builder.setView(input);
        
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newTag = input.getText().toString().trim();
            if (!newTag.isEmpty()) {
                if (listener != null) {
                    listener.onTagEdit(tag, newTag);
                }
            }
        });
        
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    public void updateTags(List<String> newTags) {
        this.tags = newTags;
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