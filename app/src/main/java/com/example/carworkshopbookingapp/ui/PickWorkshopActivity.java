package com.example.carworkshopbookingapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ListView;
import android.widget.TextView;
import com.example.carworkshopbookingapp.R;
import com.example.carworkshopbookingapp.data.WorkshopBrowser;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.ui.adapter.WorkshopAdapter;
import com.example.carworkshopbookingapp.util.WorkshopDisplay;
import java.util.ArrayList;
import java.util.List;

public final class PickWorkshopActivity extends CatalogueActivity {
    private String serviceId, note;
    private WorkshopAdapter adapter;
    private View header;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_pick_workshop);
        serviceId = getIntent().getStringExtra(BookingActivity.EXTRA_SERVICE_ID);
        note = getIntent().getStringExtra(BookingActivity.EXTRA_NOTE);
        adapter = new WorkshopAdapter(this, services);
        adapter.setRequestedService(serviceId);
        ListView list = findViewById(R.id.pick_workshop_list);
        header = LayoutInflater.from(this).inflate(R.layout.header_pick_workshop, list, false);
        list.addHeaderView(header, null, false); list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (services.get(serviceId) == null || index < 0 || index >= adapter.getCount()) return;
            startActivity(new Intent(this, BookingActivity.class).putExtra(BookingActivity.EXTRA_WORKSHOP_ID, adapter.getItem(index).id)
                    .putExtra(BookingActivity.EXTRA_SERVICE_ID, serviceId).putExtra(BookingActivity.EXTRA_NOTE, note));
        });
        findViewById(R.id.back).setOnClickListener(view -> finish());
        findViewById(R.id.refresh).setOnClickListener(view -> reload()); reload();
    }
    @Override protected void onCatalogueChanged() {
        if (adapter == null) return;
        ServiceItem service = services.get(serviceId);
        List<Workshop> offered = new ArrayList<>();
        if (service != null) for (Workshop workshop : workshops) {
            if (workshop.serviceIds != null && workshop.serviceIds.contains(serviceId)) offered.add(workshop);
        }
        adapter.submit(WorkshopBrowser.filter(offered, services, "", WorkshopBrowser.Filter.ALL, WorkshopDisplay.dhakaMinute()));
        ((TextView) header.findViewById(R.id.pick_service)).setText(service == null ? "" : getString(R.string.pick_workshop_help, service.name));
        findViewById(R.id.refresh).setEnabled(!loading);
        ((android.widget.Button)findViewById(R.id.refresh)).setText(hasError()?R.string.retry:R.string.refresh);
        header.findViewById(R.id.browse_progress).setVisibility(loading ? View.VISIBLE : View.GONE);
        int message = hasError() ? R.string.browse_error : loading && offered.isEmpty() ? R.string.browse_loading :
                service == null ? R.string.pick_service_missing : offered.isEmpty() ? R.string.pick_workshop_empty : 0;
        TextView status = header.findViewById(R.id.browse_status); status.setVisibility(message == 0 ? View.GONE : View.VISIBLE);
        if (hasError() && catalogueError!=null) status.setText(catalogueError); else if (message != 0) status.setText(message);
    }
}
