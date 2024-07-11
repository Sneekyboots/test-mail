package com.example.testemail;

public class Email {
    public String senderId;
    public String recipientId;
    public String message;

    public Email() {
    }

    public Email(String senderId, String recipientId, String message) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.message = message;
    }
}
