package com.example.carworkshopbookingapp.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.*;
import com.example.carworkshopbookingapp.data.local.SessionStore;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.text.SimpleDateFormat;
import java.util.*;

public final class BookingActivity extends CatalogueActivity {
    public static final String EXTRA_WORKSHOP_ID = "workshopId";
    public static final String EXTRA_SERVICE_ID = "serviceId";
    public static final String EXTRA_NOTE = "note";
    private String workshopId, serviceId, requestId, selectedCarId;
    private Workshop workshop;
    private ServiceItem service;
    private User user;
    private List<Car> cars = new ArrayList<>();
    private BookingAvailability.Result availability;
    private boolean availabilityLoading;
    private int availabilityGeneration, carsGeneration;
    private long selectedDay, selectedSlot;
    private FormOperation<BookingSubmission.Receipt> operation;
    private boolean started;
    private Exception handledError;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() { render(); timer.postDelayed(this, 60000); }
    };

    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_booking);
        Object retained = getLastCustomNonConfigurationInstance();
        operation = retained instanceof FormOperation ? (FormOperation<BookingSubmission.Receipt>) retained : new FormOperation<>();
        workshopId = getIntent().getStringExtra(EXTRA_WORKSHOP_ID);
        serviceId = getIntent().getStringExtra(EXTRA_SERVICE_ID);
        requestId = state == null ? UUID.randomUUID().toString() : state.getString("request_id", UUID.randomUUID().toString());
        selectedDay = state == null ? BookingSlots.dayOf(System.currentTimeMillis()) : state.getLong("day", BookingSlots.dayOf(System.currentTimeMillis()));
        selectedSlot = state == null ? 0 : state.getLong("slot");
        selectedCarId = state == null ? null : state.getString("car_id");
        if (state == null) {
            String note = getIntent().getStringExtra(EXTRA_NOTE);
            if (note == null) note = getIntent().getStringExtra("prefilledNote");
            if (note != null) ((EditText) findViewById(R.id.booking_note)).setText(note);
        }
        findViewById(R.id.back).setOnClickListener(view -> { if (!operation.busy) finish(); });
        findViewById(R.id.add_car).setOnClickListener(view -> {
            if (!operation.busy) startActivity(new Intent(this, CarFormActivity.class));
        });
        findViewById(R.id.date).setOnClickListener(view -> chooseDate());
        findViewById(R.id.refresh).setOnClickListener(view -> {
            if (!operation.busy) { availability = null; availabilityLoading = false; ++availabilityGeneration; refreshMember(); reload(); }
        });
        ((Spinner) findViewById(R.id.car_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < cars.size()) selectedCarId = cars.get(position).id;
                render();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { selectedCarId = null; }
        });
        findViewById(R.id.confirm_booking).setOnClickListener(view -> confirm());
        new SessionStore(getApplicationContext()).read(new Callback<User>() {
            @Override public void onSuccess(User result) {
                if (isFinishing() || isDestroyed()) return;
                if (result == null) { EntryNavigation.login(BookingActivity.this); return; }
                if (!"customer".equals(result.role)) { EntryNavigation.home(BookingActivity.this, result); return; }
                user = result; refreshMember(); loadCars(); onCatalogueChanged();
            }
            @Override public void onError(Exception error) { if (!isDestroyed()) showError(getString(R.string.error_profile)); }
        });
        reload();
    }

    private void refreshMember() {
        if (user==null || !com.example.carworkshopbookingapp.util.NetworkState.online(this)) return;
        new com.example.carworkshopbookingapp.data.remote.UsersRepository().get(user.uid,new Callback<User>() {
            @Override public void onSuccess(User fresh) {
                if (isFinishing() || isDestroyed()) return;
                user=fresh;
                new SessionStore(getApplicationContext()).save(fresh,new Callback<Void>() {
                    @Override public void onSuccess(Void ignored) { }
                    @Override public void onError(Exception error) { }
                });
                render();
            }
            @Override public void onError(Exception error) { }
        });
    }

    private void loadCars() {
        int request = ++carsGeneration;
        new CarStore(getApplicationContext(), user.uid).list(new RefreshCallback<List<Car>>() {
            @Override public void onSuccess(List<Car> items) {
                if (request != carsGeneration || isDestroyed() || isFinishing()) return;
                cars = items;
                List<String> labels = new ArrayList<>(); int selected = 0;
                for (int i = 0; i < cars.size(); i++) {
                    Car car = cars.get(i);
                    labels.add(getString(R.string.car_label, car.make, car.model, car.plate));
                    if (car.id.equals(selectedCarId)) selected = i;
                }
                Spinner spinner = findViewById(R.id.car_spinner);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(BookingActivity.this, R.layout.spinner_item, labels);
                adapter.setDropDownViewResource(R.layout.spinner_item); spinner.setAdapter(adapter);
                selectedCarId = cars.isEmpty() ? null : cars.get(selected).id;
                if (!cars.isEmpty()) spinner.setSelection(selected);
                render();
            }
            @Override public void onError(Exception error) {
                if (request == carsGeneration && !isDestroyed()) showError(FormViews.error(BookingActivity.this, error));
            }
            @Override public void onRefreshComplete() { if (!isDestroyed()) render(); }
        });
    }

    @Override protected void onCatalogueChanged() {
        if (operation == null) return;
        workshop = null;
        for (Workshop item : workshops) if (item.id.equals(workshopId)) { workshop = item; break; }
        service = services.get(serviceId);
        if (workshop != null && (workshop.serviceIds == null || !workshop.serviceIds.contains(serviceId))) service = null;
        if (workshop != null && service != null && user != null && availability == null && !availabilityLoading) loadAvailability();
        render();
    }

    private void loadAvailability() {
        int request = ++availabilityGeneration; availabilityLoading = true;
        BookingAvailability.load(getApplicationContext(), user.uid, workshopId, new Callback<BookingAvailability.Result>() {
            @Override public void onSuccess(BookingAvailability.Result result) {
                if (request != availabilityGeneration || isDestroyed() || isFinishing()) return;
                availabilityLoading = false; availability = result; render();
            }
            @Override public void onError(Exception error) {
                if (request != availabilityGeneration || isDestroyed() || isFinishing()) return;
                availabilityLoading = false;
                ((TextView) findViewById(R.id.availability_status)).setText(R.string.slots_error);
                render();
            }
        });
    }

    private String format(long time, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka")); return format.format(new Date(time));
    }

    private void chooseDate() {
        if (operation.busy || workshop == null) return;
        Calendar day = BookingSlots.calendar(selectedDay);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, date) -> {
            long chosen = BookingSlots.day(year, month, date);
            if (chosen < BookingSlots.dayOf(System.currentTimeMillis())) { showError(getString(R.string.booking_past)); return; }
            if (BookingSlots.closed(workshop, chosen)) { showError(getString(R.string.booking_closed)); return; }
            selectedDay = chosen; selectedSlot = 0; render();
        }, day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH));
        Calendar today = BookingSlots.calendar(System.currentTimeMillis());
        Calendar pickerMin = Calendar.getInstance(); pickerMin.clear();
        pickerMin.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(pickerMin.getTimeInMillis()); picker.show();
    }

    private boolean blocked(long slot) {
        if (availability == null || service == null) return true;
        for (Booking other : availability.bookings) if (BookingSlots.overlaps(slot, service.durationMin, other)) return true;
        return false;
    }

    private Car chosenCar() {
        for (Car car : cars) if (car.id.equals(selectedCarId)) return car;
        return null;
    }

    private void render() {
        if (operation == null || isDestroyed() || isFinishing()) return;
        if (started && operation.complete) {
            startActivity(BookingSuccessActivity.intent(this, operation.result)); finish(); return;
        }
        if (operation.error != null && handledError != operation.error) {
            handledError = operation.error;
            for (Throwable cause = operation.error; cause != null; cause = cause.getCause()) {
                if (cause instanceof com.example.carworkshopbookingapp.data.remote.BookingFailure &&
                        ((com.example.carworkshopbookingapp.data.remote.BookingFailure) cause).reason ==
                        com.example.carworkshopbookingapp.data.remote.BookingFailure.Reason.SLOT_TAKEN) {
                    selectedSlot = 0; availability = null;
                    if (user != null && workshop != null && service != null && !availabilityLoading) loadAvailability();
                    break;
                }
            }
        }
        FormViews.enabled(findViewById(R.id.main), !operation.busy);
        boolean metadata = workshop != null && service != null;
        ((TextView) findViewById(R.id.summary_workshop)).setText(workshop == null ? getString(R.string.detail_loading) : workshop.name);
        ((TextView) findViewById(R.id.summary_service)).setText(service == null ? "" : service.name);
        ((TextView) findViewById(R.id.summary_price)).setText(service == null ? "" : getString(R.string.price_duration_summary, WorkshopDisplay.money(BusinessConfig.price(service.basePrice)), service.durationMin));
        ((TextView) findViewById(R.id.service_price_line)).setText(service == null ? "" : getString(R.string.service_price_line, WorkshopDisplay.money(service.basePrice)));
        ((TextView) findViewById(R.id.total_price_line)).setText(service == null ? "" : getString(R.string.total_price_line, WorkshopDisplay.money(BusinessConfig.price(service.basePrice))));
        ((Button) findViewById(R.id.date)).setText(getString(R.string.date_label, format(selectedDay, "EEE, dd MMM yyyy")));
        findViewById(R.id.date).setEnabled(metadata && !operation.busy);
        findViewById(R.id.add_car).setEnabled(user != null && !operation.busy);
        findViewById(R.id.refresh).setEnabled(!operation.busy && !loading && !availabilityLoading);
        findViewById(R.id.car_spinner).setEnabled(!operation.busy && !cars.isEmpty());
        GridLayout grid = findViewById(R.id.slot_grid); grid.removeAllViews();
        List<Long> slots = metadata ? BookingSlots.slots(workshop, service.durationMin, selectedDay, System.currentTimeMillis()) : Collections.emptyList();
        if (metadata && user != null && availability != null && !availabilityLoading
                && (!slots.contains(selectedSlot) || blocked(selectedSlot))) selectedSlot = 0;
        int availableCount = 0;
        for (long slot : slots) {
            boolean taken = blocked(slot);
            if (!taken) availableCount++;
            Button chip = (Button) LayoutInflater.from(this).inflate(R.layout.item_slot, grid, false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(); params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int gap = getResources().getDimensionPixelSize(R.dimen.spacing) / 2;
            params.setMargins(gap, gap, gap, gap); chip.setLayoutParams(params);
            String label = format(slot, "EEE h:mm a"); chip.setText(label);
            chip.setContentDescription(taken ? getString(R.string.slot_unavailable, label) : getString(R.string.slot_label, label));
            chip.setBackgroundResource(selectedSlot == slot ? R.drawable.chip_selected : R.drawable.chip_default);
            chip.setTextColor(ContextCompat.getColor(this, taken ? R.color.text_muted : selectedSlot == slot ? R.color.accent : R.color.text));
            chip.setEnabled(!taken && !operation.busy); chip.setAlpha(taken ? 0.45f : 1f);
            chip.setOnClickListener(view -> { selectedSlot = slot; render(); }); grid.addView(chip);
        }
        TextView status = findViewById(R.id.availability_status);
        int statusText = !metadata ? (loading ? R.string.detail_loading : R.string.selection_missing) :
                availabilityLoading ? R.string.slots_loading : availability == null ? R.string.slots_error :
                BookingSlots.closed(workshop, selectedDay) ? R.string.booking_closed :
                availableCount == 0 ? R.string.slots_empty : availability.offline ? R.string.slots_offline : 0;
        status.setVisibility(statusText == 0 ? View.GONE : View.VISIBLE);
        if (statusText != 0) status.setText(statusText);
        findViewById(R.id.confirm_booking).setEnabled(!operation.busy && metadata && user != null && chosenCar() != null && selectedSlot > 0 && availability != null);
        findViewById(R.id.form_progress).setVisibility(operation.busy ? View.VISIBLE : View.GONE);
        TextView error = findViewById(R.id.form_error);
        if (operation.error != null) { error.setText(FormViews.error(this, operation.error)); error.setVisibility(View.VISIBLE); }
        else if (operation.busy) error.setVisibility(View.GONE);
    }

    private void confirm() {
        if (operation.busy || user == null || workshop == null || service == null || chosenCar() == null ||
                blocked(selectedSlot) || !BookingSlots.allowed(workshop, service.durationMin, selectedSlot, System.currentTimeMillis())) {
            showError(getString(R.string.booking_choose)); return;
        }
        Car car = chosenCar(); Booking booking = new Booking();
        booking.id = requestId; booking.userId = user.uid; booking.userName = user.name;
        booking.workshopId = workshopId; booking.workshopName = workshop.name;
        booking.serviceId = serviceId; booking.serviceName = service.name;
        booking.carId = car.id; booking.carLabel = getString(R.string.car_label, car.make, car.model, car.plate);
        booking.dateTimeMillis = selectedSlot; booking.durationMin = service.durationMin;
        booking.basePrice = service.basePrice;
        booking.price = BusinessConfig.price(service.basePrice);
        booking.createdAt = System.currentTimeMillis();
        booking.note = ((EditText) findViewById(R.id.booking_note)).getText().toString().trim();
        if(booking.note.length()>1000){showError("Keep the note within 1000 characters.");return;}
        findViewById(R.id.confirm_booking).performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        new BookingSubmission(getApplicationContext()).submit(booking, operation.begin());
    }

    private void showError(String message) {
        TextView error = findViewById(R.id.form_error); error.setText(message); error.setVisibility(View.VISIBLE);
    }
    @Override protected void onStart() { super.onStart(); started = true; operation.listener = this::render; render(); }
    @Override protected void onResume() { super.onResume(); if (user != null) loadCars(); timer.post(tick); }
    @Override protected void onPause() { timer.removeCallbacksAndMessages(null); super.onPause(); }
    @Override protected void onStop() { started = false; operation.listener = null; super.onStop(); }
    @Override public Object onRetainCustomNonConfigurationInstance() { return operation; }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("request_id", requestId); state.putString("car_id", selectedCarId);
        state.putLong("day", selectedDay); state.putLong("slot", selectedSlot); super.onSaveInstanceState(state);
    }
    @Override protected boolean allowBack() { return operation == null || !operation.busy; }
}
