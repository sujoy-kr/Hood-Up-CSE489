package com.example.carworkshopbookingapp.util;

import android.widget.ImageView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.Workshop;

public final class WorkshopImages {
    private WorkshopImages() {}
    public static void bind(ImageView view, Workshop workshop) {
        boolean night = workshop != null && "night".equals(workshop.imageKey);
        view.setImageResource(night ? R.drawable.workshop_night : R.drawable.workshop_day);
        view.setClipToOutline(true);
        view.setContentDescription(view.getContext().getString(R.string.workshop_photo,
                workshop == null ? "Workshop" : workshop.name));
    }
}
