package com.example.myproject_android11;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ListGroupActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    ArrayList<String> itemList;
    ArrayAdapter<String> adapter;

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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

    }

    protected void onStart(){
        super.onStart();
        listView = findViewById(R.id.listViewListGroup);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Initialisation de la liste et de l'adaptateur
        itemList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        //adapteradapter = new ArrayAdapter<>(this, android.R.layout.list_content, itemList);

        // Associer l'adaptateur à la ListView
        listView.setAdapter(adapter);

        setList();
    }

    public void onCreateGroupButtonClicked(View view){
        Intent intent = new Intent(ListGroupActivity.this, CreateGroupActivity.class);
        startActivity(intent);
    }

    public void setList(){
        itemList.add("Groupe 1");
        itemList.add("Groupe 2");
        adapter.notifyDataSetChanged();  // Mettre à jour l'affichage
    }
}