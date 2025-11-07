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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
}