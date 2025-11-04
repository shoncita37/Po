package com.example.po;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EventDetailActivity extends AppCompatActivity {

    public static final int RESULT_DELETED = 101; // Un código de resultado personalizado para la eliminación

    private TextView eventName, eventDate, eventNotes;
    private Button btnWishlist, btnEdit, btnDelete, btnCancelar;
    private RecyclerView recyclerViewEventTags;
    private Evento eventoActual;

    private String userId,posicionDelEvento;
    private final ActivityResultLauncher<Intent> editEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        eventoActual = result.getData().getParcelableExtra("evento_resultado", Evento.class);
                    } else {
                        eventoActual = result.getData().getParcelableExtra("evento_resultado");
                    }

                    if (eventoActual != null) {
                        // Asegurarse de que los tags no sean nulos
                        if (eventoActual.getTags() == null) {
                            eventoActual.setTags(new ArrayList<>());
                            eventoActual.addTag("todos");
                        }
                        
                        actualizarVistas();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("evento_actualizado", eventoActual);
                        resultIntent.putExtra("posicion_evento", posicionDelEvento);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Inicializar vistas
        eventName = findViewById(R.id.detailEventName);
        eventDate = findViewById(R.id.detailEventDate);
        eventNotes = findViewById(R.id.detailEventNotes);
        recyclerViewEventTags = findViewById(R.id.recyclerViewEventTags);

        btnWishlist = findViewById(R.id.btnWishlist);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnCancelar = findViewById(R.id.btnCancelarWishlist);

        // Obtener datos del intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            eventoActual = getIntent().getParcelableExtra("evento_seleccionado", Evento.class);
        } else {
            eventoActual = getIntent().getParcelableExtra("evento_seleccionado");
        }
        
        posicionDelEvento = getIntent().getStringExtra("posicion_evento");
        userId = getIntent().getStringExtra("userId");

        // Verificar y corregir los tags si es necesario
        if (eventoActual != null && eventoActual.getTags() == null) {
            eventoActual.setTags(new ArrayList<>());
            eventoActual.addTag("todos");
        }

        if (eventoActual != null) {
            actualizarVistas();
        }

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, AddEventActivity.class);
            
            // Asegurarse de que los tags no sean nulos antes de pasar el evento
            if (eventoActual.getTags() == null) {
                eventoActual.setTags(new ArrayList<>());
                eventoActual.addTag("todos");
            }
            
            intent.putExtra("evento_a_editar", eventoActual);
            intent.putExtra("userId", userId);
            intent.putExtra("index-evento", posicionDelEvento);
            editEventLauncher.launch(intent);
        });

        // Al presionar el botón de eliminar, mostramos el diálogo
        btnDelete.setOnClickListener(v -> mostrarDialogoDeConfirmacion());


        btnWishlist.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, WishlistActivity.class);
            // Enviamos el nombre del evento para usarlo como título en la siguiente pantalla

            ArrayList<Producto> listaEventosPutExtra = new ArrayList<>(eventoActual.getListaDeseo());

            intent.putParcelableArrayListExtra("lista_productos", listaEventosPutExtra);
            intent.putExtra("nombre_evento", eventoActual.getNombre());
            intent.putExtra("index-evento", posicionDelEvento);
            intent.putExtra("userId",userId);
            startActivity(intent);
        });
        btnCancelar.setOnClickListener(v ->
                finish()
                );

    }

    private void mostrarDialogoDeConfirmacion() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este evento? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Cancelar el recordatorio asociado al evento
                    cancelarRecordatorio(eventoActual);
                    
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("posicion_evento_eliminado", posicionDelEvento);
                    setResult(RESULT_DELETED, resultIntent);
                    Toast.makeText(this, "Evento eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void actualizarVistas() {
        eventName.setText(eventoActual.getNombre());
        eventDate.setText(eventoActual.getFecha());
        eventNotes.setText(eventoActual.getNotas());
        
        // Configurar RecyclerView para mostrar tags
        recyclerViewEventTags.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        TagDisplayAdapter tagAdapter = new TagDisplayAdapter(this, eventoActual.getTags());
        recyclerViewEventTags.setAdapter(tagAdapter);
    }
    
    private void cancelarRecordatorio(Evento evento) {
        try {
            // Extraer la fecha del evento
            String fecha = evento.getFecha();
            String[] fechaCadena = fecha.split("/");
            int year = Integer.parseInt(fechaCadena[2]);
            int month = Integer.parseInt(fechaCadena[1]);
            int day = Integer.parseInt(fechaCadena[0]);
            
            // Crear un intent similar al que se usó para programar la alarma
            Intent intent = new Intent(this, RecordatorioReceiver.class);
            intent.putExtra("nombreEvento", evento.getNombre());
            
            // Generar el mismo ID que se usó al crear el recordatorio
            int requestCode = (evento.getNombre() + year + month + day).hashCode();
            
            // Crear un PendingIntent con el mismo requestCode
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Obtener el AlarmManager y cancelar la alarma
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            // Manejar cualquier error que pueda ocurrir
            Toast.makeText(this, "Error al cancelar el recordatorio", Toast.LENGTH_SHORT).show();
        }
    }
}