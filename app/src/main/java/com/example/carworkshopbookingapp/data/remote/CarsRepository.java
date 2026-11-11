package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.Car;
import com.google.firebase.firestore.Source;
import java.util.List;

public final class CarsRepository extends CollectionRepository<Car> {
    public CarsRepository(String uid) { super("users/" + validId(uid) + "/cars", Car.class); }
    @Override protected String idOf(Car item) { return item.id; }
    @Override protected void assignId(Car item, String id) { item.id = id; }

    @Override public void save(Car car, Callback<Void> callback) {
        RemoteTasks.run(() -> {
            com.google.firebase.firestore.DocumentReference ref = collection().document(validId(car.id));
            return ref.getFirestore().runTransaction(transaction -> {
                transaction.get(ref);
                transaction.set(ref, car);
                return null;
            });
        }, ignored -> null, callback);
    }

    public void delete(String id, Callback<Void> callback) {
        RemoteTasks.run(() -> {
            com.google.firebase.firestore.DocumentReference ref = collection().document(validId(id));
            return ref.getFirestore().runTransaction(transaction -> {
                transaction.get(ref);
                transaction.delete(ref);
                return null;
            });
        }, value -> null, callback);
    }
}
