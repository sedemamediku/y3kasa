package com.example.Kasa;

import android.app.Activity;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;

public class AudioPlayer {
    private Activity activity;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable playRunnable;
    public AudioPlayer(Activity activity) {
        this.activity = activity;
    }

    /**
     * Starts playback of the audio file specified by audioFilePath.
     * @param audioFilePath the full path to the audio file.
     * @param listener a callback listener invoked when playback completes.
     */
    public void startPlaying(String audioFilePath, final PlaybackListener listener) {
        // 1) stop any in-flight playback
        stopPlaying();

        // 2) cancel any pending start
        if (playRunnable != null) {
            handler.removeCallbacks(playRunnable);
        }

        // 3) schedule the new play after 500ms
        playRunnable = new Runnable() {
            @Override
            public void run() {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioFilePath);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    isPlaying = true;

                    mediaPlayer.setOnCompletionListener(mp -> {
                        isPlaying = false;
                        if (listener != null) listener.onPlaybackComplete();
                    });
                } catch (IOException e) {
                    Log.e("AudioPlayer", "playback failed: " + e.getMessage());
                }
            }
        };
        handler.postDelayed(playRunnable, 500);
    }

    public void stopPlaying() {
        // 1) cancel any pending start
        if (playRunnable != null) {
            handler.removeCallbacks(playRunnable);
            playRunnable = null;
        }
        // 2) stop currently playing
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException e) {
                Log.e("AudioPlayer", "stop failed: " + e.getMessage());
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }


    public boolean isPlaying() {
        return isPlaying;
    }

    // Simple callback interface for playback completion.
    public interface PlaybackListener {
        void onPlaybackComplete();
    }
}
