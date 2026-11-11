package com.example.carworkshopbookingapp.util;

import android.app.*;
import android.content.*;
import android.net.Uri;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.receiver.ReminderReceiver;

public final class ReminderScheduler {
    private ReminderScheduler() {}
    public static void update(Context context, Booking booking) {
        Intent intent=new Intent(context,ReminderReceiver.class).setAction(ReminderReceiver.ACTION)
                .setData(Uri.parse("hoodup://reminder/"+Uri.encode(booking.userId)+"/"+Uri.encode(booking.id)))
                .putExtra("uid",booking.userId).putExtra("bookingId",booking.id);
        PendingIntent alarm=PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        manager.cancel(alarm);
        long at=com.example.carworkshopbookingapp.data.ReminderPolicy.triggerAt(booking,System.currentTimeMillis());
        if (!context.getSharedPreferences("customer_settings",Context.MODE_PRIVATE).getBoolean("reminders:"+booking.userId,true)) at=0;
        if (at>0) manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,alarm);
        else alarm.cancel();
    }
}
