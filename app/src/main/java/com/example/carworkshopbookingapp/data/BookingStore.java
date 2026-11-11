package com.example.carworkshopbookingapp.data;

import android.content.Context;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.BookingActionsRepository;
import com.example.carworkshopbookingapp.data.remote.BookingWatch;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.List;

public final class BookingStore {
    private final LocalDb local;
    private final Context context;
    public BookingStore(Context context) { this.context = context.getApplicationContext(); local = LocalDb.getInstance(context); }

    public static final class Subscription implements AutoCloseable {
        private boolean closed;
        private BookingWatch watch;
        private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
        private Runnable deadline;
        private void settled(){if(deadline!=null)timer.removeCallbacks(deadline);}

        @Override public void close() { closed = true; settled(); if (watch != null) watch.close(); }
    }

    /** Local cache + queue first; authoritative snapshots replace only this user's cached server rows. */
    public Subscription observe(String uid, RefreshCallback<List<Booking>> callback) {
        Subscription subscription = new Subscription();
        subscription.deadline=()->{if(!subscription.closed){callback.onError(new java.util.concurrent.TimeoutException());callback.onRefreshComplete();}};
        subscription.timer.postDelayed(subscription.deadline,30000);
        local.getBookings(uid, new Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> items) {
                if (subscription.closed) return;
                callback.onSuccess(items); listen();
            }
            @Override public void onError(Exception error) {
                if (subscription.closed) return;
                callback.onError(error); listen();
            }
            private void listen() {
                if (!NetworkState.online(context)) {subscription.settled();callback.onRefreshComplete();}
                if (subscription.closed) return;
                subscription.watch = new BookingWatch(uid, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> items) {
                        if (subscription.closed) return;
                        local.replaceBookings(uid, items, new Callback<Void>() {
                            @Override public void onSuccess(Void ignored) {
                                if (subscription.closed) return;
                                local.getBookings(uid, new Callback<List<Booking>>() {
                                    @Override public void onSuccess(List<Booking> result) {
                                        if (!subscription.closed) { subscription.settled(); callback.onSuccess(result); callback.onRefreshComplete(); }
                                    }
                                    @Override public void onError(Exception error) { report(error); }
                                });
                            }
                            @Override public void onError(Exception error) { report(error); }
                        });
                    }
                    @Override public void onError(Exception error) { report(error); }
                    private void report(Exception error) {
                        if (!subscription.closed) { subscription.settled(); callback.onError(error); callback.onRefreshComplete(); }
                    }
                });
            }
        });
        return subscription;
    }

    public void cancel(Booking booking, Callback<Booking> callback) {
        new BookingActionsRepository().cancel(booking, cacheResult(callback));
    }
    public void rate(String uid, String id, int stars, Callback<Booking> callback) {
        new BookingActionsRepository().rate(uid, id, stars, cacheResult(callback));
    }
    private Callback<Booking> cacheResult(Callback<Booking> callback) {
        return new Callback<Booking>() {
            @Override public void onSuccess(Booking booking) {
                com.example.carworkshopbookingapp.util.ReminderScheduler.update(context,booking);
                local.cacheBooking(booking, new Callback<Void>() {
                    @Override public void onSuccess(Void ignored) { callback.onSuccess(booking); }
                    // The server mutation succeeded; a cache failure must not invite duplicate mutations.
                    @Override public void onError(Exception error) { callback.onSuccess(booking); }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        };
    }
}
