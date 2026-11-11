package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.ui.adapter.BookingAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AdminBookingsActivity extends AdminActivity {
    private List<Booking> bookings = new ArrayList<>();
    private List<Workshop> workshops = new ArrayList<>();
    private BookingStatus status;
    private String workshopId = "";
    private BookingAdapter adapter;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (saved != null) {
            String value = saved.getString("status_filter");
            status = value == null ? null : BookingStatus.valueOf(value);
            workshopId = saved.getString("workshop_filter", "");
        }
    }

    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putString("status_filter", status == null ? null : status.name());
        saved.putString("workshop_filter", workshopId);
        super.onSaveInstanceState(saved);
    }

    @Override protected void ready() {
        header("Bookings", false, true);
        label(body, "Status", R.style.FieldLabel, 4);
        GridLayout chips = new GridLayout(this);
        chips.setColumnCount(3);
        LinearLayout.LayoutParams chipsParams = new LinearLayout.LayoutParams(-1, -2);
        chipsParams.setMargins(-dp(4), 0, -dp(4), dp(12));
        body.addView(chips, chipsParams);
        for (int i = -1; i < BookingStatus.values().length; i++) {
            BookingStatus pick = i < 0 ? null : BookingStatus.values()[i];
            Button chip = new Button(this, null, 0, status == pick ? R.style.SelectedChip : R.style.Chip);
            chip.setText(pick == null ? "All" : getString(BookingDisplay.label(pick)));
            chip.setSelected(status == pick);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            chips.addView(chip, params);
            chip.setOnClickListener(view -> { status = pick; reset(); ready(); });
        }
        label(body, "Workshop", R.style.FieldLabel, 8);
        List<String> placeholder = new ArrayList<>();
        placeholder.add("All workshops");
        Spinner spinner = spinner("Filter by workshop", placeholder);
        final TextView pending = loading();
        ListView list = new ListView(this);
        showList(list);
        adapter = new BookingAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index < 0 || index >= adapter.getCount()) return;
            startActivity(new Intent(this, AdminBookingDetailActivity.class)
                    .putExtra("bookingId", adapter.getItem(index).id));
        });
        repo.list("workshops", Workshop.class, new ScreenCallback<List<Workshop>>() {
            @Override protected void success(List<Workshop> result) {
                workshops = result;
                List<String> labels = new ArrayList<>();
                labels.add("All workshops");
                int selected = 0;
                for (Workshop workshop : result) {
                    labels.add(workshop.name);
                    if (workshop.id.equals(workshopId)) selected = labels.size() - 1;
                }
                spinner.setAdapter(new android.widget.ArrayAdapter<>(AdminBookingsActivity.this, R.layout.spinner_item, labels));
                spinner.setSelection(selected);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override public void onNothingSelected(AdapterView<?> parent) { }
                    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        workshopId = position == 0 ? "" : workshops.get(position - 1).id;
                        filter();
                    }
                });
            }
        });
        repo.list("bookings", Booking.class, new ScreenCallback<List<Booking>>() {
            @Override protected void failure(Exception e) { pending.setVisibility(View.GONE); fail(e); }
            @Override protected void success(List<Booking> result) {
                pending.setVisibility(View.GONE);
                // Server results are never waiting to sync.
                for (Booking booking : result) booking.synced = true;
                bookings = result;
                filter();
            }
        });
    }

    private void filter() {
        List<Booking> out = new ArrayList<>();
        for (Booking b : bookings) {
            if ((status == null || b.status == status) && (workshopId.isEmpty() || workshopId.equals(b.workshopId))) out.add(b);
        }
        Collections.sort(out, (a, b) -> Long.compare(b.dateTimeMillis, a.dateTimeMillis));
        adapter.submit(out);
        if (out.isEmpty()) error.setText("No bookings match these filters.");
        error.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
