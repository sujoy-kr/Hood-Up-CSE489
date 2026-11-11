package com.example.carworkshopbookingapp.service;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.*;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.ui.HomeActivity;

public final class BookingSyncService extends Service {
    private SyncRunner runner;
    private boolean foreground;
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getStringExtra("uid") == null) { stopSelf(); return START_NOT_STICKY; }
        if (intent.getBooleanExtra("foreground", false) && !promote()) return START_NOT_STICKY;
        if (runner == null) {
            String uid = intent.getStringExtra("uid");
            runner = new SyncRunner(this, uid, new SyncRunner.Host() {
                @Override public boolean syncing() { return promote(); }
                @Override public void idle() {
                    if (foreground) { stopForeground(STOP_FOREGROUND_REMOVE); foreground = false; }
                }
                @Override public void finished(boolean retry) {
                    if (retry) SyncCoordinator.schedule(BookingSyncService.this);
                    stopSelf();
                }
            });
            runner.start();
        }
        return START_NOT_STICKY;
    }
    private boolean promote() {
        if (foreground) return true;
        NotificationManager manager = getSystemService(NotificationManager.class);
        com.example.carworkshopbookingapp.util.NotificationHelper.channels(this);
        PendingIntent open = PendingIntent.getActivity(this, 80, new Intent(this, HomeActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "booking_sync") : new Notification.Builder(this);
        Notification notification = builder.setSmallIcon(R.drawable.ic_sync_notification).setContentTitle(getString(R.string.sync_title))
                .setContentText(getString(R.string.sync_body)).setContentIntent(open).setOngoing(true).setPriority(Notification.PRIORITY_LOW).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(80, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(80, notification);
            foreground = true;
            return true;
        } catch (IllegalStateException | SecurityException restricted) { SyncCoordinator.schedule(this); stopSelf(); return false; }
    }
    @Override public void onTimeout(int startId, int fgsType) { if (runner != null) runner.close(); stopSelf(); }
    @Override public void onDestroy() {
        if (runner != null) runner.close();
        if (foreground) stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
