package com.example.carworkshopbookingapp.util;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class WorkshopDisplay {
    private WorkshopDisplay() {}
    public static int dhakaMinute() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }
    public static String money(double price) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(2);
        return "৳" + format.format(price);
    }
    public static String hour(int value) {
        int hour = Math.floorMod(value, 24);
        return (hour % 12 == 0 ? 12 : hour % 12) + ":00 " + (hour < 12 ? "AM" : "PM");
    }
    public static int parseHour(String text) {
        java.util.regex.Matcher match = java.util.regex.Pattern.compile("^(1[0-2]|[1-9]):00 (AM|PM)$")
                .matcher(text.trim().toUpperCase(Locale.US));
        if (!match.matches()) throw new IllegalArgumentException("Choose a time on the hour.");
        return Integer.parseInt(match.group(1)) % 12 + ("PM".equals(match.group(2)) ? 12 : 0);
    }
    public static String hours(int open, int close) {
        return Math.floorMod(open, 24) == Math.floorMod(close, 24)
                ? "Open 24 hours" : hour(open) + " – " + hour(close);
    }
}
