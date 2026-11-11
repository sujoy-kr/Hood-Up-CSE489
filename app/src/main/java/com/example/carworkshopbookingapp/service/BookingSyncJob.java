package com.example.carworkshopbookingapp.service;

import android.app.job.*;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;

/** Connectivity alone is not an Android 12+ FGS-start exemption; execute bounded work as a job. */
public final class BookingSyncJob extends JobService {
    private SyncRunner runner;
    private int generation;
    @Override public boolean onStartJob(JobParameters params) {
        int token = ++generation;
        AppExecutors.execute(() -> FirebaseAuth.getInstance().getCurrentUser() == null ? null : FirebaseAuth.getInstance().getCurrentUser().getUid(), new Callback<String>() {
            @Override public void onSuccess(String uid) {
                if (token != generation) return;
                if (uid == null) { jobFinished(params, false); return; }
                runner = new SyncRunner(BookingSyncJob.this, uid, new SyncRunner.Host() {
                    @Override public boolean syncing() { return true; }
                    @Override public void idle() { }
                    @Override public void finished(boolean retry) { if (token == generation) { runner = null; jobFinished(params, retry); } }
                });
                runner.start();
            }
            @Override public void onError(Exception error) { if (token == generation) jobFinished(params, true); }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        generation++; if (runner != null) { runner.close(); runner = null; } return SyncCoordinator.retryAllowed();
    }
    @Override public void onDestroy() { generation++; if (runner != null) runner.close(); super.onDestroy(); }
}
