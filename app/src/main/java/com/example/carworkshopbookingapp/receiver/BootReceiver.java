package com.example.carworkshopbookingapp.receiver;

import android.content.*;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.util.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

/** Handles system BOOT_COMPLETED and package replacement to restore this signed-in user's future cached reminders. Never starts an FGS. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        PendingResult pending=goAsync();
        AppExecutors.execute(()->FirebaseAuth.getInstance().getCurrentUser()==null?null:FirebaseAuth.getInstance().getCurrentUser().getUid(),new Callback<String>() {
            @Override public void onSuccess(String uid) {
                if (uid==null) {pending.finish();return;}
                LocalDb.getInstance(context).getBookings(uid,new Callback<List<Booking>>() {
                    @Override public void onSuccess(List<Booking> items) {try {for (Booking booking:items) ReminderScheduler.update(context,booking);} finally {pending.finish();}}
                    @Override public void onError(Exception error) {pending.finish();}
                });
            }
            @Override public void onError(Exception error) {pending.finish();}
        });
    }
}
