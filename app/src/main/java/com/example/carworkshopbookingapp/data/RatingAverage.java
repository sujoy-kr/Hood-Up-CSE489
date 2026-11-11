package com.example.carworkshopbookingapp.data;

public final class RatingAverage {
    public final long count;
    public final double sum, average;
    private RatingAverage(long count, double sum) { this.count = count; this.sum = sum; average = sum / count; }

    public static RatingAverage apply(long count, double sum, int oldRating, int rating) {
        if (rating < 1 || rating > 5 || oldRating < 0 || oldRating > 5 || count < 0 ||
                !Double.isFinite(sum) || sum < 0 || (count == 0 && sum != 0) ||
                (count > 0 && (sum < count || sum > count * 5.0)) || (oldRating > 0 && (count == 0 || sum < oldRating))) {
            throw new IllegalArgumentException("Invalid rating aggregate");
        }
        return new RatingAverage(count + (oldRating == 0 ? 1 : 0), sum - oldRating + rating);
    }
}
