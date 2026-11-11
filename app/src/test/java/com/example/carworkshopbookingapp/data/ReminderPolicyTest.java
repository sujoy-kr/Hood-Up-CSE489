package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReminderPolicyTest {
    @Test public void confirmedReminderIsTwoHoursBeforeButNeverScheduledInThePast() {
        Booking booking=new Booking(); booking.status=BookingStatus.CONFIRMED; booking.dateTimeMillis=20000000L;
        assertEquals(12800000L,ReminderPolicy.triggerAt(booking,10000000L));
        assertEquals(0,ReminderPolicy.triggerAt(booking,12800000L));
        assertEquals(0,ReminderPolicy.triggerAt(booking,15000000L));
    }
    @Test public void cancellationAndOtherStatusesHaveNoReminder() {
        Booking booking=new Booking(); booking.dateTimeMillis=20000000L;
        for (BookingStatus status:BookingStatus.values()) if (status!=BookingStatus.CONFIRMED) {
            booking.status=status; assertEquals(0,ReminderPolicy.triggerAt(booking,10000000L));
        }
    }
}
