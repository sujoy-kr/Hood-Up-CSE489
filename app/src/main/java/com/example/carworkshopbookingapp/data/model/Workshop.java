package com.example.carworkshopbookingapp.data.model;

import java.util.ArrayList;
import java.util.List;

public class Workshop {
    public String id;
    public String name;
    public String address;
    public String area;
    public String phone;
    public String description;
    public String imageKey = "day";
    public double rating;
    public long ratingCount;
    public double ratingSum;
    public int openHour;
    public int closeHour;
    public boolean featured;
    // Calendar day constants: Sunday=1 through Saturday=7. Empty means open every day.
    public List<Integer> closedDays = new ArrayList<>();
    public List<String> serviceIds = new ArrayList<>();

    public Workshop() {}
}
