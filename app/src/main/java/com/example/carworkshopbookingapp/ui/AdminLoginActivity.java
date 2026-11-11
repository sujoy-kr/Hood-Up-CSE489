package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import com.example.carworkshopbookingapp.R;

public final class AdminLoginActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen(R.layout.activity_admin_login);
        findViewById(R.id.submit).setOnClickListener(view -> {
            if (busy()) return;
            boolean valid = email(R.id.email, R.id.email_error);
            valid &= password(R.id.password, R.id.password_error, false);
            if (valid) auth.signIn(value(R.id.email).trim(), value(R.id.password), true, begin());
        });
        findViewById(R.id.customer_link).setOnClickListener(view -> EntryNavigation.login(this));
    }
}
