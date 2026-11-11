package com.example.carworkshopbookingapp.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.ui.adapter.ServiceAdapter;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.ArrayList;
import java.util.List;

public final class WorkshopDetailActivity extends CatalogueActivity {
    public static final String EXTRA_WORKSHOP_ID = "workshopId";
    private String workshopId;
    private Workshop workshop;
    private ServiceAdapter adapter;
    private View header;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_workshop_detail);
        workshopId = getIntent().getStringExtra(EXTRA_WORKSHOP_ID);
        findViewById(R.id.back).setOnClickListener(view -> finish());
        ListView list = findViewById(R.id.service_list);
        list.setItemsCanFocus(true);
        header = LayoutInflater.from(this).inflate(R.layout.header_workshop_detail, list, false);
        list.addHeaderView(header, null, false);
        adapter = new ServiceAdapter(this, service -> {
            if (workshop != null) startActivity(new Intent(this, BookingActivity.class)
                    .putExtra(BookingActivity.EXTRA_WORKSHOP_ID, workshop.id)
                    .putExtra(BookingActivity.EXTRA_SERVICE_ID, service.id));
        });
        list.setAdapter(adapter);
        findViewById(R.id.refresh).setOnClickListener(view -> reload());
        header.findViewById(R.id.call).setOnClickListener(view -> {
            if (workshop == null) return;
            if (workshop.phone == null || workshop.phone.trim().isEmpty()) {
                Toast.makeText(this, R.string.phone_unavailable, Toast.LENGTH_SHORT).show();
            } else { launch(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", workshop.phone, null))); }
        });
        header.findViewById(R.id.directions).setOnClickListener(view -> {
            if (workshop == null) return;
            Uri location = Uri.parse("geo:0,0?q=" + Uri.encode(workshop.address + ", " + workshop.area + ", Dhaka, Bangladesh"));
            Intent map = new Intent(Intent.ACTION_VIEW, location).setPackage("com.google.android.apps.maps");
            try { startActivity(map); }
            catch (ActivityNotFoundException error) { launch(new Intent(Intent.ACTION_VIEW, location)); }
        });
        reload();
    }

    private void launch(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException error) { Toast.makeText(this, R.string.external_unavailable, Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onCatalogueChanged() {
        if (adapter == null) return;
        workshop = null;
        for (Workshop item : workshops) if (item.id.equals(workshopId)) { workshop = item; break; }
        List<ServiceItem> offered = new ArrayList<>();
        if (workshop != null && workshop.serviceIds != null) for (String id : workshop.serviceIds) {
            ServiceItem service = services.get(id);
            if (service != null) offered.add(service);
        }
        adapter.submit(offered);
        com.example.carworkshopbookingapp.util.WorkshopImages.bind(header.findViewById(R.id.detail_image), workshop);
        ((TextView) header.findViewById(R.id.detail_name)).setText(workshop == null ? getString(R.string.detail_missing) : workshop.name);
        ((TextView) header.findViewById(R.id.detail_address)).setText(workshop == null ? "" : workshop.address);
        ((TextView) header.findViewById(R.id.detail_rating)).setText(workshop == null ? "" : getString(R.string.rating_value, workshop.rating));
        ((TextView) header.findViewById(R.id.detail_hours)).setText(workshop == null ? "" :
                workshop.openHour == workshop.closeHour ? getString(R.string.hours_all_day) :
                getString(R.string.hours_value, WorkshopDisplay.hours(workshop.openHour, workshop.closeHour)));
        ((TextView) header.findViewById(R.id.detail_description)).setText(workshop == null ? "" : workshop.description);
        header.findViewById(R.id.call).setEnabled(workshop != null);
        header.findViewById(R.id.directions).setEnabled(workshop != null);
        findViewById(R.id.refresh).setEnabled(!loading);
        ((android.widget.Button)findViewById(R.id.refresh)).setText(hasError()?R.string.retry:R.string.refresh);
        header.findViewById(R.id.browse_progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        int message = hasError() ? R.string.browse_error : loading && (workshop == null || offered.isEmpty()) ?
                R.string.detail_loading : workshop == null ? R.string.detail_missing : offered.isEmpty() ? R.string.service_empty : 0;
        TextView status = header.findViewById(R.id.browse_status);
        status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (hasError() && catalogueError!=null) status.setText(catalogueError); else if (message != 0) status.setText(message);
    }
}
