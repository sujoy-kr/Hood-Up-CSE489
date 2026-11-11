package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.data.remote.AdminRepository;
import java.util.List;
import java.util.concurrent.TimeoutException;

/** Admin screens build their content in code inside one shared shell layout. */
public abstract class AdminActivity extends BaseActivity {
    protected final AdminRepository repo = new AdminRepository();
    protected LinearLayout body;
    protected TextView error;
    protected boolean busy;
    private final Handler deadlines = new Handler(Looper.getMainLooper());
    private ListView activeList;
    private int version;
    private boolean initialized;

    @Override protected void onCreate(Bundle state) { super.onCreate(state); bindShell(); }

    private void bindShell() {
        screen(R.layout.activity_admin_shell);
        body = findViewById(R.id.admin_body);
        error = findViewById(R.id.form_error);
        findViewById(R.id.back).setOnClickListener(view -> { if (allowBack()) navigateBack(); });
        findViewById(R.id.refresh).setOnClickListener(view -> { if (!busy) { reset(); ready(); } });
    }

    @Override protected void onStart() {
        super.onStart();
        if (activeList != null) activeList.setEnabled(false);
        body.setVisibility(View.INVISIBLE);
        findViewById(R.id.admin_gate_progress).setVisibility(View.VISIBLE);
        repo.verify(new ScreenCallback<User>() {
            @Override protected void success(User user) {
                if (activeList != null) activeList.setEnabled(true);
                findViewById(R.id.admin_gate_progress).setVisibility(View.GONE);
                body.setVisibility(View.VISIBLE);
                if (!initialized || (!busy && !keepContent())) { initialized = true; reset(); ready(); }
            }
            @Override protected void failure(Exception e) { EntryNavigation.login(AdminActivity.this); }
        });
    }

    protected final void reset() {
        // A list screen moved the shell content into a ListView header, so start from a fresh shell.
        if (activeList != null) { bindShell(); activeList = null; }
        findViewById(R.id.admin_gate_progress).setVisibility(View.GONE);
        deadlines.removeCallbacksAndMessages(null);
        version++;
        body.removeAllViews();
        error.setVisibility(View.GONE);
    }

    /** Sets the top bar for the current screen state. */
    protected final void header(String title, boolean root, boolean refreshable) {
        ((TextView) findViewById(R.id.admin_title)).setText(title);
        findViewById(R.id.back).setVisibility(root ? View.GONE : View.VISIBLE);
        Button refresh = findViewById(R.id.refresh);
        refresh.setText(R.string.refresh);
        refresh.setVisibility(refreshable ? View.VISIBLE : View.GONE);
        findViewById(R.id.admin_action).setVisibility(View.GONE);
    }

