package com.example.carworkshopbookingapp.data.model;

public class Booking {
    // Legacy snapshots are retained for existing bookings; new bookings set these false.
    public boolean plusApplied, pickupRequested, prioritySlot;
    public double basePrice;
    public String id;
    public String userId;
    public String userName;
    public String workshopId;
    public String workshopName;
    public String serviceId;
    public String serviceName;
    public String carLabel;
    public String carId;
    public int durationMin = 60;
    public String note;
    public long dateTimeMillis;
    public long createdAt;
    public double price;
    public double commission;
    public BookingStatus status = BookingStatus.PENDING;
    public boolean synced;
    public int rating;

    public Booking() {}

    public Booking(Booking other) {
        plusApplied=other.plusApplied; pickupRequested=other.pickupRequested; prioritySlot=other.prioritySlot; basePrice=other.basePrice;
        id = other.id;
        userId = other.userId;
        userName = other.userName;
        workshopId = other.workshopId;
        workshopName = other.workshopName;
        serviceId = other.serviceId;
        serviceName = other.serviceName;
        carLabel = other.carLabel;
        carId = other.carId;
        durationMin = other.durationMin;
        note = other.note;
        dateTimeMillis = other.dateTimeMillis;
        createdAt = other.createdAt;
        price = other.price;
        commission = other.commission;
        status = other.status;
        synced = other.synced;
        rating = other.rating;
    }
}
