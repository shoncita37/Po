package com.example.po;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EmpresaActivity extends AppCompatActivity {

    private TextInputLayout inputNameLayout;
    private TextInputLayout inputEmailLayout;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private Spinner spinnerTipo;
    private Spinner spinnerTag;
    private Button buttonGuardar;
    private Button buttonAsignarTrabajadores;
    private RecyclerView recyclerAsignaciones;
    private EncargadoAsignacionesAdapter asignacionesAdapter;
    private List<EncargadoAsignacionesAdapter.EncargadoAsignacion> asignacionesList = new ArrayList<>();

    private String idUser;
    private String userName;
    private TagManager tagManager;
    private List<String> availableTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empresa);

        idUser = getIntent().getStringExtra("idUser");
        userName = getIntent().getStringExtra("userName");

        inputNameLayout = findViewById(R.id.inputNameLayout);
        inputEmailLayout = findViewById(R.id.inputEmailLayout);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        spinnerTipo = findViewById(R.id.spinnerTipo);
        spinnerTag = findViewById(R.id.spinnerTag);
        buttonGuardar = findViewById(R.id.buttonGuardarEntidad);
        buttonAsignarTrabajadores = findViewById(R.id.buttonAsignarTrabajadores);
        recyclerAsignaciones = findViewById(R.id.recyclerAsignaciones);

        // Configurar tipos
        ArrayAdapter<String> tiposAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"cliente", "encargado", "trabajador"}
        );
        tiposAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipo.setAdapter(tiposAdapter);

        // Configurar tags (opcional)
        tagManager = new TagManager(this, idUser);
        List<String> baseTags = new ArrayList<>();
        baseTags.add("Sin tag");
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                baseTags
        );
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTag.setAdapter(tagAdapter);

        tagManager.loadTags(tags -> {
            availableTags = new ArrayList<>(tags);
            baseTags.clear();
            baseTags.add("Sin tag");
            baseTags.addAll(availableTags);
            tagAdapter.notifyDataSetChanged();
        });

        buttonGuardar.setOnClickListener(v -> guardarEntidad());
        buttonAsignarTrabajadores.setOnClickListener(v -> showAsignarTrabajadoresDialog());

        recyclerAsignaciones.setLayoutManager(new LinearLayoutManager(this));
        asignacionesAdapter = new EncargadoAsignacionesAdapter(asignacionesList);
        recyclerAsignaciones.setAdapter(asignacionesAdapter);

        loadEncargadosConTrabajadores();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_empresa, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export_csv) {
            exportarCsv();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportarCsv() {
        if (idUser == null || idUser.isEmpty()) {
            Toast.makeText(this, "Id de usuario inválido", Toast.LENGTH_LONG).show();
            return;
        }
        CsvExporter.exportEmpresaToCsv(this, idUser, new CsvExporter.ExportCallback() {
            @Override
            public void onSuccess(String filePath, android.net.Uri fileUri) {
                Toast.makeText(EmpresaActivity.this,
                        "CSV guardado en: " + filePath,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EmpresaActivity.this,
                        "Error exportando CSV: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void guardarEntidad() {
        String nombre = editTextName.getText() != null ? editTextName.getText().toString().trim() : "";
        String email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
        String tipo = (String) spinnerTipo.getSelectedItem();
        String tagSel = (String) spinnerTag.getSelectedItem();

        boolean valid = true;
        if (nombre.isEmpty()) {
            inputNameLayout.setError("Nombre es obligatorio");
            valid = false;
        } else {
            inputNameLayout.setError(null);
        }
        if (email.isEmpty()) {
            inputEmailLayout.setError("Email es obligatorio");
            valid = false;
        } else {
            inputEmailLayout.setError(null);
        }
        if (!valid) return;

        Entidad entidad = new Entidad();
        entidad.setNombre(nombre);
        entidad.setEmail(email);
        entidad.setTipo(tipo);
        if (tagSel != null && !tagSel.equals("Sin tag")) {
            List<String> tagsList = new ArrayList<>();
            tagsList.add(tagSel);
            entidad.setTags(tagsList);
        } else {
            entidad.setTags(new ArrayList<>());
        }

        String listaNodo = tipoNodo(tipo);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("Empresa")
                .child(listaNodo);

        ref.push().setValue(entidad, (error, ref1) -> {
            if (error == null) {
                Toast.makeText(EmpresaActivity.this, "Entidad añadida a " + listaNodo, Toast.LENGTH_SHORT).show();
                limpiarFormulario();
            } else {
                Toast.makeText(EmpresaActivity.this, "Error al guardar: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @NonNull
    private String tipoNodo(String tipo) {
        switch (tipo) {
            case "cliente":
                return "clientes";
            case "encargado":
                return "encargados";
            case "trabajador":
            default:
                return "trabajadores";
        }
    }

    private void limpiarFormulario() {
        if (editTextName != null) editTextName.setText("");
        if (editTextEmail != null) editTextEmail.setText("");
        if (spinnerTipo != null) spinnerTipo.setSelection(0);
        if (spinnerTag != null) spinnerTag.setSelection(0);
    }

    private void showAsignarTrabajadoresDialog() {
        if (idUser == null || idUser.isEmpty()) {
            Toast.makeText(this, "Id de usuario inválido", Toast.LENGTH_LONG).show();
            return;
        }

        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("Empresa");

        // Primero, cargar encargados (nombres y claves)
        baseRef.child("encargados").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> encargadoNames = new ArrayList<>();
                List<String> encargadoKeys = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Entidad e = child.getValue(Entidad.class);
                    if (e != null && e.getNombre() != null) {
                        encargadoNames.add(e.getNombre());
                        encargadoKeys.add(child.getKey());
                    }
                }

                if (encargadoNames.isEmpty()) {
                    Toast.makeText(EmpresaActivity.this, "No hay encargados creados", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Segundo, cargar trabajadores (nombres)
                baseRef.child("trabajadores").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotTrab) {
                        List<String> trabajadorNames = new ArrayList<>();
                        for (DataSnapshot child : snapshotTrab.getChildren()) {
                            Entidad t = child.getValue(Entidad.class);
                            if (t != null && t.getNombre() != null) trabajadorNames.add(t.getNombre());
                        }

                        if (trabajadorNames.isEmpty()) {
                            Toast.makeText(EmpresaActivity.this, "No hay trabajadores creados", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Seleccionar encargado (lista simple)
                        String[] encItems = encargadoNames.toArray(new String[0]);
                        final int[] selectedEncIndex = {0};
                        new AlertDialog.Builder(EmpresaActivity.this)
                                .setTitle("Selecciona encargado")
                                .setSingleChoiceItems(encItems, 0, (dialog, which) -> selectedEncIndex[0] = which)
                                .setPositiveButton("Siguiente", (d, w) -> {
                                    String encKey = encargadoKeys.get(selectedEncIndex[0]);
                                    String encName = encargadoNames.get(selectedEncIndex[0]);
                                    // Cargar asignación actual para pre-seleccionar
                                    baseRef.child("encargados").child(encKey).child("trabajadores")
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapAsign) {
                                                    List<String> asignadosActual = new ArrayList<>();
                                                    for (DataSnapshot s : snapAsign.getChildren()) {
                                                        String nombre = s.getValue(String.class);
                                                        if (nombre != null) asignadosActual.add(nombre);
                                                    }
                                                    showSelectTrabajadoresForEncargado(baseRef, encKey, encName, trabajadorNames, asignadosActual);
                                                }

                                                @Override
                                                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                                                    showSelectTrabajadoresForEncargado(baseRef, encKey, encName, trabajadorNames, new ArrayList<>());
                                                }
                                            });
                                })
                                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                                .show();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(EmpresaActivity.this, "Error cargando trabajadores", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(EmpresaActivity.this, "Error cargando encargados", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSelectTrabajadoresForEncargado(DatabaseReference baseRef, String encKey, String encName,
                                                    List<String> trabajadorNames, List<String> asignadosActual) {
        String[] items = trabajadorNames.toArray(new String[0]);
        boolean[] checked = new boolean[items.length];
        for (int i = 0; i < items.length; i++) {
            checked[i] = asignadosActual.contains(items[i]);
        }

        List<String> seleccion = new ArrayList<>(asignadosActual);
        new AlertDialog.Builder(EmpresaActivity.this)
                .setTitle("Trabajadores para " + encName)
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    String name = items[which];
                    if (isChecked) {
                        if (!seleccion.contains(name)) seleccion.add(name);
                    } else {
                        seleccion.remove(name);
                    }
                })
                .setPositiveButton("Guardar", (d, w) -> {
                    // Guardar como lista simple de nombres
                    baseRef.child("encargados").child(encKey).child("trabajadores").setValue(seleccion, (error, ref) -> {
                        if (error == null) {
                            Toast.makeText(EmpresaActivity.this, "Asignación guardada", Toast.LENGTH_SHORT).show();
                            loadEncargadosConTrabajadores();
                        } else {
                            Toast.makeText(EmpresaActivity.this, "Error al guardar: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .show();
    }

    private void loadEncargadosConTrabajadores() {
        if (idUser == null || idUser.isEmpty()) return;
        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("Empresa");
        baseRef.child("encargados").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<EncargadoAsignacionesAdapter.EncargadoAsignacion> nuevos = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    Entidad e = child.getValue(Entidad.class);
                    String nombreEnc = e != null ? e.getNombre() : null;
                    List<String> trabs = new ArrayList<>();
                    com.google.firebase.database.DataSnapshot trabSnap = child.child("trabajadores");
                    for (com.google.firebase.database.DataSnapshot t : trabSnap.getChildren()) {
                        String n = t.getValue(String.class);
                        if (n != null) trabs.add(n);
                    }
                    if (nombreEnc != null) {
                        nuevos.add(new EncargadoAsignacionesAdapter.EncargadoAsignacion(nombreEnc, trabs));
                    }
                }
                asignacionesList.clear();
                asignacionesList.addAll(nuevos);
                asignacionesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                // noop
            }
        });
    }
}