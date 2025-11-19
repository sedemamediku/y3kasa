package com.example.Kasa;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TextTranslator {

    // ───────────────────────── callback ─────────────────────────
    public interface TranslationCallback {
        /** translation = full English sentence
         *  actionClass = "A"…"E"
         *  actionContent = contact / app / topic
         *  actionObject  = message body (only class "D", else "null") */
        void onSuccess(String translation,
                       String actionClass,
                       String actionContent,
                       String actionObject);

        void onError(String errorMessage);
    }
    // ────────────────────────────────────────────────────────────

    private static final String TAG = "TextTranslator";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL   = "gpt-4o";
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final String API_KEY = com.example.Kasa.BuildConfig.OPENAI_API_KEY;

    /** Sends the Twi text to OpenAI, expects the single‑JSON answer */
    public static void translateText(String twi, TranslationCallback cb) {

        if (twi == null || twi.trim().isEmpty()) {
            cb.onError("Empty input");
            return;
        }

        String prompt =
                "You are a strict Twi‑to‑English translator and action classifier.\n" +
                        "\n" +
                        "1) Translate the user’s Twi utterance to natural English.\n" +
                        "2) Decide which of the five action classes it belongs to (do not force any translation into a class. if unsure, assign to F):\n" +
                        "     A - phone‑call  (content = contact name)\n" +
//                        "     B - open an app (content = app name)\n" +
                        "     C - news query  (content = news topic)\n" +
                        "     D - text message (content = recipient, object = message)\n" +
                        "     E - nothing / user says 'twa no so' (no recipient)\n" +
                        "     F - failed (content = command not recognised) " +
                        "\n" +
                        "Return ONE JSON object with keys \"translation\", \"actionClass\",\n" +
                        "\"content\", \"object\".  Do NOT wrap it in markdown. The news query should always be translated into a question that captures the whole query\n" +
                        "If the utterence hints towards telling someone something, assign it to the text message class. "+
                        "Each translation should map to one of the actions stated above. if it doesn't then assign it to class F" +
                        "\n" +
                        "### User utterance\n" + twi;

        // Build chat completion payload
        try {
            JSONArray msgs = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role","system");
            sys.put("content",prompt);
            msgs.put(sys);

            JSONObject req = new JSONObject();
            req.put("model", MODEL);
            req.put("messages", msgs);
            req.put("temperature", 0);

            RequestBody body = RequestBody.create(JSON, req.toString());
            OkHttpClient client =  OkHttpSingleton.client();

            Request rq = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Content-Type","application/json")
                    .addHeader("Authorization","Bearer " + API_KEY)
                    .build();

            client.newCall(rq).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG,"HTTP error: "+e.getMessage());
                    cb.onError(e.getMessage());
                }

                @Override public void onResponse(Call call, Response resp) throws IOException {
                    if (!resp.isSuccessful()) {
                        cb.onError("HTTP "+resp.code()+" "+resp.message());
                        return;
                    }
                    String raw = resp.body().string();
                    Log.d(TAG,"GPT‑response: "+raw);

                    try {
                        // Extract the assistant message
                        JSONArray choices = new JSONObject(raw)
                                .getJSONArray("choices");
                        if (choices.length()==0) throw new JSONException("no choice");
                        String content = choices.getJSONObject(0)
                                .getJSONObject("message").getString("content");

                        // Pull out the JSON block
                        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(content);
                        if (!m.find()) throw new JSONException("JSON missing");
                        JSONObject j = new JSONObject(m.group());

                        cb.onSuccess(
                                j.optString("translation",""),
                                j.optString("actionClass",""),
                                j.optString("content",""),
                                j.optString("object","null")
                        );

                    } catch (JSONException je) {
                        cb.onError("Parse error: "+je.getMessage());
                    }
                }
            });

        } catch (JSONException je) {
            cb.onError("Build JSON: "+je.getMessage());
        }
    }
}
