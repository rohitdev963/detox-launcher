package com.ansub.tvhome;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Matches only preference keys written by retired Detox mini-games. */
final class LegacyArcadeCleanupPolicy {
    private static final Set<String> FLIGHT_ARCADE_KEYS = new HashSet<>(Arrays.asList(
            "sound", "selected", "quietMode", "musicVolume", "effectsVolume", "car_style",
            "stars_v1", "best", "bestCombo", "bestDistance", "bossesDefeated", "dragonsDefeated",
            "flightAssist", "difficultyMode", "atmosphereMode", "lifetimeSparks", "runsPlayed",
            "soundEnabled", "arcadeMusicEnabled", "dailyMissionDay", "dailyMissionIndex",
            "dailyMissionProgress", "dailyMissionComplete", "dailyMissionsCompleted",
            "wizardCourse", "wizardFlyer", "wizardRelayMode", "wizardCourseLabPreset",
            "wizardTotalRescues", "wizardBroomLook", "wizardCompanionLook", "wizardEventStyle",
            "magicDiscoveredSpells", "magicSecrets", "magicVisitedAreas"
    ));

    private static final Set<String> NIGHT_FLIGHT_KEYS = new HashSet<>(Arrays.asList(
            "best", "bestCombo", "flightAssist", "difficultyMode", "atmosphereMode",
            "lifetimeSparks", "runsPlayed", "bestDistance", "soundEnabled", "arcadeMusicEnabled",
            "dragonsDefeated", "bossesDefeated", "wizardCourse", "wizardFlyer", "dailyMissionDay",
            "dailyMissionIndex", "dailyMissionProgress", "dailyMissionComplete", "dailyMissionsCompleted",
            "wizardRelayMode", "wizardCourseLabPreset", "wizardTotalRescues", "wizardBroomLook",
            "wizardCompanionLook", "wizardEventStyle", "magicDiscoveredSpells", "magicSecrets",
            "magicVisitedAreas"
    ));

    private LegacyArcadeCleanupPolicy() { }

    static boolean isRetiredPreference(String store, String key) {
        if (store == null || key == null) return false;
        if ("flight_arcade".equals(store)) return isFlightArcadeKey(key);
        if ("night-flight".equals(store)) return isNightFlightKey(key);
        return false;
    }

    private static boolean isFlightArcadeKey(String key) {
        if (FLIGHT_ARCADE_KEYS.contains(key)) return true;
        if (isNumberedKey(key, "best_", 0, 19)
                || isNumberedKey(key, "difficulty_", 0, 19)
                || isNumberedKey(key, "skin_", 0, 19)
                || isNumberedKey(key, "tutorial_", 0, 19)
                || isNumberedKey(key, "save_v1_", 0, 19)
                || isNumberedKey(key, "recorded_", 0, 19)
                || isNumberedKey(key, "runs_", 0, 19)
                || isNumberedKey(key, "wins_", 0, 19)) return true;
        String[] parts = key.split("_", -1);
        if (parts.length == 3 && "stage".equals(parts[0]))
            return inRange(parts[1], 0, 19) && inRange(parts[2], 0, 2);
        if (parts.length == 4 && "medal".equals(parts[0]))
            return inRange(parts[1], 0, 19) && inRange(parts[2], 0, 2)
                    && ("-1".equals(parts[3]) || inRange(parts[3], 0, 2));
        if (parts.length == 2 && "achievement".equals(parts[0])) return inRange(parts[1], 0, 5);
        return false;
    }

    private static boolean isNightFlightKey(String key) {
        if (NIGHT_FLIGHT_KEYS.contains(key)) return true;
        return key.startsWith("magicRestoration")
                && inRange(key.substring("magicRestoration".length()), 0, 99);
    }

    private static boolean isNumberedKey(String key, String prefix, int minimum, int maximum) {
        return key.startsWith(prefix) && inRange(key.substring(prefix.length()), minimum, maximum);
    }

    private static boolean inRange(String value, int minimum, int maximum) {
        if (value == null || value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) if (!Character.isDigit(value.charAt(i))) return false;
        try {
            int number = Integer.parseInt(value);
            return number >= minimum && number <= maximum;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
