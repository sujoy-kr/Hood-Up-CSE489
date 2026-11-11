package com.example.carworkshopbookingapp.data.remote;

public final class BookingFailure extends RuntimeException {
    public enum Reason { SLOT_TAKEN, INVALID_SELECTION, PRICE_CHANGED, SIGNED_OUT }
    public final Reason reason;
    public BookingFailure(Reason reason) { super(reason.name()); this.reason = reason; }
}
