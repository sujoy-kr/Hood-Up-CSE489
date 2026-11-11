package com.example.carworkshopbookingapp.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BusinessConfig {
    public static final int MAX_SERVICE_DURATION_MIN = 300;
    public static final double COMMISSION_RATE = 0.10;
    public static final long STANDARD_REMINDER_MS = 2 * 3600000L;
    private BusinessConfig() {}
    public static double price(double base) {
        if (!Double.isFinite(base) || base < 0) throw new IllegalArgumentException("Invalid price");
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
