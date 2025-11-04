package com.example.po;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private ProductoAdapter adapter;
    private List<Producto> productList;
    private TagManager tagManager;
    private List<String> availableTags;
    private List<String> selectedTags;

    // Filtros de productos
    private Button buttonFiltersWishlist;
    private List<String> selectedFilterTags = new ArrayList<>();
    private List<Producto> filteredProductList = new ArrayList<>();

    private SelectedTagAdapter tagAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        toolbar = findViewById(R.id.toolbarWishlist);
        setSupportActionBar(toolbar);
        // Habilitar la flecha de regreso
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Recibir el nombre del evento y ponerlo en el título
        String eventName = getIntent().getStringExtra("nombre_evento");
        ArrayList<Producto> listaDeseos = getIntent().getParcelableArrayListExtra("lista_productos");
        String userId = getIntent().getStringExtra("userId");

        // Inicializar TagManager y listas de tags
        tagManager = new TagManager(this, userId);
        availableTags = new ArrayList<>();
        selectedTags = new ArrayList<>();
        
        // Inicializar filtros ("todos" por defecto)
        buttonFiltersWishlist = findViewById(R.id.buttonFiltersWishlist);
        selectedFilterTags.clear();
        selectedFilterTags.add("todos");
        
        buttonFiltersWishlist.setOnClickListener(v -> {
            tagManager.loadTags(tags -> {
                availableTags = tags;
                showFilterDialog();
            });
        });
        
        // Cargar los tags disponibles
        tagManager.loadTags(tags -> {
            availableTags = tags;
        });

        if (eventName != null && !eventName.isEmpty()) {
            getSupportActionBar().setTitle("Deseos para " + eventName);
        }

        recyclerView = findViewById(R.id.recyclerViewWishlist);
        fab = findViewById(R.id.fabAddProduct);
        productList = new ArrayList<>();

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(productList, new ProductoAdapter.OnProductoActionListener() {
            @Override
            public void onEditProducto(Producto producto, int position) {
                mostrarDialogoEditarProducto(producto, position);
            }

            @Override
            public void onDeleteProducto(Producto producto, int position) {
                eliminarProducto(position);
            }
        });
        recyclerView.setAdapter(adapter);

        cargarDatos(listaDeseos);

        fab.setOnClickListener(v -> mostrarDialogoAgregarProducto());
    }

    private void showFilterDialog() {
        if (availableTags == null) availableTags = new ArrayList<>();
        if (!availableTags.contains("todos")) {
            availableTags.add(0, "todos");
        }
        String[] items = availableTags.toArray(new String[0]);
        boolean[] checkedItems = new boolean[items.length];
        for (int i = 0; i < items.length; i++) {
            checkedItems[i] = selectedFilterTags.contains(items[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filtrar por tags");
        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            String tag = items[which];
            if (isChecked) {
                if (!selectedFilterTags.contains(tag)) selectedFilterTags.add(tag);
                if (!"todos".equals(tag) && selectedFilterTags.contains("todos")) {
                    int idxTodos = java.util.Arrays.asList(items).indexOf("todos");
                    if (idxTodos >= 0) {
                        checkedItems[idxTodos] = false;
                        AlertDialog alert = (AlertDialog) dialog;
                        alert.getListView().setItemChecked(idxTodos, false);
                    }
                    selectedFilterTags.remove("todos");
                }
            } else {
                selectedFilterTags.remove(tag);
                if (selectedFilterTags.isEmpty()) {
                    selectedFilterTags.add("todos");
                    int idxTodos = java.util.Arrays.asList(items).indexOf("todos");
                    if (idxTodos >= 0) {
                        checkedItems[idxTodos] = true;
                        AlertDialog alert = (AlertDialog) dialog;
                        alert.getListView().setItemChecked(idxTodos, true);
                    }
                }
            }
        });
        builder.setPositiveButton("Aplicar", (dialog, which) -> {
            if (selectedFilterTags.contains("todos")) {
                selectedFilterTags.clear();
                selectedFilterTags.add("todos");
            }
            applyFiltersAndRefresh();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void applyFiltersAndRefresh() {
        if (selectedFilterTags == null || selectedFilterTags.isEmpty() || selectedFilterTags.contains("todos")) {
            adapter = new ProductoAdapter(productList, new ProductoAdapter.OnProductoActionListener() {
                @Override
                public void onEditProducto(Producto producto, int position) {
                    mostrarDialogoEditarProducto(producto, position);
                }

                @Override
                public void onDeleteProducto(Producto producto, int position) {
                    eliminarProducto(position);
                }
            });
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredProductList.clear();
        for (Producto p : productList) {
            List<String> tags = p.getTags();
            if (tags == null) {
                tags = new ArrayList<>();
                tags.add("todos");
                p.setTags(tags);
            }
            boolean contieneTodos = true;
            for (String t : selectedFilterTags) {
                if (!tags.contains(t)) {
                    contieneTodos = false;
                    break;
                }
            }
            if (contieneTodos) {
                filteredProductList.add(p);
            }
        }

        adapter = new ProductoAdapter(filteredProductList, new ProductoAdapter.OnProductoActionListener() {
            @Override
            public void onEditProducto(Producto producto, int position) {
                int realPos = productList.indexOf(producto);
                if (realPos >= 0) mostrarDialogoEditarProducto(producto, realPos);
            }

            @Override
            public void onDeleteProducto(Producto producto, int position) {
                int realPos = productList.indexOf(producto);
                if (realPos >= 0) eliminarProducto(realPos);
            }
        });
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void mostrarDialogoEditarProducto(Producto producto, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_product, null);
        builder.setView(dialogView);

        final EditText productName = dialogView.findViewById(R.id.editTextProductName);
        final EditText productUrl = dialogView.findViewById(R.id.editTextProductUrl);
        final Spinner spinnerTags = dialogView.findViewById(R.id.spinnerProductTags);
        final Button buttonManageTags = dialogView.findViewById(R.id.buttonManageProductTags);
        final RecyclerView recyclerViewTags = dialogView.findViewById(R.id.recyclerViewSelectedProductTags);

        // Ocultar el botón de gestionar tags
        buttonManageTags.setVisibility(View.GONE);

        // Prefill campos
        productName.setText(producto.getTitulo());
        productUrl.setText(producto.getUrl());

        // Configurar RecyclerView para tags seleccionados
        selectedTags = new ArrayList<>();
        if (producto.getTags() != null) {
            selectedTags.addAll(producto.getTags());
        }
        recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));

        tagAdapter = new SelectedTagAdapter(this, selectedTags, new SelectedTagAdapter.OnTagRemovedListener() {
            @Override
            public void onTagRemoved(String tag, int pos) {
                selectedTags.remove(pos);
                tagAdapter.updateTags(selectedTags);
                setupTagsSpinner(spinnerTags, tagAdapter);
            }
        });
        recyclerViewTags.setAdapter(tagAdapter);

        // Configurar spinner de tags
        setupTagsSpinner(spinnerTags, tagAdapter);

        builder.setTitle("Editar Regalo");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String name = productName.getText().toString().trim();
            String url = productUrl.getText().toString().trim();
            if (!name.isEmpty()) {
                producto.setTitulo(name);
                // No hay setUrl en Producto, así que recreamos el objeto
                Producto actualizado = new Producto(name, url, new ArrayList<>(selectedTags));
                // Copiar referencia de tags manejada por constructor (asegura "todos")
                productList.set(position, actualizado);

                String idUser = getIntent().getStringExtra("userId");
                String indexEvento = getIntent().getStringExtra("index-evento");

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(idUser)
                        .child("ListEvents")
                        .child(indexEvento)
                        .child("listaDeseo");

                ref.setValue(productList);

                applyFiltersAndRefresh();
            } else {
                Toast.makeText(this, "El nombre del producto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void eliminarProducto(int position) {
        if (position < 0 || position >= productList.size()) return;

        productList.remove(position);

        String idUser = getIntent().getStringExtra("userId");
        String indexEvento = getIntent().getStringExtra("index-evento");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("ListEvents")
                .child(indexEvento)
                .child("listaDeseo");

        ref.setValue(productList);

        applyFiltersAndRefresh();
    }

    private void mostrarDialogoAgregarProducto() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflar el layout personalizado
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_product, null);
        builder.setView(dialogView);

        final EditText productName = dialogView.findViewById(R.id.editTextProductName);
        final EditText productUrl = dialogView.findViewById(R.id.editTextProductUrl);
        final Spinner spinnerTags = dialogView.findViewById(R.id.spinnerProductTags);
        final Button buttonManageTags = dialogView.findViewById(R.id.buttonManageProductTags);
        final RecyclerView recyclerViewTags = dialogView.findViewById(R.id.recyclerViewSelectedProductTags);
        
        // Ocultar el botón de gestionar tags
        buttonManageTags.setVisibility(View.GONE);
        
        // Configurar RecyclerView para tags seleccionados
        selectedTags = new ArrayList<>();
        recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));

        // Crear el adaptador sin la referencia circular
        tagAdapter = new SelectedTagAdapter(this, selectedTags, new SelectedTagAdapter.OnTagRemovedListener() {
            @Override
            public void onTagRemoved(String tag, int position) {
                selectedTags.remove(position);
                tagAdapter.updateTags(selectedTags);
                setupTagsSpinner(spinnerTags, tagAdapter);
            }
        });
        
        recyclerViewTags.setAdapter(tagAdapter);
         
         // Configurar spinner de tags
         setupTagsSpinner(spinnerTags, tagAdapter);

        builder.setTitle("Añadir Nuevo Regalo");
        builder.setPositiveButton("Añadir", (dialog, which) -> {
            String name = productName.getText().toString().trim();
            String url = productUrl.getText().toString().trim();
            if (!name.isEmpty()) {
                Producto nuevoProducto = new Producto(name, url);
                
                // Añadir tags seleccionados al producto
                if (!selectedTags.isEmpty()) {
                    nuevoProducto.setTags(new ArrayList<>(selectedTags));
                }
                
                productList.add(nuevoProducto);

                String idUser = getIntent().getStringExtra("userId");
                String indexEvento = getIntent().getStringExtra("index-evento");

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(idUser)
                        .child("ListEvents")
                        .child(indexEvento)
                        .child("listaDeseo");

                ref.setValue(productList);

                applyFiltersAndRefresh();
            } else {
                Toast.makeText(this, "El nombre del producto no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    
    private void setupTagsSpinner(Spinner spinnerTags, SelectedTagAdapter selectedTagAdapter) {
        if (availableTags == null || availableTags.isEmpty()) {
            // Si no hay tags disponibles, cargar los predeterminados
            tagManager.loadTags(tags -> {
                availableTags = tags;
                configureTagsSpinner(spinnerTags, selectedTagAdapter);
            });
        } else {
            configureTagsSpinner(spinnerTags, selectedTagAdapter);
        }
    }
    
    private void configureTagsSpinner(Spinner spinnerTags, SelectedTagAdapter selectedTagAdapter) {
        // Filtrar tags ya seleccionados
        List<String> availableTagsFiltered = new ArrayList<>();
        for (String tag : availableTags) {
            if (!selectedTags.contains(tag)) {
                availableTagsFiltered.add(tag);
            }
        }
        
        // Añadir opción vacía al principio
        availableTagsFiltered.add(0, "Seleccionar tag");
        
        ArrayAdapter<String> tagsAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, availableTagsFiltered);
        tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTags.setAdapter(tagsAdapter);
        
        spinnerTags.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return; // Ignorar la opción "Seleccionar tag"
                
                String selectedTag = parent.getItemAtPosition(position).toString();
                if (!selectedTag.isEmpty() && !selectedTags.contains(selectedTag)) {
                    selectedTags.add(selectedTag);
                    selectedTagAdapter.updateTags(selectedTags);
                    spinnerTags.setSelection(0); // Resetear a la opción por defecto
                    setupTagsSpinner(spinnerTags, selectedTagAdapter); // Actualizar spinner
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });
    }


    private void cargarDatos(ArrayList<Producto> listaDeseos) {

        String idUser = getIntent().getStringExtra("userId");
        String indexEvento = getIntent().getStringExtra("index-evento");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("ListEvents")
                .child(indexEvento)
                .child("listaDeseo");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear(); // Limpia la lista antes de cargar nuevos datos

                // Recorremos cada hijo dentro de "listaDeseo"
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Producto producto = dataSnapshot.getValue(Producto.class);

                    if (producto != null) {
                        productList.add(producto);
                    }
                }

                // Reaplicar filtros tras actualizar la lista
                applyFiltersAndRefresh();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error al cargar los datos: " + error.getMessage());
            }
        });



        applyFiltersAndRefresh();
    }

    // Para que la flecha de regreso funcione
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}