package com.example.carworkshopbookingapp.data.remote;

import android.content.Context;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.SessionStore;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.util.AppExecutors;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

public final class AuthRepository {
    private final SessionStore session;
    private final Context context;
    private final UsersRepository users = new UsersRepository();

    public AuthRepository(Context context) { this.context = context.getApplicationContext(); session = new SessionStore(context.getApplicationContext()); }

    public void restore(Callback<User> callback) {
        AppExecutors.execute(() -> FirebaseAuth.getInstance().getCurrentUser(), new Callback<FirebaseUser>() {
            @Override public void onSuccess(FirebaseUser current) {
                if (current == null) {
                    session.clear(new Callback<Void>() {
                        @Override public void onSuccess(Void ignored) { callback.onSuccess(null); }
                        @Override public void onError(Exception error) { callback.onError(error); }
                    });
                    return;
                }
                session.read(new Callback<User>() {
                    @Override public void onSuccess(User cached) { loadProfile(current, cached, callback); }
                    @Override public void onError(Exception error) { loadProfile(current, null, callback); }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    private void loadProfile(FirebaseUser current, User cached, Callback<User> callback) {
        users.get(current.getUid(), new Callback<User>() {
            @Override public void onSuccess(User user) { persist(user, callback); }
            @Override public void onError(Exception error) {
                // Only an offline read can fall back to a session belonging to this Firebase UID.
                boolean offline = error instanceof FirebaseNetworkException ||
                        (error instanceof FirebaseFirestoreException &&
                         ((FirebaseFirestoreException) error).getCode() == FirebaseFirestoreException.Code.UNAVAILABLE);
                if (offline && cached != null && current.getUid().equals(cached.uid) && validRole(cached)) {
                    callback.onSuccess(cached);
                } else { callback.onError(error); }
            }
        });
    }

    public void signIn(String email, String password, boolean adminOnly, Callback<User> callback) {
        RemoteTasks.run(() -> FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password),
                result -> result.getUser(), new Callback<FirebaseUser>() {
            @Override public void onSuccess(FirebaseUser current) {
                if (current == null) { reject(new AuthFailure(AuthFailure.Reason.PROFILE_UNAVAILABLE), callback); return; }
                users.get(current.getUid(), new Callback<User>() {
                    @Override public void onSuccess(User user) {
                        if (adminOnly && !"admin".equals(user.role)) {
                            reject(new AuthFailure(AuthFailure.Reason.NOT_ADMIN), callback);
                        } else { persist(user, callback); }
                    }
                    @Override public void onError(Exception error) { reject(error, callback); }
                });
            }
            @Override public void onError(Exception error) { reject(error, callback); }
        });
    }

    public void register(String name, String phone, String email, String password,
                         Callback<User> callback) {
        RemoteTasks.run(() -> FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password),
                result -> result.getUser(), new Callback<FirebaseUser>() {
            @Override public void onSuccess(FirebaseUser current) {
                if (current == null) { reject(new AuthFailure(AuthFailure.Reason.PROFILE_UNAVAILABLE), callback); return; }
                User user = new User();
                user.uid = current.getUid();
                user.name = name;
                user.phone = phone;
                user.email = email;
                user.role = "customer";
                users.create(user, new Callback<Void>() {
                    @Override public void onSuccess(Void ignored) { persist(user, callback); }
                    @Override public void onError(Exception error) {
                        // Avoid leaving an Auth account with no matching profile after a rejected write.
                        RemoteTasks.run(current::delete, ignored -> null, new Callback<Void>() {
                            @Override public void onSuccess(Void ignored) { reject(error, callback); }
                            @Override public void onError(Exception rollbackError) {
                                reject(new AuthFailure(AuthFailure.Reason.ROLLBACK_FAILED), callback);
                            }
                        });
                    }
                });
            }
            @Override public void onError(Exception error) { reject(error, callback); }
        });
    }

    private static boolean validRole(User user) {
        return "customer".equals(user.role) || "admin".equals(user.role);
    }

    private void persist(User user, Callback<User> callback) {
        if (!validRole(user)) { reject(new AuthFailure(AuthFailure.Reason.PROFILE_UNAVAILABLE), callback); return; }
        session.save(user, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) {
                com.example.carworkshopbookingapp.service.SyncCoordinator.signedIn(context);
                callback.onSuccess(user);
            }
            @Override public void onError(Exception error) { reject(error, callback); }
        });
    }

    private void reject(Exception error, Callback<User> callback) {
        signOut(new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { callback.onError(error); }
            @Override public void onError(Exception cleanupError) { callback.onError(cleanupError); }
        });
    }

    public void signOut(Callback<Void> callback) {
        com.example.carworkshopbookingapp.service.SyncCoordinator.logout(context);
        AppExecutors.execute(() -> {
            FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();
            String uid=current==null?null:current.getUid();
            FirebaseAuth.getInstance().signOut();
            return uid;
        }, new Callback<String>() {
            @Override public void onSuccess(String uid) {
                if(uid==null){session.clear(callback);return;}
                com.example.carworkshopbookingapp.data.local.LocalDb.getInstance(context).getBookings(uid,new Callback<java.util.List<com.example.carworkshopbookingapp.data.model.Booking>>() {
                    @Override public void onSuccess(java.util.List<com.example.carworkshopbookingapp.data.model.Booking> items){
                        for(com.example.carworkshopbookingapp.data.model.Booking item:items){
                            com.example.carworkshopbookingapp.data.model.Booking cancelled=new com.example.carworkshopbookingapp.data.model.Booking(item);
                            cancelled.status=com.example.carworkshopbookingapp.data.model.BookingStatus.CANCELLED;
                            com.example.carworkshopbookingapp.util.ReminderScheduler.update(context,cancelled);
                        }
                        session.clear(callback);
                    }
                    // Alarm delivery also checks the current Firebase owner, so a failed cache read cannot leak a reminder.
                    @Override public void onError(Exception error){session.clear(callback);}
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }
}
