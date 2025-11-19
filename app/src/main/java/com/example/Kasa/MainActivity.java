package com.example.Kasa;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import ai.picovoice.porcupine.PorcupineException;

public class MainActivity extends AppCompatActivity {

    private Button recordButton,
            playButton,
            transcribeButton,
            performActionButton,
            processManualCommandButton;
    private TextView transcriptionTextView,
            translationTextView;
    private EditText manualCommandEditText;
    private AudioRecorder audioRecorder;
    private AudioPlayer   audioPlayer;

    // Classified action fields
    private String actionClass   = "";
    private String actionContent = "";
    private String actionObject  = "";

    // Wake-word service
    private WakeWordService wakeWordService;
    private boolean isServiceBound = false;

    private final BroadcastReceiver wakeWordReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent intent) {
            if (!WakeWordService.ACTION_WAKEWORD_DETECTED.equals(intent.getAction())) return;

            if (!audioRecorder.isRecording()) {
                if (isServiceBound) {
                    try { wakeWordService.pauseListening(); }
                    catch (PorcupineException ignored) {}
                }
                audioRecorder.startRecording();
                recordButton.setText("Stop Recording");
                Toast.makeText(MainActivity.this,
                        "Recording started (via wake word)",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            wakeWordService = ((WakeWordService.LocalBinder)binder).getService();
            isServiceBound = true;
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            wakeWordService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Launch mascot screen
        findViewById(R.id.openMascotBtn).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MascotActivity.class))
        );

        // Start & bind wake-word service
        startService(new Intent(this, WakeWordService.class));
        bindService(new Intent(this, WakeWordService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        audioRecorder = new AudioRecorder(this);
        audioPlayer   = new AudioPlayer(this);

        // Find views
        recordButton                = findViewById(R.id.recordButton);
        playButton                  = findViewById(R.id.playButton);
        transcribeButton            = findViewById(R.id.transcribeButton);
        performActionButton         = findViewById(R.id.performActionButton);
        processManualCommandButton  = findViewById(R.id.processManualCommandButton);
        transcriptionTextView       = findViewById(R.id.transcriptionTextView);
        translationTextView         = findViewById(R.id.translationTextView);
        manualCommandEditText       = findViewById(R.id.manualCommandEditText);

        // When silence stops recording
        audioRecorder.setRecordingListener(() -> {
            runOnUiThread(() -> {
                recordButton.setText("Start Recording");
                if (isServiceBound) {
                    try { wakeWordService.resumeListening(); }
                    catch (PorcupineException ignored) {}
                }
            });
        });

        // Manual record/play toggles
        recordButton.setOnClickListener(v -> {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stopPlaying();
                playButton.setText("Play Audio");
            }
            if (audioRecorder.isRecording()) {
                audioRecorder.stopRecording();
                recordButton.setText("Start Recording");
                if (isServiceBound) {
                    try { wakeWordService.resumeListening(); }
                    catch (PorcupineException ignored) {}
                }
            } else {
                if (isServiceBound) {
                    try { wakeWordService.pauseListening(); }
                    catch (PorcupineException ignored) {}
                }
                audioRecorder.startRecording();
                recordButton.setText("Stop Recording");
            }
        });

        playButton.setOnClickListener(v -> {
            if (audioPlayer.isPlaying()) {
                audioPlayer.stopPlaying();
                playButton.setText("Play Audio");
            } else {
                String path = audioRecorder.getAudioFilePath();
                if (path == null || path.isEmpty()) {
                    Toast.makeText(this, "No audio file found", Toast.LENGTH_SHORT).show();
                    return;
                }
                audioPlayer.startPlaying(path, () ->
                        runOnUiThread(() -> playButton.setText("Play Audio"))
                );
                playButton.setText("Stop Audio");
            }
        });

        // Transcribe → translate
        transcribeButton.setOnClickListener(v -> {
            String path = audioRecorder.getAudioFilePath();
            if (path == null || path.isEmpty()) {
                Toast.makeText(this, "No audio file found", Toast.LENGTH_SHORT).show();
                return;
            }
            AudioTranscriber.transcribeAudio(path, new AudioTranscriber.TranscriptionCallback() {
                @Override public void onSuccess(String transcription) {
                    runOnUiThread(() -> {
                        transcriptionTextView.setText(transcription);
                        TextTranslator.translateText(transcription, new TextTranslator.TranslationCallback() {
                            @Override public void onSuccess(String translation,
                                                            String cls,
                                                            String content,
                                                            String object) {
                                runOnUiThread(() -> {
                                    translationTextView.setText(translation);
                                    actionClass   = cls;
                                    actionContent = content;
                                    actionObject  = object;
                                });
                            }
                            @Override public void onError(String err) {
                                runOnUiThread(() ->
                                        Toast.makeText(MainActivity.this,
                                                "Translation error: " + err,
                                                Toast.LENGTH_LONG).show()
                                );
                            }
                        });
                    });
                }
                @Override public void onError(String err) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Transcription error: " + err,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            });
        });

        // Perform action
        performActionButton.setOnClickListener(v ->
                performActionByClass(actionClass, actionContent, actionObject)
        );

        // Manual command → translate → perform
        processManualCommandButton.setOnClickListener(v -> {
            String cmd = manualCommandEditText.getText().toString().trim();
            if (cmd.isEmpty()) {
                Toast.makeText(this, "Please enter a command", Toast.LENGTH_SHORT).show();
                return;
            }
            TextTranslator.translateText(cmd, new TextTranslator.TranslationCallback() {
                @Override public void onSuccess(String translation,
                                                String cls,
                                                String content,
                                                String object) {
                    runOnUiThread(() -> {
                        translationTextView.setText(translation);
                        actionClass   = cls;
                        actionContent = content;
                        actionObject  = object;
                        performActionByClass(cls, content, object);
                    });
                }
                @Override public void onError(String err) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Manual command error: " + err,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        registerReceiver(wakeWordReceiver,
                new IntentFilter(WakeWordService.ACTION_WAKEWORD_DETECTED),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onPause() {
        super.onPause();
        unregisterReceiver(wakeWordReceiver);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopService(new Intent(this, WakeWordService.class));
    }

    private void performActionByClass(String cls, String content, String object) {
        switch (cls) {
            case "A":
                PhoneCallHandler.handlePhoneCall(this, content);
                break;
            case "B":
                AppOpener.handleAppOpen(this, content);
                break;
            case "C":
                NewsHandler.handleNews(this, content);
                break;
            case "D":
                TextMessageHandler.handleSendText(this, content, object);
                break;
            default:
                Toast.makeText(this, "No valid action", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] grants) {
        if (code == AudioRecorder.REQUEST_RECORD_AUDIO_PERMISSION_CODE &&
                (grants.length == 0 || grants[0] != PackageManager.PERMISSION_GRANTED)) {
            recordButton.setText("Permission Denied");
            recordButton.setEnabled(false);
        }
        super.onRequestPermissionsResult(code, perms, grants);
    }
}
