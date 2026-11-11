package com.example.carworkshopbookingapp.util;

import android.os.Handler;
import android.os.Looper;
import com.example.carworkshopbookingapp.data.Callback;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    // Blocking AI HTTP must not hold up ordered SQLite work.
    private static final ExecutorService NETWORK = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    public static ExecutorService io() { return IO; }
    public static ExecutorService network() { return NETWORK; }
    public static void main(Runnable action) { MAIN.post(action); }

    public static <T> void execute(Callable<T> work, Callback<T> callback) {
        IO.execute(() -> {
            try {
                T result = work.call();
                main(() -> callback.onSuccess(result));
            } catch (Exception error) {
                main(() -> callback.onError(error));
            }
        });
    }
}
