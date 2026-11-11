package com.example.carworkshopbookingapp.ai;


import com.example.carworkshopbookingapp.data.model.ChatMessage;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import org.json.*;
import org.junit.Test;

import java.util.*;
import static org.junit.Assert.*;


public class GeminiRequestSecurityTest {
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
    @Test public void aliasesHideRawCatalogIdentifiersAndMapBackLocally() throws Exception {
        List<ServiceItem> services=catalogue();services.get(0).id="private-contact-id";
        services.get(0).name="Brake Pad Replacement for Ayesha Rahman";
        services.get(0).category="Contact person@example.test +8801712345678";
        ChatMessage message=new ChatMessage();message.role="user";message.text="Ayesha Rahman, 17 Banani Road, DHAKA1234: braking noise";
        String wire=GeminiProtocol.request(Collections.singletonList(message),services).toString();
        for(String pii:new String[]{"private-contact-id","Ayesha","Rahman","Banani","DHAKA1234","person@example.test","8801712345678"})assertFalse(wire.contains(pii));
        assertTrue(wire.contains("service_0"));
        JSONObject reply=new JSONObject().put("reply","A brake inspection may help.").put("urgency","LOW").put("serviceIds",new JSONArray().put("service_0"));
        JSONObject envelope=new JSONObject().put("candidates",new JSONArray().put(new JSONObject().put("finishReason","STOP").put("content",new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",reply.toString()))))));
        assertEquals(Collections.singletonList("private-contact-id"),GeminiProtocol.parse(envelope.toString(),services,"braking noise").serviceIds);
    }
    @Test public void providerEnvelopeSkipsThoughtPartsAndJoinsVisibleJson() throws Exception {
        JSONObject payload=new JSONObject().put("reply","A brake inspection may help.")
                .put("urgency","LOW").put("serviceIds",new JSONArray().put("service_0"));
        String json=payload.toString();int split=json.length()/2;
        JSONArray parts=new JSONArray()
                .put(new JSONObject().put("thought",true).put("text","untrusted internal text"))
                .put(new JSONObject().put("text",json.substring(0,split)))
                .put(new JSONObject().put("text",json.substring(split)));
        JSONObject envelope=new JSONObject().put("candidates",new JSONArray().put(new JSONObject()
                .put("finishReason","STOP").put("content",new JSONObject().put("parts",parts))));
        AdvisorReply reply=GeminiProtocol.parse(envelope.toString(),catalogue(),"braking noise");
        assertEquals(Collections.singletonList("brakes"),reply.serviceIds);
        assertFalse(reply.reply.contains("untrusted"));
    }
    @Test public void wrongServiceIdTypesAndOversizedListsFailClosed() throws Exception {
        JSONArray tooMany=new JSONArray();for(int i=0;i<4;i++)tooMany.put("brakes");
        for(JSONArray ids:Arrays.asList(new JSONArray().put(1),new JSONArray().put(JSONObject.NULL),tooMany)){
            String reply=new JSONObject().put("reply","A brake inspection may help.")
                    .put("urgency","LOW").put("serviceIds",ids).toString();
            assertThrows(JSONException.class,()->GeminiProtocol.parseReply(reply,catalogue(),"noise"));
        }
        String missingParts=new JSONObject().put("candidates",new JSONArray().put(new JSONObject()
                .put("finishReason","STOP").put("content",new JSONObject()))).toString();
        assertThrows(JSONException.class,()->GeminiProtocol.parse(missingParts,catalogue(),"noise"));
    }
}
