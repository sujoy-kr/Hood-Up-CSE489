package com.example.carworkshopbookingapp.ui;

import com.example.carworkshopbookingapp.data.RefreshCallback;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import com.example.carworkshopbookingapp.data.remote.ServicesRepository;
import com.example.carworkshopbookingapp.data.remote.WorkshopsRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CatalogueActivity extends BaseActivity {
    protected List<Workshop> workshops = new ArrayList<>();
    protected final Map<String, ServiceItem> services = new HashMap<>();
    protected boolean loading;
    private final boolean[] errors = new boolean[2];
    protected String catalogueError;
    private int generation;
    private int pending;

    protected abstract void onCatalogueChanged();

    protected final boolean hasError() { return errors[0] || errors[1]; }

    protected final void reload() {
        int request = ++generation;
        pending = 2;
        loading = true;
        errors[0] = errors[1] = false; catalogueError=null;
        onCatalogueChanged();
        new WorkshopsRepository(getApplicationContext()).list(new RefreshCallback<List<Workshop>>() {
            @Override public void onSuccess(List<Workshop> items) {
                if (!active(request)) return;
                workshops = items; errors[0] = false; onCatalogueChanged();
            }
            @Override public void onError(Exception error) {
                if (!active(request)) return;
                errors[0] = true; catalogueError=UiErrors.message(error); onCatalogueChanged();
            }
            @Override public void onRefreshComplete() { complete(request); }
        });
        new ServicesRepository(getApplicationContext()).list(new RefreshCallback<List<ServiceItem>>() {
            @Override public void onSuccess(List<ServiceItem> items) {
                if (!active(request)) return;
                services.clear();
                for (ServiceItem item : items) services.put(item.id, item);
                errors[1] = false; onCatalogueChanged();
            }
            @Override public void onError(Exception error) {
                if (!active(request)) return;
                errors[1] = true; catalogueError=UiErrors.message(error); onCatalogueChanged();
            }
            @Override public void onRefreshComplete() { complete(request); }
        });
    }

    private boolean active(int request) { return request == generation && !isFinishing() && !isDestroyed(); }
    private void complete(int request) {
        if (!active(request)) return;
        loading = --pending > 0;
        onCatalogueChanged();
    }
}
