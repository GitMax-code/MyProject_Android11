package com.example.myproject_android11;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.SetOptions;

import com.example.myproject_android11.model.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button sendButton;
    private ArrayList<Message> messagesList; // Liste des messages
    private FirebaseFirestore db;
    private String groupId; // Identifiant du groupe (passé depuis l'intention)
    private MessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        sendButton = findViewById(R.id.sendButton);

        // Initialisation de la liste des messages
        messagesList = new ArrayList<>();

        // Initialisation du RecyclerView avec un LayoutManager et un Adapter
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messagesList);
        recyclerViewMessages.setAdapter(messagesAdapter);

        // Récupérer l'ID du groupe passé par l'intention
        groupId = getIntent().getStringExtra("id");



        // Gestion de l'envoi des messages
        sendButton.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString();
            if (!messageText.isEmpty()) {
                // Ajouter le message à Firestore
                sendMessageToFirestore(messageText);

                // Créer un objet Message localement
                Message message = new Message(messageText, FirebaseAuth.getInstance().getCurrentUser().getUid(), groupId, System.currentTimeMillis());

                // Ajouter le message à la liste localement pour afficher dans RecyclerView
                messagesList.add(message);
                messagesAdapter.notifyDataSetChanged(); // Mettre à jour le RecyclerView

                // Effacer le champ de saisie
                editTextMessage.setText("");
            }
        });
    }

    public void onStart(){
        super.onStart();
        // Récupérer les messages de Firestore et les afficher
        fetchMessagesFromFirestore();
    }

    // Méthode pour récupérer les messages de Firestore
    private void fetchMessagesFromFirestore() {
        db.collection("messages")
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp") // Trier les messages par timestamp
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        return;
                    }

                    if (queryDocumentSnapshots != null) { // Correction ici
                        messagesList.clear();

                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            Message message = documentSnapshot.toObject(Message.class);
                            if (message != null) {
                                messagesList.add(message);
                            }
                        }

                        // Notifier l'adapter
                        messagesAdapter.notifyDataSetChanged();
                    }
                });
    }


    // Méthode pour envoyer un message à Firestore
    private void sendMessageToFirestore(String message) {
        // Créer un Map contenant le message, l'ID de l'utilisateur, l'ID du groupe, et l'ID du message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        messageData.put("groupId", groupId); // Ajout de l'ID du groupe
        messageData.put("timestamp", System.currentTimeMillis()); // Ajouter un timestamp

        // Ajouter le message à la collection "Message" dans Firestore
        db.collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    // Récupérer l'ID du document (ID du message)
                    String messageId = documentReference.getId();
                    // Ajouter l'ID du message dans la base de données
                    messageData.put("id", messageId);

                    // Mettre à jour le message avec l'ID dans Firestore
                    db.collection("messages")
                            .document(messageId)
                            .set(messageData, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                // Le message avec l'ID a été mis à jour avec succès
                            })
                            .addOnFailureListener(e -> {
                                // En cas d'erreur
                                e.printStackTrace();
                            });
                })
                .addOnFailureListener(e -> {
                    // En cas d'erreur
                    e.printStackTrace();
                });
    }




    // Adapter simple pour RecyclerView
    private static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

        private final ArrayList<Message> messages;

        public MessagesAdapter(ArrayList<Message> messages) {
            this.messages = messages;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_item, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.textMessage.setText(message.getMessage());
            holder.textUser.setText("User: " + message.getUserId()); // Affiche l'ID de l'utilisateur
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView textMessage, textUser;

            public MessageViewHolder(View itemView) {
                super(itemView);
                textMessage = itemView.findViewById(R.id.textMessage);
                textUser = itemView.findViewById(R.id.textUser);
            }
        }
    }


}

