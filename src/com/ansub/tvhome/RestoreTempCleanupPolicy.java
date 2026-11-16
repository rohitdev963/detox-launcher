package com.ansub.tvhome;

/** Restricts cleanup to stale, app-created restore scratch files. */
final class RestoreTempCleanupPolicy {
    static final long MIN_AGE_MS = 24L * 60L * 60L * 1000L;
    private RestoreTempCleanupPolicy() {}

    static boolean shouldDelete(String name, boolean cacheDirectory, long lastModified, long now) {
        if (name == null || lastModified <= 0 || now < lastModified
                || now - lastModified < MIN_AGE_MS) return false;
        if (cacheDirectory) {
            return name.matches("restore-(wallpaper|video)-[0-9]+\\.tmp");
        }
        // Never delete .previous or .failed-restore: they may contain the only
        // surviving copy of a user's original wallpaper/video.
        return name.matches("(selected_wallpaper\\.jpg|video-wallpaper-transfer\\.mp4)"
                + "\\.restore-[0-9]+--?[0-9]+\\.tmp");
    }
}
