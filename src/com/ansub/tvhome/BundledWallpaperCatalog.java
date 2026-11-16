package com.ansub.tvhome;

/** Saved names and stable asset paths are independent of picker positions. */
final class BundledWallpaperCatalog {
 static final String[] NAMES={"Aurora Veil","Twilight Peaks","Ember Horizon","Photo"};
 private static final String[] FILES={"aurora-veil.jpg","twilight-peaks.jpg","ember-horizon.jpg"};
 static final int[][] PALETTES={
  {0xFF080F26,0xFF071420,0xFF05111E,0x904BC4D0,0x805F4AE0},
  {0xFF141639,0xFF172239,0xFF0C192B,0x90D495B4,0x807E82E0},
  {0xFF140E1E,0xFF17172B,0xFF091122,0x90E28A61,0x805B69D9},
  {0xFF10202C,0xFF0B1520,0xFF060D17,0x904BC4D0,0x805F4AE0}
 };
 static int size(){return FILES.length;}
 static String assetAt(int index){if(index<0||index>=FILES.length)throw new IllegalArgumentException("Invalid bundled wallpaper");return "public/wallpapers/"+FILES[index];}
 static int selected(String saved){
  for(int i=0;i<NAMES.length;i++)if(NAMES[i].equals(saved))return i;
  return 0;
 }
 static boolean landscapeRotation(int index){return false;}
}
