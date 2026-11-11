package com.example.carworkshopbookingapp.util;

import android.view.View;
import java.util.HashSet;
import java.util.Set;

public final class ListMotion {
    private final Set<String> shown = new HashSet<>();
    public void bind(View view, String id) {
        view.animate().cancel();
        if (id != null && shown.add(id) && (android.os.Build.VERSION.SDK_INT<26 || android.animation.ValueAnimator.areAnimatorsEnabled())) {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(160).start();
        } else {
            view.setAlpha(1f);
        }
    }
}
