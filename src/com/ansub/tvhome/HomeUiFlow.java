package com.ansub.tvhome;
/** Timing and stopwatch arithmetic independent of Android drawing. */
final class HomeUiFlow {
 static float reveal(long elapsed,long delay){return TvMotionPolicy.progress(Math.max(0,Math.min(1,(elapsed-delay)/420f)));}
 static int drift(long minute,boolean horizontal){long index=((minute%(horizontal?17:13))+(horizontal?17:13))%(horizontal?17:13);return (int)(index*(horizontal?3:4)-(horizontal?24:24));}
 static final class Stopwatch {
  long accumulated,started=-1;
  long elapsed(long now){return Math.max(0,accumulated+(started<0?0:Math.max(0,now-started)));}
  boolean running(){return started>=0;}
  void toggle(long now){if(running()){accumulated=elapsed(now);started=-1;}else started=now;}
  void reset(){accumulated=0;started=-1;}
  static String format(long millis){long seconds=Math.max(0,millis)/1000;return String.format(java.util.Locale.getDefault(),"%02d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);}
 }
}
