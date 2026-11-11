package com.example.carworkshopbookingapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.CarStore;
import com.example.carworkshopbookingapp.data.RefreshCallback;
import com.example.carworkshopbookingapp.data.model.Car;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public final class CarFormActivity extends CustomerActivity {
    public static final String EXTRA_CAR_ID = "carId";
    private CarStore store;
    private FormOperation<Void> operation;
    private String id;
    private boolean ready;

    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_car_form);
        Object retained = getLastCustomNonConfigurationInstance();
        operation = retained instanceof FormOperation ? (FormOperation<Void>) retained : new FormOperation<>();
        id = state == null ? getIntent().getStringExtra(EXTRA_CAR_ID) : state.getString("car_id", getIntent().getStringExtra(EXTRA_CAR_ID));
        if (id == null) id = UUID.randomUUID().toString();
        ready = !getIntent().hasExtra(EXTRA_CAR_ID) || (state != null && state.getBoolean("loaded"));
        findViewById(R.id.back).setOnClickListener(view -> { if (!operation.busy) finish(); });
        findViewById(R.id.save_car).setOnClickListener(view -> save());
        loadCustomer();
    }
    @Override protected void onCustomerReady() {
        store = new CarStore(getApplicationContext(), user.uid);
        if (!ready) store.list(new RefreshCallback<List<Car>>() {
            @Override public void onSuccess(List<Car> cars) {
                if (isFinishing() || isDestroyed() || ready) return;
                for (Car car : cars) if (id.equals(car.id)) {
                    ((EditText) findViewById(R.id.car_make)).setText(car.make);
                    ((EditText) findViewById(R.id.car_model)).setText(car.model);
                    ((EditText) findViewById(R.id.car_year)).setText(Integer.toString(car.year));
                    ((EditText) findViewById(R.id.car_plate)).setText(car.plate);
                    ready = true; render(); break;
                }
            }
            @Override public void onError(Exception error) { if (!isDestroyed()) operation.onError(error); }
            @Override public void onRefreshComplete() {
                if (!ready && !isDestroyed()) {
                    TextView error = findViewById(R.id.form_error); error.setText(R.string.car_missing); error.setVisibility(View.VISIBLE);
                }
            }
        });
        render();
    }
    private String value(int id) { return ((EditText) findViewById(id)).getText().toString().trim(); }
    private boolean check(int id, boolean valid, int message) {
        TextView error = findViewById(id); error.setText(message); error.setVisibility(valid ? View.GONE : View.VISIBLE); return valid;
    }
    private void save() {
        if (operation.busy || store == null || !ready) return;
        boolean valid = check(R.id.car_make_error, !value(R.id.car_make).isEmpty(), R.string.car_make_error);
        valid &= check(R.id.car_model_error, !value(R.id.car_model).isEmpty(), R.string.car_model_error);
        String plate = value(R.id.car_plate);
        valid &= check(R.id.car_plate_error, plate.length() >= 3 && plate.length() <= 30, R.string.car_plate_error);
        int year = 0;
        try { year = Integer.parseInt(value(R.id.car_year)); } catch (NumberFormatException ignored) { }
        valid &= check(R.id.car_year_error, year >= 1900 && year <= Calendar.getInstance().get(Calendar.YEAR) + 1, R.string.car_year_error);
        if (!valid) return;
        Car car = new Car(); car.id = id; car.make = value(R.id.car_make); car.model = value(R.id.car_model); car.year = year; car.plate = plate;
        store.save(car, operation.begin());
    }
    private void render() {
        if (isFinishing() || isDestroyed()) return;
        if (operation.complete) { finish(); return; }
        FormViews.enabled(findViewById(R.id.main), !operation.busy);
        findViewById(R.id.save_car).setEnabled(store != null && ready && !operation.busy);
        findViewById(R.id.form_progress).setVisibility(operation.busy ? View.VISIBLE : View.GONE);
        TextView error = findViewById(R.id.form_error); error.setVisibility(operation.error == null ? View.GONE : View.VISIBLE);
        if (operation.error != null) error.setText(FormViews.error(this, operation.error));
    }
    @Override protected void onStart() { super.onStart(); operation.listener = this::render; render(); }
    @Override protected void onStop() { operation.listener = null; super.onStop(); }
    @Override public Object onRetainCustomNonConfigurationInstance() { return operation; }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("car_id", id); state.putBoolean("loaded", ready); super.onSaveInstanceState(state); }
    @Override protected boolean allowBack() { return operation == null || !operation.busy; }
}
