package com.example.carworkshopbookingapp.data.local;

import android.content.Context;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.User;
import com.example.carworkshopbookingapp.util.AppExecutors;
import org.json.JSONObject;

public final class SessionStore {
    private static final String KEY = "current_user";
    private final LocalDb local;

    public SessionStore(Context context) { local = LocalDb.getInstance(context); }

    public void save(User user, Callback<Void> callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("plus",user.plus); json.put("plusUntil",user.plusUntil);
            json.put("uid", user.uid);
            json.put("name", user.name);
            json.put("phone", user.phone);
            json.put("email", user.email);
            json.put("role", user.role);
            local.putSession(KEY, json.toString(), callback);
        } catch (Exception error) { AppExecutors.main(() -> callback.onError(error)); }
    }

    public void read(Callback<User> callback) {
        local.getSession(KEY, new Callback<String>() {
            @Override public void onSuccess(String payload) {
                try {
                    if (payload == null) { callback.onSuccess(null); return; }
                    JSONObject json = new JSONObject(payload);
                    User user = new User();
                    user.plus=json.optBoolean("plus"); user.plusUntil=json.optLong("plusUntil");
                    user.uid = json.getString("uid");
                    user.name = json.optString("name");
                    user.phone = json.optString("phone");
                    user.email = json.optString("email");
                    user.role = json.getString("role");
                    callback.onSuccess(user);
                } catch (Exception error) { callback.onError(error); }
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void clear(Callback<Void> callback) { local.clearSession(callback); }
}
