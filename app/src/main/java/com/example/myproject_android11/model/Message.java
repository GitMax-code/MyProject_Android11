package com.example.myproject_android11.model;

import java.util.Date;

public class Message {
    private String id;
    private String message;
    private String userId;
    private String groupId;
    private long timestamp;

    // Constructeur vide nécessaire pour Firestore
    public Message() {}

    // Constructeur
    public Message(String message, String userId, String groupId, long timestamp) {
        this.message = message;
        this.userId = userId;
        this.groupId = groupId;
        this.timestamp = timestamp;
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

