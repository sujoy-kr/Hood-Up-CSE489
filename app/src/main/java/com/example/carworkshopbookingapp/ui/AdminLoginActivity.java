package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.carworkshopbookingapp.R;

public final class AdminLoginActivity extends AuthFormActivity {
    private boolean expanded;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen(R.layout.activity_admin_login);
        expanded = savedInstanceState != null && savedInstanceState.getBoolean("admin_expanded");
        showRegistration();
        findViewById(R.id.expand_admin).setOnClickListener(view -> {
            expanded = !expanded;
            showRegistration();
            if (expanded) findViewById(R.id.invite).requestFocus();
        });
        findViewById(R.id.submit).setOnClickListener(view -> {
            if (busy()) return;
            boolean valid = email(R.id.email, R.id.email_error);
            valid &= password(R.id.password, R.id.password_error, false);
            if (valid) auth.signIn(value(R.id.email).trim(), value(R.id.password), true, begin());
        });
        findViewById(R.id.admin_register).setOnClickListener(view -> {
            if (busy()) return;
            boolean valid=check(R.id.invite_error,!value(R.id.invite).trim().isEmpty(),R.string.error_invite);
            if(valid)auth.checkAdminInvite(value(R.id.invite).trim(),begin());
        });
        findViewById(R.id.customer_link).setOnClickListener(view -> EntryNavigation.login(this));
    }

    private void showRegistration() {
        findViewById(R.id.admin_registration).setVisibility(expanded ? View.VISIBLE : View.GONE);
        Button toggle = findViewById(R.id.expand_admin);
        toggle.setSelected(expanded);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("admin_expanded", expanded);
        super.onSaveInstanceState(state);
    }
}
