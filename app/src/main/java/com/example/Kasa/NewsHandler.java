package com.example.Kasa;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsHandler {
    private static final String TAG        = "NewsHandler";
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    static final String OPENAI_KEY = com.example.Kasa.BuildConfig.OPENAI_API_KEY;
    private static final MediaType JSON     = MediaType.get("application/json; charset=utf-8");

    /** Called when actionClass == "C" */
    public static void handleNews(@NonNull Activity activity, @NonNull String topic) {


        JSONObject aiReq = new JSONObject();
        try {
            aiReq
                    .put("model", "gpt-4o-search-preview")
                    .put("web_search_options", new JSONObject())
                    .put("messages", new JSONArray()
                            // 1a) System prompt: only use web search and admit ignorance
                            .put(new JSONObject()
                                    .put("role", "system")
                                    .put("content",
                                            "You are a concise and obedient Ghanaian news reporter fluent in Twi. " +
                                                    "Search for as much news as possible. You MUST only use facts from your web search results. " +
                                                    "If there are no relevant results, reply exactly “Meda wo akyi, mennhu bi.” " +
                                                    "Do not repeat lines or invent information."))
                            // 1b) User prompt
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content",
                                            "Search the web for the latest news about \"" + topic + "\". " +
                                                    "Write a short two-sentence summary of the results in Twi. "
                                    ))
                    );

        } catch (JSONException e) {
            Toast.makeText(activity, "Error building AI request", Toast.LENGTH_SHORT).show();
            return;
        }

        Request openAiRequest = new Request.Builder()
                .url(OPENAI_URL)
                .post(RequestBody.create(JSON, aiReq.toString()))
                .addHeader("Content-Type",  "application/json")
                .addHeader("Authorization", "Bearer " + OPENAI_KEY)
                .build();

        OkHttpClient client =  OkHttpSingleton.client()
                .newBuilder()
                .callTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        client.newCall(openAiRequest).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OpenAI request failed", e);
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, "Failed to fetch news", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "OpenAI error: " + response.code() + " " + response.message());
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "AI returned error", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String aiBody = response.body().string();
                String summary;
                try {
                    JSONObject root = new JSONObject(aiBody);
                    summary = root
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                } catch (Exception ex) {
                    Log.e(TAG, "Parsing AI response", ex);
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "Invalid AI response", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // 1) Truncate at "end:" if present
                String cleanSummary = summary;
                int idx = cleanSummary.toLowerCase().indexOf("end:");
                if (idx != -1) {
                    cleanSummary = cleanSummary.substring(0, idx).trim();
                }

                // 2) Delegate all further cleaning to TextCleaner
                String finalCleanSummary = cleanSummary;
                TextCleaner.cleanText(activity, cleanSummary, new TextCleaner.CleanCallback() {
                    @Override
                    public void onSuccess(String cleanedText) {
                        activity.runOnUiThread(() -> {
                            CardView card = activity.findViewById(R.id.notificationCard);
                            TextView tv  = activity.findViewById(R.id.notificationText);

                            // pause wake‐word if needed
                            if (activity instanceof MascotActivity) {
                                ((MascotActivity)activity).pauseWakeWord();
                            }

                            // kick off TTS
                            TextToSpeech.synthesizeAndPlay(
                                    activity,
                                    cleanedText,
                                    () -> {
                                        // resume wake‐word
                                        if (activity instanceof MascotActivity) {
                                            ((MascotActivity)activity).resumeWakeWord();
                                        }
                                        // hide card after delay
                                        tv.postDelayed(() ->
                                                        card.animate()
                                                                .translationY(-card.getHeight())
                                                                .setDuration(300)
                                                                .withEndAction(() -> card.setVisibility(View.GONE))
                                                                .start(),
                                                1000
                                        );
                                    }
                            );

                            // show the card
                            tv.setText(Html.fromHtml("<p>" + cleanedText + "</p>", Html.FROM_HTML_MODE_LEGACY));
                            tv.setMovementMethod(LinkMovementMethod.getInstance());
                            card.setVisibility(View.VISIBLE);
                            card.animate().translationY(0f).setDuration(300).start();
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // fallback to un-cleaned summary
                        Log.w(TAG, "TextCleaner failed: " + errorMessage);
                        activity.runOnUiThread(() -> {
                            CardView card = activity.findViewById(R.id.notificationCard);
                            TextView tv  = activity.findViewById(R.id.notificationText);


                            String html = "<p>" + finalCleanSummary + "</p>";
                            tv.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
                            tv.setMovementMethod(LinkMovementMethod.getInstance());

                            card.setVisibility(View.VISIBLE);
                            card.animate()
                                    .translationY(0f)
                                    .setDuration(300)
                                    .start();
                        });
                    }
                });
            }

        });
    }
}
