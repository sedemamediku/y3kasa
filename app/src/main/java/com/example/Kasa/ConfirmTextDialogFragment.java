package com.example.Kasa;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/**
 * A confirmation dialog for sending text messages that
 * also listens for a voice reply (“Aane”/“Daabi”) to auto-confirm.
 */
public class ConfirmTextDialogFragment extends DialogFragment {
    public interface Listener {
        void onConfirmed();
        void onCancelled();
    }

    private static final String ARG_RECIPIENT = "arg_recipient";
    private static final String ARG_BODY      = "arg_body";

    private Listener listener;
    private String recipient, body;
    private AudioRecorder recorder;

    public static ConfirmTextDialogFragment newInstance(
            String recipient, String body, Listener listener
    ) {
        ConfirmTextDialogFragment f = new ConfirmTextDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RECIPIENT, recipient);
        args.putString(ARG_BODY, body);
        f.setArguments(args);
        f.listener = listener;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Dialog d = getDialog();
        if (d != null) {
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.getWindow()
                    .setBackgroundDrawable(new ColorDrawable(0x80000000));
        }
        return inflater.inflate(R.layout.fragment_confirm_action, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        recipient = requireArguments().getString(ARG_RECIPIENT);
        body      = requireArguments().getString(ARG_BODY);

        // 1) Blur behind
        BlurView blur = v.findViewById(R.id.blurView);
        blur.setupWith((ViewGroup) requireActivity().getWindow().getDecorView())
                .setFrameClearDrawable(
                        requireActivity().getWindow().getDecorView().getBackground())
                .setBlurAlgorithm(new RenderScriptBlur(requireContext()))
                .setBlurRadius(20f)
                .setHasFixedTransformationMatrix(true);

        // 2) Populate fields
        ((TextView)v.findViewById(R.id.tvRecipient)).setText("To: " + recipient);
        ((TextView)v.findViewById(R.id.tvMessageBody)).setText(body);

        // 3) Play confirmation audio
        MediaPlayer mp = MediaPlayer.create(requireContext(), R.raw.text_confirmation);
        mp.setOnCompletionListener(mediaPlayer -> {
            mediaPlayer.release();
            // 4) Start voice confirmation recording
            recorder = new AudioRecorder(requireActivity());
            recorder.setRecordingListener(() -> handleVoiceReply());
            recorder.startRecording();
        });
        mp.start();

        // 5) Manual buttons
        Button no  = v.findViewById(R.id.btnCancel);
        Button yes = v.findViewById(R.id.btnConfirm);
        no.setOnClickListener(x -> {
            if (recorder != null) recorder.stopRecording();
            dismiss();
            if (listener != null) listener.onCancelled();
        });
        yes.setOnClickListener(x -> {
            if (recorder != null) recorder.stopRecording();
            dismiss();
            if (listener != null) listener.onConfirmed();
        });
    }

    private void handleVoiceReply() {
        if (recorder != null) recorder.stopRecording();
        String path = recorder.getAudioFilePath();
        if (path == null) {
            Toast.makeText(getContext(), "No voice reply captured", Toast.LENGTH_SHORT).show();
            return;
        }
        AudioTranscriber.transcribeAudio(path, new AudioTranscriber.TranscriptionCallback() {
            @Override public void onSuccess(String transcription) {
                String resp = transcription.trim().toLowerCase();
                int dYes = levenshteinDistance(resp, "aane");
                int dNo  = levenshteinDistance(resp, "daabi");
                boolean isYes = (dYes < dNo && dYes <= 2);
                requireActivity().runOnUiThread(() -> {
                    dismiss();
                    if (listener != null) {
                        if (isYes) listener.onConfirmed();
                        else      listener.onCancelled();
                    }
                });
            }
            @Override public void onError(String err) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Could not understand voice reply", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // reuse the same Levenshtein from PhoneCallHandler
    private static int levenshteinDistance(String s1, String s2) {
        int n = s1.length(), m = s2.length();
        int[][] dp = new int[n+1][m+1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = s1.charAt(i-1)==s2.charAt(j-1)?0:1;
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                        dp[i-1][j-1] + cost
                );
            }
        }
        return dp[n][m];
    }
}
