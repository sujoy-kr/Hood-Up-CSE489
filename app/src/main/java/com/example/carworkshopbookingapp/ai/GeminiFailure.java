package com.example.carworkshopbookingapp.ai;

public final class GeminiFailure extends Exception {
    public GeminiFailure(String message) { super(message); }
    public static GeminiFailure unavailable() {
        return new GeminiFailure("The AI assistant is temporarily unavailable. Offline suggestions are available; please try again later.");
    }
}
