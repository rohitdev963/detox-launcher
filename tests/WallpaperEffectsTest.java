package com.ansub.tvhome;
public final class WallpaperEffectsTest {
 public static void main(String[] args){
  if(WallpaperEffects.KEYS.length!=WallpaperEffects.NAMES.length)throw new AssertionError("Catalog mismatch");
  for(int i=0;i<WallpaperEffects.KEYS.length;i++)if(!WallpaperEffects.NAMES[i].equals(WallpaperEffects.displayName(WallpaperEffects.KEYS[i])))throw new AssertionError("Wrong effect label");
  for(String invalid:new String[]{null,"removed-effect",""})if(!WallpaperEffects.NAMES[0].equals(WallpaperEffects.displayName(invalid)))throw new AssertionError("Unsafe legacy fallback");
  System.out.println("Wallpaper effects: all labels and legacy fallbacks passed");
 }
}
