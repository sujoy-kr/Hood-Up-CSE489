package com.example.carworkshopbookingapp.data.remote;

import android.content.Context;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.Workshop;
import java.util.List;

public final class WorkshopsRepository extends CachedRepository<Workshop> {
    private final LocalDb local;

    public WorkshopsRepository(Context context) {
        super("workshops", Workshop.class);
        local = LocalDb.getInstance(context);
    }

    @Override protected String idOf(Workshop item) { return item.id; }
    @Override protected void assignId(Workshop item, String id) { item.id = id; }
    @Override protected void readCache(Callback<List<Workshop>> callback) {
        local.getWorkshops(callback);
    }
    @Override protected void writeCache(List<Workshop> items, Callback<Void> callback) {
        local.replaceWorkshops(items, callback);
    }
}
