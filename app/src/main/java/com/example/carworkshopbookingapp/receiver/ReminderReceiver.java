package com.example.carworkshopbookingapp.receiver;

import android.content.*;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.util.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

/** Handles only the app's explicit inexact appointment-reminder alarm; validates current owner and cached status. */
public final class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION="com.example.carworkshopbookingapp.APPOINTMENT_REMINDER";
    @Override public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        PendingResult pending=goAsync();
        AppExecutors.execute(()->FirebaseAuth.getInstance().getCurrentUser()==null?null:FirebaseAuth.getInstance().getCurrentUser().getUid(),new Callback<String>() {
            @Override public void onSuccess(String uid) {
                if (uid==null || !uid.equals(intent.getStringExtra("uid"))) {pending.finish();return;}
                LocalDb.getInstance(context).getBookings(uid,new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> items) {
                        try {for (Booking booking:items) if (booking.id.equals(intent.getStringExtra("bookingId")) && booking.status==BookingStatus.CONFIRMED && booking.dateTimeMillis>System.currentTimeMillis()) NotificationHelper.reminder(context,booking);}
                        finally {pending.finish();}
                    }
                    @Override public void onError(Exception error) {pending.finish();}
                });
            }
            @Override public void onError(Exception error) {pending.finish();}
        });
    }
}
