package com.ansub.tvhome;

import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/** Small, process-local error summary for the TV diagnostics page and Android Logcat. */
final class DetoxDiagnostics {
    private static final AtomicInteger failures = new AtomicInteger();
    private static volatile String latestFailure = "";
    private static volatile long latestFailureAt;
    private static String lastLoggedFailure = "";
    private static long lastLoggedAt;

    private DetoxDiagnostics() {}

    static synchronized void recordFailure(String tag, String component, String action, Throwable error) {
        String type = error == null ? "Unknown error" : error.getClass().getSimpleName();
        latestFailure = component + ": " + action + " (" + type + ")";
        latestFailureAt = System.currentTimeMillis();
        failures.incrementAndGet();
        String key = component + ":" + action + ":" + type;
        if (key.equals(lastLoggedFailure) && latestFailureAt - lastLoggedAt < 60_000L) return;
        lastLoggedFailure = key;
        lastLoggedAt = latestFailureAt;
        if (error == null) Log.e(tag, latestFailure);
        else Log.e(tag, component + ": " + action, error);
    }

    static String report() {
        int count = failures.get();
        if (count == 0) return "No recorded failures since launcher start.";
        String when = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(latestFailureAt));
        return count + " recorded failure" + (count == 1 ? "" : "s")
                + " since launcher start.\nLatest at " + when + ": " + latestFailure;
    }
}
