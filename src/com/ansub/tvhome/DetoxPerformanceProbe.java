package com.ansub.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Debug;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.WindowManager;

/** Opt-in, low-overhead UI frame pacing sampler. Does not claim GPU or input latency. */
public final class DetoxPerformanceProbe implements Choreographer.FrameCallback {
    private final Activity activity;
    private final String screen;
    private boolean running;
    private long previousNanos;
    private long frames;
    private long longFrames;
    private long worstFrameNanos;
    private long totalFrameNanos;
    private long startedMs;
    private long slowThresholdNanos = 33333334L;
    private float displayHz = 60f;

    public DetoxPerformanceProbe(Activity activity, String screen) {
        this.activity = activity;
        this.screen = screen;
    }

    public void start() {
        if (running || Build.VERSION.SDK_INT < 16) return;
        // Disabled by default: no continuous profiling overhead for ordinary users.
        if (!activity.getSharedPreferences("config", Context.MODE_PRIVATE)
                .getBoolean("detoxPerformanceProbe", false)) return;
        running = true;
        previousNanos = 0L;
        frames = longFrames = worstFrameNanos = totalFrameNanos = 0L;
        startedMs = SystemClock.elapsedRealtime();
        displayHz = 60f;
        try {
            WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                float hz = wm.getDefaultDisplay().getRefreshRate();
                if (hz >= 24f && hz <= 240f) displayHz = hz;
            }
        } catch (RuntimeException ignored) { }
        // A missed vsync: interval exceeds two nominal refresh periods.
        slowThresholdNanos = (long) Math.ceil(2000000000d / displayHz);
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (previousNanos != 0L && frameTimeNanos > previousNanos) {
            long delta = frameTimeNanos - previousNanos;
            frames++;
            // Saturate instead of overflowing after unusually long sampling sessions.
            totalFrameNanos = delta > Long.MAX_VALUE - totalFrameNanos
                    ? Long.MAX_VALUE : totalFrameNanos + delta;
            if (delta > slowThresholdNanos) longFrames++;
            if (delta > worstFrameNanos) worstFrameNanos = delta;
        }
        previousNanos = frameTimeNanos;
        Choreographer.getInstance().postFrameCallback(this);
    }

    /** Last completed samples only; not live or system-wide FPS. */
    public static String report(Context context) {
        SharedPreferences config = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        SharedPreferences data = context.getSharedPreferences("detox_performance", Context.MODE_PRIVATE);
        StringBuilder out = new StringBuilder("Detox UI sampling: ")
                .append(config.getBoolean("detoxPerformanceProbe", false) ? "On" : "Off");
        for (String screen : new String[] {"home", "gaming"}) {
            long when = data.getLong(screen + "_sampled_at_ms", 0L);
            out.append("\n").append(screen.equals("home") ? "Home" : "Gaming").append(": ");
            if (when <= 0L) { out.append("No completed sample"); continue; }
            long frames = data.getLong(screen + "_frames", 0L);
            long slow = data.contains(screen + "_slow_intervals")
                    ? data.getLong(screen + "_slow_intervals", 0L)
                    : data.getLong(screen + "_over33ms", 0L); // Backward-compatible old samples.
            long duration = data.getLong(screen + "_duration_ms", 0L);
            long totalNs = data.getLong(screen + "_total_frame_ns", 0L);
            long hzX100 = data.getLong(screen + "_refresh_hz_x100", 6000L);
            out.append("frame intervals ").append(frames)
                    .append(", display Hz ").append(hzX100 / 100L).append(".")
                    .append(String.format(java.util.Locale.US, "%02d", hzX100 % 100L))
                    .append(", intervals >2 refresh periods ").append(slow)
                    .append(" (").append(frames == 0L ? 0L : Math.round(100d * slow / frames)).append("%)")
                    .append(", elapsed ").append(duration / 1000L).append("s")
                    .append(", observed cadence ")
                    .append(totalNs > 0L ? String.format(java.util.Locale.US, "%.1f", frames * 1e9d / totalNs) : "n/a")
                    .append(" Hz")
                    .append(", worst ").append(data.getLong(screen + "_worst_frame_us", 0L) / 1000L).append("ms")
                    .append(", Java heap ").append(data.getLong(screen + "_java_heap_kb", 0L) / 1024L).append("MB")
                    .append(", native heap ").append(data.getLong(screen + "_native_heap_kb", 0L) / 1024L).append("MB")
                    .append(", sampled ").append(new java.util.Date(when));
        }
        return out.toString();
    }

    public void stop() {
        if (!running) return;
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
        long duration = SystemClock.elapsedRealtime() - startedMs;
        long heapKb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L;
        long nativeKb = Debug.getNativeHeapAllocatedSize() / 1024L;
        SharedPreferences.Editor editor = activity.getSharedPreferences("detox_performance", Context.MODE_PRIVATE).edit();
        editor.putLong(screen + "_duration_ms", duration);
        editor.putLong(screen + "_frames", frames);
        editor.putLong(screen + "_refresh_hz_x100", Math.round(displayHz * 100f));
        editor.putLong(screen + "_slow_intervals", longFrames);
        editor.putLong(screen + "_total_frame_ns", totalFrameNanos);
        editor.putLong(screen + "_worst_frame_us", worstFrameNanos / 1000L);
        editor.putLong(screen + "_java_heap_kb", heapKb);
        editor.putLong(screen + "_native_heap_kb", nativeKb);
        editor.putLong(screen + "_sampled_at_ms", System.currentTimeMillis());
        editor.apply();
    }
}
