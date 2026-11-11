package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BookingHistory {
    private BookingHistory() {}
    public static boolean terminal(BookingStatus status) {
        return status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED;
    }
    public static boolean canCancel(Booking booking) {
        return booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED;
    }
    /** Active appointments stay Upcoming even when overdue; only terminal states enter History. */
    public static List<Booking> filter(List<Booking> all, boolean history) {
        List<Booking> result = new ArrayList<>();
        for (Booking booking : all) if (terminal(booking.status) == history) result.add(booking);
        Comparator<Booking> order = Comparator.comparingLong(item -> item.dateTimeMillis);
        result.sort(history ? order.reversed() : order);
        return result;
    }
}
