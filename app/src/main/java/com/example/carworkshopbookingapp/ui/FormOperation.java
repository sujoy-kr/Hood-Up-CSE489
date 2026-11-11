package com.example.carworkshopbookingapp.ui;

import com.example.carworkshopbookingapp.data.Callback;

/** Retains an in-flight mutation across rotation without retaining an Activity. */
final class FormOperation<T> implements Callback<T> {
    boolean busy, complete;
    T result;
    Exception error;
    Runnable listener;

    Callback<T> begin() {
        busy = true; complete = false; error = null;
        notifyListener(); return this;
    }
    @Override public void onSuccess(T result) {
        busy = false; complete = true; this.result = result; notifyListener();
    }
    @Override public void onError(Exception error) {
        busy = false; this.error = error; notifyListener();
    }
    private void notifyListener() { if (listener != null) listener.run(); }
}
