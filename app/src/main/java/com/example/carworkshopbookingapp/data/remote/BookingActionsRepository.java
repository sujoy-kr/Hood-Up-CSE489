package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.*;
import com.example.carworkshopbookingapp.data.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public final class BookingActionsRepository {
    private static void owner(String uid) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null || !current.getUid().equals(uid)) throw new BookingFailure(BookingFailure.Reason.SIGNED_OUT);
    }

    public void cancel(Booking request, Callback<Booking> callback) {
        RemoteTasks.run(() -> {
            owner(request.userId);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference ref = db.collection("bookings").document(request.id);
            return db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(ref);
                Booking booking = snapshot.exists() ? snapshot.toObject(Booking.class) : new Booking(request);
                if (booking == null || !request.userId.equals(booking.userId) ||
                        (snapshot.exists() && !BookingHistory.canCancel(booking) && booking.status != BookingStatus.CANCELLED)) {
                    throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                }
                if (!snapshot.exists()) {
                    DocumentReference markerRef = db.collection("booking_cancellations").document(request.id);
                    DocumentSnapshot existingMarker = transaction.get(markerRef);
                    if (existingMarker.exists()) {
                        if (!request.userId.equals(existingMarker.getString("userId"))) {
                            throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                        }
                    } else {
                        java.util.Map<String, Object> marker = new java.util.HashMap<>();
                        marker.put("userId", request.userId);
                        transaction.set(markerRef, marker);
                    }
                    booking.id=request.id;booking.status=BookingStatus.CANCELLED;booking.synced=true;return booking;
                }
                // Slot deletion and status cancellation are checked together by deployed rules.
                List<DocumentReference> release = new ArrayList<>();
                if (snapshot.exists()) for (long time : BookingSlots.locks(booking.dateTimeMillis, booking.durationMin)) {
                    DocumentReference lock = db.collection("workshops").document(booking.workshopId)
                            .collection("slots").document(Long.toString(time));
                    if (request.id.equals(transaction.get(lock).getString("bookingId"))) release.add(lock);
                }
                booking.id = request.id; booking.status = BookingStatus.CANCELLED; booking.synced = true;
                transaction.set(ref, booking);
                for (DocumentReference lock : release) transaction.delete(lock);
                return booking;
            });
        }, value -> value, callback);
    }

    public void rate(String uid, String id, int stars, Callback<Booking> callback) {
        RemoteTasks.run(() -> {
            owner(uid);
            if (stars < 1 || stars > 5) throw new IllegalArgumentException("Choose 1–5 stars");
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference ref = db.collection("bookings").document(id);
            return db.runTransaction(transaction -> {
                Booking booking = transaction.get(ref).toObject(Booking.class);
                if (booking == null || !uid.equals(booking.userId) || booking.status != BookingStatus.COMPLETED) {
                    throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                }
                DocumentReference workshop = db.collection("workshops").document(booking.workshopId);
                DocumentSnapshot snapshot = transaction.get(workshop);
                if (!snapshot.exists()) throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                Long countValue = snapshot.getLong("ratingCount");
                long count = countValue == null ? 0 : countValue;
                Double sumValue = snapshot.getDouble("ratingSum");
                Double average = snapshot.getDouble("rating");
                double sum = sumValue == null ? (average == null ? 0 : average * count) : sumValue;
                RatingAverage updated = RatingAverage.apply(count, sum, booking.rating, stars);
                transaction.update(workshop, "ratingCount", updated.count, "ratingSum", updated.sum, "rating", updated.average, "ratingBookingId",id);
                transaction.update(ref, "rating", stars);
                booking.id = id; booking.rating = stars; booking.synced = true;
                return booking;
            });
        }, value -> value, callback);
    }
}
