package com.ansub.tvhome;

import java.util.HashMap;
import java.util.Map;

public final class LegacyArcadeCleanupPolicyTest {
    public static void main(String[] args) {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("best_5", 1200);
        preferences.put("save_v1_5", "old-save");
        preferences.put("medal_5_2_1", 3);
        preferences.put("medal_5_1_-1", 2);
        preferences.put("achievement_4", true);
        preferences.put("musicVolume", 40);
        preferences.put("userNote", "keep");
        preferences.put("custom_best_5", 99);

        cleanup("flight_arcade", preferences);
        check(!preferences.containsKey("best_5"), "known numbered game score is removed");
        check(!preferences.containsKey("save_v1_5"), "known save data is removed");
        check(!preferences.containsKey("medal_5_2_1"), "known medal is removed");
        check(!preferences.containsKey("medal_5_1_-1"), "known non-campaign medal is removed");
        check(!preferences.containsKey("achievement_4"), "known achievement is removed");
        check(!preferences.containsKey("musicVolume"), "known audio preference is removed");
        check(preferences.containsKey("userNote"), "unrelated preference is preserved");
        check(preferences.containsKey("custom_best_5"), "lookalike preference is preserved");

        preferences.put("wizardCourse", 3);
        preferences.put("magicRestoration12", 100);
        preferences.put("phonePairingCode", "123456");
        preferences.put("magicRestorationNote", "keep");
        cleanup("night-flight", preferences);
        check(!preferences.containsKey("wizardCourse"), "known legacy course selection is removed");
        check(!preferences.containsKey("magicRestoration12"), "known numbered legacy restoration is removed");
        check(preferences.containsKey("phonePairingCode"), "unrelated pairing data is preserved");
        check(preferences.containsKey("magicRestorationNote"), "malformed key prefix is preserved");
        check(!LegacyArcadeCleanupPolicy.isRetiredPreference("config", "wallpaper"), "unrelated preference store is untouched");

        cleanup("flight_arcade", preferences);
        cleanup("night-flight", preferences);
        check(preferences.containsKey("userNote") && preferences.containsKey("phonePairingCode"), "repeated cleanup remains safe");
        System.out.println("Legacy game cleanup: exact keys removed, unrelated preferences preserved, repeat safe");
    }

    private static void cleanup(String store, Map<String, Object> preferences) {
        preferences.keySet().removeIf(key -> LegacyArcadeCleanupPolicy.isRetiredPreference(store, key));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
