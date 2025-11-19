package com.example.Kasa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.IOException;

public class AudioRecorder {

    public static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 200;
    private Context context;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;

    // Handler and Runnable for silence detection.
    private Handler silenceHandler = new Handler(Looper.getMainLooper());
    private Runnable silenceRunnable;
    private int silenceDuration = 0;  // in milliseconds

    // Configuration constants
    private static final int SILENCE_THRESHOLD = 3000;       // Adjust if environment is noisy
    private static final int POLLING_INTERVAL = 250;         // Polling interval in ms
    private static final int MINIMUM_SILENCE_DURATION = 2200;// 2.5 seconds of silence
    private static final int INITIAL_DELAY = 2000;           // Start checking 4 seconds after begin

    // Listener to notify when recording stops (auto‑stop due to silence).
    public interface RecordingListener {
        void onRecordingStoppedDueToSilence();
    }
    private RecordingListener recordingListener;
    public void setRecordingListener(RecordingListener listener) {
        this.recordingListener = listener;
    }

    public AudioRecorder(Context context) {
        this.context = context;
        // Use internal cache directory to avoid external storage issues on Android 13+.
        File cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        // Create a unique file name using current timestamp.
        String filename = "recorded_audio_" + System.currentTimeMillis() + ".mp4";
        File outputFile = new File(cacheDir, filename);
        if (outputFile.exists()) {
            boolean deleted = outputFile.delete();
            Log.d("AudioRecorder", "Existing file deleted: " + deleted);
        }
        audioFilePath = outputFile.getAbsolutePath();
        Log.d("AudioRecorder", "Audio file path: " + audioFilePath);
    }

    // Check if RECORD_AUDIO permission is granted.
    public boolean checkPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the RECORD_AUDIO permission.
     * Note: This only works when the context is an Activity.
     */
    public void requestPermission() {
        if (context instanceof android.app.Activity) {
            ActivityCompat.requestPermissions((android.app.Activity) context,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        } else {
            Log.w("AudioRecorder", "Context is not an Activity; permission must be granted externally.");
        }
    }

    // Start recording audio and begin silence detection after the initial delay.
    public void startRecording() {
        if (!checkPermission()) {
            requestPermission();
            return;
        }
        // Release any previous MediaRecorder instance.
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
        }
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d("AudioRecorder", "Recording started.");

            // Start silence detection after the initial delay.
            silenceDuration = 0;
            silenceRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isRecording || mediaRecorder == null) return;
                    int amplitude = mediaRecorder.getMaxAmplitude();
                    Log.d("AudioRecorder", "Amplitude: " + amplitude);
                    if (amplitude < SILENCE_THRESHOLD) {
                        silenceDuration += POLLING_INTERVAL;
                        Log.d("AudioRecorder", "Silence duration: " + silenceDuration + " ms");
                        if (silenceDuration >= MINIMUM_SILENCE_DURATION) {
                            // Stop recording due to sustained silence.
                            stopRecording();
                            Log.d("AudioRecorder", "Recording stopped due to silence.");
                            return; // Exit without rescheduling the runnable.
                        }
                    } else {
                        // Sound detected; reset silence duration counter.
                        silenceDuration = 0;
                    }
                    // Continue polling.
                    silenceHandler.postDelayed(this, POLLING_INTERVAL);
                }
            };
            silenceHandler.postDelayed(silenceRunnable, INITIAL_DELAY);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("AudioRecorder", "prepare() failed: " + e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e("AudioRecorder", "start() failed: " + e.getMessage());
        }
    }

    // Stop recording audio and cancel silence detection.
    public void stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                e.printStackTrace();
                Log.e("AudioRecorder", "stop() failed: " + e.getMessage());
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                // Cancel the silence detection callbacks.
                if (silenceHandler != null && silenceRunnable != null) {
                    silenceHandler.removeCallbacks(silenceRunnable);
                }
                Log.d("AudioRecorder", "Recording stopped.");
                // Notify the listener (e.g. MainActivity) to update the UI.
                if (recordingListener != null) {
                    recordingListener.onRecordingStoppedDueToSilence();
                }
            }
        }
    }

    // Check if currently recording.
    public boolean isRecording() {
        return isRecording;
    }

    // Get the file path of the recorded audio.
    public String getAudioFilePath() {
        return audioFilePath;
    }
}
