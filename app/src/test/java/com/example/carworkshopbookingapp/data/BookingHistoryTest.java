package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BookingHistoryTest {
    @Test public void activeOverdueAndOfflineBookingsStayUpcomingInDateOrder() {
        Booking pending = booking(BookingStatus.PENDING, 10); pending.synced = false;
        Booking ready = booking(BookingStatus.READY, 20);
        Booking done = booking(BookingStatus.COMPLETED, 30);
        List<Booking> result = BookingHistory.filter(Arrays.asList(done, ready, pending), false);
        assertEquals(Arrays.asList(pending, ready), result);
        assertTrue(BookingHistory.canCancel(pending));
        assertFalse(BookingHistory.canCancel(ready));
    }
    @Test public void historyContainsOnlyTerminalBookingsNewestFirst() {
        Booking cancelled = booking(BookingStatus.CANCELLED, 10);
        Booking completed = booking(BookingStatus.COMPLETED, 20);
        assertEquals(Arrays.asList(completed, cancelled), BookingHistory.filter(
                Arrays.asList(cancelled, booking(BookingStatus.IN_PROGRESS, 30), completed), true));
        assertFalse(BookingHistory.canCancel(completed));
        assertFalse(BookingHistory.canCancel(cancelled));
        assertTrue(BookingHistory.canCancel(booking(BookingStatus.CONFIRMED, 0)));
    }
    private Booking booking(BookingStatus status, long time) {
        Booking booking = new Booking(); booking.status = status; booking.dateTimeMillis = time; return booking;
    }
}
