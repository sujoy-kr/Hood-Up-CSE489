package com.example.carworkshopbookingapp.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Looper;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.*;
import com.example.carworkshopbookingapp.util.AppExecutors;
import java.util.ArrayList;
import java.util.List;

public final class LocalDb extends SQLiteOpenHelper {
    private static volatile LocalDb instance;

    public static LocalDb getInstance(Context context) {
        if (instance == null) {
            synchronized (LocalDb.class) {
                if (instance == null) instance = new LocalDb(context.getApplicationContext());
            }
        }
        return instance;
    }

    private LocalDb(Context context) { super(context, "hood_up.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE workshops_cache (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE services_cache (id TEXT PRIMARY KEY NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE cars (id TEXT NOT NULL, user_id TEXT NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(user_id, id))");
        db.execSQL("CREATE TABLE pending_bookings (id TEXT PRIMARY KEY NOT NULL, user_id TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE INDEX pending_user ON pending_bookings(user_id)");
        db.execSQL("CREATE TABLE chat_history (id TEXT PRIMARY KEY NOT NULL, user_id TEXT NOT NULL, created_at INTEGER NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE INDEX chat_user_time ON chat_history(user_id, created_at)");
        createBookingsCache(db);
        db.execSQL("CREATE TABLE session (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createBookingsCache(db);
    }

    private void createBookingsCache(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bookings_cache (id TEXT PRIMARY KEY NOT NULL, user_id TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE INDEX bookings_cache_user ON bookings_cache(user_id)");
    }

    public void getBookings(String uid, Callback<List<Booking>> callback) {
        AppExecutors.execute(() -> {
            java.util.Map<String, Booking> merged = new java.util.HashMap<>();
            for (Booking booking : read("pending_bookings", "user_id = ?", new String[]{uid}, null, ModelJson::decodeBooking)) merged.put(booking.id, booking);
            for (Booking booking : read("bookings_cache", "user_id = ?", new String[]{uid}, null, ModelJson::decodeBooking)) merged.put(booking.id, booking);
            List<Booking> result = new ArrayList<>(merged.values());
            result.sort(java.util.Comparator.comparingLong(item -> item.dateTimeMillis));
            return result;
        }, callback);
    }

    public void replaceBookings(String uid, List<Booking> items, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
            try {
                List<Booking> terminal=new ArrayList<>();
                for(Booking cached:read("bookings_cache","user_id = ?",new String[]{uid},null,ModelJson::decodeBooking))
                    if(cached.status==com.example.carworkshopbookingapp.data.model.BookingStatus.CANCELLED)terminal.add(cached);
                db.delete("bookings_cache", "user_id = ?", new String[]{uid});
                for(Booking cached:terminal)save("bookings_cache",cached.id,uid,ModelJson.encode(cached),null);
                for (Booking item : items) {
                    if (!uid.equals(item.userId)) throw new IllegalArgumentException("Booking owner mismatch");
                    item.synced = true;
                    save("bookings_cache", item.id, uid, ModelJson.encode(item), null);
                    db.delete("pending_bookings", "id = ? AND user_id = ?", new String[]{item.id, uid});
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return null;
        }, callback);
    }

    public void cacheBooking(Booking booking, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
            try {
                save("bookings_cache", booking.id, booking.userId, ModelJson.encode(booking), null);
                if (booking.synced) db.delete("pending_bookings", "id = ? AND user_id = ?", new String[]{booking.id, booking.userId});
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return null;
        }, callback);
    }

    private static void requireWorkerThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("SQLite must run through AppExecutors");
        }
    }

    @Override public SQLiteDatabase getReadableDatabase() {
        requireWorkerThread();
        return super.getReadableDatabase();
    }

    @Override public SQLiteDatabase getWritableDatabase() {
        requireWorkerThread();
        return super.getWritableDatabase();
    }

    private interface Decoder<T> { T decode(String payload) throws Exception; }
    private interface Encoder<T> { String encode(T value) throws Exception; }
    private interface Id<T> { String get(T value); }

    private <T> List<T> read(String table, String selection, String[] args,
                             String order, Decoder<T> decoder) throws Exception {
        List<T> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(table, new String[]{"payload"},
                selection, args, null, null, order)) {
            while (cursor.moveToNext()) result.add(decoder.decode(cursor.getString(0)));
        }
        return result;
    }

    private void save(String table, String id, String userId, String payload, Long createdAt) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Missing record ID");
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("payload", payload);
        if (userId != null) values.put("user_id", userId);
        if (createdAt != null) values.put("created_at", createdAt);
        getWritableDatabase().replaceOrThrow(table, null, values);
    }

    private <T> void replaceCache(String table, List<T> items, Id<T> id,
                                  Encoder<T> encoder) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(table, null, null);
            for (T item : items) save(table, id.get(item), null, encoder.encode(item), null);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void getWorkshops(Callback<List<Workshop>> callback) {
        AppExecutors.execute(() -> read("workshops_cache", null, null, "id", ModelJson::decodeWorkshop), callback);
    }

    public void replaceWorkshops(List<Workshop> items, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            replaceCache("workshops_cache", items, item -> item.id, ModelJson::encode);
            return null;
        }, callback);
    }

    public void getServices(Callback<List<ServiceItem>> callback) {
        AppExecutors.execute(() -> read("services_cache", null, null, "id", ModelJson::decodeServiceItem), callback);
    }

    public void replaceServices(List<ServiceItem> items, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            replaceCache("services_cache", items, item -> item.id, ModelJson::encode);
            return null;
        }, callback);
    }

    public void getCars(String uid, Callback<List<Car>> callback) {
        AppExecutors.execute(() -> read("cars", "user_id = ?", new String[]{uid}, "id", ModelJson::decodeCar), callback);
    }

    public void replaceCars(String uid, List<Car> items, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete("cars", "user_id = ?", new String[]{uid});
                for (Car item : items) save("cars", item.id, uid, ModelJson.encode(item), null);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return null;
        }, callback);
    }

    public void saveCar(String uid, Car car, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            save("cars", car.id, uid, ModelJson.encode(car), null);
            return null;
        }, callback);
    }

    public void deleteCar(String uid, String id, Callback<Void> callback) {
        deleteOwned("cars", uid, id, callback);
    }

    public void enqueueBooking(Booking booking, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            // The queue always contains unsynced records, regardless of the caller's flag.
            Booking queued = ModelJson.decodeBooking(ModelJson.encode(booking));
            queued.synced = false;
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                List<Booking> pending = read("pending_bookings", null, null, null, ModelJson::decodeBooking);
                for (Booking item : pending) {
                    if (item.id.equals(queued.id)) {
                        if (!item.userId.equals(queued.userId) || !item.workshopId.equals(queued.workshopId) ||
                                !item.serviceId.equals(queued.serviceId) || !java.util.Objects.equals(item.carId, queued.carId) ||
                                item.dateTimeMillis != queued.dateTimeMillis || item.pickupRequested != queued.pickupRequested || item.plusApplied != queued.plusApplied || Double.compare(item.price, queued.price) != 0 ||
                                !java.util.Objects.equals(item.note, queued.note)) {
                            throw new com.example.carworkshopbookingapp.data.remote.BookingFailure(
                                    com.example.carworkshopbookingapp.data.remote.BookingFailure.Reason.INVALID_SELECTION);
                        }
                        db.setTransactionSuccessful();
                        return null;
                    }
                    if (item.userId.equals(queued.userId) && item.workshopId.equals(queued.workshopId) &&
                            com.example.carworkshopbookingapp.data.BookingSlots.overlaps(queued.dateTimeMillis, queued.durationMin, item)) {
                        throw new com.example.carworkshopbookingapp.data.remote.BookingFailure(
                                com.example.carworkshopbookingapp.data.remote.BookingFailure.Reason.SLOT_TAKEN);
                    }
                }
                save("pending_bookings", queued.id, queued.userId, ModelJson.encode(queued), null);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return null;
        }, callback);
    }

    public void getPendingBookings(String uid, Callback<List<Booking>> callback) {
        AppExecutors.execute(() -> read("pending_bookings", "user_id = ?", new String[]{uid},
                "id", ModelJson::decodeBooking), callback);
    }

    /** Remove only after the remote write has been acknowledged. */
    public void removePendingBooking(String uid, String id, Callback<Void> callback) {
        deleteOwned("pending_bookings", uid, id, callback);
    }

    public void applySyncSnapshot(String uid, List<Booking> items, Callback<List<Booking>> callback) {
        AppExecutors.execute(() -> {
            SQLiteDatabase db = getWritableDatabase();
            List<Booking> changed = new ArrayList<>();
            db.beginTransaction();
            try {
                for (Booking item : items) {
                    if (!uid.equals(item.userId) || item.id == null || item.status == null) continue;
                    String key = "sync_status:" + uid + ":" + item.id;
                    String previous = null;
                    try (Cursor c = db.query("session", new String[]{"value"}, "key = ?", new String[]{key}, null, null, null)) {
                        if (c.moveToFirst()) previous = c.getString(0);
                    }
                    if (previous == null) {
                        try (Cursor c = db.query("bookings_cache", new String[]{"payload"}, "id = ? AND user_id = ?", new String[]{item.id, uid}, null, null, null)) {
                            if (c.moveToFirst()) previous = ModelJson.decodeBooking(c.getString(0)).status.name();
                        }
                    }
                    if (previous == null) {
                        try (Cursor c = db.query("pending_bookings", new String[]{"payload"}, "id = ? AND user_id = ?", new String[]{item.id, uid}, null, null, null)) {
                            if (c.moveToFirst()) previous = ModelJson.decodeBooking(c.getString(0)).status.name();
                        }
                    }
                    if (previous != null && !previous.equals(item.status.name())) changed.add(item);
                    ContentValues status = new ContentValues(); status.put("key", key); status.put("value", item.status.name());
                    db.replaceOrThrow("session", null, status);
                    item.synced = true;
                    save("bookings_cache", item.id, uid, ModelJson.encode(item), null);
                    db.delete("pending_bookings", "id = ? AND user_id = ?", new String[]{item.id, uid});
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            return changed;
        }, callback);
    }

    public void addChatMessage(ChatMessage message, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            save("chat_history", message.id, message.userId, ModelJson.encode(message), message.createdAt);
            return null;
        }, callback);
    }

    public void getChatHistory(String uid, Callback<List<ChatMessage>> callback) {
        AppExecutors.execute(() -> read("chat_history", "user_id = ?", new String[]{uid},
                "created_at ASC, id ASC", ModelJson::decodeChatMessage), callback);
    }

    public void clearChatHistory(String uid, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            getWritableDatabase().delete("chat_history", "user_id = ?", new String[]{uid});
            return null;
        }, callback);
    }

    private void deleteOwned(String table, String uid, String id, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            getWritableDatabase().delete(table, "user_id = ? AND id = ?", new String[]{uid, id});
            return null;
        }, callback);
    }

    public void putSession(String key, String value, Callback<Void> callback) {
        AppExecutors.execute(() -> {
            ContentValues values = new ContentValues();
            values.put("key", key);
            values.put("value", value);
            getWritableDatabase().replaceOrThrow("session", null, values);
            return null;
        }, callback);
    }

    public void getSession(String key, Callback<String> callback) {
        AppExecutors.execute(() -> {
            try (Cursor cursor = getReadableDatabase().query("session", new String[]{"value"},
                    "key = ?", new String[]{key}, null, null, null)) {
                return cursor.moveToFirst() ? cursor.getString(0) : null;
            }
        }, callback);
    }

    public void clearSession(Callback<Void> callback) {
        AppExecutors.execute(() -> {
            getWritableDatabase().delete("session", null, null);
            return null;
        }, callback);
    }
}
