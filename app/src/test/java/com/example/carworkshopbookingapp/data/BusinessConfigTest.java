package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BusinessConfigTest {
    @Test public void fullServicePriceAndCommissionAreUsed() {
        assertEquals(1000, BusinessConfig.price(1000), 0.000001);
        assertEquals(100, BookingPricing.commission(BusinessConfig.price(1000)), 0.000001);
        assertEquals(10.70, BusinessConfig.price(10.70), 0.000001);
        assertEquals(1.07, BookingPricing.commission(BusinessConfig.price(10.70)), 0.000001);
    }
    @Test public void legacyFlagsDoNotChangeReminderTiming() {
        Booking booking = new Booking(); booking.plusApplied = true;
        booking.status = BookingStatus.CONFIRMED; booking.dateTimeMillis = 20000000;
        assertEquals(12800000, ReminderPolicy.triggerAt(booking, 0));
    }
    @Test public void firstOpeningHourIsAvailableToEveryone() {
        Workshop workshop = new Workshop(); workshop.openHour = 9; workshop.closeHour = 12;
        long day = BookingSlots.day(2026, 10, 20);
        assertEquals(3, BookingSlots.slots(workshop, 60, day, day).size());
        assertEquals(day + 9 * BookingSlots.HOUR, (long) BookingSlots.slots(workshop,60,day,day).get(0));
    }
    @Test public void moneyRoundsToCentsBeforeCommission() {
        assertEquals(10.71, BusinessConfig.price(10.705), 0.000001);
        assertEquals(0.12, BookingPricing.commission(1.15), 0.000001);
        assertEquals(0, BusinessConfig.price(0), 0.000001);
    }
    @Test public void invalidPricesAreRejected() {
        for (double value : new double[]{-0.01,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> BusinessConfig.price(value));
    }
}
