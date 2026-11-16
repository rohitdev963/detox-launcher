package com.ansub.tvhome;
/** Pack scale applies to fallback artwork; mapped icons keep their authored canvas/padding. */
final class IconSizingPolicy {
 static float factor(float value){return Float.isNaN(value)||Float.isInfinite(value)?1:Math.max(.1f,Math.min(1,value));}
 static float scale(int width,int height,int sourceWidth,int sourceHeight,float factor){return Math.min(Math.max(1,width)/(float)Math.max(1,sourceWidth),Math.max(1,height)/(float)Math.max(1,sourceHeight))*factor(factor);}
}
