package com.example.carworkshopbookingapp.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class WorkshopDisplayTest {
    @Test public void midnightNoonAndEveningHaveUnambiguousMeridiems() {
        assertEquals("12:00 AM", WorkshopDisplay.hour(0));
        assertEquals("12:00 PM", WorkshopDisplay.hour(12));
        assertEquals("11:00 PM", WorkshopDisplay.hour(23));
        assertEquals("12:00 AM", WorkshopDisplay.hour(24));
        for (int hour = 0; hour < 24; hour++) assertEquals(hour, WorkshopDisplay.parseHour(WorkshopDisplay.hour(hour)));
        assertEquals("Open 24 hours", WorkshopDisplay.hours(0,0));
        assertEquals("9:00 AM – 8:00 PM", WorkshopDisplay.hours(9,20));
    }
}
