package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import com.example.carworkshopbookingapp.data.model.Workshop;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class BookingSlots {
    public static final long HOUR = 3600000L;
    public static final long DAY = 24 * HOUR;
    private BookingSlots() {}

    public static Calendar calendar(long time) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        result.setTimeInMillis(time);
        return result;
    }

    public static long day(int year, int month, int day) {
        Calendar result = calendar(0);
        result.clear(); result.set(year, month, day);
        return result.getTimeInMillis();
    }

    public static long dayOf(long time) {
        Calendar date = calendar(time);
        return day(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
    }

    public static boolean closed(Workshop workshop, long openingDay) {
        return workshop.closedDays != null && workshop.closedDays.contains(calendar(openingDay).get(Calendar.DAY_OF_WEEK));
    }

    public static List<Long> slots(Workshop workshop, int durationMin, long openingDay, long now) {
        List<Long> result = new ArrayList<>();
        if (durationMin <= 0 || durationMin > 24 * 60 || workshop.openHour < 0 || workshop.openHour > 23 ||
                workshop.closeHour < 0 || workshop.closeHour > 24 || closed(workshop, openingDay)) return result;
        long start = dayOf(openingDay) + workshop.openHour * HOUR;
        long end = dayOf(openingDay) + workshop.closeHour * HOUR;
        if (end <= start) end += DAY;
        boolean allDay = workshop.openHour % 24 == workshop.closeHour % 24;
        long serviceEnd = allDay && !closed(workshop, dayOf(openingDay) + DAY)
                ? end + durationMin * 60000L : end;
        for (long time = start; time < end && time + durationMin * 60000L <= serviceEnd; time += HOUR) {
            if (time > now) result.add(time);
        }
        return result;
    }

    public static boolean allowed(Workshop workshop, int durationMin, long slot, long now) {
        long date = dayOf(slot);
        return slots(workshop, durationMin, date, now).contains(slot) ||
                slots(workshop, durationMin, date - DAY, now).contains(slot);
    }

    public static List<Long> locks(long slot, int durationMin) {
        List<Long> result = new ArrayList<>();
        for (long time = slot; time < slot + durationMin * 60000L; time += HOUR) result.add(time);
        return result;
    }

    public static boolean overlaps(long start, int durationMin, Booking other) {
        if (other.status == BookingStatus.CANCELLED) return false;
        long end = start + durationMin * 60000L;
        long otherEnd = other.dateTimeMillis + Math.max(1, other.durationMin) * 60000L;
        return start < otherEnd && other.dateTimeMillis < end;
    }
}
