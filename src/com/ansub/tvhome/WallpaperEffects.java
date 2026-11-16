package com.ansub.tvhome;

final class WallpaperEffects {
    static final String[] NAMES = {"Ambient light", "Soft light sweep", "Edge shade", "Evening dim", "Off"};
    static final String[] KEYS = {"ambient", "studio", "shade", "evening", "off"};
    static String normalize(String saved) {
        for (String key : KEYS) if (key.equals(saved)) return key;
        return "ambient";
    }
    static String displayName(String saved) {
        String key=normalize(saved);
        for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return NAMES[i];
        return NAMES[0];
    }
    private WallpaperEffects() { }
}
