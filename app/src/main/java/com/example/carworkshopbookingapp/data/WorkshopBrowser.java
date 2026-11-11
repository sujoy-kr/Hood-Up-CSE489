package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorkshopBrowser {
    public enum Filter { ALL, OPEN_NOW, TOP_RATED }
    public static final double TOP_RATING = 4.5;

    private WorkshopBrowser() {}

    /** Equal opening/closing hours represent 24-hour operation; closing time is exclusive. */
    public static boolean isOpen(Workshop workshop, int minuteOfDay) {
        java.util.Calendar now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Dhaka"));
        return isOpen(workshop, minuteOfDay, now.get(java.util.Calendar.DAY_OF_WEEK));
    }
    public static boolean isOpen(Workshop workshop, int minuteOfDay, int weekday) {
        int start = workshop.openHour * 60;
        int end = workshop.closeHour * 60;
        boolean allDay = Math.floorMod(end - start, 1440) == 0;
        boolean overnight = end <= start;
        int openingDay = (overnight && minuteOfDay < start) ? (weekday == 1 ? 7 : weekday - 1) : weekday;
        if (workshop.closedDays != null && workshop.closedDays.contains(openingDay)) return false;
        if (allDay) return true;
        return start < end ? minuteOfDay >= start && minuteOfDay < end :
                minuteOfDay >= start || minuteOfDay < end;
    }

    public static double startingPrice(Workshop workshop, Map<String, ServiceItem> services) {
        double price = Double.POSITIVE_INFINITY;
        if (workshop.serviceIds != null) for (String id : workshop.serviceIds) {
            ServiceItem service = services.get(id);
            if (service != null && Double.isFinite(service.basePrice) && service.basePrice >= 0) {
                price = Math.min(price, service.basePrice);
            }
        }
        return price;
    }

    public static List<Workshop> filter(List<Workshop> source, Map<String, ServiceItem> services,
                                         String query, Filter filter, int minuteOfDay) {
        String needle = safe(query).trim().toLowerCase(Locale.ROOT);
        List<Workshop> result = new ArrayList<>();
        for (Workshop item : source) {
            if (filter == Filter.OPEN_NOW && !isOpen(item, minuteOfDay)) continue;
            if (filter == Filter.TOP_RATED && item.rating < TOP_RATING) continue;
            StringBuilder haystack = new StringBuilder(safe(item.name)).append(' ')
                    .append(safe(item.area)).append(' ').append(safe(item.address));
            if (item.serviceIds != null) for (String id : item.serviceIds) {
                ServiceItem service = services.get(id);
                if (service != null) haystack.append(' ').append(safe(service.name));
            }
            if (haystack.toString().toLowerCase(Locale.ROOT).contains(needle)) result.add(item);
        }
        result.sort(Comparator.comparingDouble((Workshop item) -> -item.rating)
                .thenComparing(item -> safe(item.name), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static String safe(String text) { return text == null ? "" : text; }
}
