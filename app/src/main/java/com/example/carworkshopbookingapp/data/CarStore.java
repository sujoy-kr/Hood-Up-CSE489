package com.example.carworkshopbookingapp.data;

import android.content.Context;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Car;
import com.example.carworkshopbookingapp.data.remote.CarsRepository;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.List;

public final class CarStore {
    private final LocalDb local;
    private final CarsRepository remote;
    private final String uid;
    private final Context context;

    public CarStore(Context context, String uid) {
        this.context = context.getApplicationContext(); this.uid = uid;
        local = LocalDb.getInstance(context); remote = new CarsRepository(uid);
    }

    public void list(RefreshCallback<List<Car>> callback) {
        local.getCars(uid, new Callback<List<Car>>() {
            @Override public void onSuccess(List<Car> cars) { callback.onSuccess(cars); refresh(callback); }
            @Override public void onError(Exception error) { callback.onError(error); refresh(callback); }
        });
    }

    private void refresh(RefreshCallback<List<Car>> callback) {
        if (!NetworkState.online(context)) { callback.onRefreshComplete(); return; }
        remote.list(new Callback<List<Car>>() {
            @Override public void onSuccess(List<Car> cars) {
                local.replaceCars(uid, cars, new Callback<Void>() {
                    @Override public void onSuccess(Void ignored) { callback.onSuccess(cars); callback.onRefreshComplete(); }
                    @Override public void onError(Exception error) { callback.onError(error); callback.onRefreshComplete(); }
                });
            }
            @Override public void onError(Exception error) { callback.onError(error); callback.onRefreshComplete(); }
        });
    }

    public void save(Car car, Callback<Void> callback) {
        remote.save(car, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { local.saveCar(uid, car, callback); }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void delete(String id, Callback<Void> callback) {
        remote.delete(id, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { local.deleteCar(uid, id, callback); }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }
}
