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

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialisation des vues et de la liste
        listView = findViewById(R.id.listViewListUserNotAdded);
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

        // Récupérer l'ID du groupe depuis l'Intent
        groupId = getIntent().getStringExtra("id");

        if (groupId == null) {
            Toast.makeText(this, "Erreur : Aucun groupe sélectionné", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Charger les utilisateurs déjà dans le groupe
        fetchUsersInGroup();
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
                            addedUserIds.add(userId); // Ajouter l'ID de l'utilisateur à la liste des utilisateurs déjà dans le groupe
                        }
                    }

                    Log.d("DEBUG", "Utilisateurs déjà dans le groupe : " + addedUserIds.toString());
                    fetchUsers(); // Charger tous les utilisateurs après avoir récupéré ceux déjà dans le groupe
                })
                .addOnFailureListener(e -> {
                    Log.e("ERROR", "Erreur lors de la récupération des utilisateurs du groupe", e);
                });
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
                        if (userName != null && !addedUserIds.contains(userId)) {
                            userList.add(userName);
                            userIdList.add(userId);
                        }
                    }

                    Log.d("DEBUG", "Utilisateurs affichés (non ajoutés au groupe) : " + userList.toString());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("ERROR", "Erreur lors de la récupération des utilisateurs", e);
                });
    }

    public void onAddUserGroupButtonClicked(View view) {
        Intent intent = new Intent(ListUserNotAddedActivity.this, ListUserNotAddedActivity.class);
        intent.putExtra("id", groupId); // Passer l'ID du groupe à ListUserNotAddedActivity
        startActivity(intent);
    }




}
