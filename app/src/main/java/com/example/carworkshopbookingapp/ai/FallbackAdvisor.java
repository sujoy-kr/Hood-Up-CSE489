package com.example.carworkshopbookingapp.ai;

import com.example.carworkshopbookingapp.data.model.ServiceItem;
import java.util.*;

public final class FallbackAdvisor {
    private FallbackAdvisor() {}
    private static String normalize(String text) {
        String[] tokens = (text == null ? "" : text.toLowerCase(Locale.ROOT)).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) switch (tokens[i]) {
            case "braking": case "brakes": tokens[i] = "brake"; break;
            case "starting": case "started": case "starts": tokens[i] = "start"; break;
            case "pulls": case "pulling": tokens[i] = "pull"; break;
            case "tires": case "tire": case "tyres": tokens[i] = "tyre"; break;
            case "diagnostics": tokens[i] = "diagnostic"; break;
            default: break;
        }
        StringBuilder result = new StringBuilder(" ");
        for (String token : tokens) result.append(token).append(' ');
        return result.toString();
    }
    public static AdvisorReply advise(String symptoms, List<ServiceItem> catalogue) {
        String text = normalize(symptoms);
        Map<String, Integer> scores = new HashMap<>();
        for (ServiceItem service : catalogue) {
            if (service.id == null || service.id.isEmpty() || service.keywords == null) continue;
            Set<String> matched = new HashSet<>();
            for (String keyword : service.keywords) {
                String normalized = normalize(keyword);
                if (!normalized.trim().isEmpty() && text.contains(normalized)) matched.add(normalized);
            }
            if (!matched.isEmpty()) scores.put(service.id, matched.size());
        }
        List<String> ids = new ArrayList<>(scores.keySet());
        ids.sort(Comparator.<String>comparingInt(id -> -scores.get(id)).thenComparing(id -> id));
        if (ids.size() > 3) ids = new ArrayList<>(ids.subList(0, 3));
        String reply = ids.isEmpty() ? "I don't have a clear service match yet. Can you describe when the problem happens?" :
                "These catalogue services may help a workshop assess the symptoms. This is a suggestion, not a diagnosis.";
        return ReplySafety.enforce(new AdvisorReply(reply, ids.isEmpty() ? AdvisorReply.Urgency.LOW : AdvisorReply.Urgency.MEDIUM,
                ids, true), symptoms);
    }
}
