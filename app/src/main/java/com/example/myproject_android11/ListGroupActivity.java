package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ListGroupActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_group);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        listView = findViewById(R.id.listViewListGroup);

        if (mAuth.getCurrentUser() == null) {
            Log.e("ListGroupActivity", "Utilisateur non connecté, redirection vers LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();
        Log.d("ListGroupActivity", "Utilisateur connecté: " + userId);

        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listView.setAdapter(adapter);
        Log.d("ListGroupActivity", "Adapter initialisé et attaché à ListView");

        setYourList();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ListGroupActivity", "onItemClick appelé");
                String selectedItem = itemList.get(position); // Récupère le nom du groupe
                Log.d("ListGroupActivity", "Item cliqué: " + selectedItem);

                // Récupérer l'ID réel du groupe à partir de Firestore
                db.collection("groups")
                        .whereEqualTo("name", selectedItem) // Recherche par nom du groupe
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // Récupérer le premier document correspondant
                                QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                                String groupId = document.getId(); // Récupérer l'ID du document

                                // Démarrer ListUserAddedActivity avec l'ID du groupe
                                Intent intent = new Intent(ListGroupActivity.this, ListUserAddedActivity.class);
                                intent.putExtra("id", groupId); // Passer l'ID réel du groupe
                                startActivity(intent);
                            } else {
                                Toast.makeText(ListGroupActivity.this, "Groupe non trouvé", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ListGroupActivity", "Erreur lors de la récupération de l'ID du groupe", e);
                        });
            }
        });
    }

    public void setYourList() {
        Log.d("ListGroupActivity", "setYourList() appelé");
        itemList.clear();

        if (userId == null) {
            Log.e("ListGroupActivity", "userId est null, arrêt du chargement des groupes");
            return;
        }

        Log.d("ListGroupActivity", "ID de l'utilisateur connecté: " + userId);

        // Étape 1 : Récupérer les groupes auxquels l'utilisateur est inscrit
        db.collection("user_groups")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(userGroupsSnapshot -> {
                    Log.d("ListGroupActivity", "Nombre de documents trouvés dans user_group: " + userGroupsSnapshot.size());
                    if (userGroupsSnapshot.isEmpty()) {
                        Log.d("ListGroupActivity", "Aucun document trouvé pour cet utilisateur dans user_group");
                        Toast.makeText(this, "L'utilisateur n'est inscrit à aucun groupe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> joinedGroupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : userGroupsSnapshot) {
                        String groupId = document.getString("groupId");
                        Log.d("ListGroupActivity", "Document trouvé: " + document.getId() + ", groupId: " + groupId);
                        if (groupId != null) {
                            joinedGroupIds.add(groupId);
                        }
                    }

                    // Étape 2 : Récupérer les détails des groupes
                    db.collection("groups")
                            .whereIn("id", joinedGroupIds)
                            .get()
                            .addOnSuccessListener(groupsSnapshot -> {
                                Log.d("ListGroupActivity", "Nombre de groupes trouvés: " + groupsSnapshot.size());
                                if (groupsSnapshot.isEmpty()) {
                                    Log.d("ListGroupActivity", "Aucun groupe trouvé pour les IDs récupérés");
                                    Toast.makeText(this, "Aucun groupe trouvé", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                for (QueryDocumentSnapshot document : groupsSnapshot) {
                                    String groupName = document.getString("name");
                                    Log.d("ListGroupActivity", "Groupe trouvé: " + groupName);
                                    if (groupName != null) {
                                        itemList.add(groupName);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ListGroupActivity", "Erreur lors de la récupération des groupes", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ListGroupActivity", "Erreur lors de la récupération des user_group", e);
                });
    }

    public void onCreateGroupButtonClicked(View v) {
        Intent intent = new Intent(ListGroupActivity.this, CreateGroupActivity.class);
        startActivity(intent);
    }

    public void onJoinGroupButtonClicked(View v) {
        Intent intent = new Intent(ListGroupActivity.this, ListOtherGroupActivity.class);
        startActivity(intent);
    }
}
