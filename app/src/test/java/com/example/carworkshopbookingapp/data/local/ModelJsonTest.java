package com.example.carworkshopbookingapp.data.local;

import com.example.carworkshopbookingapp.data.model.*;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ModelJsonTest {
    @Test public void workshopPhotoAndAllDayHoursSurviveCacheRoundTrip() throws Exception {
        Workshop shop = new Workshop(); shop.id = "all-day"; shop.imageKey = "night";
        shop.openHour = shop.closeHour = 0; shop.serviceIds = Arrays.asList("oil", "ac");
        Workshop cached = ModelJson.decodeWorkshop(ModelJson.encode(shop));
        assertEquals("night", cached.imageKey);
        assertEquals(0, cached.openHour); assertEquals(0, cached.closeHour);
        assertEquals(shop.serviceIds, cached.serviceIds);
        assertEquals("day", ModelJson.decodeWorkshop("{}").imageKey);
    }
    @Test public void historicalBookingAmountsAndFlagsAreNotRewritten() throws Exception {
        Booking old = ModelJson.decodeBooking("{\"id\":\"legacy\",\"price\":950,\"commission\":95,\"basePrice\":1000,\"plusApplied\":true,\"pickupRequested\":true,\"prioritySlot\":true,\"status\":\"CONFIRMED\"}");
        Booking cached = ModelJson.decodeBooking(ModelJson.encode(new Booking(old)));
        assertEquals(950, cached.price, 0.000001);
        assertEquals(95, cached.commission, 0.000001);
        assertEquals(1000, cached.basePrice, 0.000001);
        assertTrue(cached.plusApplied); assertTrue(cached.pickupRequested); assertTrue(cached.prioritySlot);
        assertEquals(BookingStatus.CONFIRMED, cached.status);
    }
}
