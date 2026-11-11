package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.util.Patterns;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.*;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.data.remote.*;
import com.example.carworkshopbookingapp.util.ReminderScheduler;
import java.util.List;

public final class ProfileActivity extends CustomerActivity {
    private FormOperation<User> operation;
    private boolean loaded;
    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);screen(R.layout.activity_profile);
        Object retained=getLastCustomNonConfigurationInstance();operation=retained instanceof FormOperation?(FormOperation<User>)retained:new FormOperation<>();
        loaded=state!=null && state.getBoolean("loaded");
        CustomerNavigation.bind(this);
        findViewById(R.id.manage_cars).setOnClickListener(v->startActivity(new Intent(this,CarsActivity.class)));
        findViewById(R.id.my_bookings).setOnClickListener(v->startActivity(new Intent(this,MyBookingsActivity.class)));
        findViewById(R.id.about).setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));
        findViewById(R.id.save_profile).setOnClickListener(v->{
            if (operation.busy || user==null) return;
            String name=value(R.id.profile_name),phone=value(R.id.profile_phone),email=value(R.id.profile_email);
            if (!InputRules.name(name) || !InputRules.phone(phone) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {error("Enter a name, valid phone and contact email.");return;}
            new CustomerRepository(getApplicationContext()).update(user.uid,name,phone,email,operation.begin());
        });
        findViewById(R.id.sign_out).setOnClickListener(v->{
            if (operation.busy || user==null) return;
            operation.begin();
            com.example.carworkshopbookingapp.service.SyncCoordinator.logout(this);
            FormViews.enabled(findViewById(R.id.main),false);
            logout();
        });
        loadCustomer();
    }
    private String value(int id) {return ((EditText)findViewById(id)).getText().toString().trim();}
    private void error(String text) {notice(text,R.color.danger);}
    private void notice(String text,int color) {if(isDestroyed()||isFinishing())return;TextView message=findViewById(R.id.form_error);message.setText(text);message.setTextColor(androidx.core.content.ContextCompat.getColor(this,color));message.setVisibility(View.VISIBLE);}
    private void logout() {
        new AuthRepository(getApplicationContext()).signOut(new Callback<Void>() {
            @Override public void onSuccess(Void ignored) {operation.onSuccess(null);}
            @Override public void onError(Exception error) {operation.onError(error);}
        });
    }
    @Override protected void onCustomerReady() {
        if (!loaded) {
            ((EditText)findViewById(R.id.profile_name)).setText(user.name);((EditText)findViewById(R.id.profile_phone)).setText(user.phone);((EditText)findViewById(R.id.profile_email)).setText(user.email);loaded=true;
        }
        initials();
        Switch toggle=findViewById(R.id.reminders_toggle);
        toggle.setChecked(getSharedPreferences("customer_settings",MODE_PRIVATE).getBoolean("reminders:"+user.uid,true));
        toggle.setOnCheckedChangeListener((button,enabled)->{
            getSharedPreferences("customer_settings",MODE_PRIVATE).edit().putBoolean("reminders:"+user.uid,enabled).apply();
            LocalDb.getInstance(this).getBookings(user.uid,new Callback<List<Booking>>() {
                @Override public void onSuccess(List<Booking> items) {for (Booking booking:items) ReminderScheduler.update(ProfileActivity.this,booking);}
                @Override public void onError(Exception failure) {if (!isDestroyed()) error("Reminder settings saved. Some alarms could not be refreshed.");}
            });
        });render();
    }
    private void initials() {
        StringBuilder result=new StringBuilder();String[] parts=(user.name==null?"":user.name.trim()).split("\\s+");
        for (int i=0;i<Math.min(2,parts.length);i++) if (!parts[i].isEmpty()) result.appendCodePoint(parts[i].codePointAt(0));
        ((TextView)findViewById(R.id.avatar)).setText(result.toString().toUpperCase(java.util.Locale.ROOT));
    }
    private void render() {
        if (operation==null || isDestroyed()) return;
        if (operation.complete && operation.result==null) {EntryNavigation.login(this);return;}
        if (operation.complete) {user=operation.result;operation.complete=false;initials();notice(getString(R.string.profile_saved),R.color.success);}
        FormViews.enabled(findViewById(R.id.main),!operation.busy);
        findViewById(R.id.save_profile).setEnabled(!operation.busy && user!=null);
        findViewById(R.id.form_progress).setVisibility(operation.busy?View.VISIBLE:View.GONE);
        if (operation.error!=null) error(FormViews.error(this,operation.error));
    }
    @Override protected boolean allowBack() { return operation==null || !operation.busy; }
    @Override protected void onStart() {super.onStart();operation.listener=this::render;render();}
    @Override protected void onStop() {operation.listener=null;super.onStop();}
    @Override public Object onRetainCustomNonConfigurationInstance() {return operation;}
    @Override protected void onSaveInstanceState(Bundle state) {state.putBoolean("loaded",loaded);super.onSaveInstanceState(state);}
}
