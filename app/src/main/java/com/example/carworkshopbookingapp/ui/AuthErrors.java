package com.example.carworkshopbookingapp.ui;

import android.content.Context;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.remote.AuthFailure;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

final class AuthErrors {
    private AuthErrors() {}

    static String message(Context context, Exception error) {
        int resource = R.string.error_generic;
        if (error instanceof AuthFailure) {
            switch (((AuthFailure) error).reason) {
                case NOT_ADMIN: resource = R.string.error_not_admin; break;
                case ROLLBACK_FAILED: resource = R.string.error_registration_cleanup; break;
                default: resource = R.string.error_profile;
            }
        } else if (error instanceof FirebaseNetworkException) {
            resource = R.string.error_network;
        } else if (error instanceof FirebaseTooManyRequestsException) {
            resource = R.string.error_too_many;
        } else if (error instanceof FirebaseAuthException) {
            switch (((FirebaseAuthException) error).getErrorCode()) {
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS": resource = R.string.error_credentials; break;
                case "ERROR_USER_NOT_FOUND": resource = R.string.error_unknown_account; break;
                case "ERROR_INVALID_EMAIL": resource = R.string.error_email; break;
                case "ERROR_EMAIL_ALREADY_IN_USE": resource = R.string.error_email_used; break;
                case "ERROR_WEAK_PASSWORD": resource = R.string.error_weak_password; break;
                case "ERROR_USER_DISABLED": resource = R.string.error_disabled; break;
                case "ERROR_OPERATION_NOT_ALLOWED": resource = R.string.error_not_enabled; break;
                default: resource = R.string.error_generic;
            }
        } else if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) error).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) return UiErrors.message(error);
            resource = code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ?
                    R.string.error_network : R.string.error_profile;
        }
        return context.getString(resource);
    }
}
