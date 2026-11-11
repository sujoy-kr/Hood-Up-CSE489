package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.WorkshopBrowser;
import com.example.carworkshopbookingapp.data.local.SessionStore;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.ui.adapter.WorkshopAdapter;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.List;

public final class HomeActivity extends CatalogueActivity {
    private com.example.carworkshopbookingapp.receiver.OfflineBanner offlineBanner;
    private WorkshopAdapter adapter;
    private EditText search;
    private View header;
    private WorkshopBrowser.Filter filter = WorkshopBrowser.Filter.ALL;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() { onCatalogueChanged(); timer.postDelayed(this, 60000); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_home);
        com.example.carworkshopbookingapp.util.NotificationHelper.permission(this);
        offlineBanner = new com.example.carworkshopbookingapp.receiver.OfflineBanner(findViewById(R.id.offline_banner));
        ListView list = findViewById(R.id.workshop_list);
        list.setItemsCanFocus(true);
        header = LayoutInflater.from(this).inflate(R.layout.header_home, list, false);
        list.addHeaderView(header, null, false);
        adapter = new WorkshopAdapter(this, services);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index >= 0 && index < adapter.getCount()) startActivity(new Intent(this, WorkshopDetailActivity.class)
                    .putExtra(WorkshopDetailActivity.EXTRA_WORKSHOP_ID, adapter.getItem(index).id));
        });
        CustomerNavigation.bind(this);
        header.findViewById(R.id.profile_icon).setOnClickListener(view -> startActivity(new Intent(this, ProfileActivity.class)));
        header.findViewById(R.id.bot_card).setOnClickListener(view -> startActivity(new Intent(this, ChatbotActivity.class)));
        header.findViewById(R.id.refresh).setOnClickListener(view -> reload());
        search = header.findViewById(R.id.search);
        if (state != null) {
            try { filter = WorkshopBrowser.Filter.valueOf(state.getString("filter", "ALL")); }
            catch (IllegalArgumentException ignored) { filter = WorkshopBrowser.Filter.ALL; }
            search.setText(state.getString("query", ""));
        }
        int[] chips = {R.id.filter_all, R.id.filter_open, R.id.filter_top};
        for (int i = 0; i < chips.length; i++) {
            WorkshopBrowser.Filter selected = WorkshopBrowser.Filter.values()[i];
            header.findViewById(chips[i]).setOnClickListener(view -> { filter = selected; onCatalogueChanged(); });
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onCatalogueChanged(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        ((TextView) header.findViewById(R.id.greeting)).setText(getString(R.string.hey_name, getString(R.string.greeting_fallback)));
        new SessionStore(getApplicationContext()).read(new Callback<User>() {
            @Override public void onSuccess(User user) {
                if (isFinishing() || isDestroyed()) return;
                if (user == null) { EntryNavigation.login(HomeActivity.this); return; }
                if ("admin".equals(user.role)) { EntryNavigation.home(HomeActivity.this, user); return; }
                String name = user.name == null ? "" : user.name.trim();
                String first = name.isEmpty() ? getString(R.string.greeting_fallback) : name.split("\\s+")[0];
                ((TextView) header.findViewById(R.id.greeting)).setText(getString(R.string.hey_name, first));
            }
            @Override public void onError(Exception error) {
                if (!isFinishing() && !isDestroyed()) EntryNavigation.login(HomeActivity.this);
            }
        });
        reload();
    }

    @Override protected void onCatalogueChanged() {
        if (adapter == null) return;
        List<Workshop> visible = WorkshopBrowser.filter(workshops, services, search.getText().toString(),
                filter, WorkshopDisplay.dhakaMinute());
        adapter.submit(visible);
        int[] chips = {R.id.filter_all, R.id.filter_open, R.id.filter_top};
        for (int i = 0; i < chips.length; i++) {
            Button chip = header.findViewById(chips[i]);
            boolean selected = i == filter.ordinal();
            chip.setSelected(selected);
            chip.setBackgroundResource(selected ? R.drawable.chip_selected : R.drawable.chip_default);
            chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.accent : R.color.text_muted));
        }
        header.findViewById(R.id.refresh).setEnabled(!loading);
        ((android.widget.Button)header.findViewById(R.id.refresh)).setText(hasError()?R.string.retry:R.string.refresh);
        header.findViewById(R.id.browse_progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        header.findViewById(R.id.skeleton).setVisibility(loading && workshops.isEmpty() ? View.VISIBLE : View.GONE);
        TextView status = header.findViewById(R.id.browse_status);
        int message = hasError() ? R.string.browse_error : loading && workshops.isEmpty() ? R.string.browse_loading :
                workshops.isEmpty() ? R.string.browse_empty : visible.isEmpty() ? R.string.browse_no_matches : 0;
        status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (hasError() && catalogueError!=null) status.setText(catalogueError); else if (message != 0) status.setText(message);
    }

    @Override protected void navigateBack() { finishAffinity(); }

    @Override protected void onStart() {
        super.onStart();
        offlineBanner.start(this);
        com.example.carworkshopbookingapp.service.SyncCoordinator.request(this);
    }

    @Override protected void onStop() { unregisterReceiver(offlineBanner); super.onStop(); }

    @Override protected void onResume() { super.onResume(); timer.post(tick); }
    @Override protected void onPause() { timer.removeCallbacksAndMessages(null); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("filter", filter.name());
        state.putString("query", search.getText().toString());
        super.onSaveInstanceState(state);
    }
}
