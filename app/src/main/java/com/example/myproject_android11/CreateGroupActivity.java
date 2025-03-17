package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myproject_android11.model.UserGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import com.example.myproject_android11.model.Group;

public class CreateGroupActivity extends AppCompatActivity {

    EditText NameGroup;
    EditText TimeGroup;
    CalendarView calendarView;
    String date;
    FirebaseAuth mAuth;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_group);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        NameGroup = findViewById(R.id.NameGroup);
        TimeGroup = findViewById(R.id.TimeGroupe);
        calendarView = findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener((calendarView, year, month, dayOfMonth) -> {
            date = dayOfMonth + "/" + (month + 1) + "/" + year;  // Mois commence à 0, donc +1
        });
    }

    public void createGroupButton(View view) {

        String name = NameGroup.getText().toString();
        String time = TimeGroup.getText().toString();



        if (name.isEmpty() || date.isEmpty() || date == null || time.isEmpty()) {
            Toast.makeText(CreateGroupActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        createGroup(name, date, time);

       // Créer une intention pour démarrer SecondActivity
       Intent intent = new Intent(CreateGroupActivity.this, MainActivity.class);
       // Démarrer l'activité
       startActivity(intent);
    }
    public void createGroup(String name, String date, String time) {
        Group group = new Group( null, name, mAuth.getCurrentUser().getUid(), date, time);
        db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();
                    group.setId(groupId);
                    documentReference.update("id", groupId).addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateGroupActivity.this, "Groupe créé avec succès !", Toast.LENGTH_SHORT).show();
                        addCreatorToUserGroup(mAuth.getCurrentUser().getUid(), groupId);
                    }).addOnFailureListener(e ->
                            Toast.makeText(CreateGroupActivity.this, "Erreur lors de l'ajout de l'ID : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateGroupActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCreatorToUserGroup(String creatorId, String groupId) {
        // 🔹 Créer un UserGroup pour le créateur
        UserGroup userGroup = new UserGroup(null, creatorId, groupId);

        db.collection("user_groups")
                .add(userGroup)
                .addOnSuccessListener(documentReference -> {
                    String userGroupId = documentReference.getId();
                    documentReference.update("id", userGroupId);
                    Log.d("CreateGroupActivity", "Créateur ajouté au groupe avec userGroupId : " + userGroupId);
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateGroupActivity", "Erreur lors de l'ajout du créateur au groupe", e);
                });
    }

}