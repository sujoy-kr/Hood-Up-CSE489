package com.example.carworkshopbookingapp.data.remote;

import android.content.Context;
import com.example.carworkshopbookingapp.data.*;
import com.example.carworkshopbookingapp.data.local.SessionStore;
import com.example.carworkshopbookingapp.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

public final class CustomerRepository {
    private final SessionStore session;
    public CustomerRepository(Context context) {session=new SessionStore(context);}
    public void update(String uid,String name,String phone,String email,Callback<User> callback) {
        RemoteTasks.run(()->{
            if (FirebaseAuth.getInstance().getCurrentUser()==null || !uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) throw new IllegalStateException("Sign in again");
            DocumentReference ref=FirebaseFirestore.getInstance().collection("users").document(uid);
            return ref.getFirestore().runTransaction(transaction->{
                User user=transaction.get(ref).toObject(User.class);
                if (user==null) throw new IllegalStateException("Profile unavailable");
                user.uid=uid;
                user.name=name; user.phone=phone; user.email=email;
                transaction.update(ref,"name",name,"phone",phone,"email",email);
                return user;
            });
        }, user->user,new Callback<User>() {
            @Override public void onSuccess(User user) {session.save(user,new Callback<Void>() {
                @Override public void onSuccess(Void ignored) {callback.onSuccess(user);}
                @Override public void onError(Exception error) {callback.onSuccess(user);}
            });}
            @Override public void onError(Exception error) {callback.onError(error);}
        });
    }
}
