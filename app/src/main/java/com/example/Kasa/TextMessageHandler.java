package com.example.Kasa;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TextMessageHandler {
    private static final String TAG = "TextMessageHandler";
    
    // Pattern to validate phone numbers: allows digits, +, -, spaces, parentheses
    // Minimum 7 digits, maximum 15 digits (E.164 standard)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]{7,15}$");
    
    // Maximum SMS message length (SMS standard is 160 characters for single message)
    // For multipart messages, each part is 153 characters
    private static final int MAX_SMS_LENGTH = 1000; // Reasonable limit for multipart messages

    private static class ContactInfo {
        String name, phone;
        ContactInfo(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    /**
     * Entry point: fuzzy‐match + confirmation dialog.
     */
    public static void handleSendText(
            FragmentActivity activity,
            String queryName,
            String messageBody
    ) {
        // 1) Permissions
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_CONTACTS}, 1);
            return;
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.SEND_SMS}, 2);
            return;
        }

        // 2) Find best contact
        ContactInfo best = findBestContact(activity, queryName);
        if (best == null) return;

        // 3) Show confirm-text dialog which handles voice or tap
        ConfirmTextDialogFragment dlg = ConfirmTextDialogFragment.newInstance(
                best.name,
                messageBody,
                new ConfirmTextDialogFragment.Listener() {
                    @Override public void onConfirmed() {
                        // Validate phone number and message before sending
                        if (!validatePhoneNumber(best.phone)) {
                            showAlert(activity, "Invalid Phone Number",
                                    "The phone number format is invalid.");
                            return;
                        }
                        
                        if (!validateMessageBody(messageBody)) {
                            showAlert(activity, "Invalid Message",
                                    "The message is too long or empty.");
                            return;
                        }
                        
                        // Sanitize phone number
                        String sanitizedPhone = sanitizePhoneNumber(best.phone);
                        
                        // actually send SMS
                        try {
                            SmsManager.getDefault().sendTextMessage(
                                    sanitizedPhone, null, messageBody, null, null);
                            Log.d(TAG, "SMS sent to " + best.name + " (" + sanitizedPhone + ")");
                            MediaPlayer sent = MediaPlayer.create(activity, R.raw.message_sent);
                            sent.setOnCompletionListener(MediaPlayer::release);
                            sent.start();
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to send SMS", e);
                            showAlert(activity, "Error",
                                    "Failed to send message: " + e.getMessage());
                        }
                    }
                    @Override public void onCancelled() {
                        Log.d(TAG, "User cancelled SMS to " + best.name);
                        MediaPlayer ok = MediaPlayer.create(activity, R.raw.okay);
                        ok.setOnCompletionListener(MediaPlayer::release);
                        ok.start();
                    }
                }
        );
        dlg.show(activity.getSupportFragmentManager(), "confirmText");
    }

    // --- internal helpers ---

    private static ContactInfo findBestContact(FragmentActivity activity, String queryName) {
        List<ContactInfo> list = new ArrayList<>();
        ContentResolver cr = activity.getContentResolver();
        Cursor c = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                }, null, null, null
        );
        if (c != null) {
            int idxName = c.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int idxPhone = c.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (c.moveToNext()) {
                String n = c.getString(idxName);
                String p = c.getString(idxPhone);
                list.add(new ContactInfo(n, p != null ? p : ""));
            }
            c.close();
        }
        if (list.isEmpty()) {
            showAlert(activity, "No Contacts", "Your address book is empty.");
            return null;
        }
        ContactInfo best = null;
        int min = Integer.MAX_VALUE;
        String ql = queryName.toLowerCase();
        for (ContactInfo ci : list) {
            int d = levenshteinDistance(ql, ci.name.toLowerCase());
            if (d < min) { min = d; best = ci; }
        }
        if (best == null || best.phone.isEmpty()) {
            showAlert(activity, "Contact Not Found",
                    "No matching contact for \"" + queryName + "\"");
            return null;
        }
        return best;
    }

    private static int levenshteinDistance(String s1, String s2) {
        int n = s1.length(), m = s2.length();
        int[][] dp = new int[n+1][m+1];
        for (int i=0;i<=n;i++) dp[i][0] = i;
        for (int j=0;j<=m;j++) dp[0][j] = j;
        for (int i=1;i<=n;i++) {
            for (int j=1;j<=m;j++) {
                int cost = s1.charAt(i-1)==s2.charAt(j-1)?0:1;
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j]+1, dp[i][j-1]+1),
                        dp[i-1][j-1]+cost
                );
            }
        }
        return dp[n][m];
    }

    private static void showAlert(Activity activity, String title, String msg) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Validates phone number format
     */
    private static boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String sanitized = phoneNumber.trim().replaceAll("\\s+", "");
        
        // Basic format validation
        if (!PHONE_PATTERN.matcher(sanitized).matches()) {
            return false;
        }
        
        // Ensure it contains at least some digits
        String digitsOnly = sanitized.replaceAll("[^0-9+]", "");
        return digitsOnly.length() >= 7;
    }
    
    /**
     * Sanitizes phone number for SMS sending
     */
    private static String sanitizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        // Remove all non-digit characters except +
        return phoneNumber.replaceAll("[^0-9+]", "");
    }
    
    /**
     * Validates message body length and content
     */
    private static boolean validateMessageBody(String messageBody) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (messageBody.length() > MAX_SMS_LENGTH) {
            return false;
        }
        
        return true;
    }
}