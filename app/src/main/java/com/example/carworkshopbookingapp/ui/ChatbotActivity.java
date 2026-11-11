package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.ai.ChatSession;

public final class ChatbotActivity extends CustomerActivity {
    private ChatSession session;
    private ChatBubbles bubbles;
    private EditText input;
    private boolean started;
    private int renderedCount = -1;
    private boolean wasBusy;
    private final Runnable observer = this::render;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_chatbot);
        input = findViewById(R.id.chat_input);
        bubbles = new ChatBubbles(this, new ChatBubbles.Actions() {
            @Override public void pick(String serviceId, String note) {
                startActivity(new Intent(ChatbotActivity.this, PickWorkshopActivity.class)
                        .putExtra(BookingActivity.EXTRA_SERVICE_ID, serviceId).putExtra(BookingActivity.EXTRA_NOTE, note));
            }
            @Override public void retry() { if (session != null) session.retry(); }
        });
        findViewById(R.id.back).setOnClickListener(view -> finish());
        findViewById(R.id.send_chat).setOnClickListener(view -> {
            if (session != null && session.send(input.getText().toString())) input.setText("");
        });
        findViewById(R.id.retry_chat).setOnClickListener(view -> { if (session != null) session.retry(); });
        findViewById(R.id.clear_chat).setOnClickListener(view -> {
            if (session == null || session.busy || !session.loaded) return;
            new AlertDialog.Builder(this).setTitle(R.string.chat_clear_title).setMessage(R.string.chat_clear_body)
                    .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.chat_clear, (dialog, which) -> session.clear()).show();
        });
        int[] chips = {R.id.quick_brake, R.id.quick_ac, R.id.quick_start, R.id.quick_steering, R.id.quick_warning};
        int[] texts = {R.string.quick_brake, R.string.quick_ac, R.string.quick_start, R.string.quick_steering, R.string.quick_warning};
        for (int i = 0; i < chips.length; i++) {
            final int text = texts[i];
            findViewById(chips[i]).setOnClickListener(view -> {
                if (session != null && session.canSend()) session.send(getString(text));
            });
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSend(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        render(); loadCustomer();
    }
    @Override protected void onCustomerReady() {
        session = ChatSession.obtain(getApplicationContext(), user.uid);
        if (started) session.attach(observer);
    }
    private void updateSend() {
        findViewById(R.id.send_chat).setEnabled(session != null && session.canSend() && !input.getText().toString().trim().isEmpty());
    }
    private void render() {
        if (isFinishing() || isDestroyed() || input == null) return;
        boolean loaded = session != null && session.loaded;
        boolean busy = session != null && session.busy;
        input.setEnabled(loaded && !busy);
        updateSend();
        findViewById(R.id.clear_chat).setEnabled(loaded && !busy);
        FormViews.enabled(findViewById(R.id.quick_starts), session != null && session.canSend());
        TextView status = findViewById(R.id.form_error);
        String error = session == null || (!loaded && busy) ? getString(R.string.chat_loading) : session.error;
        if (loaded && error.isEmpty() && session.unfinished() && !busy) error = getString(R.string.chat_unfinished);
        if (session != null && session.clearing) error = getString(R.string.chat_clearing);
        status.setText(error); status.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.retry_chat).setVisibility(session != null && !busy && !error.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.chat_typing).setVisibility(loaded && busy && !session.clearing ? View.VISIBLE : View.GONE);
        if (loaded) {
            bubbles.render((LinearLayout) findViewById(R.id.chat_messages), session.messages(), session.catalogue(), session.canRetryAnswer());
            int count = session.messages().size();
            if (count != renderedCount || wasBusy != busy) {
                ScrollView scroll = findViewById(R.id.chat_scroll);
                scroll.post(() -> { if (!isDestroyed()) scroll.fullScroll(View.FOCUS_DOWN); });
            }
            renderedCount = count;
        }
        wasBusy = busy;
    }
    @Override protected void onStart() { super.onStart(); started = true; if (session != null) session.attach(observer); }
    @Override protected void onStop() { started = false; if (session != null) session.detach(observer); super.onStop(); }
}
