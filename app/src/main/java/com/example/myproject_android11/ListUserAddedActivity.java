package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListUserAddedActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayList<String> userList;
    private ArrayList<String> userIdList;
    private ArrayAdapter<String> adapter;
    private HashMap<String, Boolean> presenceMap; // Pour stocker la présence de chaque utilisateur
    private Button saveButton; // Bouton pour enregistrer la présence
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
        presenceMap = new HashMap<>(); // Initialiser la map de présence

        // Utiliser un adaptateur personnalisé pour afficher les CheckBox
        adapter = new ArrayAdapter<String>(this, R.layout.user_item, R.id.userName, userList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                CheckBox checkBox = view.findViewById(R.id.checkBox);
                String userId = userIdList.get(position);

                // Définir l'état de la CheckBox
                checkBox.setChecked(presenceMap.get(userId));

                // Gérer le clic sur la CheckBox
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    presenceMap.put(userId, isChecked); // Mettre à jour la présence dans la map
                });

                return view;
            }
        };
        listView.setAdapter(adapter);

        // Initialiser le bouton "Enregistrer"
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> savePresenceStatus());
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
    }

    public void onMapButtonClicked(View view){
        Intent intent = new Intent(ListUserAddedActivity.this, MapActivity.class);
        intent.putExtra("group_id", groupId); // Passer l'ID du groupe à ChatActivity
        startActivity(intent);
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

                    Log.d("DEBUG", "UserIds trouvés : " + userIds.toString());
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
                    userIdList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userName = document.getString("name");
                        String userId = document.getId();
                        if (userName != null) {
                            userList.add(userName);
                            userIdList.add(userId);
                            presenceMap.put(userId, false); // Initialiser la présence à false par défaut
                        }
                    }

                    Log.d("DEBUG", "Utilisateurs ajoutés à la ListView : " + userList.toString());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("ERROR", "Erreur lors de la récupération des utilisateurs", e);
                });
    }

    private void savePresenceStatus() {
        for (int i = 0; i < userIdList.size(); i++) {
            String userId = userIdList.get(i);
            boolean isPresent = presenceMap.get(userId); // Récupérer la présence depuis la map

            // Mettre à jour la présence dans Firestore
            updatePresenceStatus(userId, isPresent);
        }

        Toast.makeText(this, "Présence enregistrée", Toast.LENGTH_SHORT).show();
    }

    private void updatePresenceStatus(String userId, boolean isPresent) {
        // Créer un objet pour stocker la présence
        Map<String, Object> presenceData = new HashMap<>();
        presenceData.put("userId", userId);
        presenceData.put("groupId", groupId);
        presenceData.put("isPresent", isPresent);

        // Mettre à jour ou créer un document dans la collection "presence"
        db.collection("presence")
                .document(userId + "_" + groupId) // ID unique pour chaque utilisateur-groupe
                .set(presenceData)
                .addOnSuccessListener(aVoid -> Log.d("DEBUG", "Présence mise à jour pour l'utilisateur: " + userId))
                .addOnFailureListener(e -> Log.e("ERROR", "Erreur lors de la mise à jour de la présence", e));
    }

    public void onChatButtonClicked(View view) {
        Intent intent = new Intent(ListUserAddedActivity.this, ChatActivity.class);
        intent.putExtra("id", groupId); // Passer l'ID du groupe à ChatActivity
        startActivity(intent);
    }
}