package com.example.po;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    // Declaración de los componentes de la UI
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private TextView textViewForgotPassword;

    private FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            currentUser.reload();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();




        // Enlazar los componentes de la UI con sus IDs del XML
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        // Configurar el listener para el botón de Iniciar Sesión
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailCheck = editTextEmail.getText().toString();
                String passCheck = editTextPassword.getText().toString();

                if(emailCheck.isBlank() || passCheck.isEmpty()){
                    Toast.makeText(MainActivity.this, "Error: los datos son obligatorios.",
                            Toast.LENGTH_SHORT).show();

                }else {
                    checkSingIn(emailCheck, passCheck);
                }            }
        });

        // Configurar el listener para el botón de Registrarse
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navega a la pantalla de registro
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Configurar el listener para el texto de Olvidé mi contraseña
        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lógica para la recuperación de contraseña (simulación)
                Intent intent = new Intent(MainActivity.this, RemakePass.class);
                startActivity(intent);            }
        });
    }

    void checkSingIn(String email, String password){

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            String idUser = user.getUid();
                            cargarEventos(idUser);




                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
    }

    private void cargarEventos(String idUser) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(idUser)
                .child("ListEvents");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Evento> listaEventos = new ArrayList<>();

                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    String fecha = eventSnap.child("fecha").getValue(String.class);
                    String nombre = eventSnap.child("nombre").getValue(String.class);
                    String notas = eventSnap.child("notas").getValue(String.class);
                    String id = eventSnap.child("id").getValue(String.class);
                    Integer tipoRecordatorio = eventSnap.child("tipoRecordatorio").getValue(Integer.class);


                    // Recuperar listaDeseo
                    List<Producto> productos = new ArrayList<>();
                    for (DataSnapshot productoSnap : eventSnap.child("listaDeseo").getChildren()) {
                        String titulo = productoSnap.child("titulo").getValue(String.class);
                        String url = productoSnap.child("url").getValue(String.class);
                        productos.add(new Producto(titulo, url));
                    }

                    // Recuperar tags del evento
                    List<String> tags = new ArrayList<>();
                    for (DataSnapshot tagSnap : eventSnap.child("tags").getChildren()) {
                        String tag = tagSnap.getValue(String.class);
                        if (tag != null) {
                            tags.add(tag);
                        }
                    }

                    // Construir evento incluyendo tipoRecordatorio y tags (si existen)
                    Evento evento = new Evento(fecha, nombre, notas, productos, id);
                    if (tipoRecordatorio != null) {
                        evento.setTipoRecordatorio(tipoRecordatorio);
                    }
                    // setTags asegura que "todos" esté presente
                    evento.setTags(tags);
                    listaEventos.add(evento);
                }

                //analizar los datos capturados.
                Log.d("Firebase", "Eventos cargados: " + listaEventos.size());
                for (Evento e : listaEventos) {
                    Log.d("Firebase", "Evento: " + e.getNombre() + " (" + e.getFecha() + ")");
                    Log.d("Firebase", "Notas: " + e.getNotas());
                    for (Producto p : e.getListaDeseo()) {
                        Log.d("Firebase", "   Producto: " + p.getTitulo());
                    }
                }
                //


                ArrayList<Evento> listaEventosPutExtra = new ArrayList<>(listaEventos);

                Intent currentUser = new Intent(MainActivity.this, HomeActivity.class);

                currentUser.putParcelableArrayListExtra("listaEventos", listaEventosPutExtra);
                currentUser.putExtra("idUser", idUser);

                startActivity(currentUser);



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error: " + error.getMessage());
            }
        });
    }

}