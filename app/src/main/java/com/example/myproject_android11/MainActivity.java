package com.example.myproject_android11;

import static android.content.ContentValues.TAG;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArraySet;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myproject_android11.model.Group;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.myproject_android11.model.User;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId = null;
    TextView  mainTopTextView;
    List<Group> groupList;

    //notification
    private final String CHANNEL_ID = "personal_notifications";
    //private final int NOTIFICATION_ID = 101;
    private final int NOTIFICATION_ID = 001;
    private final int PERMISSION_REQUEST_CODE = 1001;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity.onCreate", "entree-------------------");
        //FirebaseAuth.getInstance().signOut();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        if(mAuth.getCurrentUser() != null){
            userId = mAuth.getCurrentUser().getUid();
        }

        groupList = new ArrayList<>();

        createNotificationChannel();

    }



    @Override
    public void onStart(){
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            getUser(userId);
            mainTopTextView = findViewById(R.id.mainTopTextView);
            //mainTopTextView.setText(String.format("%s %s", userMap.get("name").toString(), userMap.get("commune").toString()));
            //int size = userMap.size();
            //mainTopTextView.setText(String.format("%d",size));
            checkGroups();
        }

    }

    public void onLogoutButtonClicked(View view){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this,MainActivity.class));
    }

    public void onGroupButtonClicked(View view){
        Intent intent = new Intent(MainActivity.this, ListGroupActivity.class);
        startActivity(intent);
    }

    public void getUser(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);
        Log.d(TAG,"getUser : User id [" + userId + "]");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "getUser : DocumentSnapshot data: " + document.getData());
                        //user = (HashMap<String, Object>) document.getData();
                        //Get an object of type UserPOJO
                        User user = document.toObject(User.class);
                        mainTopTextView.setText(String.format("%s %s", user.getName(), user.getCommune()));
                    } else {
                        Log.d(TAG, "getUser :  No such document");
                    }
                } else {
                    Log.d(TAG, "getUser : get failed with ", task.getException());
                }
            }
        });
    }


    public void checkGroups() {
        groupList.clear();
        Log.d("checkGroups", "checkGroups appelé");

        if (userId == null) {
            Log.e("checkGroups", "userId est null, arrêt du chargement des groupes");
            return;
        }

        Log.d("checkGroups", "ID de l'utilisateur connecté: " + userId);

        // Étape 1 : Récupérer les groupes auxquels l'utilisateur est inscrit
        db.collection("user_groups")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(userGroupsSnapshot -> {
                    Log.d("checkGroups", "Nombre de documents trouvés dans user_group: " + userGroupsSnapshot.size());
                    if (userGroupsSnapshot.isEmpty()) {
                        Log.d("checkGroups", "Aucun document trouvé pour cet utilisateur dans user_group");
                        Toast.makeText(this, "L'utilisateur n'est inscrit à aucun groupe", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> joinedGroupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : userGroupsSnapshot) {
                        String groupId = document.getString("groupId");
                        Log.d("checkGroups", "Document trouvé: " + document.getId() + ", groupId: " + groupId);
                        if (groupId != null) {
                            joinedGroupIds.add(groupId);
                        }
                    }

                    // Étape 2 : Récupérer les détails des groupes
                    db.collection("groups")
                            .whereIn("id", joinedGroupIds)
                            .get()
                            .addOnSuccessListener(groupsSnapshot -> {
                                Log.d("checkGroups", "Nombre de groupes trouvés: " + groupsSnapshot.size());
                                if (groupsSnapshot.isEmpty()) {
                                    Log.d("checkGroups", "Aucun groupe trouvé pour les IDs récupérés");
                                    Toast.makeText(this, "Aucun groupe trouvé", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                for (QueryDocumentSnapshot document : groupsSnapshot) {
                                    Group group = new Group(document.getString("id"), document.getString("name"), document.getString("creator"), document.getString("date"), document.getString("time"));
                                    //String groupId = document.getString("id");
                                    Log.d("checkGroups", "Groupe trouvé: " + group.getName());
                                        groupList.add(group);
                                }
                                createNotifications();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("checkGroups", "Erreur lors de la récupération des groupes", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("checkGroups", "Erreur lors de la récupération des user_group", e);
                });
    }


    private void createNotifications() {
        int notificationId = NOTIFICATION_ID;
        Log.d(TAG, "createNotifications ---------------------");
        for ( Group group :groupList) {
            Log.d(TAG, "-----------Group : " + group.getName());
            createNotification(group.getName(), notificationId);
            notificationId++;
        }
    }

    private void createNotification(String name, int notificationId) {

        try {
            // 1. Vérifier si les notifications sont activées
            NotificationManagerCompat nm = NotificationManagerCompat.from(this);
            if (!nm.areNotificationsEnabled()) {
                openNotificationSettings();
                return;
            }

            // 2. Créer une notification impossible à ignorer
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    //.setSmallIcon(android.R.drawable.ic_dialog_alert) // Icône système garantie
                    .setSmallIcon(R.drawable.ic_notification) // Icône personnalisée)
                    .setContentTitle("Notification de test Android 15")
                    .setContentText("Vous devriez voir ce message !" + name)
                    //.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    //.setCategory(NotificationCompat.CATEGORY_ALARM)
                    //.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    //.setVibrate(new long[]{0, 500, 250, 500})
                    //.setAutoCancel(true)
                    //.setOnlyAlertOnce(false)
                    //.setShowWhen(true)
                    ;
            Log.d(TAG, "System.currentTimeMillis"+System.currentTimeMillis());

            // 3. Envoyer la notification
            nm.notify(notificationId, builder.build());

        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel ");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notifications Critiques",
                    NotificationManager.IMPORTANCE_HIGH); // IMPORTANCE_MAX pour Android 15

            channel.setDescription("Notifications urgentes");
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void openNotificationSettings() {
        Toast.makeText(this,
                "Activez les notifications dans les paramètres",
                Toast.LENGTH_LONG).show();

        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    public void onJSONButtonClicked(View view){
        // Créer une intention pour démarrer SecondActivity
        Intent intent = new Intent(MainActivity.this, DataListActivity.class);
        // Démarrer l'activité
        startActivity(intent);
    }







}