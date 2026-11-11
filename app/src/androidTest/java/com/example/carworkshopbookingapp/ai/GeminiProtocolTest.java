package com.example.carworkshopbookingapp.ai;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.carworkshopbookingapp.data.model.ChatMessage;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeminiProtocolTest {
    private List<ServiceItem> catalogue() {
        ServiceItem service = new ServiceItem(); service.id = "brakes"; service.name = "Brake Pad Replacement";
        service.category = "Brakes"; service.basePrice = 12345; service.description = "private description";
        return Collections.singletonList(service);
    }
    @Test public void validOutputIsWhitelistedAndDangerCannotBeDowngraded() throws Exception {
        AdvisorReply reply = GeminiProtocol.parseReply("{\"reply\":\"A brake inspection may help.\",\"urgency\":\"LOW\",\"serviceIds\":[\"brakes\",\"brakes\"]}", catalogue(), "brakes failed");
        assertEquals(Collections.singletonList("brakes"), reply.serviceIds);
        assertEquals(AdvisorReply.Urgency.HIGH, reply.urgency); assertTrue(reply.reply.startsWith("Stop driving"));
    }
    @Test public void malformedUnknownAndPricedRepliesAreRejected() {
        for (String text : new String[]{"not JSON", "{", "{}", "{\"reply\":4,\"urgency\":\"LOW\",\"serviceIds\":[]}",
                "{\"reply\":\"Try this\",\"urgency\":\"LOW\",\"serviceIds\":[\"invented\"]}",
                "{\"reply\":\"That costs 500 taka\",\"urgency\":\"LOW\",\"serviceIds\":[]}",
                "{\"reply\":\"Try this\",\"urgency\":\"UNKNOWN\",\"serviceIds\":[]}"}) {
            assertThrows(JSONException.class, () -> GeminiProtocol.parseReply(text, catalogue(), "braking noise"));
        }
        assertThrows(JSONException.class, () -> GeminiProtocol.parse("{\"candidates\":[]}", catalogue(), "noise"));
        assertThrows(JSONException.class, () -> GeminiProtocol.parse("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}", catalogue(), "noise"));
    }
    @Test public void requestUsesRecentHistoryAndOnlyAllowedCatalogueFields() throws Exception {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            ChatMessage message = new ChatMessage(); message.userId = "private-account-uid";
            message.role = i % 2 == 0 ? "user" : "model"; message.text = "turn-" + i;
            history.add(message);
        }
        history.get(10).text = "Contact person@example.com or +8801712345678; brakes squeal";
        JSONObject body = GeminiProtocol.request(history, catalogue()); String wire = body.toString();
        assertTrue(body.getJSONArray("contents").length() <= 8);
        assertEquals("user", body.getJSONArray("contents").getJSONObject(0).getString("role"));
        assertFalse(wire.contains("private-account-uid")); assertFalse(wire.contains("person@example.com"));
        assertFalse(wire.contains("8801712345678")); assertFalse(wire.contains("12345"));
        assertFalse(wire.contains("private description"));
        assertEquals("application/json", body.getJSONObject("generationConfig").getString("responseMimeType"));
        assertTrue(body.getJSONObject("generationConfig").has("responseSchema"));
    }
}
