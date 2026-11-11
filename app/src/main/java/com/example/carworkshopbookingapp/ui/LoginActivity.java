package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import com.example.carworkshopbookingapp.R;

public final class LoginActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen(R.layout.activity_login);
        findViewById(R.id.submit).setOnClickListener(view -> {
            if (busy()) return;
            boolean valid = email(R.id.email, R.id.email_error);
            valid &= password(R.id.password, R.id.password_error, false);
            if (valid) auth.signIn(value(R.id.email).trim(), value(R.id.password), false, begin());
        });
        findViewById(R.id.register_link).setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.admin_link).setOnClickListener(view -> startActivity(new Intent(this, AdminLoginActivity.class)));
    }
}
