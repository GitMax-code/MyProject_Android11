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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ListUserAddedActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<String> userList;
    private ArrayList<String> userIdList;
    private ArrayAdapter<String> adapter;
    private HashSet<String> addedUserIds;
    private String selectedUserId = null; // Stocke l'ID de l'utilisateur sélectionné
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String groupId; // L'ID du groupe récupéré depuis la page précédente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_user_added);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialiser Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();



        // Initialisation des vues et de la liste
        listView = findViewById(R.id.listViewListUserAdded);
        userList = new ArrayList<>();
        userIdList = new ArrayList<>();
        addedUserIds = new HashSet<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        listView.setAdapter(adapter);
    }
    @Override
    protected void onStart() {
        super.onStart();

        // Récupérer l'ID du groupe depuis l'Intent envoyé par ListGroupActivity
        groupId = getIntent().getStringExtra("id");
        if (groupId == null) {
            Toast.makeText(this, "Erreur : Aucun groupe sélectionné", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Charger les utilisateurs qui sont déjà dans le groupe
        fetchUsersFromUserGroups(groupId);

        // Gérer le clic sur un utilisateur (stocke son ID)
        listView.setOnItemClickListener((parent, view, position, id) -> {

        });
    }





    private void fetchUsersFromUserGroups(String groupId) {
        db.collection("user_groups")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("userId");
                        if (userId != null) {
                            userIds.add(userId);
                        }
                    }

                    Log.d("DEBUG", "UserIds trouvés : " + userIds.toString()); // Vérifiez cette ligne
                    fetchUsersByIds(userIds);
                })
                .addOnFailureListener(e -> {
                    Log.e("ERROR", "Erreur lors de la récupération des user_groups", e);
                });
    }

    private void fetchUsersByIds(List<String> userIds) {
        if (userIds.isEmpty()) {
            Log.d("DEBUG", "Aucun userId trouvé.");
            return;
        }

        db.collection("users")
                .whereIn(FieldPath.documentId(), userIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userName = document.getString("name");
                        if (userName != null) {
                            userList.add(userName);
                        }
                    }

                    Log.d("DEBUG", "Utilisateurs ajoutés à la ListView : " + userList.toString()); // Vérifiez cette ligne
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("ERROR", "Erreur lors de la récupération des utilisateurs", e);
                });
    }
    public void onAddUserGroupButtonClicked(View view) {
        Intent intent = new Intent(ListUserAddedActivity.this, ListUserNotAddedActivity.class);
        intent.putExtra("id", groupId); // Passer l'ID du groupe à ListUserNotAddedActivity
        startActivity(intent);
    }

    public void onChatButtonClicked(View view) {
        Intent intent = new Intent(ListUserAddedActivity.this, ChatActivity.class);
        intent.putExtra("id", groupId); // Passer l'ID du groupe à ChatActivity
        startActivity(intent);
    }




}