package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.BookingHistory;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.ui.adapter.BookingAdapter;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.List;

public final class MyBookingsActivity extends BookingFeedActivity {
    private com.example.carworkshopbookingapp.receiver.OfflineBanner offlineBanner;
    private boolean history;
    private View header;
    private BookingAdapter adapter;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_bookings);
        offlineBanner = new com.example.carworkshopbookingapp.receiver.OfflineBanner(findViewById(R.id.offline_banner));
        history = state != null && state.getBoolean("history");
        adapter = new BookingAdapter(this);
        ListView list = findViewById(R.id.booking_list);
        header = LayoutInflater.from(this).inflate(R.layout.header_bookings, list, false);
        list.addHeaderView(header, null, false); list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index >= 0 && index < adapter.getCount()) startActivity(new Intent(this, BookingDetailActivity.class)
                    .putExtra(BookingDetailActivity.EXTRA_BOOKING_ID, adapter.getItem(index).id));
        });
        header.findViewById(R.id.refresh).setOnClickListener(view -> reload());
        header.findViewById(R.id.tab_upcoming).setOnClickListener(view -> { history = false; onBookingsChanged(); });
        header.findViewById(R.id.tab_history).setOnClickListener(view -> { history = true; onBookingsChanged(); });
        CustomerNavigation.bind(this); onBookingsChanged(); loadCustomer();
    }
    @Override protected void onBookingsChanged() {
        if (adapter == null) return;
        List<Booking> visible = BookingHistory.filter(bookings, history); adapter.submit(visible);
        int[] chips = {R.id.tab_upcoming, R.id.tab_history};
        for (int i = 0; i < chips.length; i++) {
            Button chip = header.findViewById(chips[i]); boolean selected = history == (i == 1);
            chip.setSelected(selected); chip.setBackgroundResource(selected ? R.drawable.chip_selected : R.drawable.chip_default);
            chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.accent : R.color.text_muted));
        }
        header.findViewById(R.id.refresh).setEnabled(!loading && user != null);
        ((Button)header.findViewById(R.id.refresh)).setText(failed?R.string.retry:R.string.refresh);
        header.findViewById(R.id.form_progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        TextView state = header.findViewById(R.id.bookings_state);
        int message = failed ? R.string.bookings_refresh_error : loading && bookings.isEmpty() ? R.string.bookings_loading :
                visible.isEmpty() ? (bookings.isEmpty() && !NetworkState.online(this) ? R.string.bookings_offline_empty : R.string.bookings_empty) : !NetworkState.online(this) ? R.string.bookings_offline : 0;
        state.setVisibility(message == 0 ? View.GONE : View.VISIBLE); if(failed&&bookingError!=null)state.setText(bookingError);else if (message != 0) state.setText(message);
    }
    @Override protected void onStart() { super.onStart(); offlineBanner.start(this); }
    @Override protected void onStop() { unregisterReceiver(offlineBanner); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("history", history); super.onSaveInstanceState(state); }
}
