package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BookingWatch implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private ListenerRegistration registration;

    public BookingWatch(String uid, Callback<List<Booking>> callback) {
        AppExecutors.io().execute(() -> {
            if (closed.get()) return;
            try {
                // Sort locally so status updates do not depend on a deployed composite index.
                registration = FirebaseFirestore.getInstance().collection("bookings").whereEqualTo("userId", uid)
                        .addSnapshotListener(AppExecutors.io(), MetadataChanges.INCLUDE, (snapshot, error) -> {
                            if (closed.get()) return;
                            if (error != null) { fail(callback, error); return; }
                            if (snapshot == null || snapshot.getMetadata().isFromCache() || snapshot.getMetadata().hasPendingWrites()) return;
                            try {
                                List<Booking> items = new ArrayList<>();
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    Booking booking = doc.toObject(Booking.class);
                                    if (booking == null) throw new IllegalStateException("Empty booking");
                                    booking.id = doc.getId(); booking.synced = true; items.add(booking);
                                }
                                items.sort(java.util.Comparator.comparingLong((Booking item) -> item.dateTimeMillis)
                                        .thenComparing(item -> item.id));
                                AppExecutors.main(() -> { if (!closed.get()) callback.onSuccess(items); });
                            } catch (Exception mappingError) { fail(callback, mappingError); }
                        });
                if (closed.get()) registration.remove();
            } catch (Exception error) { fail(callback, error); }
        });
    }
    private void fail(Callback<List<Booking>> callback, Exception error) {
        AppExecutors.main(() -> { if (!closed.get()) callback.onError(error); });
    }
    @Override public void close() {
        closed.set(true);
        AppExecutors.io().execute(() -> { if (registration != null) registration.remove(); });
    }
}
