package com.example.Kasa;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class WakeWordService extends Service {

    // Action broadcast: sent when wake word is detected.
    public static final String ACTION_WAKEWORD_DETECTED = "com.example.Kasa.ACTION_WAKEWORD_DETECTED";

    private PorcupineManager porcupineManager;
    private final IBinder binder = new LocalBinder();

    // Binder class for MainActivity to interact with the service.
    public class LocalBinder extends Binder {
        public WakeWordService getService() {
            return WakeWordService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPorcupineDetection();
    }

    /**
     * Initialize the PorcupineManager to listen continuously.
     * When the wake word is detected, send a broadcast so MainActivity can start recording.
     */
    private void initPorcupineDetection() {
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(com.example.Kasa.BuildConfig.PORCUPINE_ACCESS_KEY)
                    .setKeywordPath("porcupine/yeah-casa_en_android_v3_0_0.ppn")            // Ensure this file is in assets.
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(), new PorcupineManagerCallback() {
                        @Override
                        public void invoke(int keywordIndex) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // You can keep the toast for debugging or remove it.
//                                Toast.makeText(WakeWordService.this, "Wake word detected", Toast.LENGTH_SHORT).show();
                                // Send the broadcast to signal MainActivity to start recording.
                                Intent intent = new Intent(ACTION_WAKEWORD_DETECTED);
                                sendBroadcast(intent);
                            });
                        }
                    });
            porcupineManager.start();
        } catch (PorcupineException e) {
            e.printStackTrace();
//            Toast.makeText(this, "Failed to initialize wake word detection", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Pauses wake word detection (stops microphone capture).
     */
    public void pauseListening() throws PorcupineException {
        if (porcupineManager != null) {
            porcupineManager.stop();
//            Toast.makeText(this, "Wake word detection paused", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resumes wake word detection.
     */
    public void resumeListening() throws PorcupineException {
        if (porcupineManager != null) {
            porcupineManager.start();
//            Toast.makeText(this, "Wake word detection resumed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Run normally. Since we want the service only to run when the app is open,
        // we return START_NOT_STICKY so that if the app is closed, the service stops.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            porcupineManager.delete();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
