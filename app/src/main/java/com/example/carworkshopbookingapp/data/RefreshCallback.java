package com.example.carworkshopbookingapp.data;

/** The cache can succeed before the server refresh finishes, including when the cache is empty. */
public interface RefreshCallback<T> extends Callback<T> {
    void onRefreshComplete();
}
