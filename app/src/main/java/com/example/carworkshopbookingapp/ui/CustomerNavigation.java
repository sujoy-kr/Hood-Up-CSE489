package com.example.carworkshopbookingapp.ui;

import android.app.Activity;
import android.content.Intent;
import com.example.carworkshopbookingapp.R;

final class CustomerNavigation {
    private CustomerNavigation() {}

    static void bind(Activity activity) {
        bind(activity, R.id.nav_home, HomeActivity.class);
        bind(activity, R.id.nav_bookings, MyBookingsActivity.class);
        bind(activity, R.id.nav_profile, ProfileActivity.class);
    }

    private static void bind(Activity activity, int id, Class<?> destination) {
        activity.findViewById(id).setSelected(activity.getClass() == destination);
        activity.findViewById(id).setOnClickListener(view -> {
            if (activity.getClass() == destination) return;
            activity.startActivity(new Intent(activity, destination)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            if (!(activity instanceof HomeActivity)) activity.finish();
        });
    }
}
