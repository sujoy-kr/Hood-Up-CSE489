package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.google.android.gms.tasks.Task;
import java.util.concurrent.Callable;

final class RemoteTasks {
    interface Mapper<T, R> { R map(T value) throws Exception; }

    private RemoteTasks() {}

    static <T, R> void run(Callable<Task<T>> request, Mapper<T, R> mapper, Callback<R> callback) {
        AppExecutors.io().execute(() -> {
            try {
                request.call().addOnCompleteListener(AppExecutors.io(), task -> {
                    try {
                        if (!task.isSuccessful()) {
                            Exception error = task.getException();
                            throw error == null ? new IllegalStateException("Remote operation failed") : error;
                        }
                        R result = mapper.map(task.getResult());
                        AppExecutors.main(() -> callback.onSuccess(result));
                    } catch (Exception error) {
                        AppExecutors.main(() -> callback.onError(error));
                    }
                });
            } catch (Exception error) {
                AppExecutors.main(() -> callback.onError(error));
            }
        });
    }
}
