package com.example.po;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class TagsActivity extends AppCompatActivity implements TagAdapter.OnTagActionListener {

    private RecyclerView recyclerViewTags;
    private TagAdapter tagAdapter;
    private TagManager tagManager;
    private String userId;
    private List<String> tagsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags);

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Gestionar Tags");

        // Obtener userId
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getIntent().getStringExtra("idUser");
        }
        
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar TagManager
        tagManager = new TagManager(this, userId);
        tagsList = new ArrayList<>();

        // Configurar RecyclerView
        recyclerViewTags = findViewById(R.id.recyclerViewTags);
        recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
        tagAdapter = new TagAdapter(this, tagsList, tagManager, userId, this);
        recyclerViewTags.setAdapter(tagAdapter);

        // Configurar FAB para añadir tags
        FloatingActionButton fabAddTag = findViewById(R.id.fabAddTag);
        fabAddTag.setOnClickListener(v -> showAddTagDialog());

        // Cargar tags
        loadTags();
    }

    private void loadTags() {
        tagManager.loadTags(tags -> {
            tagsList.clear();
            tagsList.addAll(tags);
            tagAdapter.notifyDataSetChanged();
        });
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_tag, null);
        builder.setView(view);

        EditText editTextTag = view.findViewById(R.id.editTextTag);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnAdd = view.findViewById(R.id.btnAdd);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String tag = editTextTag.getText().toString().trim();
            if (!tag.isEmpty()) {
                if ("todos".equalsIgnoreCase(tag)) {
                    Toast.makeText(this, "No se puede añadir el tag 'todos' manualmente", Toast.LENGTH_SHORT).show();
                } else {
                    tagManager.addTag(tag);
                    loadTags();
                    dialog.dismiss();
                }
            } else {
                Toast.makeText(this, "El nombre del tag no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTagEdit(String oldTag, String newTag) {
        tagManager.updateTag(oldTag, newTag);
        loadTags();
    }

    @Override
    public void onTagDelete(String tag) {
        if ("todos".equals(tag)) {
            Toast.makeText(this, "No se puede eliminar el tag 'todos'", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar Tag");
        builder.setMessage("¿Estás seguro de que deseas eliminar este tag? Se eliminará de todos los eventos y productos.");
        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            tagManager.removeTagFromAllItems(tag, success -> {
                if (success) {
                    Toast.makeText(TagsActivity.this, "Tag eliminado correctamente", Toast.LENGTH_SHORT).show();
                    loadTags();
                } else {
                    Toast.makeText(TagsActivity.this, "Error al eliminar el tag", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}