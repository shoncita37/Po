package com.example.po;

import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity implements EventoAdapter.OnItemClickListener  {

    private RecyclerView recyclerViewEventos;
    private EventoAdapter eventoAdapter;
    private Button buttonManageTags;
    private Button buttonFilters;

    //
    private List<Evento> listaDeEventos;
    //

    private String idUser;
    private FloatingActionButton fabAgregarEvento;
    private Toolbar toolbar;

    // Gestión de tags y filtros
    private TagManager tagManager;
    private List<String> availableTags = new ArrayList<>();
    private List<String> selectedFilterTags = new ArrayList<>();
    private List<Evento> listaFiltrada = new ArrayList<>();


    private final ActivityResultLauncher<Intent> addEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Evento nuevoEvento = result.getData().getParcelableExtra("evento_resultado");

                    if (nuevoEvento == null) return;

                    if (listaDeEventos == null) {
                        listaDeEventos = new ArrayList<>();
                        listaDeEventos.add(nuevoEvento);

                        eventoAdapter = new EventoAdapter(listaDeEventos);
                        eventoAdapter.setOnItemClickListener(this);
                        recyclerViewEventos.setAdapter(eventoAdapter);
                        // Respetar filtros activos
                        applyFiltersAndRefresh();
                    }
                    else {
                        listaDeEventos.add(nuevoEvento);
                        applyFiltersAndRefresh();
                    }

                    Toast.makeText(this, "Evento agregado correctamente", Toast.LENGTH_SHORT).show();


                }
            });

    private final ActivityResultLauncher<Intent> detailEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Si el resultado es OK, significa que se editó
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Evento eventoActualizado = result.getData().getParcelableExtra("evento_actualizado");
                    String positionKey = result.getData().getStringExtra("posicion_evento");

                    if (positionKey != null && !positionKey.isEmpty() && eventoActualizado != null) {

                        eventoActualizado.setId(positionKey);

                        DatabaseReference ref = FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(idUser)
                                .child("ListEvents");

                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                DataSnapshot eventSnap = snapshot.child(positionKey);

                                if (eventSnap.exists()) {
                                    List<Producto> productos = new ArrayList<>();
                                    for (DataSnapshot productoSnap : eventSnap.child("listaDeseo").getChildren()) {
                                        // Leer el producto completo para preservar sus tags
                                        Producto producto = productoSnap.getValue(Producto.class);
                                        if (producto != null) {
                                            productos.add(producto);
                                        }
                                    }

                                    eventoActualizado.setListaDeseo(productos);

                                    ref.child(positionKey).setValue(eventoActualizado)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getApplicationContext(), "Actualización realizada", Toast.LENGTH_SHORT).show();
                                                // Actualizar la lista en memoria y refrescar el item
                                                if (listaDeEventos != null && eventoAdapter != null) {
                                                    int idx = -1;
                                                    for (int i = 0; i < listaDeEventos.size(); i++) {
                                                        if (positionKey.equals(listaDeEventos.get(i).getId())) {
                                                            idx = i;
                                                            break;
                                                        }
                                                    }
                                                    if (idx != -1) {
                                                        listaDeEventos.set(idx, eventoActualizado);
                                                    } else {
                                                        // Si no se encuentra, agregar como fallback
                                                        listaDeEventos.add(eventoActualizado);
                                                    }
                                                    applyFiltersAndRefresh();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getApplicationContext(), "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("Firebase", "Error: " + error.getMessage());
                            }
                        });
                    }
                }

                // Si el resultado es DELETED, significa que se eliminó
                else if (result.getResultCode() == EventDetailActivity.RESULT_DELETED && result.getData() != null) {
                    String positionKey = result.getData().getStringExtra("posicion_evento_eliminado");
                    if (positionKey != null && !positionKey.isEmpty()) {


                        DatabaseReference ref = FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(idUser)
                                .child("ListEvents")
                                .child(positionKey);

                        ref.removeValue()
                                .addOnSuccessListener(aVoid -> {

                                    for(int n = 0; n < listaDeEventos.size() ;n++){
                                        if(positionKey.equals(listaDeEventos.get(n).getId())){
                                            listaDeEventos.remove(n);
                                            break;
                                        }
                                    }
                                    applyFiltersAndRefresh();


                                    Toast.makeText(this, "Evento eliminado correctamente", Toast.LENGTH_SHORT).show();

                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error al eliminar evento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerViewEventos = findViewById(R.id.recyclerViewEventos);
        fabAgregarEvento = findViewById(R.id.fabAgregarEvento);
        buttonManageTags = findViewById(R.id.buttonManageTags);
        buttonFilters = findViewById(R.id.buttonFilters);
        
        // Inicializar TagManager y configuración de filtros
        idUser = getIntent().getStringExtra("idUser");
        tagManager = new TagManager(this, idUser);
        selectedFilterTags.clear();
        selectedFilterTags.add("todos");
        
        buttonManageTags.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, TagsActivity.class);
            intent.putExtra("userId", idUser);
            startActivity(intent);
        });
        
        buttonFilters.setOnClickListener(v -> {
            // Cargar tags y mostrar el diálogo de filtros
            tagManager.loadTags(tags -> {
                availableTags = new ArrayList<>(tags);
                showFilterDialog();
            });
        });

        recyclerViewEventos.setLayoutManager(new LinearLayoutManager(this));


        ArrayList<Evento> listaEventos = getIntent().getParcelableArrayListExtra("listaEventos");

        cargarDatos(listaEventos);


        if(listaDeEventos != null){
            eventoAdapter = new EventoAdapter(listaDeEventos);
            eventoAdapter.setOnItemClickListener(this);

            recyclerViewEventos.setAdapter(eventoAdapter);
            applyFiltersAndRefresh();




        }
        fabAgregarEvento.setOnClickListener(view -> {

            Intent intent = new Intent(HomeActivity.this, AddEventActivity.class);
            intent.putExtra("data", listaEventos);
            intent.putExtra("idUser", idUser); // Añadiendo el ID de usuario

            addEventLauncher.launch(intent);
        });

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
                // Seleccionar un tag
                if (!selectedFilterTags.contains(tag)) {
                    selectedFilterTags.add(tag);
                }
                // Si se selecciona cualquier tag distinto de "todos", desactivar "todos"
                if (!"todos".equals(tag) && selectedFilterTags.contains("todos")) {
                    // Desmarcar "todos" visualmente
                    int idxTodos = java.util.Arrays.asList(items).indexOf("todos");
                    if (idxTodos >= 0) {
                        checkedItems[idxTodos] = false;
                        AlertDialog alert = (AlertDialog) dialog;
                        alert.getListView().setItemChecked(idxTodos, false);
                    }
                    selectedFilterTags.remove("todos");
                }
            } else {
                // Deseleccionar un tag
                selectedFilterTags.remove(tag);
                // Si se deseleccionan todos, activar "todos" por defecto
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
            // Si el usuario marca "todos", desmarcar todos los demás
            if (selectedFilterTags.contains("todos")) {
                selectedFilterTags.clear();
                selectedFilterTags.add("todos");
            }
            applyFiltersAndRefresh();
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    private void applyFiltersAndRefresh() {
        if (selectedFilterTags == null || selectedFilterTags.isEmpty() || selectedFilterTags.contains("todos")) {
            // Mostrar todos
            eventoAdapter = new EventoAdapter(listaDeEventos);
            eventoAdapter.setOnItemClickListener(this);
            recyclerViewEventos.setAdapter(eventoAdapter);
            eventoAdapter.notifyDataSetChanged();
            return;
        }

        // Filtrar por intersección: el evento debe contener TODOS los tags seleccionados
        listaFiltrada.clear();
        for (Evento e : listaDeEventos) {
            List<String> tags = e.getTags();
            if (tags == null) {
                tags = new ArrayList<>();
                tags.add("todos");
                e.setTags(tags);
            }
            boolean contieneTodos = true;
            for (String t : selectedFilterTags) {
                if (!tags.contains(t)) {
                    contieneTodos = false;
                    break;
                }
            }
            if (contieneTodos) {
                listaFiltrada.add(e);
            }
        }

        eventoAdapter = new EventoAdapter(listaFiltrada);
        eventoAdapter.setOnItemClickListener(this);
        recyclerViewEventos.setAdapter(eventoAdapter);
        eventoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(Evento evento) {
        String posicion = evento.getId();
        Intent intent = new Intent(this, EventDetailActivity.class);
        
        // Asegurarse de que los tags no sean nulos antes de pasar el evento
        if (evento.getTags() == null) {
            evento.setTags(new ArrayList<>());
            evento.addTag("todos");
        }
        
        intent.putExtra("evento_seleccionado", evento);
        intent.putExtra("posicion_evento", posicion);
        Log.d("Firebase", "Eventos ID: " + posicion);

        intent.putExtra("userId",idUser);
        detailEventLauncher.launch(intent);
    }

    private void cargarDatos(ArrayList<Evento> data) {

        Log.d("Firebase", "Eventos cargados: " + data.size());

        listaDeEventos = data;

    }

}