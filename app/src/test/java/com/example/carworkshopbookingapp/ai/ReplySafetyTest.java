package com.example.carworkshopbookingapp.ai;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplySafetyTest {
    @Test public void dangerousSymptomsOverrideLowUrgencyEvenWithoutSuggestions() {
        for (String symptom : new String[]{"brake failure", "smoke from the bonnet", "fuel smell", "engine overheating", "brakes stopped working", "smell of petrol"}) {
            AdvisorReply reply = FallbackAdvisor.advise(symptom, Collections.emptyList());
            assertEquals(AdvisorReply.Urgency.HIGH, reply.urgency);
            assertTrue(reply.reply.startsWith("Stop driving"));
        }
        assertFalse(ReplySafety.dangerous("No smoke and not overheating; AC is weak"));
    }
    @Test public void safetyWarningAndInspectionSurviveWordLimit() {
        String longText = String.join(" ", Collections.nCopies(150, "perhaps"));
        AdvisorReply reply = ReplySafety.enforce(new AdvisorReply(longText, AdvisorReply.Urgency.LOW, Collections.emptyList(), false), "brakes failed");
        assertTrue(reply.reply.split("\\s+").length <= 70);
        assertTrue(reply.reply.startsWith("Stop driving")); assertTrue(reply.reply.contains("physical inspection"));
        assertEquals(AdvisorReply.Urgency.HIGH, reply.urgency);
    }
}
