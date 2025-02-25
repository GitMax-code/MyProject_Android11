package com.example.myproject_android11;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireStoreActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fire_store);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();
    }


    public void createGroup(String groupId, String groupName, String meetingTime, String location, List<String> members) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> group = new HashMap<>();
        group.put("name", groupName);
        group.put("meeting_time", meetingTime);
        group.put("location", location);
        group.put("members", members);

        db.collection("groups").document(groupId)
                .set(group)
                .addOnSuccessListener(aVoid -> System.out.println("Groupe ajouté avec succès !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de l'ajout du groupe : " + e));
    }

    // Ajouter un utilisateur
    public void createUser(String userId, String name, String email) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("groups", Collections.emptyList());  // Initialement, pas de groupes

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur ajouté !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de l'ajout de l'utilisateur : " + e));
    }

    // Ajouter un utilisateur à un groupe
    public void addUserToGroup(String userId, String groupId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ajouter le groupe à la liste des groupes de l'utilisateur
        db.collection("users").document(userId)
                .update("groups", FieldValue.arrayUnion(groupId))
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur ajouté au groupe !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de l'ajout au groupe : " + e));

        // Ajouter l'utilisateur à la liste des membres du groupe
        db.collection("groups").document(groupId)
                .update("members", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur ajouté au groupe (côté groupe) !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de l'ajout au groupe (côté groupe) : " + e));
    }


    public void getUserGroups(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        System.out.println("Groupes de l'utilisateur : " + documentSnapshot.get("groups"));
                    } else {
                        System.out.println("Aucun utilisateur trouvé !");
                    }
                })
                .addOnFailureListener(e -> System.err.println("Erreur lors de la récupération des groupes : " + e));
    }

    public void getGroupMembers(String groupId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("groups").document(groupId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        System.out.println("Membres du groupe : " + documentSnapshot.get("members"));
                    } else {
                        System.out.println("Aucun groupe trouvé !");
                    }
                })
                .addOnFailureListener(e -> System.err.println("Erreur lors de la récupération des membres : " + e));
    }

    public void removeUserFromGroup(String userId, String groupId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId)
                .update("groups", FieldValue.arrayRemove(groupId))
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur retiré du groupe !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de la suppression de l'utilisateur du groupe : " + e));

        db.collection("groups").document(groupId)
                .update("members", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(aVoid -> System.out.println("Utilisateur retiré du groupe (côté groupe) !"))
                .addOnFailureListener(e -> System.err.println("Erreur lors de la suppression du membre (côté groupe) : " + e));
    }

}