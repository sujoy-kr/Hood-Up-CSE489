package com.example.carworkshopbookingapp.data.remote;

public final class AuthFailure extends Exception {
    public enum Reason { ADMIN_APPROVAL_REQUIRED, NOT_ADMIN, INVALID_INVITE, PROFILE_UNAVAILABLE, ROLLBACK_FAILED }
    public final Reason reason;

    public AuthFailure(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }
}
