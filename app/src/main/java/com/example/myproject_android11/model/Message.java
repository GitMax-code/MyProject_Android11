package com.example.myproject_android11.model;

import java.util.Date;

public class Message {
    private String senderId;
    private String content;
    private Date timestamp;

    public Message(String senderId, String content, Date timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}

