package com.ansub.tvhome;

public final class WallpaperSlideshowPolicyTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  check(WallpaperSlideshowPolicy.shouldSchedule(true,false,true,false,30,2),"two photos continue behind ordinary settings menus");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,false,true,true,30,2),"a video must not conceal advancing photo selections");
  check(!WallpaperSlideshowPolicy.shouldSchedule(false,false,true,false,30,2),"paused activities must not load photos");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,true,true,false,30,2),"photo previews and crop screens are protected");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,false,false,false,30,2),"Off remains Off");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,false,true,false,0,2),"zero interval remains paused");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,false,true,false,30,1),"single photos need no repeating timer");
  check(!WallpaperSlideshowPolicy.shouldSchedule(true,false,true,false,30,0),"empty lists do not schedule");
  check(WallpaperSlideshowPolicy.activationSeconds(0)==300&&WallpaperSlideshowPolicy.activationSeconds(-1)==300,"explicit activation starts a paused legacy interval");
  check(WallpaperSlideshowPolicy.activationSeconds(30)==30&&WallpaperSlideshowPolicy.activationSeconds(3600)==3600,"activation preserves configured intervals");
  check(WallpaperSlideshowPolicy.currentIndex(9,2)==1&&WallpaperSlideshowPolicy.currentIndex(-1,2)==0&&WallpaperSlideshowPolicy.currentIndex(0,0)==0,"restored or shortened playlists have bounded indices");
  WallpaperSlideshowPolicy.Timing timing=new WallpaperSlideshowPolicy.Timing();
  check(timing.delay(1000,30)==30000,"initial interval");
  check(timing.delay(11000,30)==20000&&timing.delay(30500,30)==500,"opening menus cannot postpone the next photo indefinitely");
  check(timing.delay(32000,30)==0,"an elapsed deadline runs once immediately");
  timing.reset();check(timing.delay(32000,30)==30000,"completed slide or pause starts a fresh interval");
  check(timing.delay(33000,60)==60000,"changing the interval resets its deadline");
  System.out.println("Slideshow: video exclusion, lifecycle, saved intervals and bounded selection passed");
 }
}
