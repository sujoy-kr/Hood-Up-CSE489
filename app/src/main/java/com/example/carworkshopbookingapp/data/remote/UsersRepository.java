package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;

public final class UsersRepository extends CollectionRepository<User> {
    public UsersRepository() { super("users", User.class); }
    @Override protected String idOf(User item) { return item.uid; }
    @Override protected void assignId(User item, String id) { item.uid = id; }

    /** A transaction cannot queue an offline profile write after Auth rollback. */
    public void create(User user, Callback<Void> callback) {
        RemoteTasks.run(() -> {
            if (user == null || !("customer".equals(user.role) || "admin".equals(user.role))) {
                throw new IllegalArgumentException("Role must be customer or admin");
            }
            DocumentReference document = collection().document(validId(user.uid));
            return document.getFirestore().runTransaction(transaction -> {
                if (transaction.get(document).exists()) {
                    throw new FirebaseFirestoreException("Profile already exists",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS);
                }
                transaction.set(document, user);
                return null;
            });
        }, ignored -> null, callback);
    }

    @Override public void save(User user, Callback<Void> callback) {
        RemoteTasks.run(() -> {
            if (user == null || !("customer".equals(user.role) || "admin".equals(user.role))) {
                throw new IllegalArgumentException("Role must be customer or admin");
            }
            return collection().document(validId(user.uid)).set(user);
        }, value -> null, callback);
    }
}
