package com.example.carworkshopbookingapp.ai;

import com.example.carworkshopbookingapp.BuildConfig;
import com.example.carworkshopbookingapp.data.Callback;
import com.example.carworkshopbookingapp.data.model.ChatMessage;
import com.example.carworkshopbookingapp.data.model.ServiceItem;
import com.example.carworkshopbookingapp.util.AppExecutors;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public final class GeminiClient {
    private final android.content.Context context;
    public GeminiClient(android.content.Context context){this.context=context.getApplicationContext();}
    public static final String MODEL = "gemini-3.5-flash-lite";
    private static final int MAX_RESPONSE_BYTES = 131072;

    public void ask(List<ChatMessage> history, List<ServiceItem> catalogue, Callback<AdvisorReply> callback) {
        List<ChatMessage> conversation = new ArrayList<>(history);
        List<ServiceItem> services = new ArrayList<>(catalogue);
        AppExecutors.network().execute(() -> {
            try {
                AdvisorReply reply = generate(conversation, services);
                AppExecutors.main(() -> callback.onSuccess(reply));
            } catch (Exception error) {
                Exception friendly = error instanceof GeminiFailure ? error : new GeminiFailure(
                        error instanceof IOException ? "The AI connection timed out or was interrupted. You can retry." :
                        "The AI reply wasn't usable. You can retry for another answer.");
                AppExecutors.main(() -> callback.onError(friendly));
            }
        });
    }

    private AdvisorReply generate(List<ChatMessage> history, List<ServiceItem> services) throws Exception {
        if (BuildConfig.GEMINI_API_KEY.trim().isEmpty()) throw new GeminiFailure("The AI key is not configured. Offline suggestions are available.");
        byte[] payload = GeminiProtocol.request(history, services).toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent").openConnection();
        try {
            connection.setRequestMethod("POST"); connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
            connection.setRequestProperty("X-Android-Package",context.getPackageName());
            connection.setRequestProperty("X-Android-Cert",com.example.carworkshopbookingapp.util.AppIdentity.signingSha1(context));
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream stream = connection.getOutputStream()) { stream.write(payload); }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String body = read(connection.getErrorStream());
                if (status == 404 || status == 410 || unavailable(body)) throw GeminiFailure.unavailable();
                if (status == 429) throw new GeminiFailure("The AI is busy or its request limit was reached. Please try again later.");
                if (status == 401 || status == 403) throw new GeminiFailure("AI access is unavailable. Please check the API key configuration.");
                throw new GeminiFailure("The AI service couldn't answer right now. Please try again later.");
            }
            String body = read(connection.getInputStream());
            return GeminiProtocol.parse(body, services, GeminiProtocol.symptoms(history));
        } finally { connection.disconnect(); }
    }
    private static boolean unavailable(String body) {
        try {
            JSONObject error = new JSONObject(body).optJSONObject("error");
            if (error == null) return false;
            String message = error.optString("message").toLowerCase(Locale.ROOT);
            return "NOT_FOUND".equals(error.optString("status")) || (message.contains("model") &&
                    (message.contains("not found") || message.contains("not supported") || message.contains("no longer available")));
        } catch (Exception ignored) { return false; }
    }
    private static String read(InputStream source) throws IOException {
        if (source == null) return "";
        try (InputStream stream = source; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = stream.read(buffer)) != -1) {
                if (result.size() + count > MAX_RESPONSE_BYTES) throw new IOException("Response exceeds limit");
                result.write(buffer, 0, count);
            }
            return new String(result.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
