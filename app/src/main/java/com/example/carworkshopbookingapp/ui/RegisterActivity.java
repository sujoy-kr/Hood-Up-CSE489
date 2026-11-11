package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import com.example.carworkshopbookingapp.R;

public final class RegisterActivity extends AuthFormActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen(R.layout.activity_register);
        findViewById(R.id.submit).setOnClickListener(view -> {
            if (busy()) return;
            boolean valid = name(R.id.name, R.id.name_error);
            String phone = value(R.id.phone).trim();
            valid &= check(R.id.phone_error, com.example.carworkshopbookingapp.data.InputRules.phone(phone), R.string.error_phone);
            valid &= email(R.id.email, R.id.email_error);
            valid &= password(R.id.password, R.id.password_error, true);
            valid &= check(R.id.confirm_error, value(R.id.password).equals(value(R.id.confirm)), R.string.error_confirm);
            if (valid) auth.register(value(R.id.name).trim(), phone, value(R.id.email).trim(),
                    value(R.id.password), begin());
        });
        findViewById(R.id.login_link).setOnClickListener(view -> EntryNavigation.login(this));
    }
}
