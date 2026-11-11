package com.example.carworkshopbookingapp.data;

import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.data.model.Workshop;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorkshopBrowserTest {
    @Test public void openingHoursIncludeOpeningAndExcludeClosingIncludingOvernight() {
        Workshop day = workshop("Day", false, 4.5, 9, 20);
        assertFalse(WorkshopBrowser.isOpen(day, 8 * 60 + 59));
        assertTrue(WorkshopBrowser.isOpen(day, 9 * 60));
        assertFalse(WorkshopBrowser.isOpen(day, 20 * 60));
        Workshop night = workshop("Night", false, 4.5, 22, 6);
        assertTrue(WorkshopBrowser.isOpen(night, 23 * 60));
        assertTrue(WorkshopBrowser.isOpen(night, 5 * 60 + 59));
        assertFalse(WorkshopBrowser.isOpen(night, 6 * 60));
        assertFalse(WorkshopBrowser.isOpen(night, 12 * 60));
    }

    @Test public void searchAndFiltersSortByRatingWithoutFeaturedPriority() {
        Workshop featured = workshop("Banani Auto", true, 4.5, 9, 20);
        Workshop highlyRated = workshop("Banani Motors", false, 4.9, 9, 20);
        Workshop lowRated = workshop("Banani Wheels", false, 4.4, 9, 20);
        List<Workshop> input = Arrays.asList(highlyRated, lowRated, featured);
        List<Workshop> result = WorkshopBrowser.filter(input, Collections.emptyMap(), "  BANANI  ",
                WorkshopBrowser.Filter.TOP_RATED, 12 * 60);
        assertEquals(Arrays.asList(highlyRated, featured), result);
        assertTrue(WorkshopBrowser.filter(input, Collections.emptyMap(), "banani",
                WorkshopBrowser.Filter.OPEN_NOW, 21 * 60).isEmpty());
        assertSame(highlyRated, input.get(0));
    }

    @Test public void serviceSearchAndStartingPriceUseOnlyOfferedServices() {
        Workshop workshop = workshop("Gulshan Auto", false, 4.8, 9, 20);
        workshop.serviceIds = Arrays.asList("oil", "missing");
        ServiceItem oil = new ServiceItem(); oil.name = "Engine Oil Change"; oil.basePrice = 3500;
        ServiceItem wash = new ServiceItem(); wash.name = "Car Wash"; wash.basePrice = 1000;
        Map<String, ServiceItem> services = new HashMap<>();
        services.put("oil", oil); services.put("wash", wash);
        assertEquals(3500, WorkshopBrowser.startingPrice(workshop, services), 0.001);
        assertEquals(1, WorkshopBrowser.filter(Collections.singletonList(workshop), services, "engine oil",
                WorkshopBrowser.Filter.ALL, 12 * 60).size());
        assertTrue(WorkshopBrowser.filter(Collections.singletonList(workshop), services, "car wash",
                WorkshopBrowser.Filter.ALL, 12 * 60).isEmpty());
        assertTrue(Double.isInfinite(WorkshopBrowser.startingPrice(workshop, Collections.emptyMap())));
    }

    private Workshop workshop(String name, boolean featured, double rating, int open, int close) {
        Workshop workshop = new Workshop(); workshop.name = name; workshop.featured = featured;
        workshop.rating = rating; workshop.openHour = open; workshop.closeHour = close;
        return workshop;
    }
    @Test public void allDayBadgeRespectsClosedOpeningDays() {
        Workshop shop = workshop("All day", false, 4.5, 0, 0);
        assertTrue(WorkshopBrowser.isOpen(shop, 0, java.util.Calendar.MONDAY));
        assertTrue(WorkshopBrowser.isOpen(shop, 23 * 60 + 59, java.util.Calendar.MONDAY));
        shop.closedDays = Collections.singletonList(java.util.Calendar.MONDAY);
        assertFalse(WorkshopBrowser.isOpen(shop, 12 * 60, java.util.Calendar.MONDAY));
        assertTrue(WorkshopBrowser.isOpen(shop, 12 * 60, java.util.Calendar.TUESDAY));
    }
}
