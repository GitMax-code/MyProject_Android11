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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListOtherGroupActivity extends AppCompatActivity {
    private ListView listView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<String> itemList;
    private ArrayAdapter<String> adapter;
    private String userId;
    private ArrayList<String> groupIdList;
    private String selectedGroupId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_other_group);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        listView = findViewById(R.id.listViewListGroup);
        itemList = new ArrayList<>();
        groupIdList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemList);
        listView.setAdapter(adapter);

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            Log.d("ListOtherGroupActivity", "User ID: " + userId);
        } else {
            Log.e("ListOtherGroupActivity", "User not logged in");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedGroupId = groupIdList.get(position);
            Toast.makeText(this, "Selected group: " + selectedGroupId, Toast.LENGTH_SHORT).show();
        });

        loadAvailableGroups();
    }

    private void loadAvailableGroups() {
        // 1. Get groups the user has already joined
        db.collection("user_groups")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(userGroupsSnapshot -> {
                    List<String> joinedGroupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : userGroupsSnapshot) {
                        String groupId = document.getString("groupId");
                        if (groupId != null) {
                            joinedGroupIds.add(groupId);
                        }
                    }

                    // 2. Get all groups and filter out joined ones
                    db.collection("groups")
                            .get()
                            .addOnSuccessListener(groupsSnapshot -> {
                                itemList.clear();
                                groupIdList.clear();

                                for (QueryDocumentSnapshot document : groupsSnapshot) {
                                    String groupName = document.getString("name");
                                    String groupId = document.getId();

                                    if (groupName != null && !joinedGroupIds.contains(groupId)) {
                                        itemList.add(groupName);
                                        groupIdList.add(groupId);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ListOtherGroupActivity", "Error loading groups", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ListOtherGroupActivity", "Error checking user groups", e);
                });
    }

    public void onJoinGroupButtonClick(View view) {
        if (selectedGroupId == null) {
            Toast.makeText(this, "Please select a group first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add user to the selected group
        HashMap<String, Object> userGroup = new HashMap<>();
        userGroup.put("userId", userId);
        userGroup.put("groupId", selectedGroupId);

        db.collection("user_groups")
                .add(userGroup)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Successfully joined group", Toast.LENGTH_SHORT).show();
                    loadAvailableGroups(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join group", Toast.LENGTH_SHORT).show();
                    Log.e("ListOtherGroupActivity", "Error joining group", e);
                });

        Intent intent = new Intent(this, ListGroupActivity.class);
        startActivity(intent);
    }
}