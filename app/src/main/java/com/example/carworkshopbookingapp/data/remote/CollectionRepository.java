package com.example.carworkshopbookingapp.data.remote;

import com.example.carworkshopbookingapp.data.Callback;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public abstract class CollectionRepository<T> {
    private final String path;
    private final Class<T> modelType;

    protected CollectionRepository(String path, Class<T> modelType) {
        this.path = path;
        this.modelType = modelType;
    }

    protected CollectionReference collection() {
        return FirebaseFirestore.getInstance().collection(path);
    }

    protected static String validId(String id) {
        if (id == null || id.trim().isEmpty() || id.contains("/")) {
            throw new IllegalArgumentException("A nonempty document ID without slashes is required");
        }
        return id;
    }

    protected abstract String idOf(T item);
    protected abstract void assignId(T item, String id);

    protected T decode(DocumentSnapshot doc) {
        if (!doc.exists()) throw new IllegalStateException("Document not found: " + doc.getId());
        T item = doc.toObject(modelType);
        if (item == null) throw new IllegalStateException("Empty document: " + doc.getId());
        assignId(item, doc.getId());
        return item;
    }

    protected List<T> decodeAll(QuerySnapshot snapshot) {
        List<T> items = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) items.add(decode(doc));
        return items;
    }

    public void get(String id, Callback<T> callback) {
        RemoteTasks.run(() -> collection().document(validId(id)).get(Source.SERVER), this::decode, callback);
    }

    /** Supply a stable ID so retrying an offline write cannot create duplicates. */
    public void save(T item, Callback<Void> callback) {
        RemoteTasks.run(() -> collection().document(validId(idOf(item))).set(item), value -> null, callback);
    }

    public void list(Callback<List<T>> callback) {
        RemoteTasks.run(() -> collection().get(Source.SERVER), this::decodeAll, callback);
    }
}
