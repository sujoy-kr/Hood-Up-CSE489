package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import com.example.carworkshopbookingapp.data.model.Workshop;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BookingSlotsTest {
    private final long monday = BookingSlots.day(2026, Calendar.NOVEMBER, 16);

    @Test public void durationMustFitAndPastStartIsExcluded() {
        Workshop workshop = workshop(9, 12);
        List<Long> slots = BookingSlots.slots(workshop, 90, monday, monday);
        assertEquals(2, slots.size());
        assertEquals(monday + 9 * BookingSlots.HOUR, (long) slots.get(0));
        assertEquals(monday + 10 * BookingSlots.HOUR, (long) slots.get(1));
        assertEquals(1, BookingSlots.slots(workshop, 90, monday, monday + 9 * BookingSlots.HOUR).size());
        assertFalse(BookingSlots.allowed(workshop, 90, monday + 11 * BookingSlots.HOUR, monday));
    }

    @Test public void closedDaysAndOvernightOpeningAreRespected() {
        Workshop workshop = workshop(22, 2);
        List<Long> slots = BookingSlots.slots(workshop, 60, monday, monday);
        assertEquals(4, slots.size());
        assertTrue(slots.contains(monday + BookingSlots.DAY + BookingSlots.HOUR));
        assertTrue(BookingSlots.allowed(workshop, 60, monday + BookingSlots.DAY + BookingSlots.HOUR, monday));
        workshop.closedDays = Collections.singletonList(Calendar.MONDAY);
        assertTrue(BookingSlots.slots(workshop, 60, monday, monday).isEmpty());
        assertFalse(BookingSlots.allowed(workshop, 60, monday + BookingSlots.DAY + BookingSlots.HOUR, monday));
    }

    @Test public void overlapsUseDurationButAllowAdjacentAndCancelledBookings() {
        Booking other = new Booking(); other.dateTimeMillis = monday + 9 * BookingSlots.HOUR; other.durationMin = 90;
        assertTrue(BookingSlots.overlaps(monday + 10 * BookingSlots.HOUR, 60, other));
        assertFalse(BookingSlots.overlaps(monday + 11 * BookingSlots.HOUR, 60, other));
        assertFalse(BookingSlots.overlaps(monday + 8 * BookingSlots.HOUR, 60, other));
        other.status = BookingStatus.CANCELLED;
        assertFalse(BookingSlots.overlaps(monday + 9 * BookingSlots.HOUR, 60, other));
        assertEquals(2, BookingSlots.locks(monday + 9 * BookingSlots.HOUR, 90).size());
    }

    @Test public void invalidDurationsProduceNoSlots() {
        assertTrue(BookingSlots.slots(workshop(9, 20), 0, monday, monday).isEmpty());
        assertTrue(BookingSlots.slots(workshop(9, 20), 1500, monday, monday).isEmpty());
    }

    private Workshop workshop(int open, int close) {
        Workshop result = new Workshop(); result.openHour = open; result.closeHour = close; return result;
    }
    @Test public void hourlyOccupancyLocksCoverRemainderAndReleaseAtExactEnd() {
        long start=monday+10*BookingSlots.HOUR;
        assertEquals(java.util.Arrays.asList(start),BookingSlots.locks(start,60));
        assertEquals(java.util.Arrays.asList(start,start+BookingSlots.HOUR),BookingSlots.locks(start,61));
        Booking occupied=new Booking();occupied.dateTimeMillis=start;occupied.durationMin=120;
        assertTrue(BookingSlots.overlaps(start+BookingSlots.HOUR,60,occupied));
        assertFalse(BookingSlots.overlaps(start+2*BookingSlots.HOUR,60,occupied));
    }
    @Test public void allDayShopHasAllHoursAndAllowsFinishingAfterMidnight() {
        Workshop workshop = workshop(0, 0);
        assertEquals(24, BookingSlots.slots(workshop, 90, monday, monday - 1).size());
        assertTrue(BookingSlots.allowed(workshop, 90, monday + 23 * BookingSlots.HOUR, monday));
        workshop.closedDays = Collections.singletonList(Calendar.TUESDAY);
        assertFalse(BookingSlots.slots(workshop, 90, monday, monday).contains(monday + 23 * BookingSlots.HOUR));
        assertTrue(BookingSlots.slots(workshop, 60, monday, monday).contains(monday + 23 * BookingSlots.HOUR));
    }
}
