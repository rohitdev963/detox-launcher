package com.ansub.tvhome;

public final class BackupEntryPolicyTest {
    private static int passed;
    private static void check(boolean value) { if (!value) throw new AssertionError(); passed++; }
    private static void rejects(String... names) {
        BackupEntryPolicy policy = new BackupEntryPolicy();
        boolean failed = false;
        try { for (String name : names) policy.accept(name, false); }
        catch (java.io.IOException expected) { failed = true; }
        check(failed);
    }
    public static void main(String[] args) throws Exception {
        BackupEntryPolicy policy = new BackupEntryPolicy();
        policy.accept("backup.json", false);
        policy.accept("wallpaper.jpg", false);
        policy.accept("video-wallpaper.bin", false);
        check(policy.hasSettings());
        rejects("backup.json", "backup.json");
        rejects("backup.json", "settings.json");
        rejects("settings.json", "backup.json");
        rejects("../backup.json");
        rejects("other.txt");
        rejects("wallpaper.jpg", "wallpaper.jpg");
        rejects("backup.json", "wallpaper.jpg", "video-wallpaper.bin", "wallpaper.jpg");
        BackupEntryPolicy empty = new BackupEntryPolicy();
        check(!empty.hasSettings());
        boolean directoryRejected = false;
        try { empty.accept("backup.json", true); } catch (java.io.IOException expected) { directoryRejected = true; }
        check(directoryRejected);
        System.out.println("Backup policy tests passed: " + passed);
    }
}
