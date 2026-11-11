package com.example.carworkshopbookingapp.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BookingPricing {
    public static final double COMMISSION_RATE = BusinessConfig.COMMISSION_RATE;

    private BookingPricing() {}

    public static double commission(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be finite and nonnegative");
        }
        return BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(COMMISSION_RATE))
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
