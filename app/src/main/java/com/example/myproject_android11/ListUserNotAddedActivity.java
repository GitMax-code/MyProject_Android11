package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myproject_android11.model.UserGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;

public class ListUserNotAddedActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<String> userList;
    private ArrayList<String> userIdList;
    private ArrayAdapter<String> adapter;
    private HashSet<String> addedUserIds;
    private String selectedUserId = null; // Stocke l'ID de l'utilisateur sélectionné
    private String groupId; // L'ID du groupe récupéré depuis la page précédente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_user_not_added);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Récupérer l'ID du groupe depuis l'Intent envoyé par ListGroupActivity
        groupId = getIntent().getStringExtra("id");

        if (groupId == null) {
            Toast.makeText(this, "Erreur : Aucun groupe sélectionné", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialisation des vues et de la liste
        listView = findViewById(R.id.listViewListUser);
        userList = new ArrayList<>();
        userIdList = new ArrayList<>();
        addedUserIds = new HashSet<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Charger les utilisateurs qui sont déjà dans le groupe
        fetchUsersInGroup();

        // Gérer le clic sur un utilisateur (stocke son ID)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedUserId = userIdList.get(position);
            Toast.makeText(this, "Utilisateur sélectionné: " + userList.get(position), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchUsersInGroup() {
        addedUserIds.clear();

        db.collection("user_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("userId");
                        if (userId != null) {
                            addedUserIds.add(userId);
                        }
                    }
                    Log.d("DEBUG", "Utilisateurs déjà ajoutés: " + addedUserIds.toString()); // Vérification
                    fetchUsers(); // Charger les utilisateurs après filtrage
                })
                .addOnFailureListener(e -> Log.e("ListUserActivity", "Erreur récupération users groupe", e));
    }


    private void fetchUsers() {
        userList.clear();
        userIdList.clear();

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userName = document.getString("name");
                        String userId = document.getId();

                        // Vérifier si l'utilisateur n'est pas déjà dans le groupe
                        if (userName != null) {
                            if (!addedUserIds.contains(userId)) {
                                userList.add(userName);
                                userIdList.add(userId);
                            } else {
                                Log.d("DEBUG", "Utilisateur filtré (déjà dans groupe) : " + userName);
                            }
                        }
                    }
                    Log.d("DEBUG", "Utilisateurs affichés: " + userList.toString());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ListUserActivity", "Erreur lors de la récupération des utilisateurs", e));
    }


    public void onAddUserGroupButtonClicked(View view) {
        if (selectedUserId == null) {
            Toast.makeText(this, "Veuillez sélectionner un utilisateur", Toast.LENGTH_SHORT).show();
            return;
        }

        if (groupId == null) {
            Toast.makeText(this, "Veuillez sélectionner un groupe", Toast.LENGTH_SHORT).show();
            return;
        }

        UserGroup userGroup = new UserGroup(null, selectedUserId, groupId);

        db.collection("user_groups")
                .add(userGroup)
                .addOnSuccessListener(documentReference -> {
                    String userGroupId = documentReference.getId();
                    userGroup.setId(userGroupId); // Ajouter l'ID généré
                    Toast.makeText(this, "Utilisateur ajouté au groupe!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                });
    }



}
