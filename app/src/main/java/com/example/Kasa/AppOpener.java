package com.example.Kasa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

public class AppOpener {

    // Simple container for app details.
    public static class AppDetails {
        public String packageName;

        public AppDetails(String packageName) {
            this.packageName = packageName;
        }
    }

    // Map app names to their package names.
    private static final Map<String, AppDetails> appDetailsMap = new HashMap<>();

    static {
        appDetailsMap.put("YouTube",  new AppDetails("com.google.android.youtube"));
        appDetailsMap.put("Facebook", new AppDetails("com.facebook.katana"));
        appDetailsMap.put("Twitter",  new AppDetails("com.twitter.android"));
        appDetailsMap.put("Instagram",new AppDetails("com.instagram.android"));
        appDetailsMap.put("WhatsApp", new AppDetails("com.whatsapp"));
        // Add any other apps with their package names...
    }

    /**
     * Opens an app if installed on the device using its package name. If the app
     * is not installed, an alert dialog notifies the user.
     *
     * @param activity The calling activity.
     * @param appName  The key/name for the app in the map above (e.g. "WhatsApp").
     */
    public static void handleAppOpen(Activity activity, String appName) {
        AppDetails details = appDetailsMap.get(appName);
        if (details == null) {
            new AlertDialog.Builder(activity)
                    .setTitle("App Not Supported")
                    .setMessage("No details defined for \"" + appName + "\"")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        PackageManager packageManager = activity.getPackageManager();
        // Attempt to get the main launch intent for the package
        Intent launchIntent = packageManager.getLaunchIntentForPackage(details.packageName);

        if (launchIntent != null) {
            // If found, start the activity
            activity.startActivity(launchIntent);
        } else {
            // Otherwise, notify the user the app may not be installed
            new AlertDialog.Builder(activity)
                    .setTitle("App Not Installed")
                    .setMessage(appName + " is not installed on your device.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
