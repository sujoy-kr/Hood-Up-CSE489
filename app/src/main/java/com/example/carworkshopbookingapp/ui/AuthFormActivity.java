package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.data.remote.AuthRepository;

public abstract class AuthFormActivity extends BaseActivity {
    protected AuthRepository auth;
    private FormState state;

    // A retained request owns no Activity. A replacement Activity attaches only while started.
    private static final class FormState implements Callback<User> {
        boolean busy;
        User result;
        Exception error;
        Runnable listener;
        @Override public void onSuccess(User user) {
            busy = false; result = user; error = null;
            if (listener != null) listener.run();
        }
        @Override public void onError(Exception error) {
            busy = false; this.error = error;
            if (listener != null) listener.run();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object retained = getLastCustomNonConfigurationInstance();
        state = retained instanceof FormState ? (FormState) retained : new FormState();
        auth = new AuthRepository(getApplicationContext());
    }

    @Override public Object onRetainCustomNonConfigurationInstance() { return state; }

    @Override protected void onStart() {
        super.onStart();
        state.listener = this::render;
        render();
    }

    @Override protected void onStop() {
        state.listener = null;
        super.onStop();
    }

    protected boolean busy() { return state.busy; }

    protected Callback<User> begin() {
        state.busy = true; state.error = null; state.result = null;
        render();
        return state;
    }

    private void render() {
        enableForm(findViewById(R.id.main), !state.busy);
        findViewById(R.id.auth_progress).setVisibility(state.busy ? View.VISIBLE : View.GONE);
        TextView message = findViewById(R.id.auth_error);
        message.setVisibility(state.error == null ? View.GONE : View.VISIBLE);
        if (state.error != null) {
            message.setText(AuthErrors.message(this, state.error));
        }
        if (state.result != null) {
            User user = state.result;
            state.result = null;
            EntryNavigation.home(this, user);
        }
    }

    private void enableForm(View view, boolean enabled) {
        if (view instanceof Button || view instanceof EditText) view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enableForm(group.getChildAt(i), enabled);
        }
    }

    protected String value(int id) { return ((EditText) findViewById(id)).getText().toString(); }

    protected boolean check(int errorId, boolean valid, int message) {
        TextView error = findViewById(errorId);
        error.setText(message);
        error.setVisibility(valid ? View.GONE : View.VISIBLE);
        return valid;
    }

    protected boolean email(int id, int error) {
        return check(error, Patterns.EMAIL_ADDRESS.matcher(value(id).trim()).matches(), R.string.error_email);
    }

    protected boolean password(int id, int error, boolean registration) {
        String password = value(id);
        return check(error, registration ? password.length() >= 6 : !password.isEmpty(),
                registration ? R.string.error_password_short : R.string.error_password);
    }

    protected boolean name(int id, int error) {
        return check(error, com.example.carworkshopbookingapp.data.InputRules.name(value(id)), R.string.error_name);
    }

    @Override protected boolean allowBack() { return !busy(); }
}
