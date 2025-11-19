package com.example.Kasa;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * TextCleaner: calls OpenAI GPT-4o to clean up raw summaries by removing links,
 * repetitions, and foreign characters.
 */
public class TextCleaner {
    private static final String TAG       = "TextCleaner";
    private static final String API_URL   = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY   = NewsHandler.OPENAI_KEY; // reuse same key
    private static final String MODEL     = "gpt-4o";
    private static final MediaType JSON   = MediaType.get("application/json; charset=utf-8");

    /**
     * Callback for cleaned text result.
     */
    public interface CleanCallback {
        /**
         * Called when cleaning succeeds.
         * @param cleanedText the cleaned summary without links, duplicates, or foreign chars.
         */
        void onSuccess(@NonNull String cleanedText);

        /**
         * Called when an error occurs.
         * @param errorMessage description of the failure.
         */
        void onError(@NonNull String errorMessage);
    }

    /**
     * Cleans the provided text by calling GPT-4o.
     * @param activity for Toast and threading
     * @param rawText the original summary text
     * @param cb callback to receive cleaned text or errors
     */
    public static void cleanText(@NonNull Activity activity,
                                 @NonNull String rawText,
                                 @NonNull CleanCallback cb) {
        try {
            // Build messages array
            JSONArray messages = new JSONArray();
            // System prompt
            JSONObject system = new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "You are a helpful Twi text cleaner. " +
                                    "Remove all URLs, duplicate sentences or phrases, and any non-standard or foreign characters. " +
                                    "Return only the cleaned text.");
            messages.put(system);
            // User prompt + raw text
            JSONObject user = new JSONObject()
                    .put("role", "user")
                    .put("content", "Clean the following text:\n" + rawText);
            messages.put(user);

            // Build full request JSON
            JSONObject payload = new JSONObject()
                    .put("model", MODEL)
                    .put("temperature", 0)
                    .put("messages", messages);

            RequestBody body = RequestBody.create(JSON, payload.toString());
            OkHttpClient client =  OkHttpSingleton.client()
                    .newBuilder()
                    .callTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "OpenAI request failed", e);
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Failed to clean text", Toast.LENGTH_SHORT).show();
                        cb.onError(e.getMessage());
                    });
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String err = "HTTP " + response.code() + " " + response.message();
                        Log.e(TAG, err);
                        activity.runOnUiThread(() -> cb.onError(err));
                        return;
                    }

                    String raw = response.body().string();
                    try {
                        JSONObject root = new JSONObject(raw);
                        JSONArray choices = root.getJSONArray("choices");
                        if (choices.length() == 0) throw new JSONException("no choices");
                        String content = choices
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                                .trim();

                        final String cleaned = content;
                        activity.runOnUiThread(() -> cb.onSuccess(cleaned));

                    } catch (JSONException je) {
                        Log.e(TAG, "Parsing response", je);
                        activity.runOnUiThread(() -> cb.onError("Parse error: " + je.getMessage()));
                    }
                }
            });

        } catch (JSONException je) {
            Log.e(TAG, "Building request JSON", je);
            activity.runOnUiThread(() -> cb.onError("Build JSON error: " + je.getMessage()));
        }
    }
}
