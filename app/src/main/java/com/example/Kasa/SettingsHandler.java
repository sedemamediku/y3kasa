package com.example.Kasa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

public class SettingsHandler {
    private static final String TAG = "SettingsHandler";

    /**
     * Change screen brightness.
     * @param activity         Activity context (for window attributes & WRITE_SETTINGS permission)
     * @param brightnessPercent 0–100
     */
    public static void setScreenBrightness(Activity activity, int brightnessPercent) {
        if (!Settings.System.canWrite(activity)) {
            requestWriteSettingsPermission(activity);
            return;
        }

        brightnessPercent = Math.max(0, Math.min(brightnessPercent, 100));
        int systemValue = (int)(brightnessPercent / 100f * 255);
        Settings.System.putInt(
                activity.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                systemValue
        );

        // Also apply to this Window immediately
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        lp.screenBrightness = brightnessPercent / 100f;
        activity.getWindow().setAttributes(lp);

        Log.d(TAG, "Screen brightness set to " + brightnessPercent + "%");
    }

    /**
     * Change global font scale (text zoom) for the device.
     * @param activity Activity context
     * @param scale    Typical range: 0.85f (small) to 1.15f (large)
     */
    public static void setFontScale(Activity activity, float scale) {
        if (!Settings.System.canWrite(activity)) {
            requestWriteSettingsPermission(activity);
            return;
        }

        scale = Math.max(0.5f, Math.min(scale, 2.0f));
        Settings.System.putFloat(
                activity.getContentResolver(),
                Settings.System.FONT_SCALE,
                scale
        );

        Log.d(TAG, "Global font scale set to " + scale);
    }

    /**
     * Change the volume of a given audio stream.
     * @param context      Application or activity context
     * @param streamType   AudioManager.STREAM_* constant (e.g., STREAM_MUSIC)
     * @param volumeLevel  0–100
     */
    public static void setVolume(Context context, int streamType, int volumeLevel) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            Log.e(TAG, "AudioManager unavailable");
            return;
        }

        int max = am.getStreamMaxVolume(streamType);
        int v = (int)(Math.max(0, Math.min(volumeLevel, 100)) / 100f * max);
        am.setStreamVolume(streamType, v, 0);
        Log.d(TAG, "Stream " + streamType + " volume set to " + v + " / " + max);
    }

    /**
     * Prompt user to grant WRITE_SETTINGS permission.
     */
    private static void requestWriteSettingsPermission(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
        Log.d(TAG, "Requested WRITE_SETTINGS permission");
    }
}
