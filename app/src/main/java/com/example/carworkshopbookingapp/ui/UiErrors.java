package com.example.carworkshopbookingapp.ui;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.concurrent.TimeoutException;

final class UiErrors {
    private UiErrors() {}
    static String message(Exception error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof FirebaseFirestoreException) {
                switch (((FirebaseFirestoreException) cause).getCode()) {
                    case PERMISSION_DENIED: return "You don't have permission for this action. Check your account or contact the workshop.";
                    case UNAUTHENTICATED: return "Your session has expired. Please log in again.";
                    case DEADLINE_EXCEEDED: return "This is taking too long. Check your connection and retry. A submitted booking may still finish syncing.";
                    case UNAVAILABLE: return "Can't reach the server. Your saved data is still available. Check your connection and retry.";
                    case ABORTED: return "This entry changed on another device. Refresh and try again.";
                    default: break;
                }
            }
            if (cause instanceof TimeoutException) return "The request timed out. Check your connection and retry.";
            if (cause instanceof SecurityException) return "Please log in with an account allowed to perform this action.";
        }
        if (NetworkState.connectionError(error)) return "You're offline. Check your connection and retry.";
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) return error.getMessage() == null ? "Check the details and try again." : error.getMessage();
        return "Couldn't complete this action. Please retry.";
    }
}
