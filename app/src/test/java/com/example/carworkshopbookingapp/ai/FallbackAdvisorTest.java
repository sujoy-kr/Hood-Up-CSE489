package com.example.carworkshopbookingapp.ai;

import com.example.carworkshopbookingapp.data.model.ServiceItem;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class FallbackAdvisorTest {
    @Test public void quickStartsMatchCatalogueKeywordsIncludingWordVariants() {
        ServiceItem brakes = service("brakes", "brake", "squeal");
        ServiceItem ac = service("ac", "ac", "air conditioning");
        ServiceItem battery = service("battery", "starting", "clicking");
        assertEquals(Collections.singletonList("brakes"), FallbackAdvisor.advise("Strange noise when braking", Arrays.asList(brakes, ac, battery)).serviceIds);
        assertEquals(Collections.singletonList("ac"), FallbackAdvisor.advise("AC not cooling", Arrays.asList(brakes, ac, battery)).serviceIds);
        assertEquals(Collections.singletonList("battery"), FallbackAdvisor.advise("Car won't start", Arrays.asList(brakes, ac, battery)).serviceIds);
    }
    @Test public void shortKeywordsMustMatchWholeWordsAndUnknownSymptomsAskFollowUp() {
        AdvisorReply reply = FallbackAdvisor.advise("My black car seems odd", Collections.singletonList(service("ac", "ac")));
        assertTrue(reply.serviceIds.isEmpty()); assertTrue(reply.reply.contains("?")); assertTrue(reply.offline);
        assertTrue(FallbackAdvisor.advise("smoke", Collections.emptyList()).serviceIds.isEmpty());
    }
    @Test public void suggestionsAreLimitedAndCannotInventIds() {
        AdvisorReply reply = FallbackAdvisor.advise("warning", Arrays.asList(service("a", "warning"), service("b", "warning"), service("c", "warning"), service("d", "warning")));
        assertEquals(3, reply.serviceIds.size()); assertEquals(Arrays.asList("a", "b", "c"), reply.serviceIds);
    }
    private ServiceItem service(String id, String... keywords) {
        ServiceItem service = new ServiceItem(); service.id = id; service.keywords = Arrays.asList(keywords); return service;
    }
    @Test public void repeatedKeywordsDoNotOutrankDistinctMatchingSymptoms() {
        ServiceItem repeated=service("repeated","noise","noise","NOISE");
        ServiceItem specific=service("specific","noise","air conditioning");
        AdvisorReply reply=FallbackAdvisor.advise("AIR CONDITIONING noise",Arrays.asList(repeated,specific));
        assertEquals(Arrays.asList("specific","repeated"),reply.serviceIds);
        assertTrue(reply.offline);
    }
}
