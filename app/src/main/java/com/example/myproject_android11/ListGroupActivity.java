package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

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
                String selectedItem = itemList.get(position);
                Log.d("ListGroupActivity", "Item cliqué: " + selectedItem);
                Intent intent = new Intent(ListGroupActivity.this, ListUserNotAddedActivity.class);
                intent.putExtra("id", selectedItem);
                startActivity(intent);
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

        db.collection("groups")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("ListGroupActivity", "Données récupérées depuis Firestore");
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("ListGroupActivity", "Aucun groupe trouvé");
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String groupName = document.getString("name");
                            String creatorId = document.getString("creator");

                            if (groupName != null && creatorId != null && creatorId.equals(userId)) {
                                Log.d("ListGroupActivity", "Ajout du groupe: " + groupName);
                                itemList.add(groupName);
                            } else {
                                Log.d("ListGroupActivity", "Groupe ignoré: " + groupName + " | creatorId: " + creatorId);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("ListGroupActivity", "Nombre total de groupes affichés: " + itemList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("ListGroupActivity", "Erreur lors de la récupération des groupes", e);
                });

        listView.postDelayed(() -> {
            Log.d("ListGroupActivity", "Final itemList content: " + itemList);
        }, 2000);
    }

    public void onCreateGroupButtonClicked(View v) {
        Intent intent = new Intent(ListGroupActivity.this, CreateGroupActivity.class);
        startActivity(intent);
    }
}
