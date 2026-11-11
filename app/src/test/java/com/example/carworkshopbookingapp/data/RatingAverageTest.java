package com.example.carworkshopbookingapp.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class RatingAverageTest {
    @Test public void newReviewsAndEditsDoNotDoubleCount() {
        RatingAverage first = RatingAverage.apply(0, 0, 0, 5);
        assertEquals(1, first.count); assertEquals(5, first.average, 0.00001);
        RatingAverage second = RatingAverage.apply(first.count, first.sum, 0, 3);
        assertEquals(2, second.count); assertEquals(4, second.average, 0.00001);
        RatingAverage edited = RatingAverage.apply(second.count, second.sum, 5, 1);
        assertEquals(2, edited.count); assertEquals(2, edited.average, 0.00001);
        RatingAverage retry = RatingAverage.apply(edited.count, edited.sum, 1, 1);
        assertEquals(edited.count, retry.count); assertEquals(edited.sum, retry.sum, 0.00001);
    }
    @Test public void invalidStarsAndBrokenAggregatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RatingAverage.apply(0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> RatingAverage.apply(0, 0, 0, 6));
        assertThrows(IllegalArgumentException.class, () -> RatingAverage.apply(0, 0, 5, 3));
        assertThrows(IllegalArgumentException.class, () -> RatingAverage.apply(2, 20, 0, 3));
    }
}
