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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.myproject_android11.model.Group;
import com.example.myproject_android11.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ListGroupActivity extends AppCompatActivity {

    private ListView groupListView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private ArrayList<Group> itemGroupList;
    private ArrayAdapter<Group> groupAdapter;
    private ArrayList<Group> groupList;
    private TextView headerTextView;
    private final String CHANNEL_ID = "personal_notifications";
    private final int NOTIFICATION_ID = 001;

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

        groupListView = findViewById(R.id.listViewListGroup);
        headerTextView = findViewById(R.id.headerTextView);
        groupList = new ArrayList<>();

        if (mAuth.getCurrentUser() == null) {
            Log.e("ListGroupActivity", "Utilisateur non connecté, redirection vers LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();
        Log.d("ListGroupActivity", "Utilisateur connecté: " + userId);


        itemGroupList = new ArrayList<>();
        groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemGroupList);
        groupListView.setAdapter(groupAdapter);
        groupListView.setSelector(R.drawable.list_selector);
        Log.d("ListGroupActivity", "Adapter initialisé et attaché à ListView");



        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ListGroupActivity", "onItemClick appelé");
                Group selectedGroupItem = itemGroupList.get(position);// Récupère le nom du groupe

                Log.d("ListGroupActivity", "Item cliqué: " + selectedGroupItem);

                // Récupérer l'ID réel du groupe à partir de Firestore
                db.collection("groups")
                        .whereEqualTo("name", selectedGroupItem.getName()) // Recherche par nom du groupe
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
        createNotificationChannel();
        checkGroups();
    }

    @Override
    public void onStart(){
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
           getUser(userId);
            setYourList();
            //checkGroups();
            //scheduleNotifications("Coucou","Contenu",1);

        }

    }

    public void scheduleNotifications(String title, String content, int notificationId){
        Data inputData = new Data.Builder()
                .putString("notification_title", title)
                .putString("notification_content", content)
                .putInt("notification_id", notificationId)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInputData(inputData)
                //.setInitialDelay(1, TimeUnit.HOURS) // Delay of 1 hour
                .setInitialDelay(10, TimeUnit.SECONDS) // Delay of 10 seconds
                .build();
        //WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(workRequest.toString(), ExistingWorkPolicy.REPLACE,workRequest );

    }



    public void setYourList() {
        Log.d("ListGroupActivity", "setYourList() appelé");
        //itemList.clear();
        itemGroupList.clear();

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
                                    Group group = document.toObject(Group.class);
                                    if (group != null) {
                                        Log.d("ListGroupActivity", "Groupe trouvé: " + group.getName());
                                        itemGroupList.add(group);
                                    }
                                }
                                groupAdapter.notifyDataSetChanged();
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

    public void onLogoutButtonClicked(View view){
        FirebaseAuth.getInstance().signOut();
        //startActivity(new Intent(this,MainActivity.class));
        startActivity(new Intent(this,LoginActivity.class));
    }

    //Main

    public void getUser(String userId) {
        Log.d("ListGroupActivity","getUser : User id [" + userId + "]");
        DocumentReference docRef = db.collection("users").document(userId);

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
                        headerTextView.setText(String.format("%s %s", user.getName(), user.getCommune()));
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
        //groupList.clear();
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
                                    Group group = new Group(document.getString("id"), document.getString("name"), document.getString("creator"), document.getString("dayOfWeek"), document.getString("time"));
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
            //createNotification(group.getName(), group.getDayOfWeek(), group.getTime(), notificationId);
            String notificationContent = "Rendez-vous pour " + group.getName() + " le " + group.getDayOfWeek() + " à " + group.getTime();
            scheduleNotifications(group.getName(), notificationContent, notificationId);
            notificationId++;
        }
    }
    private void checkNotificationsSettings(NotificationManagerCompat nm) {
        // 1. Vérifier si les notifications sont activées
        if (!nm.areNotificationsEnabled()) {
            Toast.makeText(this,
                    "Activez les notifications dans les paramètres",
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }
    }
    private void createNotification(String name, String dayOfWeek, String time, int notificationId) {

        try {
            NotificationManagerCompat nm = NotificationManagerCompat.from(this);
            checkNotificationsSettings(nm);

            // 2. Créer une notification impossible à ignorer
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    //.setSmallIcon(android.R.drawable.ic_dialog_alert) // Icône système garantie
                    .setSmallIcon(R.drawable.ic_notification) // Icône personnalisée)
                    .setContentTitle("Rendez-vous pour " + name)
                    .setContentText("Vous avez cour le " + dayOfWeek + " à " + time)
                    //.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    //.setCategory(NotificationCompat.CATEGORY_ALARM)
                    //.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    //.setVibrate(new long[]{0, 500, 250, 500})
                    //.setAutoCancel(true)
                    //.setOnlyAlertOnce(false)
                    //.setShowWhen(true)
                    ;
            Log.d(TAG, "System.currentTimeMillis "+System.currentTimeMillis());

            // 3. Envoyer la notification
            nm.notify(notificationId, builder.build());

        } catch (Exception e) {
            Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
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

}