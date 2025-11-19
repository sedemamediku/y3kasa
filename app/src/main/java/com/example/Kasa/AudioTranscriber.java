package com.example.Kasa;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioTranscriber {

    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String errorMessage);
    }

    private static final String TAG = "AudioTranscriber";

    public static void transcribeAudio(String audioFilePath, final TranscriptionCallback callback) {
        File file = new File(audioFilePath);
        if (!file.exists()) {
            callback.onError("Audio file not found");
            return;
        }
        try {
            byte[] audioBytes = readFileToByteArray(file);
            MediaType mediaType = MediaType.parse("audio/mpeg");
            RequestBody body = RequestBody.create(mediaType, audioBytes);

            OkHttpClient client =  OkHttpSingleton.client();
            Request request = new Request.Builder()
                    .url("https://translation-api.ghananlp.org/asr/v1/transcribe?language=tw")
                    .post(body)
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Ocp-Apim-Subscription-Key", com.example.Kasa.BuildConfig.GHANA_NLP_API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Transcription call failed: " + e.getMessage());
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(response.isSuccessful()) {
                        String responseStr = response.body().string();
                        Log.d(TAG, "Transcription response: " + responseStr);
                        callback.onSuccess(responseStr);
                    } else {
                        String errorMsg = "Error: " + response.message();
                        Log.e(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            callback.onError(e.getMessage());
        }
    }

    // Helper method to read a file into a byte array.
    private static byte[] readFileToByteArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] bytes = new byte[(int) file.length()];
            int read = fis.read(bytes);
            if (read != file.length()) {
                throw new IOException("Could not completely read file " + file.getName());
            }
            return bytes;
        } finally {
            fis.close();
        }
    }
}
