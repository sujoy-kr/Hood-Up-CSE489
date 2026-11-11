package com.example.carworkshopbookingapp.data;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class BookingPricingTest {
    @Test public void commissionUsesTenPercentAndRoundsToMoneyPrecision() {
        assertEquals(125.0, BookingPricing.commission(1250), 0.000001);
        assertEquals(1.01, BookingPricing.commission(10.05), 0.000001);
        assertEquals(0.0, BookingPricing.commission(0), 0.000001);
    }

    @Test public void rejectsInvalidPrices() {
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> BookingPricing.commission(invalid));
        }
    }
}
