package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.google.firebase.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.carworkshopbookingapp.data.BookingSlots;
import com.example.carworkshopbookingapp.data.BookingPricing;
import com.example.carworkshopbookingapp.data.model.*;
import java.util.ArrayList;
import java.util.List;

public final class BookingsRepository extends CollectionRepository<Booking> {
    public BookingsRepository() { super("bookings", Booking.class); }
    @Override protected String idOf(Booking item) { return item.id; }
    @Override protected void assignId(Booking item, String id) { item.id = id; }

    @Override public void save(Booking booking, Callback<Void> callback) {
        createAtomic(booking, new Callback<Booking>() {
            @Override public void onSuccess(Booking result) { callback.onSuccess(null); }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    /** Idempotent booking creation. Every reservation writer, including queue sync, must use this. */
    public void createAtomic(Booking request, Callback<Booking> callback) {
        RemoteTasks.run(() -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || !current.getUid().equals(request.userId)) {
                throw new BookingFailure(BookingFailure.Reason.SIGNED_OUT);
            }
            DocumentReference bookingRef = collection().document(validId(request.id));
            DocumentReference workshopRef = db.collection("workshops").document(validId(request.workshopId));
            DocumentReference serviceRef = db.collection("services").document(validId(request.serviceId));
            DocumentReference carRef = db.collection("users").document(validId(request.userId))
                    .collection("cars").document(validId(request.carId));
            return db.runTransaction(transaction -> {
                DocumentSnapshot cancelled=transaction.get(db.collection("booking_cancellations").document(request.id));
                DocumentSnapshot existing = transaction.get(bookingRef);
                if (existing.exists()) {
                    Booking saved = existing.toObject(Booking.class);
                    if (saved != null && request.userId.equals(saved.userId) && request.workshopId.equals(saved.workshopId) &&
                            request.serviceId.equals(saved.serviceId) && request.carId.equals(saved.carId) &&
                            request.dateTimeMillis == saved.dateTimeMillis && request.pickupRequested == saved.pickupRequested) return saved;
                    throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                }
                if(cancelled.exists()){
                    if(!request.userId.equals(cancelled.getString("userId")))throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                    Booking result=new Booking(request);result.status=BookingStatus.CANCELLED;result.synced=true;return result;
                }
                Workshop workshop = transaction.get(workshopRef).toObject(Workshop.class);
                ServiceItem service = transaction.get(serviceRef).toObject(ServiceItem.class);
                Car car = transaction.get(carRef).toObject(Car.class);
                User account=transaction.get(db.collection("users").document(request.userId)).toObject(User.class);
                double payable=service==null?0:com.example.carworkshopbookingapp.data.BusinessConfig.price(service.basePrice);
                if (account == null || workshop == null || service == null || car == null || workshop.serviceIds == null ||
                        !workshop.serviceIds.contains(request.serviceId) || !Double.isFinite(service.basePrice) ||
                        service.basePrice < 0 || service.durationMin>com.example.carworkshopbookingapp.data.BusinessConfig.MAX_SERVICE_DURATION_MIN || !BookingSlots.allowed(workshop, service.durationMin, request.dateTimeMillis,
                        System.currentTimeMillis()) || (request.note != null && request.note.length() > 1000)) {
                    throw new BookingFailure(BookingFailure.Reason.INVALID_SELECTION);
                }
                if (request.plusApplied || request.pickupRequested || request.prioritySlot ||
                        Double.compare(request.price, payable) != 0 || request.durationMin != service.durationMin) {
                    throw new BookingFailure(BookingFailure.Reason.PRICE_CHANGED);
                }
                List<DocumentReference> locks = new ArrayList<>();
                for (long time : BookingSlots.locks(request.dateTimeMillis, service.durationMin)) {
                    DocumentReference lock = workshopRef.collection("slots").document(Long.toString(time));
                    DocumentSnapshot occupied = transaction.get(lock);
                    String owner = occupied.getString("bookingId");
                    if (owner != null) throw new BookingFailure(BookingFailure.Reason.SLOT_TAKEN);
                    locks.add(lock);
                }
                request.userName=account.name; request.workshopName = workshop.name; request.serviceName = service.name;
                request.carLabel = car.make + " " + car.model + " · " + car.plate;
                request.basePrice=service.basePrice;
                request.plusApplied=false; request.pickupRequested=false; request.prioritySlot=false;
                request.price = payable;
                request.commission = BookingPricing.commission(payable);
                request.status = BookingStatus.PENDING; request.synced = true;
                for (DocumentReference lock : locks) {
                    java.util.Map<String, Object> reservation = new java.util.HashMap<>();
                    reservation.put("bookingId", request.id);
                    transaction.set(lock, reservation);
                }
                transaction.set(bookingRef, request);
                return request;
            });
        }, value -> value, callback);
    }

    public void listForUser(String uid, Callback<List<Booking>> callback) {
        RemoteTasks.run(() -> collection().whereEqualTo("userId", validId(uid)).get(Source.SERVER),
                snapshot -> {
                    List<Booking> items = decodeAll(snapshot);
                    items.sort(java.util.Comparator.comparingLong((Booking item) -> item.dateTimeMillis)
                            .thenComparing(item -> item.id));
                    return items;
                }, callback);
    }

    /** Occupancy contains only opaque booking IDs and slot timestamps, never customer booking details. */
    public void listOccupiedSlots(String workshopId, Callback<List<Booking>> callback) {
        RemoteTasks.run(() -> com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("workshops")
                .document(validId(workshopId)).collection("slots").get(Source.SERVER), snapshot -> {
            List<Booking> occupied=new ArrayList<>();
            for(DocumentSnapshot doc:snapshot){Booking slot=new Booking();slot.id=doc.getString("bookingId");
                slot.workshopId=workshopId;slot.dateTimeMillis=Long.parseLong(doc.getId());slot.durationMin=60;
                slot.status=BookingStatus.CONFIRMED;occupied.add(slot);}
            return occupied;
        },callback);
    }
}
