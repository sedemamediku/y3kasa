package com.example.Kasa;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import ai.picovoice.porcupine.PorcupineException;

public class MascotActivity extends AppCompatActivity {
    private static final String TAG = "MascotActivity";

    // UI
    private MaterialCardView notificationCard;
    private TextView  notificationText;
    private TextView  titleText;
    private ImageView mascotView;
    private TextView  transcriptionText;
    private TextView  translationText;
    private ImageView waveformView;

    // Action params
    private String actionClass   = "";
    private String actionContent = "";
    private String actionObject  = "";

    // Audio & animation
    private AudioRecorder  audioRecorder;
    private AudioPlayer    audioPlayer;
    private float          initialX, initialY, centerX, centerY;
    private float          viewWidth, viewHeight;
    private boolean        hasLaidOut = false;
    private ObjectAnimator floatAnimator;

    // Wake-word service binding
    private WakeWordService wakeWordService;
    private boolean         isBound = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            wakeWordService = ((WakeWordService.LocalBinder) service).getService();
            isBound = true;
            Log.d(TAG, "WakeWordService bound.");
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            wakeWordService = null;
        }
    };

    private final BroadcastReceiver wwReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (!WakeWordService.ACTION_WAKEWORD_DETECTED.equals(intent.getAction())) return;
            Log.d(TAG, "Wake word detected.");

            // clear old text
            transcriptionText.setText("");
            translationText.setText("");
            transcriptionText.setVisibility(View.GONE);
            translationText.setVisibility(View.GONE);
            notificationCard.setVisibility(View.GONE);

            // show waveform + center mascot
            waveformView.setVisibility(View.VISIBLE);
            Glide.with(MascotActivity.this)
                    .asGif()
                    .load(R.drawable.waveform)
                    .into(waveformView);
            moveMascotToCenterAndEnlarge();

            // play y3kasa_response, then start recording
            MediaPlayer prompt = MediaPlayer.create(thisActivity(), R.raw.y3kasa_response);
            prompt.setOnCompletionListener(mp -> {
                mp.release();
                audioRecorder.startRecording();
                if (isBound) {
                    try { wakeWordService.pauseListening(); }
                    catch (PorcupineException ignored) {}
                }
            });
            prompt.start();
        }
        private Context thisActivity() { return MascotActivity.this; }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title + notification bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_mascot);


        // bind UI
        notificationCard   = findViewById(R.id.notificationCard);
        notificationText   = findViewById(R.id.notificationText);
        titleText          = findViewById(R.id.titleText);
        mascotView         = findViewById(R.id.mascotView);
        transcriptionText  = findViewById(R.id.transcriptionText);
        translationText    = findViewById(R.id.translationText);
        waveformView       = findViewById(R.id.waveformView);

        // hide until needed
        notificationCard.setVisibility(View.GONE);
        transcriptionText.setVisibility(View.GONE);
        translationText.setVisibility(View.GONE);
        waveformView.setVisibility(View.GONE);

        final View notificationCard = findViewById(R.id.notificationCard);
        notificationCard.post(() -> {
            notificationCard.setTranslationY(-notificationCard.getHeight());
        });
        MaterialButton btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {          // defensive: make sure it's in the layout
            btnCancel.setOnClickListener(v -> cancelAndRefresh());
        }

        // make mascot tappable -> MainActivity
        mascotView.setOnClickListener(v -> {
            startActivity(new Intent(MascotActivity.this, MainActivity.class));
            finish();
        });


        // color “yɛkasa”
        SpannableString span = new SpannableString("yɛkasa");
        span.setSpan(new ForegroundColorSpan(Color.BLACK), 0,2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(this, R.color.yekasa_dark_green)
        ), 2, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        titleText.setText(span);

        // mascot GIF
        Glide.with(this)
                .asGif()
                .load(R.drawable.y3kasa_mascot)
                .into(mascotView);

        audioRecorder = new AudioRecorder(this);
        audioPlayer   = new AudioPlayer(this);

        audioRecorder.setRecordingListener(() -> {
            Log.d(TAG, "Recording ended.");
            processRecording();
            startFloatingAnimation();
            runOnUiThread(() -> {
                waveformView.setVisibility(View.GONE);
                Glide.with(MascotActivity.this).clear(waveformView);
            });
            if (isBound) {
                try { wakeWordService.resumeListening(); }
                catch (PorcupineException ignored) {}
            }
        });

        // measure for centering animation
        mascotView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        if (hasLaidOut) return;
                        hasLaidOut = true;
                        mascotView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        initialX   = mascotView.getX();
                        initialY   = mascotView.getY();
                        viewWidth  = mascotView.getWidth();
                        viewHeight = mascotView.getHeight();
                        View root = findViewById(R.id.mascotRoot);
                        centerX = (root.getWidth()  - viewWidth )/2f;
                        centerY = (root.getHeight() - viewHeight)/2f;
                    }
                });

        // start & bind wake-word service
        startService(new Intent(this, WakeWordService.class));
        bindService(new Intent(this, WakeWordService.class),
                conn, BIND_AUTO_CREATE);
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(wwReceiver,
                new IntentFilter(WakeWordService.ACTION_WAKEWORD_DETECTED),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceiver(wwReceiver);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (isBound) unbindService(conn);
        stopService(new Intent(this, WakeWordService.class));
    }

    private void cancelAndRefresh() {
        /* 1) Stop anything that might still be running */
        try { audioRecorder.stopRecording(); } catch (Exception ignored) {}
        try { audioPlayer.stopPlaying(); }          catch (Exception ignored) {}
        if (floatAnimator != null) { floatAnimator.cancel(); }

        /* 2) Cancel in‑flight network calls (NewsHandler, TTS, etc.) */
        OkHttpSingleton.cancelAll();   // see helper below

        /* 3) Hide the notification card immediately */
        findViewById(R.id.notificationCard).setVisibility(View.GONE);

        /* 4) Optionally stop/pause wake‑word service */
        if (isBound && wakeWordService != null) {
            try { wakeWordService.resumeListening(); } catch (Exception ignored) {}
        }

        /* 5) Recreate the activity – quick way to reset UI */
        recreate();
    }
    public void pauseWakeWord() {
        if (isBound && wakeWordService != null) {
            try { wakeWordService.pauseListening(); }
            catch (PorcupineException e) { Log.e(TAG, "pauseListening failed", e); }
        }
    }

    public void resumeWakeWord() {
        if (isBound && wakeWordService != null) {
            try { wakeWordService.resumeListening(); }
            catch (PorcupineException e) { Log.e(TAG, "resumeListening failed", e); }
        }
    }


    private void moveMascotToCenterAndEnlarge() {
        mascotView.animate()
                .x(centerX).y(centerY)
                .scaleX(2f).scaleY(2f)
                .setDuration(400)
                .start();
    }

    private void startFloatingAnimation() {
        floatAnimator = ObjectAnimator.ofFloat(mascotView, "y",
                centerY - 30f, centerY + 30f);
        floatAnimator.setDuration(1000);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.start();
    }

    private void stopFloatingAndReturn() {
        if (floatAnimator != null) {
            floatAnimator.cancel();
            floatAnimator = null;
        }
        mascotView.animate()
                .x(initialX).y(initialY)
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .start();
    }

    private void processRecording() {
        String path = audioRecorder.getAudioFilePath();
        if (path == null) return;

        AudioTranscriber.transcribeAudio(path, new AudioTranscriber.TranscriptionCallback() {
            @Override public void onSuccess(final String transcription) {
                runOnUiThread(() -> {
                    transcriptionText.setText(transcription);
                    transcriptionText.setVisibility(View.VISIBLE);

                    TextTranslator.translateText(transcription, new TextTranslator.TranslationCallback() {
                        @Override public void onSuccess(String translation,
                                                        String cls,
                                                        String cnt,
                                                        String obj) {
                            runOnUiThread(() -> {
                                translationText.setText(translation);
                                translationText.setVisibility(View.VISIBLE);
                                actionClass   = cls;
                                actionContent = cnt;
                                actionObject  = obj;

                                stopFloatingAndReturn();

                                switch (actionClass) {
                                    case "A":
                                        PhoneCallHandler.handlePhoneCall(
                                                MascotActivity.this, actionContent);
                                        break;
                                    case "B":
                                        AppOpener.handleAppOpen(
                                                MascotActivity.this, actionContent);
                                        break;
                                    case "C":
                                        // Fetch the real news summary and read *that* out loud
                                        // 2) Play a “please wait” sound
                                        MediaPlayer wait = MediaPlayer.create(MascotActivity.this, R.raw.news_wait);
                                        wait.setOnCompletionListener(MediaPlayer::release);
                                        wait.start();
                                        NewsHandler.handleNews(MascotActivity.this, actionContent);
                                        break;
                                    case "D":
                                        TextMessageHandler.handleSendText(
                                                MascotActivity.this,
                                                actionContent,
                                                actionObject
                                        );
                                        break;
                                    case "F":
                                        MediaPlayer again = MediaPlayer.create(MascotActivity.this, R.raw.come_again);
                                        again.setOnCompletionListener(MediaPlayer::release);
                                        again.start();
                                        break;

                                }
                            });
                        }
                        @Override public void onError(String err) {
                            runOnUiThread(() -> {
                                stopFloatingAndReturn();
                                MediaPlayer again = MediaPlayer.create(
                                        MascotActivity.this, R.raw.come_again);
                                again.setOnCompletionListener(MediaPlayer::release);
                                again.start();
                            });
                        }
                    });
                });
            }

            @Override public void onError(String err) {
                runOnUiThread(() -> {
                    stopFloatingAndReturn();
                    MediaPlayer again = MediaPlayer.create(
                            MascotActivity.this, R.raw.come_again);
                    again.setOnCompletionListener(MediaPlayer::release);
                    again.start();
                });
            }
        });
    }

    private void performAction() {
        // no-op: everything already handled in processRecording()
    }
}
