package com.example.Kasa;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager; // <-- not needed here
import android.util.Log;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PhoneCallHandler {
    private static final String TAG = "PhoneCallHandler";
    
    // Pattern to validate phone numbers: allows digits, +, -, spaces, parentheses
    // Minimum 7 digits, maximum 15 digits (E.164 standard)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{7,15}$");

    private static class ContactInfo {
        String name, phone;
        ContactInfo(String n, String p) { name = n; phone = p; }
    }

    /** Entry point: fuzzy-match + confirmation UI. */
    public static void handlePhoneCall(
            FragmentActivity activity,
            String queryName
    ) {
        // 1) Permissions
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{ Manifest.permission.READ_CONTACTS }, 1);
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{ Manifest.permission.CALL_PHONE }, 2);
            return;
        }

        // 2) Find best contact
        ContactInfo best = findBestContact(activity, queryName);
        if (best == null) return;

        // 3) Show confirmation dialog
        ConfirmCallDialogFragment dlg = ConfirmCallDialogFragment.newInstance(
                best.name,
                best.phone,
                new ConfirmCallDialogFragment.Listener() {
                    @Override public void onConfirmed() {
                        // actually place the call
                        placeCall(activity, best.phone);

                    }
                    @Override public void onCancelled() {
                        Log.d(TAG, "User cancelled call to " + best.name);

                        // play “okay” sound
                        MediaPlayer okay = MediaPlayer.create(activity, R.raw.okay);
                        okay.setOnCompletionListener(MediaPlayer::release);
                        okay.start();
                    }
                }
        );
        dlg.show(activity.getSupportFragmentManager(), "confirmCall");

    }

    // --- internal helpers ---

    private static ContactInfo findBestContact(FragmentActivity activity, String query) {
        List<ContactInfo> list = new ArrayList<>();
        ContentResolver cr = activity.getContentResolver();
        Cursor c = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null, null
        );
        if (c != null) {
            int idxName  = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int idxPhone = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (c.moveToNext()) {
                String n = c.getString(idxName);
                String p = c.getString(idxPhone);
                list.add(new ContactInfo(n, p != null ? p : ""));
            }
            c.close();
        }
        if (list.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("No Contacts")
                    .setMessage("Your address book is empty.")
                    .setPositiveButton("OK",null)
                    .show();
            return null;
        }
        // fuzzy-match (Levenshtein)
        ContactInfo best = null; int min = Integer.MAX_VALUE;
        String ql = query.toLowerCase();
        for (ContactInfo ci : list) {
            int d = levenshteinDistance(ql, ci.name.toLowerCase());
            if (d < min) { min = d; best = ci; }
        }
        if (best == null || best.phone.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Contact Not Found")
                    .setMessage("No matching contact for \""+query+"\"")
                    .setPositiveButton("OK",null)
                    .show();
            return null;
        }
        return best;
    }

    private static void placeCall(FragmentActivity activity, String phoneNumber) {
        // Validate and sanitize phone number
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Log.e(TAG, "Invalid phone number: null or empty");
            return;
        }
        
        // Remove whitespace and validate format
        String sanitized = phoneNumber.trim().replaceAll("\\s+", "");
        
        // Basic format validation
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            Log.e(TAG, "Invalid phone number format: " + phoneNumber);
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Invalid Phone Number")
                    .setMessage("The phone number format is invalid.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        // Additional validation: ensure it contains at least some digits
        String digitsOnly = sanitized.replaceAll("[^0-9+]", "");
        if (digitsOnly.length() < 7) {
            Log.e(TAG, "Phone number too short: " + phoneNumber);
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Invalid Phone Number")
                    .setMessage("The phone number is too short.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        
        // Use PhoneNumberUtils to format and validate
        if (!PhoneNumberUtils.isGlobalPhoneNumber(sanitized)) {
            Log.w(TAG, "Phone number may not be valid: " + sanitized);
            // Continue anyway as PhoneNumberUtils can be strict
        }
        
        // Sanitize for URI: only allow tel: scheme with safe characters
        String uriSafe = sanitized.replaceAll("[^0-9+\\-]", "");
        Uri telUri = Uri.parse("tel:" + uriSafe);
        
        // Validate the URI is safe
        if (!"tel".equals(telUri.getScheme())) {
            Log.e(TAG, "Invalid URI scheme");
            return;
        }
        
        Intent call = new Intent(Intent.ACTION_CALL, telUri);
        activity.startActivity(call);
    }

    private static int levenshteinDistance(String s1, String s2) {
        int n=s1.length(), m=s2.length();
        int[][] dp = new int[n+1][m+1];
        for(int i=0;i<=n;i++) dp[i][0]=i;
        for(int j=0;j<=m;j++) dp[0][j]=j;
        for(int i=1;i<=n;i++){
            for(int j=1;j<=m;j++){
                int cost = s1.charAt(i-1)==s2.charAt(j-1)?0:1;
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j]+1, dp[i][j-1]+1),
                        dp[i-1][j-1]+cost
                );
            }
        }
        return dp[n][m];
    }
}
