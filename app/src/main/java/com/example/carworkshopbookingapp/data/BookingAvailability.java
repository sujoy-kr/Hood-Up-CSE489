package com.example.carworkshopbookingapp.data;

import android.content.Context;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.BookingsRepository;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.ArrayList;
import java.util.List;

public final class BookingAvailability {
    public static final class Result {
        public final List<Booking> bookings;
        public final boolean offline;
        Result(List<Booking> bookings, boolean offline) { this.bookings = bookings; this.offline = offline; }
    }
    private BookingAvailability() {}

    public static void load(Context context, String uid, String workshopId, Callback<Result> callback) {
        Context app = context.getApplicationContext();
        LocalDb.getInstance(app).getPendingBookings(uid, new Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> queued) {
                List<Booking> local = new ArrayList<>();
                for (Booking booking : queued) if (workshopId.equals(booking.workshopId)) local.add(booking);
                if (!NetworkState.online(app)) { callback.onSuccess(new Result(local, true)); return; }
                new BookingsRepository().listOccupiedSlots(workshopId, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> remote) {
                        local.addAll(remote); callback.onSuccess(new Result(local, false));
                    }
                    @Override public void onError(Exception error) {
                        if (NetworkState.connectionError(error)) callback.onSuccess(new Result(local, true));
                        else callback.onError(error);
                    }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }
}
