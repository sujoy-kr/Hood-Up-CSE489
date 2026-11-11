package com.example.carworkshopbookingapp.util;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.TaskStackBuilder;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.ui.*;

public final class NotificationHelper {
    public static final String UPDATES="booking_updates", REMINDERS="reminders", SYNC="booking_sync";
    private NotificationHelper() {}
    public static void channels(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(UPDATES,"Booking updates",NotificationManager.IMPORTANCE_DEFAULT));
        manager.createNotificationChannel(new NotificationChannel(REMINDERS,"Reminders",NotificationManager.IMPORTANCE_HIGH));
        manager.createNotificationChannel(new NotificationChannel(SYNC,"Sync",NotificationManager.IMPORTANCE_MIN));
    }
    public static void permission(Activity activity) {
        channels(activity);
        if (Build.VERSION.SDK_INT < 33 || activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) return;
        android.content.SharedPreferences preferences=activity.getSharedPreferences("notification_permission",Context.MODE_PRIVATE);
        if (preferences.getBoolean("asked",false)) return;
        preferences.edit().putBoolean("asked",true).apply();
        new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Booking notifications")
                .setMessage("Allow Hood Up to notify you when your booking changes and remind you before your appointment.")
                .setNegativeButton("Not now",null).setPositiveButton("Continue",(dialog,which)->activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},909)).show();
    }
    public static void bookingSubmitted(Context context, Booking booking) {
        post(context,booking,"Booking submitted",booking.serviceName+" · waiting for workshop confirmation",false);
    }
    public static void bookingStatusChanged(Context context, Booking booking) {
        ReminderScheduler.update(context,booking);
        if (booking.status==BookingStatus.PENDING) return;
        String title=booking.status==BookingStatus.READY?"Your car is ready":context.getString(BookingDisplay.label(booking.status));
        post(context,booking,title,booking.serviceName+" · "+booking.workshopName,false);
    }
    public static void reminder(Context context, Booking booking) {
        if (!context.getSharedPreferences("customer_settings",Context.MODE_PRIVATE).getBoolean("reminders:"+booking.userId,true)) return;
        post(context,booking,"Appointment reminder",booking.serviceName+" · "+BookingDisplay.time(booking.dateTimeMillis),true);
    }
    private static void post(Context context, Booking booking, String title, String text, boolean reminder) {
        channels(context);
        if (Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        Intent detail=new Intent(context,BookingDetailActivity.class).putExtra("bookingId",booking.id)
                .setData(Uri.parse("hoodup://booking/"+Uri.encode(booking.userId)+"/"+Uri.encode(booking.id)));
        PendingIntent open=TaskStackBuilder.create(context).addNextIntent(new Intent(context,HomeActivity.class))
                .addNextIntent(detail)
                .getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,reminder?REMINDERS:UPDATES):new Notification.Builder(context);
        Notification notification=builder.setSmallIcon(R.drawable.ic_sync_notification).setContentTitle(title).setContentText(text)
                .setContentIntent(open).setAutoCancel(true).setPriority(reminder?Notification.PRIORITY_HIGH:Notification.PRIORITY_DEFAULT).build();
        try {context.getSystemService(NotificationManager.class).notify((reminder?"reminder:":"booking:")+booking.userId+":"+booking.id,0,notification);}
        catch (SecurityException denied) { }
    }
}
