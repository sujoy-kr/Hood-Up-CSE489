package com.example.carworkshopbookingapp.receiver;

import android.content.*;
import com.example.carworkshopbookingapp.service.SyncRunner;

/** Dynamically handles the package-only BOOKING_STATUS_CHANGED broadcast while a booking screen is started. */
public final class BookingStatusReceiver extends BroadcastReceiver {
    private final Runnable reload;
    public BookingStatusReceiver(Runnable reload) {this.reload=reload;}
    @Override public void onReceive(Context context,Intent intent) {if (SyncRunner.BOOKING_STATUS_CHANGED.equals(intent.getAction())) reload.run();}
}
