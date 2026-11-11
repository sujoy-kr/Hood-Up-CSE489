package com.example.carworkshopbookingapp.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class SymptomPrivacyTest {
    @Test public void arbitraryIdentifiersNeverLeaveThroughConceptExtraction(){
        String wire=SymptomPrivacy.forAi("I am Ayesha Rahman, email ayesha@example.test, phone +8801712345678, plate DHAKA1234, at 17 Banani Road. Brakes squeal.");
        assertTrue(wire.contains("brakes"));assertTrue(wire.contains("squealing"));
        for(String pii:new String[]{"Ayesha","Rahman","ayesha","example","8801712345678","DHAKA","Banani","Road","1234"})assertFalse(wire.contains(pii));
    }
    @Test public void unknownOrOnlyPersonalInputBecomesFixedFollowUp(){
        assertEquals(SymptomPrivacy.forAi(""),SymptomPrivacy.forAi("My name is Ayesha Rahman; 01712345678"));
    }
    @Test public void absentSymptomsRemainDistinctFromReportedSymptoms() {
        assertEquals("smoke", SymptomPrivacy.forAi("smoke"));
        assertEquals("absence of smoke", SymptomPrivacy.forAi("no smoke"));
        String wire = SymptomPrivacy.forAi("No smoke, no fuel smell, not overheating");
        assertTrue(wire.contains("absence of smoke"));
        assertTrue(wire.contains("absence of fuel smell or fuel issue"));
        assertTrue(wire.contains("absence of overheating"));
        assertFalse(wire.contains("not working"));
    }
    @Test public void startingFailureRemainsAProblem() {
        assertTrue(SymptomPrivacy.forAi("Engine not starting").contains("starting problem"));
        assertFalse(SymptomPrivacy.forAi("Engine not starting").contains("absence of starting"));
    }
}
