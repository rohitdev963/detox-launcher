package com.ansub.tvhome;

/** Existing typography preferences and menu hierarchy, independent of Android views. */
final class MenuTextPolicy {
 static final int RAINBOW=1,WALLPAPER=2;
 static final int[] RAINBOW_COLORS={0xFFFF4D6D,0xFFFFB84D,0xFFFFF052,0xFF58D68D,0xFF40C4FF,0xFF7B61FF,0xFFE056FD,0xFFFF4D6D};
 static String fontAsset(String key){
  if("nunito".equals(key))return "fonts/Nunito.ttf";
  if("orbitron".equals(key))return "fonts/Orbitron.ttf";
  if("playfair".equals(key))return "fonts/PlayfairDisplay.ttf";
  if("pacifico".equals(key))return "fonts/Pacifico-Regular.ttf";
  if("poppins".equals(key))return "fonts/Poppins-Regular.ttf";
  return null;
 }
 static float scale(int percent){return Math.max(80,Math.min(140,percent))/100f;}
 static int ink(int requested,int wallpaper){return requested==WALLPAPER?wallpaper:requested==RAINBOW?0xFFFFFFFF:requested;}
 static int shadow(int ink){int luminance=((ink>>16&255)*299+(ink>>8&255)*587+(ink&255)*114)/1000;return luminance<110?0xC0FFFFFF:0xD9000000;}
 static float headingSize(float body){return body*4f/3f;}
 private MenuTextPolicy(){}
}
