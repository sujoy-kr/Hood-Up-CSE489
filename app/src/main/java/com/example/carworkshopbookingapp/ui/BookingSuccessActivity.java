package com.example.carworkshopbookingapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.BookingSubmission;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class BookingSuccessActivity extends BaseActivity {
    static Intent intent(Context context, BookingSubmission.Receipt receipt) {
        Booking b = receipt.booking;
        return new Intent(context, BookingSuccessActivity.class).putExtra("queued", receipt.queued)
                .putExtra("bookingId", b.id).putExtra("workshop", b.workshopName).putExtra("service", b.serviceName)
                .putExtra("car", b.carLabel).putExtra("time", b.dateTimeMillis).putExtra("price", b.price);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_booking_success);
        boolean queued = getIntent().getBooleanExtra("queued", false);
        ((TextView) findViewById(R.id.success_title)).setText(queued ? R.string.booking_queue_title : R.string.booking_sent);
        ((TextView) findViewById(R.id.success_message)).setText(queued ? getString(R.string.booking_queued) + "\n\n" +
                getString(R.string.booking_queue_explainer) : getString(R.string.booking_pending_explainer));
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy · h:mm a", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        ((TextView) findViewById(R.id.success_details)).setText(getString(R.string.booking_success_details,
                getIntent().getStringExtra("workshop"), getIntent().getStringExtra("service"), getIntent().getStringExtra("car"),
                format.format(new Date(getIntent().getLongExtra("time", 0))) + " (Dhaka)",
                WorkshopDisplay.money(getIntent().getDoubleExtra("price", 0))));
        ((TextView) findViewById(R.id.success_reference)).setText(getString(R.string.booking_reference, getIntent().getStringExtra("bookingId")));
        findViewById(R.id.back).setOnClickListener(view -> finish());
        findViewById(R.id.view_bookings).setOnClickListener(view -> {
            startActivity(new Intent(this, MyBookingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
    }
}
