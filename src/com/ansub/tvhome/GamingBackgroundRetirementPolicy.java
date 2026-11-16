package com.ansub.tvhome;
import java.util.*;

/** Exact retired Gaming decoration keys; media, music and launcher appearance stay independent. */
final class GamingBackgroundRetirementPolicy {
 private static final Set<String> KEYS=new HashSet<>(Arrays.asList("gamingTheme","gamingIntensity","gamingLivingScenes","gamingLivingMood","gamingLivingEffect","gamingLivingIntensity","gamingShaderBackground","gamingShaderScene","detoxExtensionsShader","detoxShaderScene","gamingNight"));
 static boolean retired(String key){return key!=null&&(KEYS.contains(key)||key.startsWith("gamingScene_"));}
 static Set<String> keysToRemove(Set<String> existing){Set<String> result=new HashSet<>();for(String key:existing)if(retired(key))result.add(key);return result;}
 private GamingBackgroundRetirementPolicy(){}
}
