package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.myproject_android11.model.UserGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.myproject_android11.model.Group;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CreateGroupActivity extends AppCompatActivity {

    EditText NameGroup;
    EditText TimeGroup;
    Spinner dayOfWeekSpinner;
    String selectedDay;
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
        dayOfWeekSpinner = findViewById(R.id.dayOfWeekSpinner);

        setupDayOfWeekSpinner();
    }

    private void setupDayOfWeekSpinner() {
        // Tableau des jours de la semaine (vous pouvez le traduire si besoin)
        String[] daysOfWeek = new String[]{
                "Select a day", // Premier élément comme hint
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Sunday"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                daysOfWeek
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dayOfWeekSpinner.setAdapter(adapter);

        // Écouteur pour la sélection
        dayOfWeekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Ignorer le premier élément "Select a day"
                    selectedDay = parent.getItemAtPosition(position).toString();
                } else {
                    selectedDay = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDay = null;
            }
        });
    }

    public void createGroupButton(View view) {
        String name = NameGroup.getText().toString();
        String time = TimeGroup.getText().toString();

        if (name.isEmpty() || selectedDay == null || time.isEmpty()) {
            Toast.makeText(CreateGroupActivity.this, "Veuillez remplir tous les champs et sélectionner un jour", Toast.LENGTH_SHORT).show();
            return;
        }

        createGroup(name, selectedDay, time);

        // Redirection vers MainActivity
        //Intent intent = new Intent(CreateGroupActivity.this, MainActivity.class);


    }

    // Les méthodes createGroup() et addCreatorToUserGroup() restent identiques


    // Modifiez votre méthode createGroup
    public void createGroup(String name, String day, String time) {
        Group group = new Group(null, name, mAuth.getCurrentUser().getUid(), day, time);
        db.collection("groups")
                .add(group)
                .addOnSuccessListener(documentReference -> {
                    String groupId = documentReference.getId();
                    group.setId(groupId);
                    documentReference.update("id", groupId).addOnSuccessListener(aVoid -> {
                        Toast.makeText(CreateGroupActivity.this, "Groupe créé avec succès !", Toast.LENGTH_SHORT).show();
                        addCreatorToUserGroup(mAuth.getCurrentUser().getUid(), groupId);

                        goToListGroup();

                    }).addOnFailureListener(e ->
                            Toast.makeText(CreateGroupActivity.this, "Erreur lors de l'ajout de l'ID : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateGroupActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addCreatorToUserGroup(String creatorId, String groupId) {
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



    private int convertDayOfWeek(String day) {
        switch (day.toLowerCase()) {
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            case "sunday": return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }

    private void goToListGroup(){
        Intent intent = new Intent(CreateGroupActivity.this, ListGroupActivity.class);
        startActivity(intent);
    }


}