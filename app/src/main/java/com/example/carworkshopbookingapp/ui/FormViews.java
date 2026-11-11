package com.example.carworkshopbookingapp.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.remote.BookingFailure;
import com.example.carworkshopbookingapp.util.NetworkState;

final class FormViews {
    private FormViews() {}
    static void enabled(View view, boolean enabled) {
        if (view instanceof Button || view instanceof EditText || view instanceof Spinner || view instanceof android.widget.CompoundButton) view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enabled(group.getChildAt(i), enabled);
        }
    }
    static String error(Context context, Exception error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof BookingFailure) {
                switch (((BookingFailure) cause).reason) {
                    case SLOT_TAKEN: return context.getString(R.string.booking_taken);
                    case PRICE_CHANGED: return context.getString(R.string.booking_changed);
                    case SIGNED_OUT: return context.getString(R.string.booking_signed_out);
                    default: return context.getString(R.string.booking_invalid);
                }
            }
        }
        return UiErrors.message(error);
    }
}
