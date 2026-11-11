package com.example.carworkshopbookingapp.data.model;

public class ChatMessage {
    public String id;
    public String userId;
    public String role;
    public String text;
    public long createdAt;
    public String urgency = "LOW";
    public java.util.List<String> serviceIds = new java.util.ArrayList<>();
    public String symptom = "";
    public String error = "";
    public boolean offline;
    public boolean retryable;

    public ChatMessage() {}
}
