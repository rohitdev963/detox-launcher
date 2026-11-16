package com.ansub.tvhome;

/** Saved names and stable asset paths are independent of picker positions. */
final class BundledWallpaperCatalog {
 static final String[] NAMES={"Aurora Veil","Twilight Peaks","Ember Horizon","Photo"};
 private static final String[] FILES={"aurora-veil.jpg","twilight-peaks.jpg","ember-horizon.jpg"};
 static int size(){return FILES.length;}
 static String assetAt(int index){if(index<0||index>=FILES.length)throw new IllegalArgumentException("Invalid bundled wallpaper");return "public/wallpapers/"+FILES[index];}
 static int selected(String saved){
  for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(saved))return i;
  return 0;
 }
 static boolean landscapeRotation(int index){return false;}
}
