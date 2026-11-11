package com.example.carworkshopbookingapp.data.remote;

import android.content.Context;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import java.util.List;

public final class ServicesRepository extends CachedRepository<ServiceItem> {
    private final LocalDb local;

    public ServicesRepository(Context context) {
        super("services", ServiceItem.class);
        local = LocalDb.getInstance(context);
    }

    @Override protected String idOf(ServiceItem item) { return item.id; }
    @Override protected void assignId(ServiceItem item, String id) { item.id = id; }
    @Override protected void readCache(Callback<List<ServiceItem>> callback) {
        local.getServices(callback);
    }
    @Override protected void writeCache(List<ServiceItem> items, Callback<Void> callback) {
        local.replaceServices(items, callback);
    }
}
