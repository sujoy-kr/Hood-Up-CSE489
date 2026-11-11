package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.data.remote.AuthRepository;

public final class SplashActivity extends BaseActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AuthRepository auth;
    private long shownAt;
    private boolean started;
    private boolean ready;
    private User user;
    private Exception error;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        screen(R.layout.activity_splash);
        shownAt = SystemClock.elapsedRealtime();
        auth = new AuthRepository(getApplicationContext());
        findViewById(R.id.retry).setOnClickListener(view -> restore());
        findViewById(R.id.splash_login).setOnClickListener(view -> {
            loading();
            auth.signOut(new Callback<Void>() {
                @Override public void onSuccess(Void ignored) { complete(null, null); }
                @Override public void onError(Exception error) { complete(null, error); }
            });
        });
        restore();
    }

    private void loading() {
        ready = false;
        findViewById(R.id.auth_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.auth_error).setVisibility(View.GONE);
        findViewById(R.id.retry).setVisibility(View.GONE);
        findViewById(R.id.splash_login).setVisibility(View.GONE);
    }

    private void restore() {
        loading();
        auth.restore(new Callback<User>() {
            @Override public void onSuccess(User result) { complete(result, null); }
            @Override public void onError(Exception error) { complete(null, error); }
        });
    }

    private void complete(User user, Exception error) {
        if (isFinishing() || isDestroyed()) return;
        this.user = user;
        this.error = error;
        ready = true;
        render();
    }

    private void render() {
        if (!started || !ready) return;
        handler.removeCallbacksAndMessages(null);
        long remaining = Math.max(0, 1000 - (SystemClock.elapsedRealtime() - shownAt));
        handler.postDelayed(() -> {
            if (!started || isFinishing() || isDestroyed()) return;
            if (error != null) {
                findViewById(R.id.auth_progress).setVisibility(View.GONE);
                TextView message = findViewById(R.id.auth_error);
                message.setText(AuthErrors.message(this, error));
                message.setVisibility(View.VISIBLE);
                findViewById(R.id.retry).setVisibility(View.VISIBLE);
                findViewById(R.id.splash_login).setVisibility(View.VISIBLE);
            } else if (user == null) { EntryNavigation.login(this); }
            else { EntryNavigation.home(this, user); }
        }, remaining);
    }

    @Override protected void onStart() { super.onStart(); started = true; render(); }
    @Override protected void onStop() {
        started = false;
        handler.removeCallbacksAndMessages(null);
        super.onStop();
    }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
