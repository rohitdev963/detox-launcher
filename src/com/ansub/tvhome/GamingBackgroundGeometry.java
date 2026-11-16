package com.ansub.tvhome;

/** Aspect-preserving image placement; calculated on load/resize, not in a render loop. */
final class GamingBackgroundGeometry {
 static float[] matrix(int sourceWidth,int sourceHeight,int width,int height,boolean fill,float zoom,float x,float y){
  if(sourceWidth<=0||sourceHeight<=0||width<=0||height<=0)return new float[]{1,0,0};
  float scale=fill?Math.max(width/(float)sourceWidth,height/(float)sourceHeight):Math.min(width/(float)sourceWidth,height/(float)sourceHeight);
  scale*=bound(zoom,1,2.5f,1);
  return new float[]{scale,(width-sourceWidth*scale)*bound(x,0,1,.5f),(height-sourceHeight*scale)*bound(y,0,1,.5f)};
 }
 private static float bound(float n,float min,float max,float fallback){return Float.isNaN(n)||Float.isInfinite(n)?fallback:Math.max(min,Math.min(max,n));}
}
