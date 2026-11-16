package com.ansub.tvhome;

public final class HomeProfilePolicyTest {
    public static void main(String[] args) {
        check("Living room 2".equals(HomeProfilePolicy.validateName(" Living room 2 ")), "profile names trim");
        rejects(null); rejects(""); rejects("  "); rejects("../../private"); rejects("name/"); rejects("x".repeat(33));
        check(HomeProfilePolicy.isProfileSetting("wallpaper"), "wallpaper setting included");
        check(HomeProfilePolicy.isProfileSetting("remoteShortcutAction2"), "remote shortcut included");
        check(HomeProfilePolicy.isProfileSetting("gamingMusicTrack"), "Gaming preference included");
        check(!HomeProfilePolicy.isProfileSetting("homeCompanionMode"), "retired visual setting excluded");
        check(!HomeProfilePolicy.isProfileSetting("profileStore"), "profile storage is excluded");
        check(!HomeProfilePolicy.isProfileSetting("stamp"), "install synchronization stamp is excluded");
        check(!HomeProfilePolicy.isProfileSetting("detoxPerformanceProbe"), "diagnostics setting is excluded");
        check(!HomeProfilePolicy.isProfileSetting("phoneCompanionToken"), "unrelated private settings are excluded");
        System.out.println("Home profile policy: name validation and preference boundaries passed");
    }

    private static void rejects(String value) {
        try { HomeProfilePolicy.validateName(value); throw new AssertionError("accepted invalid profile name"); }
        catch (IllegalArgumentException expected) { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