    protected final void headerAction(String label, Runnable action) {
        Button button = findViewById(R.id.admin_action);
        button.setText(label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(view -> { if (!busy) action.run(); });
    }

    protected final void showList(ListView list) {
        ScrollView scroll = findViewById(R.id.admin_scroll);
        LinearLayout content = findViewById(R.id.admin_content);
        scroll.removeView(content);
        scroll.setVisibility(View.GONE);
        list.addHeaderView(content, null, false);
        list.setItemsCanFocus(true);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setVerticalScrollBarEnabled(false);
        ((FrameLayout) findViewById(R.id.admin_frame)).addView(list, new FrameLayout.LayoutParams(-1, -1));
        activeList = list;
    }

    protected final boolean alive() { return !isDestroyed() && !isFinishing(); }

    protected abstract class ScreenCallback<T> implements Callback<T> {
        private final int request = version;
        private boolean completed;
        private final Runnable timeout = () -> onError(new TimeoutException());
        protected ScreenCallback() { deadlines.postDelayed(timeout, 30000); }
        public final void onSuccess(T value) {
            if (completed) return;
            completed = true; deadlines.removeCallbacks(timeout);
            if (alive() && request == version) success(value);
        }
        public final void onError(Exception e) {
            if (completed) return;
            completed = true; deadlines.removeCallbacks(timeout);
            if (alive() && request == version) failure(e);
        }
        protected abstract void success(T value);
        protected void failure(Exception e) { fail(e); }
    }

    @Override protected void onDestroy() { deadlines.removeCallbacksAndMessages(null); version++; super.onDestroy(); }
    @Override protected boolean allowBack() { return !busy; }
    protected boolean keepContent() { return false; }
    protected abstract void ready();

    protected int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density); }

    private LinearLayout.LayoutParams below(int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(bottomDp);
        return params;
    }

    protected TextView label(ViewGroup parent, String value, int style, int bottomDp) {
        TextView view = new TextView(this, null, 0, style);
        view.setText(value);
        parent.addView(view, below(bottomDp));
        return view;
    }
    protected TextView muted(String value) { return label(body, value, R.style.MutedText, 12); }
    protected TextView section(String value) {
        TextView view = label(body, value, R.style.SectionHeader, 12);
        ((LinearLayout.LayoutParams) view.getLayoutParams()).topMargin = dp(8);
        return view;
    }
    protected TextView loading() { return muted("Loading…"); }

    protected LinearLayout card(ViewGroup parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        parent.addView(card, below(16));
        return card;
    }

    /** Adds a "label … value" line to a card. */
    protected void row(LinearLayout card, String name, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView key = new TextView(this, null, 0, R.style.MutedText);
        key.setText(name);
        row.addView(key, new LinearLayout.LayoutParams(dp(112), -2));
        TextView detail = new TextView(this, null, 0, R.style.TextBody);
        detail.setText(value == null || value.trim().isEmpty() ? "—" : value);
        row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(row);
    }

    protected Button button(ViewGroup parent, String value, int style, Runnable action) {
        Button button = new Button(this, null, 0, style);
        button.setText(value);
        parent.addView(button, below(12));
        button.setOnClickListener(view -> { if (!busy) action.run(); });
        return button;
    }
    protected Button button(String value, Runnable action) { return button(body, value, R.style.SecondaryButton, action); }
    protected Button primary(String value, Runnable action) { return button(body, value, R.style.PrimaryButton, action); }
    protected Button danger(String value, Runnable action) { return button(body, value, R.style.DangerButton, action); }

    /** Adds a tappable card with an icon, title and subtitle. */
    protected void entry(int icon, String title, String subtitle, Runnable action) {
        View view = getLayoutInflater().inflate(R.layout.row_admin_entry, body, false);
        ((ImageView) view.findViewById(R.id.entry_icon)).setImageResource(icon);
        ((TextView) view.findViewById(R.id.entry_title)).setText(title);
        ((TextView) view.findViewById(R.id.entry_subtitle)).setText(subtitle);
        view.setContentDescription(title + ". " + subtitle);
        view.setFocusable(true);
        view.setOnClickListener(clicked -> { if (!busy) action.run(); });
        body.addView(view);
    }

    protected EditText input(String name, String value, int type) {
        TextView caption = label(body, name, R.style.FieldLabel, 8);
        EditText field = new EditText(this, null, 0, R.style.TextInput);
        field.setId(0x00F00000 + body.getChildCount());
        field.setInputType(type);
        boolean phone = type == InputType.TYPE_CLASS_PHONE;
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(phone ? 30 : 1000)});
        field.setSingleLine((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        field.setText(value == null ? "" : value);
        caption.setLabelFor(field.getId());
        body.addView(field, below(16));
        return field;
    }

    protected Spinner spinner(String description, List<String> labels) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.bg_spinner);
        spinner.setPopupBackgroundResource(R.drawable.bg_card_alt);
        spinner.setMinimumHeight(dp(52));
        spinner.setContentDescription(description);
        spinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, labels));
        body.addView(spinner, below(16));
        return spinner;
    }

    protected void open(Class<?> screen) { startActivity(new Intent(this, screen)); }

    protected void fail(Exception e) {
        if (!alive()) return;
        error.setText(UiErrors.message(e));
        error.setVisibility(View.VISIBLE);
        ((Button) findViewById(R.id.refresh)).setText(R.string.retry);
        if (e instanceof SecurityException) EntryNavigation.login(this);
    }

    protected Callback<Void> mutation(Runnable done) {
        busy = true;
        FormViews.enabled(body, false);
        return new Callback<Void>() {
            public void onSuccess(Void v) {
                if (!alive()) return;
                busy = false; FormViews.enabled(body, true); error.setVisibility(View.GONE); done.run();
            }
            public void onError(Exception e) {
                if (!alive()) return;
                busy = false; FormViews.enabled(body, true); fail(e);
            }
        };
    }
}
