package com.ansub.tvhome;
/** New native faces; retired painters remain removed. */
final class ClockFacePolicy {
 static final String[] KEYS={"digital","orbit","words","worlddial","squareworld",
  "premium_glass","premium_minimal_digital","premium_neo","premium_anime","premium_classic","premium_anime_analog",
  "premium_hud","premium_flip","premium_weather","premium_progress","premium_neon","premium_minimal_analog"};
 static final String[] NAMES={"Digital clock","Orbit clock","Matrix words","World dial","Square world",
  "Digital Glass","Minimal Digital","Neo Glow","Anime Style","Analog Classic","Anime Analog",
  "Futuristic HUD","Flip Clock","Weather Clock","Circle Progress","Neon Widget","Minimal Analog"};
 // Only clockStyle is migrated; all other clock settings remain untouched.
 static String normalise(String key){for(String active:KEYS)if(active.equals(key))return active;return "digital";}
 static boolean analog(String key){return "worlddial".equals(key)||"squareworld".equals(key)||"premium_classic".equals(key)||"premium_anime_analog".equals(key)||"premium_minimal_analog".equals(key);}
 static boolean premium(String key){return key!=null&&key.startsWith("premium_")&&key.equals(normalise(key));}
 static boolean wide(String key){return "digital".equals(key)||"premium_glass".equals(key)||"premium_minimal_digital".equals(key)||"premium_anime".equals(key)||"premium_flip".equals(key)||"premium_weather".equals(key)||"premium_neon".equals(key);}
 static boolean background(String key){return premium(key)&&!"premium_minimal_digital".equals(key)&&!"premium_minimal_analog".equals(key);}
 static boolean glow(String key){return "premium_glass".equals(key)||"premium_neo".equals(key)||"premium_classic".equals(key)||"premium_anime_analog".equals(key)||"premium_neon".equals(key);}
 static boolean accent(String key){return premium(key)&&!"premium_flip".equals(key)&&!"premium_weather".equals(key)&&!"premium_anime".equals(key);}
 static boolean animated(String key){return premium(key)&&!"premium_minimal_digital".equals(key)&&!"premium_weather".equals(key);}
}
