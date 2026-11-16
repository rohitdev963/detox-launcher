package com.ansub.tvhome;

public final class RestoreTempCleanupPolicyTest {
    private static int passed;
    private static void check(boolean expected, String name, boolean cache, long modified, long now) {
        boolean actual = RestoreTempCleanupPolicy.shouldDelete(name, cache, modified, now);
        if (actual != expected) throw new AssertionError(name + " expected=" + expected + " got=" + actual);
        passed++;
    }
    public static void main(String[] args) {
        long now = 200000000L, old = now - RestoreTempCleanupPolicy.MIN_AGE_MS - 1;
        check(true, "restore-wallpaper-123.tmp", true, old, now);
        check(true, "restore-video-456.tmp", true, old, now);
        check(true, "selected_wallpaper.jpg.restore-123-456.tmp", false, old, now);
        check(true, "selected_wallpaper.jpg.restore-123--456.tmp", false, old, now);
        check(true, "video-wallpaper-transfer.mp4.restore-123-456.tmp", false, old, now);
        check(false, "selected_wallpaper.jpg.restore-123-456.previous", false, old, now);
        check(false, "video-wallpaper-transfer.mp4.failed-restore-123", false, old, now);
        check(false, "selected_wallpaper.jpg", false, old, now);
        check(false, "restore-video-456.tmp", true, now - 1000, now);
        check(false, "restore-video-456.tmp", false, old, now);
        check(false, "restore-wallpaper-../../bad.tmp", true, old, now);
        check(false, "restore-wallpaper-123.tmp", true, now + 1, now);
        check(false, "restore-wallpaper-123.tmp", true, 0, now);
        System.out.println("Restore scratch cleanup: " + passed + "/" + passed + " passed");
    }
}
