package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myproject_android11.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    EditText firstnameRegister;
    EditText emailEditText;
    EditText passwordEditText;
    EditText communeEditText;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firstnameRegister = findViewById(R.id.firstnameRegister);
        emailEditText = findViewById(R.id.emailAddressRegister);
        passwordEditText = findViewById(R.id.passwordRegister);
        communeEditText = findViewById(R.id.communeRegister);
        progressBar = (ProgressBar) findViewById(R.id.progressBarRegister);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();


    }

    @Override
    public void onStart(){
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }


    public void onSignInButtonClicked(View view){
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String commune = communeEditText.getText().toString();

        //Validation section
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (passwordEditText.length() < 6) {
            passwordEditText.setError("Should be greater than 6");
        }

        //authenticate user with email/password by adding complete listener
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            // there was an error
                            Toast.makeText(RegisterActivity.this, "Authentication failed." + task.getException(),
                                    Toast.LENGTH_LONG).show();
                            Log.e("MyTag", task.getException().toString());

                        } else {
                            // Récupérer l'UID de l'utilisateur après la création
                            // Create a new user whith fireStone
                            String userId = mAuth.getCurrentUser().getUid();
                            createUser(userId, firstnameRegister.getText().toString(), email, commune);

                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    // Ajouter un utilisateur
    public void createUser(String userId, String name, String email, String commune) {
        User user = new User(userId, name, email, commune, false);
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur ajouté !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de l'ajout de l'utilisateur : " + e));
    }



}