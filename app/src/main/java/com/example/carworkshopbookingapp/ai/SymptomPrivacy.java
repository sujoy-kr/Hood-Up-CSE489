package com.example.carworkshopbookingapp.ai;

public final class SymptomPrivacy {
    private SymptomPrivacy() {}
    // Only these fixed mechanical concepts cross the network. Unknown free text stays local.
    // Unlike name-pattern redaction, this cannot forward a typed person's name, plate or address.
    public static String forAi(String text) {
        String value = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z ]", " ");
        String[][] concepts = {
            {"brak", "brakes"}, {"squeal", "squealing noise"}, {"grind", "grinding noise"},
            {"noise", "unusual noise"}, {"sound", "unusual noise"}, {"vibrat", "vibration"},
            {"shak", "shaking"}, {"smoke", "smoke"}, {"burn", "burning smell"},
            {"fuel", "fuel smell or fuel issue"}, {"petrol", "fuel smell or fuel issue"},
            {"overheat", "overheating"}, {"hot", "high temperature"}, {"temperature", "high temperature"},
            {"coolant", "coolant issue"}, {"ac", "air conditioning"}, {"air condition", "air conditioning"},
            {"cooling", "cooling issue"}, {"battery", "battery"}, {"start", "starting problem"},
            {"click", "clicking noise"}, {"engine", "engine"}, {"oil", "engine oil"},
            {"leak", "fluid leak"}, {"steer", "steering"}, {"pull", "pulling to one side"},
            {"align", "wheel alignment"}, {"wheel", "wheels"}, {"tyre", "tyres"}, {"tire", "tyres"},
            {"suspension", "suspension"}, {"bump", "bumps or rough ride"}, {"warning", "warning light"},
            {"light", "dashboard light"}, {"diagnos", "diagnostics"}, {"wash", "car washing"},
            {"detail", "detailing"}, {"general", "general maintenance"}, {"maintenance", "maintenance"},
            {"fail", "failure"}, {"not working", "not working"}, {"won t", "not working"},
            {"weak", "weak performance"}, {"slow", "slow performance"}, {"stop", "stopping issue"},
            {"replace", "replacement"}, {"service", "service"}, {"brake pad", "brake pads"},
            {"balance", "balancing"}, {"repair", "repair"}
        };
        java.util.Set<String> found = new java.util.LinkedHashSet<>();
        for (String[] pair : concepts) {
            String pattern = "\\b" + java.util.regex.Pattern.quote(pair[0]) + (pair[0].length()<=3?"\\b":"[a-z]*\\b");
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(value);
            while (matcher.find()) {
                String prefix = value.substring(0, matcher.start());
                boolean absent = java.util.regex.Pattern.compile(
                        "\\b(?:no|without|not)(?:\\s+(?:any|signs|of|longer|currently))*(?:\\s+)$")
                        .matcher(prefix).find();
                // "Not starting" describes a starting problem, rather than its absence.
                if (pair[0].equals("start") || pair[0].equals("not working")) absent = false;
                found.add((absent ? "absence of " : "") + pair[1]);
            }
        }
        StringBuilder result = new StringBuilder();
        for (String concept : found) { if (result.length() > 0) result.append("; "); result.append(concept); }
        return result.length() == 0 ? "Unspecified vehicle symptom; ask one short mechanical follow-up question." : result.toString();
    }
}
