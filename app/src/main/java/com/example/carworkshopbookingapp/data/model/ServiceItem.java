package com.example.carworkshopbookingapp.data.model;

import java.util.ArrayList;
import java.util.List;

public class ServiceItem {
    public String id;
    public String name;
    public String description;
    public String category;
    public double basePrice;
    public int durationMin;
    public List<String> keywords = new ArrayList<>();

    public ServiceItem() {}
}
