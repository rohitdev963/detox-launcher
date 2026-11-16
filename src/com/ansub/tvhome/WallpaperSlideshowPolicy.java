package com.ansub.tvhome;

/** Shared slideshow timing and activation rules; never owns or deletes photo files. */
final class WallpaperSlideshowPolicy {
 static boolean shouldSchedule(boolean resumed,boolean previewBlocked,boolean enabled,boolean videoActive,int seconds,int photos){
  return resumed&&!previewBlocked&&enabled&&!videoActive&&seconds>0&&photos>1;
 }
 static int currentIndex(int saved,int photos){return photos<=0?0:Math.max(0,Math.min(photos-1,saved));}
 static int activationSeconds(int saved){return saved>0?saved:300;}
 static final class Timing {
  private long nextAt;private int interval;
  long delay(long now,int seconds){
   if(nextAt==0||interval!=seconds){interval=seconds;nextAt=now+(long)seconds*1000L;}
   return Math.max(0,nextAt-now);
  }
  void reset(){nextAt=0;interval=0;}
 }
 private WallpaperSlideshowPolicy(){}
}
