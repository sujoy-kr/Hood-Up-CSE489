package com.example.carworkshopbookingapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.AdminLogic;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.List;

public final class AdminBookingDetailActivity extends AdminActivity {
    @Override protected void ready() {
        header("Booking details", false, true);
        String id = getIntent().getStringExtra("bookingId");
        final View pending = loading();
        repo.list("bookings", Booking.class, new ScreenCallback<List<Booking>>() {
            @Override protected void failure(Exception e) { pending.setVisibility(View.GONE); fail(e); }
            @Override protected void success(List<Booking> values) {
                pending.setVisibility(View.GONE);
                Booking found = null;
                for (Booking b : values) if (b.id.equals(id)) found = b;
                if (found == null) { muted("Booking not found."); return; }
                found.synced = true; // Loaded from the server, so it is never waiting to sync.
                show(found);
            }
        });
    }

    private void show(Booking b) {
        View summary = getLayoutInflater().inflate(R.layout.row_booking, body, false);
        new BookingDisplay.Fields(summary).bind(b);
        body.addView(summary);

        LinearLayout details = card(body);
        row(details, "Customer", b.userName);
        row(details, "Commission", WorkshopDisplay.money(b.commission));
        row(details, "Duration", b.durationMin + " min");
        row(details, "Note", b.note);
        LinearLayout contact = new LinearLayout(this);
        contact.setOrientation(LinearLayout.VERTICAL);
        body.addView(contact, new LinearLayout.LayoutParams(-1, -2));
        repo.customer(b.userId, new ScreenCallback<User>() {
            @Override protected void success(User u) {
                if (u == null || u.phone == null || u.phone.trim().isEmpty()) return;
                row(details, "Phone", u.phone);
                button(contact, "Call " + u.phone, R.style.SecondaryButton, () -> {
                    try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", u.phone, null))); }
                    catch (ActivityNotFoundException unavailable) {
                        fail(new IllegalStateException("No phone app is installed on this device."));
                    }
                });
            }
        });

        boolean any = false;
        for (BookingStatus next : BookingStatus.values()) {
            if (!AdminLogic.allows(b.status, next)) continue;
            if (!any) { section("Update status"); any = true; }
            String name = getString(BookingDisplay.label(next));
            Runnable confirm = () -> new AlertDialog.Builder(this).setTitle("Update booking?")
                    .setMessage(getString(BookingDisplay.label(b.status)) + " → " + name)
                    .setNegativeButton("Keep", null)
                    .setPositiveButton("Update", (d, w) -> repo.status(b.id, b.status, next, mutation(() -> { reset(); ready(); })))
                    .show();
            if (next == BookingStatus.CANCELLED) danger("Cancel booking", confirm);
            else primary("Mark as " + name.toLowerCase(java.util.Locale.ROOT), confirm);
        }
    }
}
