package com.example.carworkshopbookingapp.service;

import android.app.job.*;
import android.content.*;
import android.net.*;
import android.os.Build;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.BookingWatch;
import com.example.carworkshopbookingapp.util.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

/** Only visible activities may launch the foreground Service. Jobs handle background connectivity. */
public final class SyncCoordinator {
    private static final int QUEUE_JOB = 801, STATUS_JOB = 802;
    private static int visible, generation;
    private static boolean loggedOut;
    private static BookingWatch visibleWatch;
    private static String watchedUid;
    private SyncCoordinator() {}

    public static void enter(Context context) {
        visible++;
        Context app = context.getApplicationContext();
        if (!loggedOut) com.example.carworkshopbookingapp.receiver.ConnectivityMonitor.start(app);
        request(app);
    }
    public static void leave() {
        visible = Math.max(0, visible - 1);
        if (visible == 0) closeWatch();
    }
    public static void request(Context context) {
        if (loggedOut) return;
        Context app = context.getApplicationContext();
        int token = generation;
        AppExecutors.execute(() -> FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid(), new Callback<String>() {
            @Override public void onSuccess(String uid) {
                if (token != generation || uid == null || visible == 0) return;
                observe(app, uid);
                periodic(app);
                LocalDb.getInstance(app).getPendingBookings(uid, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> queue) {
                        if (token != generation || visible == 0) { if (!queue.isEmpty()) schedule(app); return; }
                        boolean foreground = !queue.isEmpty() && NetworkState.online(app);
                        Intent intent = new Intent(app, BookingSyncService.class).putExtra("uid", uid).putExtra("foreground", foreground);
                        try {
                            if (foreground && Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent);
                            else app.startService(intent);
                        } catch (IllegalStateException | SecurityException restricted) { schedule(app); }
                    }
                    @Override public void onError(Exception error) { schedule(app); }
                });
            }
            @Override public void onError(Exception error) { schedule(app); }
        });
    }
    private static void observe(Context app, String uid) {
        if (uid.equals(watchedUid) && visibleWatch != null) return;
        closeWatch(); watchedUid = uid;
        int token = generation;
        visibleWatch = new BookingWatch(uid, new Callback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> items) {
                if (token != generation || visible == 0) return;
                for (Booking item : items) ReminderScheduler.update(app,item);
                LocalDb.getInstance(app).applySyncSnapshot(uid, items, new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> changed) {
                        if (token != generation) return;
                        for (Booking booking : changed) {
                            NotificationHelper.bookingStatusChanged(app, booking);
                            app.sendBroadcast(new Intent(SyncRunner.BOOKING_STATUS_CHANGED).setPackage(app.getPackageName())
                                    .putExtra("bookingId", booking.id).putExtra("userId", uid).putExtra("status", booking.status.name()));
                        }
                    }
                    @Override public void onError(Exception error) { }
                });
            }
            @Override public void onError(Exception error) { }
        });
    }
    private static void closeWatch() { if (visibleWatch != null) visibleWatch.close(); visibleWatch = null; watchedUid = null; }
    public static void schedule(Context context) {
        if (loggedOut) return;
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        scheduler.schedule(new JobInfo.Builder(QUEUE_JOB, new ComponentName(context, BookingSyncJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());
    }
    private static void periodic(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler.getPendingJob(STATUS_JOB) == null) scheduler.schedule(new JobInfo.Builder(STATUS_JOB, new ComponentName(context, BookingSyncJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15 * 60 * 1000L).build());
    }
    public static void signedIn(Context context) { loggedOut = false; if (visible > 0) request(context); }
    public static void networkRestored(Context context) { if (visible>0) request(context); else schedule(context); }
    public static boolean retryAllowed() { return !loggedOut; }
    public static void logout(Context context) {
        loggedOut = true; generation++; closeWatch();
        com.example.carworkshopbookingapp.receiver.ConnectivityMonitor.stop(context);
        context.getSystemService(android.app.NotificationManager.class).cancelAll();
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        scheduler.cancel(QUEUE_JOB); scheduler.cancel(STATUS_JOB);
        context.stopService(new Intent(context, BookingSyncService.class));
    }
}
