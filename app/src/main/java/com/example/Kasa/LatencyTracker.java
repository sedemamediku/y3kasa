package com.example.Kasa;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class LatencyTracker {
    private static final String PREFS = "app_latency";
    private SharedPreferences prefs;
    private static final String COUNT = "lat_count";
    private static final String TOTAL = "lat_total_ns";

    public LatencyTracker(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    public long start() {
        return System.nanoTime();
    }

    public void endAndRecord(long startTime) {
        long dt = System.nanoTime() - startTime;
        int count = prefs.getInt(COUNT, 0) + 1;
        long total = prefs.getLong(TOTAL, 0L) + dt;
        prefs.edit()
                .putInt(COUNT, count)
                .putLong(TOTAL, total)
                .apply();
    }

    public double getAverageMs() {
        int count = prefs.getInt(COUNT, 0);
        long total = prefs.getLong(TOTAL, 0L);
        return count == 0 ? 0 : (total / 1e6) / count;
    }
}
