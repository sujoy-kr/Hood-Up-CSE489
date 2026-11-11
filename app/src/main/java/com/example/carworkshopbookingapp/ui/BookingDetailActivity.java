package com.example.carworkshopbookingapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.BookingHistory;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.data.remote.WorkshopsRepository;
import com.example.carworkshopbookingapp.util.NetworkState;

public final class BookingDetailActivity extends BookingFeedActivity {
    public static final String EXTRA_BOOKING_ID = "bookingId";
    private String id, phone;
    private Booking booking;
    private BookingDisplay.Fields fields;
    private FormOperation<Booking> operation;
    private boolean foreground;
    private int stars;
    private int lastSavedRating = -1;

    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_booking_detail);
        id = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        Object retained = getLastCustomNonConfigurationInstance();
        operation = retained instanceof FormOperation ? (FormOperation<Booking>) retained : new FormOperation<>();
        stars = state == null ? 0 : state.getInt("stars");
        fields = new BookingDisplay.Fields(findViewById(R.id.booking_content));
        findViewById(R.id.back).setOnClickListener(view -> { if (!operation.busy) finish(); });
        findViewById(R.id.refresh).setOnClickListener(view -> { if (!operation.busy) reload(); });
        findViewById(R.id.cancel_booking).setOnClickListener(view -> {
            if (operation.busy || booking == null || !BookingHistory.canCancel(booking)) return;
            new AlertDialog.Builder(this).setTitle(R.string.booking_cancel_title)
                    .setMessage(booking.synced ? R.string.booking_cancel_body : R.string.booking_cancel_queued)
                    .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.booking_cancel, (dialog, which) -> {
                        if (!operation.busy && booking != null) store.cancel(booking, operation.begin());
                    }).show();
        });
        int[] buttons = {R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5};
        for (int i = 0; i < buttons.length; i++) {
            final int rating = i + 1;
            findViewById(buttons[i]).setOnClickListener(view -> { if (!operation.busy) { stars = rating; render(); } });
        }
        findViewById(R.id.save_rating).setOnClickListener(view -> {
            if (operation.busy || booking == null || booking.status != BookingStatus.COMPLETED) return;
            if (stars < 1 || stars > 5) { showError(getString(R.string.rating_required)); return; }
            store.rate(user.uid, id, stars, operation.begin());
        });
        findViewById(R.id.call_workshop).setOnClickListener(view -> {
            if (phone == null || phone.isEmpty()) { Toast.makeText(this, R.string.phone_unavailable, Toast.LENGTH_SHORT).show(); return; }
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))); }
            catch (ActivityNotFoundException error) { Toast.makeText(this, R.string.external_unavailable, Toast.LENGTH_SHORT).show(); }
        });
        loadCustomer(); render();
    }

    @Override protected void onBookingsChanged() {
        if (operation == null) return;
        Booking found = null;
        for (Booking item : bookings) if (item.id.equals(id)) { found = item; break; }
        String oldWorkshop = booking == null ? null : booking.workshopId;
        booking = found;
        if (booking != null && !booking.workshopId.equals(oldWorkshop)) loadPhone(booking.workshopId);
        if (booking != null && booking.rating != lastSavedRating) {
            if (lastSavedRating >= 0 || stars == 0) stars = booking.rating;
            lastSavedRating = booking.rating;
        }
        render();
    }

    private void loadPhone(String workshopId) {
        phone = null;
        new WorkshopsRepository(getApplicationContext()).get(workshopId, new Callback<Workshop>() {
            @Override public void onSuccess(Workshop workshop) {
                if (!isDestroyed() && booking != null && workshop.id.equals(booking.workshopId)) { phone = workshop.phone; render(); }
            }
            @Override public void onError(Exception error) { /* Cached phone remains usable after a refresh error. */ }
        });
    }

    private void render() {
        if (operation == null || isFinishing() || isDestroyed()) return;
        if (foreground && operation.complete) {
            booking = operation.result; operation.complete = false;
            stars = booking.rating; lastSavedRating = booking.rating;
            for (int i = 0; i < bookings.size(); i++) if (bookings.get(i).id.equals(id)) bookings.set(i, booking);
            reload();
        }
        FormViews.enabled(findViewById(R.id.main), !operation.busy);
        findViewById(R.id.refresh).setEnabled(!operation.busy && !loading && user != null);
        findViewById(R.id.form_progress).setVisibility(loading || operation.busy ? View.VISIBLE : View.GONE);
        TextView state = findViewById(R.id.detail_state);
        int message = failed ? R.string.bookings_refresh_error : loading && booking == null ? R.string.bookings_loading :
                booking == null ? (!NetworkState.online(this) ? R.string.booking_offline_missing : R.string.booking_unavailable) : !NetworkState.online(this) ? R.string.bookings_offline : 0;
        state.setVisibility(message == 0 ? View.GONE : View.VISIBLE); if(failed&&bookingError!=null)state.setText(bookingError);else if (message != 0) state.setText(message);
        findViewById(R.id.booking_content).setVisibility(booking == null ? View.GONE : View.VISIBLE);
        if (operation.error != null) showError(FormViews.error(this, operation.error));
        else findViewById(R.id.form_error).setVisibility(View.GONE);
        if (booking == null) return;
        fields.bind(booking);
        ((TextView) findViewById(R.id.booking_duration)).setText(getString(R.string.booking_duration_value, booking.durationMin));
        ((TextView) findViewById(R.id.booking_created)).setText(getString(R.string.booking_created_value, BookingDisplay.time(booking.createdAt)));
        ((TextView) findViewById(R.id.booking_note)).setText(getString(R.string.booking_note_value, booking.note == null ? "" : booking.note));
        ((TextView) findViewById(R.id.booking_ref)).setText(getString(R.string.booking_reference, booking.id));
        findViewById(R.id.cancel_booking).setVisibility(BookingHistory.canCancel(booking) ? View.VISIBLE : View.GONE);
        findViewById(R.id.call_workshop).setEnabled(!operation.busy && phone != null && !phone.isEmpty());
        timeline();
        findViewById(R.id.rating_section).setVisibility(booking.status == BookingStatus.COMPLETED ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.rating_label)).setText(stars == 0 ? getString(R.string.booking_rating_select) : getString(R.string.booking_rating_value, stars));
        int[] buttons = {R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5};
        for (int i = 0; i < buttons.length; i++) {
            Button star = findViewById(buttons[i]); star.setText(i < stars ? R.string.rating_full_star : R.string.rating_empty_star);
            star.setContentDescription(getString(R.string.rating_stars_description, i + 1)); star.setSelected(i + 1 == stars);
            star.setTextColor(ContextCompat.getColor(this, i < stars ? R.color.warning : R.color.text_muted));
        }
        findViewById(R.id.save_rating).setEnabled(!operation.busy && stars >= 1 && stars <= 5 && stars != booking.rating);
    }

    private void timeline() {
        LinearLayout timeline = findViewById(R.id.status_timeline); timeline.removeAllViews();
        if (booking.status == BookingStatus.CANCELLED) {
            TextView text = new TextView(this); text.setTextAppearance(R.style.TextBody); text.setText(R.string.booking_cancelled_timeline);
            text.setTextColor(ContextCompat.getColor(this, R.color.danger)); timeline.addView(text);
            return;
        }
        int[] labels = {R.string.timeline_booked, R.string.timeline_confirmed, R.string.timeline_in_progress, R.string.timeline_ready, R.string.timeline_completed};
        int current = booking.status == BookingStatus.CANCELLED ? -1 : booking.status == null ? 0 : booking.status.ordinal();
        int gap = getResources().getDimensionPixelSize(R.dimen.spacing), dot = gap * 3 / 4;
        for (int i = 0; i < labels.length; i++) {
            int tone = i < current ? R.color.accent : i == current ? R.color.primary : R.color.stroke;
            if (i > 0) {
                View line = new View(this); line.setBackgroundColor(ContextCompat.getColor(this, i <= current ? R.color.accent : R.color.stroke));
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(gap / 8, gap);
                lineParams.setMarginStart(dot / 2 - gap / 16); lineParams.topMargin = lineParams.bottomMargin = gap / 4;
                timeline.addView(line, lineParams);
            }
            LinearLayout row = new LinearLayout(this); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            View marker = new View(this); marker.setBackgroundResource(R.drawable.bg_dot);
            marker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, tone)));
            row.addView(marker, new LinearLayout.LayoutParams(dot, dot));
            TextView step = new TextView(this); step.setTextAppearance(i == current ? R.style.SectionHeader : R.style.TextBody);
            step.setText(i == current ? getString(R.string.timeline_current, getString(labels[i])) : getString(labels[i]));
            step.setTextColor(ContextCompat.getColor(this, i == current ? R.color.primary : i < current ? R.color.text : R.color.text_muted));
            LinearLayout.LayoutParams stepParams = new LinearLayout.LayoutParams(-1, -2); stepParams.setMarginStart(gap * 3 / 4);
            row.addView(step, stepParams);
            timeline.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }
    private void showError(String message) { TextView error = findViewById(R.id.form_error); error.setText(message); error.setVisibility(View.VISIBLE); }
    @Override protected void onStart() { super.onStart(); foreground = true; operation.listener = this::render; render(); }
    @Override protected void onStop() { foreground = false; operation.listener = null; super.onStop(); }
    @Override public Object onRetainCustomNonConfigurationInstance() { return operation; }
    @Override protected void onSaveInstanceState(Bundle state) { state.putInt("stars", stars); super.onSaveInstanceState(state); }
    @Override protected boolean allowBack() { return operation == null || !operation.busy; }
}
