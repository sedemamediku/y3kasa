package com.example.Kasa;

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TextToSpeech {
    private static final String TAG     = "TextToSpeech";
    private static final String TTS_URL = "https://translation-api.ghananlp.org/tts/v1/synthesize";
    private static final String TTS_KEY = com.example.Kasa.BuildConfig.GHANA_NLP_API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static void synthesizeAndPlay(
            Activity activity,
            String text,
            AudioPlayer.PlaybackListener listener
    ) {
        JSONObject ttsReq = new JSONObject();
        try {
            ttsReq.put("text", text)
                    .put("language", "tw")
                    .put("speaker_id", "twi_speaker_7");
        } catch (JSONException ex) {
            Log.e(TAG, "Building TTS JSON", ex);
            return;
        }

        Request req = new Request.Builder()
                .url(TTS_URL)
                .post(RequestBody.create(JSON, ttsReq.toString()))
                .addHeader("Content-Type","application/json")
                .addHeader("Ocp-Apim-Subscription-Key", TTS_KEY)
                .build();

        // give the TTS request longer timeouts
        OkHttpClient client =  OkHttpSingleton.client()
                .newBuilder()
                .callTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "TTS request failed", e);
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful()) {
                    Log.e(TAG, "TTS error: " + resp.message());
                    return;
                }
                byte[] audioBytes = resp.body().bytes();
                File out = new File(activity.getCacheDir(), "tts.mp3");
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(audioBytes);
                } catch (IOException ioEx) {
                    Log.e(TAG, "Writing TTS file failed", ioEx);
                    return;
                }

                activity.runOnUiThread(() ->
                        new AudioPlayer(activity)
                                .startPlaying(out.getAbsolutePath(), listener)
                );
            }
        });
    }
}
