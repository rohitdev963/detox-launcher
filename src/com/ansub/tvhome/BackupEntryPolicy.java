package com.ansub.tvhome;

import java.util.HashSet;
import java.util.Set;

/** Reject ambiguous or unexpected backup ZIP contents before restoring preferences. */
final class BackupEntryPolicy {
    private final Set<String> seen = new HashSet<>();
    private int count;
    private boolean hasSettings;

    void accept(String name, boolean directory) throws java.io.IOException {
        if (++count > 8) throw new java.io.IOException("Backup has too many files");
        if (directory || name == null || !seen.add(name))
            throw new java.io.IOException("Invalid or duplicate backup entry");
        switch (name) {
            case "settings.json":
            case "backup.json":
                if (hasSettings) throw new java.io.IOException("Ambiguous backup settings");
                hasSettings = true;
                break;
            case "wallpaper.jpg":
            case "video-wallpaper.bin":
                break;
            default:
                throw new java.io.IOException("Unexpected backup file");
        }
    }

    boolean hasSettings() { return hasSettings; }
}
