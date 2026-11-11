package com.example.carworkshopbookingapp.data.local;

import com.example.carworkshopbookingapp.data.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

final class ModelJson {
    private ModelJson() {}

    static String encode(Car value) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", value.id);
        json.put("make", value.make);
        json.put("model", value.model);
        json.put("plate", value.plate);
        json.put("year", value.year);
        return json.toString();
    }

    static Car decodeCar(String payload) throws JSONException {
        JSONObject json = new JSONObject(payload);
        Car value = new Car();
        value.id = json.optString("id", "");
        value.make = json.optString("make", "");
        value.model = json.optString("model", "");
        value.plate = json.optString("plate", "");
        value.year = json.optInt("year");
        return value;
    }

    static String encode(Workshop value) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", value.id);
        json.put("name", value.name);
        json.put("address", value.address);
        json.put("area", value.area);
        json.put("phone", value.phone);
        json.put("description", value.description);
        json.put("imageKey", value.imageKey);
        json.put("rating", value.rating);
        json.put("ratingCount", value.ratingCount);
        json.put("ratingSum", value.ratingSum);
        json.put("openHour", value.openHour);
        json.put("closeHour", value.closeHour);
        json.put("featured", value.featured);
        json.put("closedDays", new JSONArray(value.closedDays == null ? java.util.Collections.emptyList() : value.closedDays));
        json.put("serviceIds", new JSONArray(value.serviceIds == null ? java.util.Collections.emptyList() : value.serviceIds));
        return json.toString();
    }

    static Workshop decodeWorkshop(String payload) throws JSONException {
        JSONObject json = new JSONObject(payload);
        Workshop value = new Workshop();
        value.id = json.optString("id", "");
        value.name = json.optString("name", "");
        value.address = json.optString("address", "");
        value.area = json.optString("area", "");
        value.phone = json.optString("phone", "");
        value.description = json.optString("description", "");
        value.imageKey = json.optString("imageKey", "day");
        value.rating = json.optDouble("rating", 0);
        value.ratingCount = json.optLong("ratingCount");
        value.ratingSum = json.optDouble("ratingSum", 0);
        value.openHour = json.optInt("openHour");
        value.closeHour = json.optInt("closeHour");
        value.featured = json.optBoolean("featured");
        JSONArray closedDays = json.optJSONArray("closedDays");
        if (closedDays != null) for (int i = 0; i < closedDays.length(); i++) value.closedDays.add(closedDays.getInt(i));
        JSONArray serviceIds = json.optJSONArray("serviceIds");
        if (serviceIds != null) for (int i = 0; i < serviceIds.length(); i++) value.serviceIds.add(serviceIds.getString(i));
        return value;
    }

    static String encode(ServiceItem value) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", value.id);
        json.put("name", value.name);
        json.put("description", value.description);
        json.put("category", value.category);
        json.put("basePrice", value.basePrice);
        json.put("durationMin", value.durationMin);
        json.put("keywords", new JSONArray(value.keywords == null ? java.util.Collections.emptyList() : value.keywords));
        return json.toString();
    }

    static ServiceItem decodeServiceItem(String payload) throws JSONException {
        JSONObject json = new JSONObject(payload);
        ServiceItem value = new ServiceItem();
        value.id = json.optString("id", "");
        value.name = json.optString("name", "");
        value.description = json.optString("description", "");
        value.category = json.optString("category", "");
        value.basePrice = json.optDouble("basePrice", 0);
        value.durationMin = json.optInt("durationMin");
        JSONArray keywords = json.optJSONArray("keywords");
        if (keywords != null) for (int i = 0; i < keywords.length(); i++) value.keywords.add(keywords.getString(i));
        return value;
    }

    static String encode(Booking value) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", value.id);
        json.put("userId", value.userId);
        json.put("userName", value.userName);
        json.put("workshopId", value.workshopId);
        json.put("workshopName", value.workshopName);
        json.put("serviceId", value.serviceId);
        json.put("serviceName", value.serviceName);
        json.put("plusApplied",value.plusApplied); json.put("pickupRequested",value.pickupRequested); json.put("prioritySlot",value.prioritySlot); json.put("basePrice",value.basePrice);
        json.put("carId", value.carId);
        json.put("durationMin", value.durationMin);
        json.put("carLabel", value.carLabel);
        json.put("note", value.note);
        json.put("dateTimeMillis", value.dateTimeMillis);
        json.put("createdAt", value.createdAt);
        json.put("price", value.price);
        json.put("commission", value.commission);
        json.put("status", value.status == null ? BookingStatus.PENDING.name() : value.status.name());
        json.put("synced", value.synced);
        json.put("rating", value.rating);
        return json.toString();
    }

    static Booking decodeBooking(String payload) throws JSONException {
        JSONObject json = new JSONObject(payload);
        Booking value = new Booking();
        value.id = json.optString("id", "");
        value.userId = json.optString("userId", "");
        value.userName = json.optString("userName", "");
        value.workshopId = json.optString("workshopId", "");
        value.workshopName = json.optString("workshopName", "");
        value.serviceId = json.optString("serviceId", "");
        value.serviceName = json.optString("serviceName", "");
        value.plusApplied=json.optBoolean("plusApplied"); value.pickupRequested=json.optBoolean("pickupRequested"); value.prioritySlot=json.optBoolean("prioritySlot"); value.basePrice=json.optDouble("basePrice",json.optDouble("price",0));
        value.carId = json.optString("carId", "");
        value.durationMin = json.optInt("durationMin", 60);
        value.carLabel = json.optString("carLabel", "");
        value.note = json.optString("note", "");
        value.dateTimeMillis = json.optLong("dateTimeMillis");
        value.createdAt = json.optLong("createdAt");
        value.price = json.optDouble("price", 0);
        value.commission = json.optDouble("commission", 0);
        value.status = BookingStatus.valueOf(json.optString("status", "PENDING"));
        value.synced = json.optBoolean("synced");
        value.rating = json.optInt("rating");
        return value;
    }

    static String encode(ChatMessage value) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", value.id);
        json.put("userId", value.userId);
        json.put("role", value.role);
        json.put("text", value.text);
        json.put("createdAt", value.createdAt);
        json.put("urgency", value.urgency);
        json.put("serviceIds", new JSONArray(value.serviceIds));
        json.put("symptom", value.symptom);
        json.put("error", value.error);
        json.put("offline", value.offline);
        json.put("retryable", value.retryable);
        return json.toString();
    }

    static ChatMessage decodeChatMessage(String payload) throws JSONException {
        JSONObject json = new JSONObject(payload);
        ChatMessage value = new ChatMessage();
        value.id = json.optString("id", "");
        value.userId = json.optString("userId", "");
        value.role = json.optString("role", "");
        value.text = json.optString("text", "");
        value.createdAt = json.optLong("createdAt");
        value.urgency = json.optString("urgency", "LOW");
        value.symptom = json.optString("symptom", "");
        value.error = json.optString("error", "");
        value.offline = json.optBoolean("offline");
        value.retryable = json.optBoolean("retryable");
        JSONArray ids = json.optJSONArray("serviceIds");
        if (ids != null) for (int n = 0; n < ids.length(); n++) value.serviceIds.add(ids.getString(n));
        return value;
    }
}
