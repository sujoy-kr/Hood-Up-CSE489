package com.example.carworkshopbookingapp.ai;

import java.util.Locale;

public final class ReplySafety {
    private ReplySafety() {}
    public static boolean dangerous(String symptom) {
        String text = symptom == null ? "" : symptom.toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("[^\\p{L}\\p{N}']+", " ");
        for (String negation : new String[]{"no smoke", "not smoking", "not overheating", "no overheating", "no fuel smell", "no smell of fuel"}) {
            text = text.replace(negation, "");
        }
        for (String danger : new String[]{"brake failure", "brakes failed", "brake failed", "brakes not working", "brakes don't work",
                "brakes aren't working", "brakes stopped working", "no brakes", "cannot stop", "can't stop", "pedal goes to the floor", "pedal goes to floor",
                "brake pedal sinks", "smoke", "smoking", "fuel smell", "smell of fuel", "smell fuel", "petrol smell",
                "smell petrol", "smell of petrol", "smell of gasoline", "fuel leak", "petrol leak", "gasoline smell", "overheat", "engine on fire"}) if (text.contains(danger)) return true;
        return false;
    }

    public static AdvisorReply enforce(AdvisorReply reply, String symptoms) {
        boolean danger = dangerous(symptoms) || reply.urgency == AdvisorReply.Urgency.HIGH;
        String prefix = danger ? "Stop driving as soon as it is safe and arrange roadside help. " : "";
        String inspection = " A physical inspection is needed to confirm the cause.";
        String text = prefix + words(reply.reply, danger ? 44 : 58) + inspection;
        return new AdvisorReply(words(text, 70), danger ? AdvisorReply.Urgency.HIGH : reply.urgency, reply.serviceIds, reply.offline);
    }
    private static String words(String text, int maximum) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= maximum) return text.trim();
        StringBuilder shortened = new StringBuilder();
        for (int i = 0; i < maximum; i++) {
            if (i > 0) shortened.append(' ');
            shortened.append(words[i]);
        }
        return shortened.append('…').toString();
    }
}
