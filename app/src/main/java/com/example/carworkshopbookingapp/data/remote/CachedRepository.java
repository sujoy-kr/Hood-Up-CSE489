package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.RefreshCallback;
import java.util.List;

/** Delivers cache first, then a server result only after the cache transaction commits.
 * A refresh error does not invalidate the already-delivered cached value.
 */
public abstract class CachedRepository<T> extends CollectionRepository<T> {
    protected CachedRepository(String path, Class<T> type) { super(path, type); }

    protected abstract void readCache(Callback<List<T>> callback);
    protected abstract void writeCache(List<T> items, Callback<Void> callback);

    @Override public void list(Callback<List<T>> callback) {
        readCache(new Callback<List<T>>() {
            @Override public void onSuccess(List<T> items) {
                callback.onSuccess(items);
                refresh(callback);
            }
            @Override public void onError(Exception error) {
                callback.onError(error);
                refresh(callback);
            }
        });
    }

    @Override public void get(String id, Callback<T> callback) {
        readCache(new Callback<List<T>>() {
            @Override public void onSuccess(List<T> items) {
                for (T item : items) {
                    if (idOf(item).equals(id)) {
                        callback.onSuccess(item);
                        break;
                    }
                }
                refreshItem(id, callback);
            }
            @Override public void onError(Exception error) {
                callback.onError(error);
                refreshItem(id, callback);
            }
        });
    }

    private void refreshItem(String id, Callback<T> callback) {
        refresh(new Callback<List<T>>() {
            @Override public void onSuccess(List<T> items) {
                for (T item : items) {
                    if (idOf(item).equals(id)) {
                        callback.onSuccess(item);
                        return;
                    }
                }
                callback.onError(new IllegalStateException("Document not found: " + id));
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    private void refresh(Callback<List<T>> callback) {
        super.list(new Callback<List<T>>() {
            @Override public void onSuccess(List<T> items) {
                writeCache(items, new Callback<Void>() {
                    @Override public void onSuccess(Void ignored) { callback.onSuccess(items); finished(callback); }
                    @Override public void onError(Exception error) { callback.onError(error); finished(callback); }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); finished(callback); }
        });
    }

    private void finished(Callback<List<T>> callback) {
        if (callback instanceof RefreshCallback) ((RefreshCallback<?>) callback).onRefreshComplete();
    }
}
