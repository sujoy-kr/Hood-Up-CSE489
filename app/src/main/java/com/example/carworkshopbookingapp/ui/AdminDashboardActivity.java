package com.example.carworkshopbookingapp.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.AdminLogic;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.remote.AuthRepository;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class AdminDashboardActivity extends AdminActivity {
    @Override protected void ready() {
        header("Admin dashboard", true, true);
        headerAction("Log out", () -> new AuthRepository(getApplicationContext())
                .signOut(mutation(() -> EntryNavigation.login(this))));
        section("Overview");
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        body.addView(stats, new LinearLayout.LayoutParams(-1, -2));
        TextView loading = label(stats, "Loading statistics…", R.style.MutedText, 16);
        section("Manage");
        entry(R.drawable.ic_calendar, "Bookings", "Review appointments and update status",
                () -> open(AdminBookingsActivity.class));
        entry(R.drawable.ic_store, "Workshops", "Edit listings, hours and offered services",
                () -> open(AdminWorkshopsActivity.class));
        entry(R.drawable.ic_wrench, "Services", "Set prices, durations and keywords",
                () -> open(AdminServicesActivity.class));
        repo.list("bookings", Booking.class, new ScreenCallback<List<Booking>>() {
            @Override protected void failure(Exception error) { loading.setVisibility(View.GONE); fail(error); }
            @Override protected void success(List<Booking> bookings) {
                long now = System.currentTimeMillis();
                AppExecutors.execute(() -> AdminLogic.stats(bookings, now), new ScreenCallback<AdminLogic.Stats>() {
                    @Override protected void failure(Exception error) { loading.setVisibility(View.GONE); fail(error); }
                    @Override protected void success(AdminLogic.Stats result) {
                        loading.setVisibility(View.GONE);
                        showStats(stats, result, now);
                    }
                });
            }
        });
    }

    private void showStats(LinearLayout host, AdminLogic.Stats stats, long now) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.setMargins(-dp(4), 0, -dp(4), dp(12));
        host.addView(grid, gridParams);
        stat(grid, "Appointments today", Integer.toString(stats.today), R.color.text);
        stat(grid, "Pending bookings", Integer.toString(stats.pending), R.color.warning);
        stat(grid, "Completed this month", Integer.toString(stats.month), R.color.success);
        stat(grid, "Commission this month", WorkshopDisplay.money(stats.commission), R.color.primary);

        LinearLayout week = card(host);
        label(week, "Appointments · last 7 days", R.style.SectionHeader, 4);
        int total = 0;
        for (int count : stats.days) total += count;
        if (total == 0) {
            label(week, "No appointments scheduled in the last 7 days.", R.style.MutedText, 0);
            return;
        }
        label(week, "Grouped by appointment date in Dhaka.", R.style.MutedText, 8);
        TableLayout table = new TableLayout(this);
        table.setColumnStretchable(0, true);
        week.addView(table, new LinearLayout.LayoutParams(-1, -2));
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"));
        day.setTimeInMillis(now);
        day.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat date = new SimpleDateFormat("EEE, d MMM", Locale.US);
        date.setTimeZone(day.getTimeZone());
        for (int count : stats.days) {
            dateRow(table, date.format(day.getTime()), count);
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void stat(GridLayout grid, String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        TextView number = new TextView(this, null, 0, R.style.ScreenTitle);
        number.setText(value);
        number.setTextColor(ContextCompat.getColor(this, color));
        card.addView(number);
        TextView title = new TextView(this, null, 0, R.style.Caption);
        title.setText(label);
        title.setPadding(0, dp(4), 0, 0);
        card.addView(title);
        card.setContentDescription(label + ": " + value);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setGravity(Gravity.FILL);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(card, params);
    }

    private void dateRow(TableLayout table, String date, int count) {
        TableRow row = new TableRow(this);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView day = new TextView(this, null, 0, count == 0 ? R.style.MutedText : R.style.TextBody);
        day.setText(date);
        row.addView(day);
        TextView number = new TextView(this, null, 0, count == 0 ? R.style.MutedText : R.style.SectionHeader);
        number.setText(Integer.toString(count));
        number.setGravity(Gravity.END);
        row.addView(number);
        table.addView(row);
    }
}
