package com.example.carworkshopbookingapp.ui;

import com.example.carworkshopbookingapp.data.BookingStore;
import com.example.carworkshopbookingapp.data.RefreshCallback;
import com.example.carworkshopbookingapp.data.model.Booking;
import java.util.ArrayList;
import java.util.List;

public abstract class BookingFeedActivity extends CustomerActivity {
    protected List<Booking> bookings = new ArrayList<>();
    protected BookingStore store;
    protected boolean loading = true, failed;
    private final com.example.carworkshopbookingapp.receiver.BookingStatusReceiver statusReceiver = new com.example.carworkshopbookingapp.receiver.BookingStatusReceiver(this::reload);
    private BookingStore.Subscription subscription;
    protected String bookingError;
    private boolean started;
    private int generation;
    protected abstract void onBookingsChanged();

    @Override protected void onCustomerReady() {
        store = new BookingStore(getApplicationContext());
        if (started) reload();
    }

    /** Public entry point for the later BOOKING_STATUS_CHANGED receiver. */
    public final void reload() {
        if (store == null || !started) return;
        if (subscription != null) subscription.close();
        int request = ++generation;
        loading = true; failed = false; bookingError=null; onBookingsChanged();
        subscription = store.observe(user.uid, new RefreshCallback<List<Booking>>() {
            @Override public void onSuccess(List<Booking> items) {
                if (!active(request)) return;
                bookings = items; failed = false; onBookingsChanged();
            }
            @Override public void onError(Exception error) {
                if (!active(request)) return;
                failed = true; bookingError=UiErrors.message(error); onBookingsChanged();
            }
            @Override public void onRefreshComplete() {
                if (!active(request)) return;
                loading = false; onBookingsChanged();
            }
        });
    }
    private boolean active(int request) { return started && request == generation && !isDestroyed() && !isFinishing(); }
    @Override protected void onStart() { super.onStart(); androidx.core.content.ContextCompat.registerReceiver(this,statusReceiver,new android.content.IntentFilter(com.example.carworkshopbookingapp.service.SyncRunner.BOOKING_STATUS_CHANGED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED); started = true; if (user != null) reload(); }
    @Override protected void onStop() {
        unregisterReceiver(statusReceiver);
        started = false; generation++;
        if (subscription != null) { subscription.close(); subscription = null; }
        super.onStop();
    }
}
