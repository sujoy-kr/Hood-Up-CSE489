package com.example.carworkshopbookingapp.ai;

import android.content.Context;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.local.LocalDb;
import com.example.carworkshopbookingapp.data.model.ChatMessage;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import java.util.*;

/** One active account's controller; application context only, with a detachable UI observer. */
public final class ChatSession {
    private static ChatSession current;
    public static synchronized ChatSession obtain(Context context, String uid) {
        if (current == null || !current.uid.equals(uid)) current = new ChatSession(context, uid);
        return current;
    }
    private final android.content.Context context;
    private final String uid;
    private final LocalDb local;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<ServiceItem> catalogue = new ArrayList<>();
    private Runnable listener;
    private ChatMessage pendingUser, pendingBot;
    private boolean clearFailed;
    public boolean loaded, busy, clearing;
    public String error = "";

    private ChatSession(Context context, String uid) { this.context=context.getApplicationContext(); this.uid = uid; local = LocalDb.getInstance(context.getApplicationContext()); }
    public List<ChatMessage> messages() { return Collections.unmodifiableList(messages); }
    public List<ServiceItem> catalogue() { return Collections.unmodifiableList(catalogue); }
    public boolean canSend() { return loaded && !busy && pendingUser == null && pendingBot == null && !unfinished(); }
    public boolean unfinished() { return !messages.isEmpty() && "user".equals(messages.get(messages.size() - 1).role); }
    public boolean canRetryAnswer() { return loaded && !busy && pendingUser == null && pendingBot == null; }
    public void attach(Runnable listener) { this.listener = listener; notifyUi(); if (!loaded && !busy) load(); else if (loaded && !busy) refreshCatalogue(); }
    public void detach(Runnable listener) { if (this.listener == listener) this.listener = null; }
    private void notifyUi() { if (listener != null) listener.run(); }

    public void load() {
        if (busy || loaded) return;
        busy = true; error = ""; notifyUi();
        local.getChatHistory(uid, new Callback<List<ChatMessage>>() {
            @Override public void onSuccess(List<ChatMessage> history) {
                messages.clear(); messages.addAll(history);
                local.getServices(new Callback<List<ServiceItem>>() {
                    @Override public void onSuccess(List<ServiceItem> services) {
                        catalogue.clear(); catalogue.addAll(services); loaded = true; busy = false; notifyUi();
                    }
                    @Override public void onError(Exception failure) { failedLoad(); }
                });
            }
            @Override public void onError(Exception failure) { failedLoad(); }
        });
    }
    private void failedLoad() { busy = false; error = "Chat history or services couldn't be loaded. Tap Retry."; notifyUi(); }
    private void refreshCatalogue() {
        local.getServices(new Callback<List<ServiceItem>>() {
            @Override public void onSuccess(List<ServiceItem> services) { catalogue.clear(); catalogue.addAll(services); notifyUi(); }
            @Override public void onError(Exception failure) { /* Sending retries the cache read and can use the last loaded catalogue. */ }
        });
    }

    public boolean send(String text) {
        String symptom = text == null ? "" : text.trim();
        if (!canSend() || symptom.isEmpty() || symptom.length() > 2000) return false;
        clearFailed = false;
        ChatMessage user = message("user", symptom); messages.add(user); pendingUser = user;
        busy = true; error = ""; notifyUi(); persistUser(); return true;
    }
    private void persistUser() {
        local.addChatMessage(pendingUser, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { pendingUser = null; request(null); }
            @Override public void onError(Exception failure) {
                busy = false; error = "Your message couldn't be saved. Tap Retry or Clear chat."; notifyUi();
            }
        });
    }
    public void retry() {
        if (busy) return;
        if (clearFailed) { clear(); return; }
        if (!loaded) { load(); return; }
        if (pendingUser != null) { busy = true; error = ""; notifyUi(); persistUser(); return; }
        if (pendingBot != null) { busy = true; error = ""; notifyUi(); persistBot(); return; }
        if (!messages.isEmpty()) {
            ChatMessage last = messages.get(messages.size() - 1);
            if (!"user".equals(last.role) && last.retryable) { busy = true; error = ""; notifyUi(); request(last); }
            // A process may have ended after saving the user turn but before saving a reply.
            else if ("user".equals(last.role)) { busy = true; error = ""; notifyUi(); request(null); }
        }
    }
    private void request(ChatMessage replace) {
        List<ChatMessage> history = new ArrayList<>(messages);
        if (replace != null) history.remove(history.size() - 1);
        String symptoms = GeminiProtocol.symptoms(history);
        local.getServices(new Callback<List<ServiceItem>>() {
            @Override public void onSuccess(List<ServiceItem> services) {
                catalogue.clear(); catalogue.addAll(services);
                List<ServiceItem> snapshot = new ArrayList<>(services);
                if (snapshot.isEmpty()) { fallback(replace, symptoms, snapshot, "The service catalogue is empty. Refresh workshops on Home, then retry."); return; }
                new GeminiClient(context).ask(history, snapshot, new Callback<AdvisorReply>() {
                    @Override public void onSuccess(AdvisorReply reply) { answer(replace, symptoms, reply, ""); }
                    @Override public void onError(Exception failure) { fallback(replace, symptoms, snapshot, failure.getMessage()); }
                });
            }
            @Override public void onError(Exception failure) {
                fallback(replace, symptoms, new ArrayList<>(catalogue), "Services couldn't be refreshed. You can retry.");
            }
        });
    }
    private void fallback(ChatMessage replace, String symptoms, List<ServiceItem> services, String failure) {
        answer(replace, symptoms, FallbackAdvisor.advise(symptoms, services), failure == null ? "The AI couldn't answer. You can retry." : failure);
    }
    private void answer(ChatMessage replace, String symptoms, AdvisorReply reply, String failure) {
        ChatMessage bot = message("model", reply.reply);
        if (replace != null) { bot.id = replace.id; bot.createdAt = replace.createdAt; }
        bot.urgency = reply.urgency.name(); bot.serviceIds = new ArrayList<>(reply.serviceIds);
        bot.symptom = symptoms.length() > 1000 ? symptoms.substring(symptoms.length() - 1000) : symptoms;
        bot.offline = reply.offline; bot.error = failure; bot.retryable = reply.offline;
        if (replace == null) messages.add(bot); else messages.set(messages.size() - 1, bot);
        pendingBot = bot; notifyUi(); persistBot();
    }
    private void persistBot() {
        local.addChatMessage(pendingBot, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) { pendingBot = null; busy = false; error = ""; notifyUi(); }
            @Override public void onError(Exception failure) { busy = false; error = "The reply couldn't be saved. Tap Retry or Clear chat."; notifyUi(); }
        });
    }
    public void clear() {
        if (busy || !loaded) return;
        busy = true; clearing = true; clearFailed = false; error = ""; notifyUi();
        local.clearChatHistory(uid, new Callback<Void>() {
            @Override public void onSuccess(Void ignored) {
                messages.clear(); pendingUser = pendingBot = null; busy = clearing = false; error = ""; notifyUi();
            }
            @Override public void onError(Exception failure) { busy = clearing = false; clearFailed = true; error = "Chat couldn't be cleared. Tap Retry or Clear chat."; notifyUi(); }
        });
    }
    private ChatMessage message(String role, String text) {
        ChatMessage message = new ChatMessage(); message.id = UUID.randomUUID().toString();
        message.userId = uid; message.role = role; message.text = text;
        message.createdAt = Math.max(System.currentTimeMillis(), messages.isEmpty() ? 0 : messages.get(messages.size() - 1).createdAt + 1);
        return message;
    }
}
