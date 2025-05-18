package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    EditText firstnameRegister;
    EditText emailEditText;
    EditText passwordEditText;
    //EditText communeEditText;

    private Spinner communeSpinner;
    private List<String> communeNames = new ArrayList<>();
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
        communeSpinner = findViewById(R.id.communeRegister);
        progressBar = (ProgressBar) findViewById(R.id.progressBarRegister);

        // Initialize API
        fetchCommunesFromApi();
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();


    }

    @Override
    public void onStart(){
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            //startActivity(new Intent(this, MainActivity.class));
            startActivity(new Intent(this, ListGroupActivity.class));
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
        String commune = communeSpinner.getSelectedItem().toString();

        // Validation supplémentaire
        if (commune.equals("Sélectionnez votre commune")) {
            Toast.makeText(this, "Veuillez sélectionner une commune", Toast.LENGTH_SHORT).show();
            return;
        }

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

                            //Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            Intent intent = new Intent(RegisterActivity.this, ListGroupActivity.class);
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

    private void fetchCommunesFromApi() {
        new Thread(() -> {
            String apiUrl = "https://opendata.brussels.be/api/explore/v2.1/catalog/datasets/limites-administratives-des-communes-en-region-de-bruxelles-capitale/records?limit=20";
            String jsonResponse = getJsonFromUrl(apiUrl);

            if (jsonResponse != null) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONArray results = jsonObject.getJSONArray("results");

                    // Ajouter une option vide en premier
                    communeNames.add("Sélectionnez votre commune");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject commune = results.getJSONObject(i);
                        String name = commune.getString("name_fr");
                        communeNames.add(name);
                    }

                    // Mettre à jour l'UI sur le thread principal
                    runOnUiThread(() -> setupCommuneSpinner());
                } catch (JSONException e) {
                    Log.e("RegisterActivity", "Error parsing communes", e);
                }
            }
        }).start();
    }
    // Ajoutez cette méthode (identique à celle de MapActivity)
    private String getJsonFromUrl(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonStr = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            jsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e("RegisterActivity", "Error fetching communes", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("RegisterActivity", "Error closing stream", e);
                }
            }
        }
        return jsonStr;
    }


    private void setupCommuneSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                communeNames
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        communeSpinner.setAdapter(adapter);
    }




}