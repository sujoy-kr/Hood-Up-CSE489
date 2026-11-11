package com.example.carworkshopbookingapp.data;

import android.content.Context;
import com.example.carworkshopbookingapp.util.NotificationHelper;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.BookingsRepository;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.example.carworkshopbookingapp.util.NetworkState;

public final class BookingSubmission {
    public static final class Receipt {
        public final Booking booking;
        public final boolean queued;
        Receipt(Booking booking, boolean queued) { this.booking = booking; this.queued = queued; }
    }
    private final Context context;
    public BookingSubmission(Context context) { this.context = context.getApplicationContext(); }

    public void submit(Booking booking, Callback<Receipt> callback) {
        AppExecutors.execute(() -> {
            booking.commission = BookingPricing.commission(booking.price);
            booking.synced = false;
            return NetworkState.online(context);
        }, new Callback<Boolean>() {
            @Override public void onSuccess(Boolean online) {
                if (!online) { enqueue(booking, callback); return; }
                new BookingsRepository().createAtomic(booking, new Callback<Booking>() {
                    @Override public void onSuccess(Booking saved) {
                        LocalDb.getInstance(context).cacheBooking(saved, new Callback<Void>() {
                            @Override public void onSuccess(Void ignored) { finishSubmission(saved,callback); }
                            @Override public void onError(Exception error) { finishSubmission(saved,callback); }
                        });
                    }
                    @Override public void onError(Exception error) {
                        if (NetworkState.connectionError(error)) enqueue(booking, callback);
                        else callback.onError(error);
                    }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    private void finishSubmission(Booking saved,Callback<Receipt> callback){
        if(saved.status==com.example.carworkshopbookingapp.data.model.BookingStatus.CANCELLED){callback.onError(new IllegalStateException("This booking was already cancelled. Start a new booking."));return;}
        NotificationHelper.bookingSubmitted(context,saved);callback.onSuccess(new Receipt(saved,false));
    }

    private void enqueue(Booking booking, Callback<Receipt> callback) {
        LocalDb.getInstance(context).enqueueBooking(booking, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { booking.synced = false; com.example.carworkshopbookingapp.service.SyncCoordinator.schedule(context); callback.onSuccess(new Receipt(booking, true)); }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }
}
