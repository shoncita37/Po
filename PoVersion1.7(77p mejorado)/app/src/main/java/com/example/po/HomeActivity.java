package com.example.po;

import android.content.Intent;
import android.content.SharedPreferences;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    private ImageButton buttonOverflowMenu;

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
    private Boolean isBusiness = null;
    private String cachedUserName = null;
    private static final int MENU_FILTERS = 1;
    private static final int MENU_MANAGE_TAGS = 2;
    private static final int MENU_MANAGE_EMPRESA = 3;
    private static final int MENU_EXPORT_CSV = 4;
    private static final int MENU_IMPORT_CSV = 5;
    private ActivityResultLauncher<String> pickCsvLauncher;


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
        buttonOverflowMenu = findViewById(R.id.buttonOverflowMenu);
        
        // Inicializar TagManager y configuración de filtros
        idUser = getIntent().getStringExtra("idUser");
        if (idUser == null || idUser.isEmpty()) {
            // Fallback: recuperar el usuario autenticado actual
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current != null) {
                idUser = current.getUid();
            }
        }
        tagManager = new TagManager(this, idUser);
        selectedFilterTags.clear();
        selectedFilterTags.add("todos");

        // Cargar datos del usuario (business y name) para mostrar gestión de empresa si aplica
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("user");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean business = snapshot.child("business").getValue(Boolean.class);
                String userName = snapshot.child("name").getValue(String.class);
                isBusiness = business;
                cachedUserName = userName;
                invalidateOptionsMenu();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // En caso de error, mantener oculto cualquier referencia de empresa
                isBusiness = false;
            }
        });

        // Registrador para seleccionar un archivo CSV y ejecutar importación
        pickCsvLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                if (idUser == null || idUser.isEmpty()) {
                    Toast.makeText(this, "Id de usuario inválido", Toast.LENGTH_SHORT).show();
                    return;
                }
                CsvExporter.importFromCsv(this, idUser, uri, new CsvExporter.ImportCallback() {
                    @Override
                    public void onSuccess(int entidadesCount, int eventosCount, int productosCount) {
                        Toast.makeText(HomeActivity.this,
                                "Importado: " + entidadesCount + " entidades, " + eventosCount + " eventos, " + productosCount + " productos",
                                Toast.LENGTH_LONG).show();
                        // Tras importar, recargar los eventos desde Firebase para mostrarlos
                        reloadEventosFromFirebase();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(HomeActivity.this, "Error importando: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Archivo no seleccionado", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Configurar menú desplegable
        buttonOverflowMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(HomeActivity.this, buttonOverflowMenu);
            Menu menu = popup.getMenu();
            menu.add(Menu.NONE, MENU_FILTERS, Menu.NONE, "Filtros");
            menu.add(Menu.NONE, MENU_MANAGE_TAGS, Menu.NONE, "Gestionar Tags");
            if (Boolean.TRUE.equals(isBusiness)) {
                menu.add(Menu.NONE, MENU_MANAGE_EMPRESA, Menu.NONE, "Gestionar Empresa");
                menu.add(Menu.NONE, MENU_EXPORT_CSV, Menu.NONE, "Exportar CSV");
                menu.add(Menu.NONE, MENU_IMPORT_CSV, Menu.NONE, "Importar CSV");
            }

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case MENU_FILTERS:
                        tagManager.loadTags(tags -> {
                            availableTags = new ArrayList<>(tags);
                            showFilterDialog();
                        });
                        return true;
                    case MENU_MANAGE_TAGS: {
                        Intent intent = new Intent(HomeActivity.this, TagsActivity.class);
                        intent.putExtra("userId", idUser);
                        startActivity(intent);
                        return true;
                    }
                    case MENU_MANAGE_EMPRESA: {
                        if (!Boolean.TRUE.equals(isBusiness)) {
                            Toast.makeText(HomeActivity.this, "Solo disponible para empresas", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        Intent intent = new Intent(HomeActivity.this, EmpresaActivity.class);
                        intent.putExtra("idUser", idUser);
                        intent.putExtra("userName", cachedUserName);
                        startActivity(intent);
                        return true;
                    }
                    case MENU_EXPORT_CSV: {
                        if (!Boolean.TRUE.equals(isBusiness)) {
                            Toast.makeText(HomeActivity.this, "Solo disponible para empresas", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        if (idUser == null || idUser.isEmpty()) {
                            Toast.makeText(HomeActivity.this, "Id de usuario inválido", Toast.LENGTH_LONG).show();
                        } else {
                            CsvExporter.exportEmpresaToCsv(HomeActivity.this, idUser, new CsvExporter.ExportCallback() {
                                @Override
                                public void onSuccess(String filePath, android.net.Uri fileUri) {
                                    Toast.makeText(HomeActivity.this,
                                            "CSV guardado en: " + filePath,
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(HomeActivity.this,
                                            "Error exportando CSV: " + message,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        return true;
                    }
                    case MENU_IMPORT_CSV: {
                        if (!Boolean.TRUE.equals(isBusiness)) {
                            Toast.makeText(HomeActivity.this, "Solo disponible para empresas", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        pickCsvLauncher.launch("text/*");
                        return true;
                    }
                }
                return false;
            });

            popup.show();
        });

        recyclerViewEventos.setLayoutManager(new LinearLayoutManager(this));


        ArrayList<Evento> listaEventos = getIntent().getParcelableArrayListExtra("listaEventos");

        if (listaEventos != null) {
            cargarDatos(listaEventos);
        } else {
            // Si no hay datos en el Intent (por re-creación), cargar desde Firebase
            if (idUser != null && !idUser.isEmpty()) {
                reloadEventosFromFirebase();
            }
        }


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

    @Override
    protected void onResume() {
        super.onResume();
        // Guardar la última pantalla para restaurar si el usuario vuelve sin cerrar la app
        SharedPreferences sp = getSharedPreferences("app_prefs", MODE_PRIVATE);
        sp.edit()
                .putString("last_screen", "home")
                .putString("last_user_id", idUser != null ? idUser : "")
                .apply();
    }


    private void reloadEventosFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("ListEvents");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Evento> nuevosEventos = new ArrayList<>();

                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    String fecha = eventSnap.child("fecha").getValue(String.class);
                    String nombre = eventSnap.child("nombre").getValue(String.class);
                    String notas = eventSnap.child("notas").getValue(String.class);
                    String id = eventSnap.child("id").getValue(String.class);
                    Integer tipoRecordatorio = eventSnap.child("tipoRecordatorio").getValue(Integer.class);

                    // Recuperar listaDeseo completa (incluyendo tags de productos)
                    List<Producto> productos = new ArrayList<>();
                    for (DataSnapshot productoSnap : eventSnap.child("listaDeseo").getChildren()) {
                        Producto producto = productoSnap.getValue(Producto.class);
                        if (producto != null) {
                            productos.add(producto);
                        }
                    }

                    // Recuperar tags del evento
                    List<String> tags = new ArrayList<>();
                    for (DataSnapshot tagSnap : eventSnap.child("tags").getChildren()) {
                        String tag = tagSnap.getValue(String.class);
                        if (tag != null) {
                            tags.add(tag);
                        }
                    }

                    Evento evento = new Evento(fecha, nombre, notas, productos, id);
                    if (tipoRecordatorio != null) {
                        evento.setTipoRecordatorio(tipoRecordatorio);
                    }
                    evento.setTags(tags);
                    nuevosEventos.add(evento);
                }

                listaDeEventos = nuevosEventos;
                applyFiltersAndRefresh();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Error recargando eventos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

    // Se eliminan las opciones del Toolbar; las acciones están en el PopupMenu

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