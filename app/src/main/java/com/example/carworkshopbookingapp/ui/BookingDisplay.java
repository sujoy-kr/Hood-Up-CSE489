package com.example.carworkshopbookingapp.ui;

import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.Booking;
import com.example.carworkshopbookingapp.data.model.BookingStatus;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class BookingDisplay {
    private BookingDisplay() {}
    public static String time(long value) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy · h:mm a", Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        return format.format(new Date(value)) + " (Dhaka)";
    }
    public static int label(BookingStatus status) {
        if (status == null) return R.string.status_pending;
        switch (status) {
            case CONFIRMED: return R.string.status_confirmed;
            case IN_PROGRESS: return R.string.status_in_progress;
            case READY: return R.string.status_ready;
            case COMPLETED: return R.string.status_completed;
            case CANCELLED: return R.string.status_cancelled;
            default: return R.string.status_pending;
        }
    }
    public static final class Fields {
        private final TextView service, workshop, car, time, price, status;
        private final View waiting;
        public Fields(View view) {
            service = view.findViewById(R.id.booking_service); workshop = view.findViewById(R.id.booking_workshop);
            car = view.findViewById(R.id.booking_car); time = view.findViewById(R.id.booking_time);
            price = view.findViewById(R.id.booking_price); status = view.findViewById(R.id.booking_status);
            waiting = view.findViewById(R.id.waiting_sync);
        }
        public void bind(Booking booking) {
            service.setText(booking.serviceName);
            workshop.setText(booking.workshopName);
            car.setText(booking.carLabel);
            time.setText(time(booking.dateTimeMillis));
            price.setText(WorkshopDisplay.money(booking.price));
            status.setText(label(booking.status));
            int shape = R.drawable.status_pending, color = R.color.warning;
            if (booking.status != null) switch (booking.status) {
                case CONFIRMED: shape = R.drawable.status_confirmed; color = R.color.accent; break;
                case IN_PROGRESS: shape = R.drawable.status_in_progress; color = R.color.primary; break;
                case READY: shape = R.drawable.status_ready; color = R.color.success; break;
                case COMPLETED: shape = R.drawable.status_completed; color = R.color.success; break;
                case CANCELLED: shape = R.drawable.status_cancelled; color = R.color.danger; break;
                default: break;
            }
            status.setBackgroundResource(shape); status.setTextColor(ContextCompat.getColor(status.getContext(), color));
            waiting.setVisibility(booking.synced ? View.GONE : View.VISIBLE);
        }
    }
}
