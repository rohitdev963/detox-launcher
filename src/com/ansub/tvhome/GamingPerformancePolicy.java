package com.ansub.tvhome;

/** Persisted Gaming pacing presets. These pace Detox overlays; the video decoder keeps source cadence. */
public final class GamingPerformancePolicy {
    public static final String[] NAMES = {"Smooth", "Balanced", "Cinematic"};
    public static final int DEFAULT = 1;

    private GamingPerformancePolicy() { }

    public static int normalize(int preset) {
        return Math.max(0, Math.min(NAMES.length - 1, preset));
    }

    public static String name(int preset) { return NAMES[normalize(preset)]; }

    public static float displayHz(float hz) {
        return Float.isFinite(hz) && hz >= 24f && hz <= 240f ? hz : 60f;
    }

    public static int targetHz(int preset, float displayHz) {
        float refresh = displayHz(displayHz);
        int requested = normalize(preset) == 0 ? 30 : normalize(preset) == 1 ? 45 : 60;
        return Math.max(24, (int) Math.floor(Math.min(requested, refresh) + 0.001f));
    }

    public static long frameIntervalNanos(int preset, float displayHz) {
        return Math.round(1_000_000_000d / targetHz(preset, displayHz));
    }
    public static long nextFrameDeadline(long previous, long frameTime, long interval) {
        return Math.max((previous == 0 ? frameTime : previous) + interval, frameTime);
    }

}
