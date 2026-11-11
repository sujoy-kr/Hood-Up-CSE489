package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.CarStore;
import com.example.carworkshopbookingapp.data.RefreshCallback;
import com.example.carworkshopbookingapp.data.model.Car;
import com.example.carworkshopbookingapp.ui.adapter.CarAdapter;
import com.example.carworkshopbookingapp.util.NetworkState;
import java.util.List;

public final class CarsActivity extends CustomerActivity {
    private CarStore store;
    private CarAdapter adapter;
    private FormOperation<Void> operation;
    private int generation;

    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_cars);
        ListView rows=findViewById(R.id.cars_list);rows.addHeaderView(getLayoutInflater().inflate(R.layout.header_cars,rows,false),null,false);
        Object retained = getLastCustomNonConfigurationInstance();
        operation = retained instanceof FormOperation ? (FormOperation<Void>) retained : new FormOperation<>();
        findViewById(R.id.back).setOnClickListener(view -> { if (!operation.busy) finish(); });
        findViewById(R.id.add_car).setOnClickListener(view -> { if (!operation.busy && user != null) startActivity(new Intent(this, CarFormActivity.class)); });
        findViewById(R.id.refresh).setOnClickListener(view -> { if (!operation.busy) {if(store!=null)loadCars();else loadCustomer();} });
        adapter = new CarAdapter(this, new CarAdapter.Actions() {
            @Override public void edit(Car car) {
                if (!operation.busy) startActivity(new Intent(CarsActivity.this, CarFormActivity.class).putExtra(CarFormActivity.EXTRA_CAR_ID, car.id));
            }
            @Override public void delete(Car car) {
                if (operation.busy) return;
                new AlertDialog.Builder(CarsActivity.this).setTitle(R.string.delete_car_title).setMessage(R.string.delete_car_body)
                        .setNegativeButton(R.string.cancel_action, null).setPositiveButton(R.string.delete_car,
                        (dialog, which) -> { if (!operation.busy) store.delete(car.id, operation.begin()); }).show();
            }
        });
        rows.setItemsCanFocus(true); rows.setAdapter(adapter);
        findViewById(R.id.add_car).setEnabled(false);
        loadCustomer();
    }
    @Override protected void onCustomerReady() { store = new CarStore(getApplicationContext(), user.uid); loadCars(); }
    private void loadCars() {
        int request = ++generation;
        TextView status=findViewById(R.id.cars_status);status.setText(R.string.cars_loading);((android.widget.Button)findViewById(R.id.refresh)).setText(R.string.refresh);status.setVisibility(View.VISIBLE);
        findViewById(R.id.refresh).setEnabled(false);
        store.list(new RefreshCallback<List<Car>>() {
            @Override public void onSuccess(List<Car> cars) {
                if (!active(request)) return;
                adapter.submit(cars);
                TextView status = findViewById(R.id.cars_status);
                status.setText(cars.isEmpty() ? R.string.cars_empty : R.string.cars_cached);
                status.setVisibility(cars.isEmpty() || !NetworkState.online(CarsActivity.this) ? View.VISIBLE : View.GONE);
            }
            @Override public void onError(Exception error) {
                if (!active(request)) return;
                TextView status = findViewById(R.id.cars_status); status.setText(UiErrors.message(error));((android.widget.Button)findViewById(R.id.refresh)).setText(R.string.retry); status.setVisibility(View.VISIBLE);
            }
            @Override public void onRefreshComplete() {
                if (!active(request)) return;
                findViewById(R.id.refresh).setEnabled(!operation.busy); render();
            }
        });
    }
    private boolean active(int request) { return request == generation && !isFinishing() && !isDestroyed(); }
    private void render() {
        if (isFinishing() || isDestroyed()) return;
        FormViews.enabled(findViewById(R.id.main), !operation.busy);
        findViewById(R.id.add_car).setEnabled(user != null && !operation.busy);
        findViewById(R.id.form_progress).setVisibility(operation.busy ? View.VISIBLE : View.GONE);
        TextView error = findViewById(R.id.form_error);
        error.setVisibility(operation.error == null ? View.GONE : View.VISIBLE);
        if (operation.error != null) error.setText(FormViews.error(this, operation.error));
        if (operation.complete && store != null) { operation.complete = false; loadCars(); }
    }
    @Override protected void onStart() { super.onStart(); operation.listener = this::render; render(); }
    @Override protected void onResume() { super.onResume(); if (store != null && !operation.busy) loadCars(); }
    @Override protected void onStop() { operation.listener = null; super.onStop(); }
    @Override public Object onRetainCustomNonConfigurationInstance() { return operation; }
    @Override protected boolean allowBack() { return operation == null || !operation.busy; }
}
