package com.ansub.tvhome;
import java.util.*;
/** Exact preference ownership for isolated Settings resets and shared clock accents. */
final class SettingsPreferencePolicy {
 private static final Set<String> UTILITY_KEYS=new HashSet<>(Arrays.asList("utilityTextColor","utilitiesPlacement","utilityIconScalePercent","utilityIconOpacity","utilityAppearanceStyle","utilityAppearanceMotion"));
 static Set<String> utilityResetKeys(Set<String> existing){Set<String> keys=new HashSet<>();for(String key:existing)if(UTILITY_KEYS.contains(key)||key.startsWith("utilityHidden_"))keys.add(key);return keys;}
 static int clockAccent(int saved,int shared,int rainbow){return saved==0?shared:saved==rainbow?0xFFFFFFFF:saved;}
 private SettingsPreferencePolicy(){}
}
