package com.example.carworkshopbookingapp.data;

/** Success and error are delivered on the main thread. Cache reads can succeed twice. */
public interface Callback<T> {
    void onSuccess(T result);
    void onError(Exception error);
}
