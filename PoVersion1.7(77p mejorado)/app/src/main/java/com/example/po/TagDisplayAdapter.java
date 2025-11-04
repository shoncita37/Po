package com.example.po;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TagDisplayAdapter extends RecyclerView.Adapter<TagDisplayAdapter.TagViewHolder> {
    private List<String> tags;
    private Context context;

    public TagDisplayAdapter(Context context, List<String> tags) {
        this.context = context;
        this.tags = tags;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag_display, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = tags.get(position);
        holder.tagText.setText(tag);
    }

    @Override
    public int getItemCount() {
        return tags != null ? tags.size() : 0;
    }

    public void updateTags(List<String> newTags) {
        this.tags = newTags;
        notifyDataSetChanged();
    }

    static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagText;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagText = itemView.findViewById(R.id.textViewTag);
        }
    }
}