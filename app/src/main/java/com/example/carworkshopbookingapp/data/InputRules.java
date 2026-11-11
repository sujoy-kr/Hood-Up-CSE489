package com.example.carworkshopbookingapp.data;

public final class InputRules {
    private InputRules() {}
    public static boolean phone(String value) {
        if (value == null) return false;
        String phone = value.trim();
        String digits = phone.replaceAll("[^0-9]", "");
        return phone.matches("\\+?[0-9 ()-]+") && digits.length() >= 7 && digits.length() <= 15;
    }
    public static boolean name(String value) {
        return value != null && value.trim().length() >= 2 && value.trim().length() <= 100;
    }
}
