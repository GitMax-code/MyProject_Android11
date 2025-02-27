package com.example.myproject_android11;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.myproject_android11.model.User;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private HashMap userMap;
    private User user;

    TextView  mainTopTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //FirebaseAuth.getInstance().signOut();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

    }

    public void onTestButtonClicked(View view){
        // Créer une intention pour démarrer SecondActivity
        Intent intent = new Intent(MainActivity.this, TestActivity.class);
        // Démarrer l'activité
        startActivity(intent);
    }

    public void onJSONButtonClicked(View view){
        // Créer une intention pour démarrer SecondActivity
        Intent intent = new Intent(MainActivity.this, DataListActivity.class);
        // Démarrer l'activité
        startActivity(intent);
    }

    @Override
    public void onStart(){
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            getUser(mAuth.getCurrentUser().getUid());
            mainTopTextView = findViewById(R.id.mainTopTextView);
            //mainTopTextView.setText(String.format("%s %s", userMap.get("name").toString(), userMap.get("commune").toString()));
            //int size = userMap.size();
            //mainTopTextView.setText(String.format("%d",size));

        }
    }

    public void onLogoutButtonClicked(View view){
        Intent intent = new Intent(MainActivity.this, LogoutActivity.class);
        // Démarrer l'activité
        startActivity(intent);
        finish();
    }

    public void onMapButtonClicked(View view){
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
    }

    public void onGroupButtonClicked(View view){
        Intent intent = new Intent(MainActivity.this, ListGroupActivity.class);
        startActivity(intent);
    }

    public void getUser(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);
        Log.d(TAG,"getUser : User id [" + userId + "]");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "getUser : DocumentSnapshot data: " + document.getData());
                        //user = (HashMap<String, Object>) document.getData();
                        //Get an object of type UserPOJO
                        User user = document.toObject(User.class);
                        mainTopTextView.setText(String.format("%s %s", user.getName(), user.getCommune()));
                    } else {
                        Log.d(TAG, "getUser :  No such document");
                    }
                } else {
                    Log.d(TAG, "getUser : get failed with ", task.getException());
                }
            }
        });
    }


}