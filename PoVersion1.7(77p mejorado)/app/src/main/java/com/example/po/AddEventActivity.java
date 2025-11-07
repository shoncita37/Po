package com.example.po;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity implements SelectedTagAdapter.OnTagRemovedListener {

    private TextInputEditText editTextEventName, editTextEventDate, editTextEventNotes;
    private Button btnGuardarEvento, btnCancelarEvento;
    private TextView textViewTitle;
    private Spinner spinnerRecordatorio, spinnerTags;
    private RecyclerView recyclerViewSelectedTags;
    private List<Producto> listaDeseocheck;
    private Evento eventoEditable; // Variable para guardar el evento que estamos editando
    private List<String> selectedTags;
    private SelectedTagAdapter selectedTagAdapter;
    private TagManager tagManager;
    private List<String> availableTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        // Obtener el ID del usuario actual del Intent
        // Intentar obtener userId de diferentes formas para asegurar compatibilidad
        String userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            userId = getIntent().getStringExtra("idUser");
        }

        // Inicializar TagManager con el ID del usuario
        tagManager = new TagManager(this, userId);
        selectedTags = new ArrayList<>();
        selectedTags.add("todos"); // Siempre incluir el tag "todos"
        
        // Cargar tags disponibles desde Firebase
        tagManager.loadTags(tags -> {
            availableTags = tags;
            setupTagsSpinner();
        });

        editTextEventName = findViewById(R.id.editTextEventName);
        editTextEventDate = findViewById(R.id.editTextEventDate);
        editTextEventNotes = findViewById(R.id.editTextEventNotes);
        spinnerRecordatorio = findViewById(R.id.spinnerRecordatorio);
        spinnerTags = findViewById(R.id.spinnerTags);
        recyclerViewSelectedTags = findViewById(R.id.recyclerViewSelectedTags);

        btnGuardarEvento = findViewById(R.id.btnGuardarEvento);
        btnCancelarEvento = findViewById(R.id.btnCancelarEvento);

        textViewTitle = findViewById(R.id.textViewTitle);
        
        // Configurar el spinner con las opciones de recordatorio
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.opciones_recordatorio, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordatorio.setAdapter(adapter);
        
        // Establecer la opción en blanco como predeterminada para nuevos eventos
        spinnerRecordatorio.setSelection(0);
        
        // Configurar RecyclerView para tags seleccionados
        recyclerViewSelectedTags.setLayoutManager(new LinearLayoutManager(this));
        selectedTagAdapter = new SelectedTagAdapter(this, selectedTags, this);
        recyclerViewSelectedTags.setAdapter(selectedTagAdapter);
        
        // Configurar spinner de tags
        setupTagsSpinner();
        
        // Comprobar si nos enviaron un evento para editar
        if (getIntent().hasExtra("evento_a_editar")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                eventoEditable = getIntent().getParcelableExtra("evento_a_editar", Evento.class);
            } else {
                eventoEditable = getIntent().getParcelableExtra("evento_a_editar");
            }
            
            if (eventoEditable != null) {
                modoEdicion();
            } else {
                Toast.makeText(this, "Error al cargar el evento", Toast.LENGTH_SHORT).show();
                finish(); // Cerrar la actividad si hay un error
            }
        }

        editTextEventDate.setOnClickListener(v -> mostrarDialogoDeFecha());
        btnGuardarEvento.setOnClickListener(v -> guardarCambios());
        btnCancelarEvento.setOnClickListener(v -> cancelarCambios());
    }

    private void modoEdicion() {
        textViewTitle.setText("Editar Evento");
        editTextEventName.setText(eventoEditable.getNombre());
        editTextEventDate.setText(eventoEditable.getFecha());
        editTextEventNotes.setText(eventoEditable.getNotas());
        // Siempre mostrar la opción en blanco al editar
        spinnerRecordatorio.setSelection(0);

        if(eventoEditable.getListaDeseo() != null){
            listaDeseocheck = eventoEditable.getListaDeseo();
        }else{
            listaDeseocheck = new ArrayList<>();
        }
        
        // Cargar tags del evento
        selectedTags.clear();
        selectedTags.add("todos"); // Asegurar que "todos" siempre está incluido
        
        if (eventoEditable.getTags() != null) {
            // Añadir el resto de tags, evitando duplicados
            for (String tag : eventoEditable.getTags()) {
                if (!selectedTags.contains(tag)) {
                    selectedTags.add(tag);
                }
            }
        }
        
        // Actualizar el adaptador después de cargar los tags
        if (selectedTagAdapter != null) {
            selectedTagAdapter.updateTags(selectedTags);
        }
    }
    
    private void setupTagsSpinner() {
        if (availableTags == null) {
            return; // Si los tags aún no se han cargado, salir
        }
        
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
                    setupTagsSpinner(); // Actualizar spinner
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });
    }
    
    private void showManageTagsDialog() {
        Intent intent = new Intent(this, TagsActivity.class);
        intent.putExtra("idUser", getIntent().getStringExtra("idUser"));
        startActivity(intent);
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        // Recargar los tags disponibles cuando se regresa a esta actividad
        String idUser = getIntent().getStringExtra("idUser");
        tagManager.loadTags(tags -> {
            availableTags = tags;
            setupTagsSpinner();
        });
    }
    
    @Override
    public void onTagRemoved(String tag, int position) {
        selectedTags.remove(position);
        selectedTagAdapter.updateTags(selectedTags);
        setupTagsSpinner(); // Actualizar spinner
    }
    private void cancelarCambios() {
        finish();
    }
    @SuppressLint("ScheduleExactAlarm")
    private void guardarCambios() {
        String eventName = editTextEventName.getText().toString().trim();
        String eventDate = editTextEventDate.getText().toString().trim();
        
        // Asegurarse de que los tags incluyan "todos"
        if (!selectedTags.contains("todos")) {
            selectedTags.add("todos");
        }
        String eventNotes = editTextEventNotes.getText().toString().trim();
        int tipoRecordatorio = spinnerRecordatorio.getSelectedItemPosition();

        if (eventName.isEmpty() || eventDate.isEmpty()) {
            Toast.makeText(this, "El nombre y la fecha son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Verificar que no se haya seleccionado la opción en blanco (posición 0)
        if (tipoRecordatorio == 0) {
            Toast.makeText(this, "Debes seleccionar un tipo de recordatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        Evento eventoResultado;

        if (eventoEditable != null) {
            // Si estamos editando, conservamos el ID del evento
            if(listaDeseocheck == null) {
                eventoResultado = new Evento(eventDate, eventName, eventNotes, tipoRecordatorio);
            } else {
                eventoResultado = new Evento(eventDate, eventName, eventNotes, listaDeseocheck, tipoRecordatorio);
            }
            eventoResultado.setId(eventoEditable.getId());
            eventoResultado.setTags(selectedTags); // Establecer los tags seleccionados
        } else {
            // Nuevo evento
            if(listaDeseocheck == null) {
                eventoResultado = new Evento(eventDate, eventName, eventNotes, tipoRecordatorio);
            } else {
                eventoResultado = new Evento(eventDate, eventName, eventNotes, listaDeseocheck, tipoRecordatorio);
            }
            eventoResultado.setTags(selectedTags); // Establecer los tags seleccionados
        }

        // Obtener el ID del usuario actual del Intent
        String idUser = getIntent().getStringExtra("idUser");

        if (idUser != null && !idUser.isEmpty()) {
            DatabaseReference ref;
            
            if (eventoEditable != null) {
                // Si estamos editando, actualizamos el evento existente
                ref = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(idUser)
                        .child("ListEvents")
                        .child(eventoResultado.getId());
            } else {
                // Nuevo evento
                ref = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(idUser)
                        .child("ListEvents")
                        .push();
                
                eventoResultado.setId(ref.getKey());
            }

            ref.setValue(eventoResultado)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Evento agregado correctamente", Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            // Asegurarse de que los tags no sean nulos antes de devolver el evento
            if (eventoResultado.getTags() == null) {
                eventoResultado.setTags(new ArrayList<>());
                eventoResultado.addTag("todos");
            }
            resultIntent.putExtra("evento_resultado", eventoResultado);
                        setResult(RESULT_OK, resultIntent);

                        String fecha = eventoResultado.getFecha();
                        String[] fechaCadena = fecha.split("/");

                        try {
                            int year = Integer.parseInt(fechaCadena[2]); // Corregido el orden
                            int mes = Integer.parseInt(fechaCadena[1]);
                            int dia = Integer.parseInt(fechaCadena[0]);

                            programarNotificacion(this, eventoResultado.getNombre(), year, mes, dia);
                        } catch (Exception e) {
                            Log.e("AddEventActivity", "Error al procesar fecha: " + e.getMessage());
                        }

                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al agregar evento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Fallback al comportamiento anterior si no tenemos idUser
            Intent resultIntent = new Intent();
            // Asegurarse de que los tags no sean nulos antes de devolver el evento
            if (eventoResultado.getTags() == null) {
                eventoResultado.setTags(new ArrayList<>());
                eventoResultado.addTag("todos");
            }
            resultIntent.putExtra("evento_resultado", eventoResultado);
            setResult(RESULT_OK, resultIntent);

            String fecha = eventoResultado.getFecha();
            String[] fechaCadena = fecha.split("/");

            try {
                int year = Integer.parseInt(fechaCadena[2]); // Corregido el orden
                int mes = Integer.parseInt(fechaCadena[1]);
                int dia = Integer.parseInt(fechaCadena[0]);

                programarNotificacion(this, eventoResultado.getNombre(), year, mes, dia);
            } catch (Exception e) {
                Log.e("AddEventActivity", "Error al procesar fecha: " + e.getMessage());
            }

            finish();
        }
    }

    private void mostrarDialogoDeFecha() {
        final Calendar calendario = Calendar.getInstance();
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);
        int dia = calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            editTextEventDate.setText(fechaSeleccionada);
        }, anio, mes, dia);
        datePickerDialog.show();
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private void programarNotificacion(Context context, String nombreEvento, int year, int month, int day) {
        // Obtener el tipo de recordatorio seleccionado
        int tipoRecordatorio = spinnerRecordatorio.getSelectedItemPosition();
        
        // Crear Intent para el Receiver
        Intent intent = new Intent(context, RecordatorioReceiver.class);
        intent.putExtra("nombreEvento", nombreEvento);
        
        // Generar un ID único basado en el nombre del evento y la fecha
        int requestCode = (nombreEvento + year + month + day).hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode, // ID único basado en el evento
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calcular el momento exacto (00:00 del día del evento o días antes según la selección)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // enero = 0
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // Ajustar la fecha según el tipo de recordatorio
        switch (tipoRecordatorio) {
            case 1: // El mismo día del evento
                // No se modifica la fecha
                break;
            case 2: // 1 día antes
                calendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case 3: // 3 días antes
                calendar.add(Calendar.DAY_OF_MONTH, -3);
                break;
            case 4: // 1 semana antes
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                break;
            case 5: // 2 semanas antes
                calendar.add(Calendar.DAY_OF_MONTH, -14);
                break;
            default: // No debería llegar aquí porque ya validamos
                // No se modifica la fecha
                break;
        }

        // Programar con AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

}