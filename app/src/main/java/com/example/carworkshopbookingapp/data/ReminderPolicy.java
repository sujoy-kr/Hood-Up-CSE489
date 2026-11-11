package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;

public final class ReminderPolicy {
    private ReminderPolicy() {}
    public static long triggerAt(Booking booking,long now) {
        long trigger=booking.dateTimeMillis-BusinessConfig.STANDARD_REMINDER_MS;
        return booking.status==BookingStatus.CONFIRMED && trigger>now ? trigger : 0;
    }
}
