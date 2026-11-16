package com.ansub.tvhome;

/** Keeps named Home profiles limited to user-facing launcher preferences. */
final class HomeProfilePolicy {
    static final int MAX_PROFILES = 8;

    private HomeProfilePolicy() { }

    static String validateName(String value) {
        if (value == null) throw new IllegalArgumentException("Profile name required");
        String name = value.trim();
        if (!name.matches("[A-Za-z0-9 _-]{1,32}")) throw new IllegalArgumentException("Use 1–32 letters, numbers, spaces, _ or -");
        return name;
    }

    static boolean isProfileSetting(String key) {
        if (key == null || key.isEmpty() || key.equals("profileStore") || key.equals("activeProfile")
                || key.equals("stamp") || key.equals("homeLastFocusedApp") || key.equals("detoxPerformanceProbe")
                || key.equals("homeCompanionMode") || GamingBackgroundRetirementPolicy.retired(key)) return false;
        // Allow only launcher presentation and media-selection settings. Keep
        // device access, pairing secrets, migration flags, diagnostics and
        // focus restoration outside a portable profile.
        return key.equals("apps") || key.equals("quick") || key.equals("wallpaper")
                || key.startsWith("wallpaperMotion") || key.startsWith("wallpaperPerformance")
                || key.startsWith("wallpaperParallax") || key.startsWith("wallpaperFraming")
                || key.startsWith("wallpaperContrast") || key.startsWith("wallpaperSlideshow")
                || key.startsWith("wallpaperTransition") || key.equals("photoUri") || key.equals("photoFile")
                || key.startsWith("liveWallpaper") || key.startsWith("clock") || key.startsWith("font")
                || key.startsWith("utility") || key.startsWith("appLayout") || key.startsWith("appIcon")
                || key.startsWith("home") && !key.equals("homeLastFocusedApp")
                || key.startsWith("idleWallpaper") || key.startsWith("gaming")
                || key.startsWith("remoteShortcut") || key.startsWith("look") || key.startsWith("theme")
                || key.startsWith("premium") || key.startsWith("focus") || key.startsWith("highContrast")
                || key.equals("launcherAnimationMode") || key.equals("launcherMotionPreset")
                || key.equals("detoxPerformancePreset");
    }
}
