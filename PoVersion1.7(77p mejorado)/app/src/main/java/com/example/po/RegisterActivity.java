package com.example.po; // Asegúrate de que este sea tu paquete correcto

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    // Declaramos los componentes del diseño
    private TextInputEditText editTextName;
    private EditText editTextEmail, editTextPassword;
    private SwitchMaterial switchEmpresa;
    private Button buttonRegister, buttonBack;

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
        setContentView(R.layout.activity_register);



        mAuth = FirebaseAuth.getInstance();

        // Enlazamos las variables con los IDs del XML
        editTextName = findViewById(R.id.editName);
        editTextEmail = findViewById(R.id.editEmail);
        editTextPassword = findViewById(R.id.editPass);
        switchEmpresa = findViewById(R.id.switchEmpresa);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonBack = findViewById(R.id.buttonGoBack);

        // Configurar el listener para el botón de registro
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nameCheck = editTextName.getText().toString().trim();
                String emailCheck = editTextEmail.getText().toString();
                String passCheck = editTextPassword.getText().toString();
                boolean isBusiness = switchEmpresa.isChecked();

                if(nameCheck.isEmpty() || emailCheck.isBlank() || passCheck.isEmpty()){
                    Toast.makeText(RegisterActivity.this, "Error: todos los datos son obligatorios.",
                            Toast.LENGTH_SHORT).show();

                }else {
                    check(nameCheck, emailCheck, passCheck, isBusiness);

                }

            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();


            }
        });
    }

    void check(String name, String email, String password, boolean isBusiness){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");

                            FirebaseUser user = mAuth.getCurrentUser();
                            String idUser = user.getUid().toString();

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference myRef = database.getReference("Users");

                            //estructura interna, usuario, calendario, lista de deseos;
                            HashMap<String, Object> userData = new HashMap<>();
                            HashMap<String, Object> dataMap = new HashMap<>();


                            userData.put("name", name);
                            userData.put("business", isBusiness);




                            dataMap.put("user", userData);


                            myRef.child(idUser).setValue(dataMap);

                            finish();



                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Authentication failed:" + task.getException().toString(),
                                    Toast.LENGTH_SHORT).show();



                        }
                    }
                });
    }
}