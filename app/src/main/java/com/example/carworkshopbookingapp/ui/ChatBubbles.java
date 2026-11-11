package com.example.carworkshopbookingapp.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.ChatMessage;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChatBubbles {
    interface Actions { void pick(String serviceId, String note); void retry(); }
    private final Context context;
    private final int gap;
    private final Actions actions;
    ChatBubbles(Context context, Actions actions) {
        this.context = context; this.actions = actions;
        gap = context.getResources().getDimensionPixelSize(R.dimen.spacing);
    }
    void render(LinearLayout container, List<ChatMessage> messages, List<ServiceItem> catalogue, boolean canRetry) {
        container.removeAllViews();
        Map<String, ServiceItem> services = new HashMap<>();
        for (ServiceItem service : catalogue) services.put(service.id, service);
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            boolean user = "user".equals(message.role);
            LinearLayout group = new LinearLayout(context); group.setOrientation(LinearLayout.VERTICAL);
            group.setGravity(user ? Gravity.END : Gravity.START);
            LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(-1, -2); groupParams.bottomMargin = gap;
            container.addView(group, groupParams);
            TextView bubble = text(message.text, R.style.TextBody);
            bubble.setMaxWidth((int) (context.getResources().getDisplayMetrics().widthPixels * 0.82));
            bubble.setPadding(gap, gap * 3 / 4, gap, gap * 3 / 4);
            bubble.setBackgroundResource(user ? R.drawable.bg_chat_user : R.drawable.bg_chat_bot);
            bubble.setTextColor(ContextCompat.getColor(context, user ? R.color.bg : R.color.text));
            bubble.setContentDescription(context.getString(user ? R.string.chat_user : R.string.chat_bot) + ": " + message.text);
            group.addView(bubble, new LinearLayout.LayoutParams(-2, -2));
            if (user) continue;
            if (message.offline) group.addView(text(context.getString(R.string.offline_suggestion), R.style.MutedText));
            if ("HIGH".equals(message.urgency)) group.addView(urgency(message.urgency));
            for (String id : message.serviceIds) {
                ServiceItem service = services.get(id);
                if (service == null || !Double.isFinite(service.basePrice) || service.basePrice < 0) continue;
                LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(gap, gap, gap, gap); card.setBackgroundResource(R.drawable.bg_card);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = gap;
                group.addView(card, params);
                card.addView(text(service.name, R.style.SectionHeader));
                card.addView(text(context.getString(R.string.starting_price, WorkshopDisplay.money(service.basePrice)), R.style.MutedText));
                card.addView(urgency(message.urgency)); card.setForeground(androidx.appcompat.content.res.AppCompatResources.getDrawable(context,R.drawable.fg_card_ripple));card.setMinimumHeight(gap*3);card.setClickable(true); card.setFocusable(true);
                card.setContentDescription(context.getString(R.string.suggestion_description, service.name, WorkshopDisplay.money(service.basePrice)));
                card.setOnClickListener(view -> actions.pick(service.id, message.symptom));
            }
            if (message.error != null && !message.error.isEmpty()) {
                TextView error = text(message.error, R.style.MutedText); error.setPadding(0, gap, 0, 0); group.addView(error);
            }
            if (message.retryable && i == messages.size() - 1) {
                Button retry = (Button) android.view.LayoutInflater.from(context).inflate(R.layout.chat_retry_button, group, false);
                retry.setText(R.string.chat_retry); retry.setEnabled(canRetry);
                retry.setOnClickListener(view -> actions.retry()); group.addView(retry, new LinearLayout.LayoutParams(-2, -2));
            }
        }
    }
    private TextView text(String value, int style) {
        TextView view = new TextView(context); view.setTextAppearance(style); view.setText(value); return view;
    }
    private TextView urgency(String urgency) {
        boolean high = "HIGH".equals(urgency), medium = "MEDIUM".equals(urgency);
        TextView label = text(context.getString(high ? R.string.urgency_high : medium ? R.string.urgency_medium : R.string.urgency_low), R.style.StatusPill);
        label.setTextColor(ContextCompat.getColor(context, high ? R.color.danger : medium ? R.color.warning : R.color.success));
        label.setBackgroundResource(high ? R.drawable.status_cancelled : medium ? R.drawable.status_pending : R.drawable.status_ready);
        label.setPadding(gap * 3 / 4, gap / 4, gap * 3 / 4, gap / 4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2); params.topMargin = gap / 2;
        label.setLayoutParams(params); return label;
    }
}
