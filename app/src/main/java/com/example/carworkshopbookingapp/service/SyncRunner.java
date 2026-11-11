package com.example.carworkshopbookingapp.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.BookingWatch;
import com.example.carworkshopbookingapp.data.remote.BookingsRepository;
import com.example.carworkshopbookingapp.util.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Finite work shared by the started Service and the background-safe JobScheduler fallback. */
public final class SyncRunner implements AutoCloseable {
    public static final String BOOKING_STATUS_CHANGED = "com.example.carworkshopbookingapp.BOOKING_STATUS_CHANGED";
    public interface Host { boolean syncing(); void idle(); void finished(boolean retry); }
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private final Context context;
    private final String uid;
    private final Host host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Booking> queue = new ArrayList<>();
    private BookingWatch watch;
    private boolean closed, ownsLock, needsRetry;
    private int position;

    public SyncRunner(Context context, String uid, Host host) { this.context = context.getApplicationContext(); this.uid = uid; this.host = host; }
    public void start() {
        if (!RUNNING.compareAndSet(false, true)) { host.finished(true); return; }
        ownsLock = true;
        main.postDelayed(() -> finish(true), 120000);
        AppExecutors.execute(() -> FirebaseAuth.getInstance().getCurrentUser() != null &&
                uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()), new Callback<Boolean>() {
            @Override public void onSuccess(Boolean valid) {
                if (closed) return;
                if (!valid) { finish(false); return; }
                LocalDb.getInstance(context).getPendingBookings(uid, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> items) {
                        if (closed) return;
                        queue.addAll(items);
                        if (!NetworkState.online(context)) { finish(!items.isEmpty()); return; }
                        if (!items.isEmpty() && !host.syncing()) { finish(true); return; }
                        next();
                    }
                    @Override public void onError(Exception error) { finish(true); }
                });
            }
            @Override public void onError(Exception error) { finish(true); }
        });
    }
    private void next() {
        if (closed) return;
        if (position >= queue.size()) { listen(); return; }
        Booking booking = queue.get(position++);
        new BookingsRepository().createAtomic(booking, new Callback<Booking>() {
            @Override public void onSuccess(Booking saved) {
                if (closed) return;
                saved.synced = true;
                LocalDb.getInstance(context).applySyncSnapshot(uid, java.util.Collections.singletonList(saved), new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> changes) {
                        if (!closed) {
                            for (Booking changed : changes) NotificationHelper.bookingStatusChanged(context, changed);
                            if (saved.status == com.example.carworkshopbookingapp.data.model.BookingStatus.PENDING) NotificationHelper.bookingSubmitted(context,saved);
                            broadcast(saved); next();
                        }
                    }
                    @Override public void onError(Exception error) { needsRetry = true; next(); }
                });
            }
            @Override public void onError(Exception error) {
                if (closed) return;
                if (NetworkState.connectionError(error)) { finish(true); return; }
                // A rejected slot/quote remains queued for the user to cancel/rebook; never report it synced.
                next();
            }
        });
    }
    private void listen() {
        if (closed) return;
        host.idle();
        watch = new BookingWatch(uid, new Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> items) {
                if (closed) return;
                for (Booking item : items) ReminderScheduler.update(context,item);
                LocalDb.getInstance(context).applySyncSnapshot(uid, items, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> changes) {
                        if (closed) return;
                        for (Booking booking : changes) {
                            NotificationHelper.bookingStatusChanged(context, booking); broadcast(booking);
                        }
                        // A bounded listener observes changes during this sync pass, then releases the Service.
                        main.postDelayed(() -> finish(needsRetry), 2000);
                    }
                    @Override public void onError(Exception error) { finish(true); }
                });
            }
            @Override public void onError(Exception error) { finish(NetworkState.connectionError(error)); }
        });
        main.postDelayed(() -> finish(needsRetry), 15000);
    }
    private void broadcast(Booking booking) {
        context.sendBroadcast(new Intent(BOOKING_STATUS_CHANGED).setPackage(context.getPackageName())
                .putExtra("bookingId", booking.id).putExtra("userId", uid).putExtra("status", booking.status.name()));
    }
    private void finish(boolean retry) {
        if (closed) return;
        close(); host.finished(retry);
    }
    @Override public void close() {
        if (closed) return;
        closed = true; main.removeCallbacksAndMessages(null);
        if (watch != null) watch.close();
        if (ownsLock) { ownsLock = false; RUNNING.set(false); }
    }
}
