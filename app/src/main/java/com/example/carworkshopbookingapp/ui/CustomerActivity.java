package com.example.carworkshopbookingapp.ui;

import android.view.View;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.SessionStore;
import com.example.carworkshopbookingapp.data.model.User;

public abstract class CustomerActivity extends BaseActivity {
    protected User user;
    protected abstract void onCustomerReady();

    protected final void loadCustomer() {
        new SessionStore(getApplicationContext()).read(new Callback<User>() {
            @Override public void onSuccess(User result) {
                if (isFinishing() || isDestroyed()) return;
                if (result == null) { EntryNavigation.login(CustomerActivity.this); return; }
                if (!"customer".equals(result.role)) { EntryNavigation.home(CustomerActivity.this, result); return; }
                user = result; onCustomerReady();
            }
            @Override public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) return;
                TextView message = findViewById(R.id.form_error);
                message.setText(R.string.error_profile); message.setVisibility(View.VISIBLE);
            }
        });
    }
}
