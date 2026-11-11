package com.example.carworkshopbookingapp.ui;

import android.app.Activity;
import android.content.Intent;
import com.example.carworkshopbookingapp.data.model.User;

final class EntryNavigation {
    private EntryNavigation() {}

    static void home(Activity activity, User user) {
        open(activity, "admin".equals(user.role) ? AdminDashboardActivity.class : HomeActivity.class);
    }

    static void login(Activity activity) { open(activity, LoginActivity.class); }

    private static void open(Activity activity, Class<?> destination) {
        activity.startActivity(new Intent(activity, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
}
