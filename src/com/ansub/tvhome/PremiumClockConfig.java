package com.ansub.tvhome;

/** Immutable options; existing style/colour/size/position/visibility keys remain authoritative. */
final class PremiumClockConfig {
 final boolean date,seconds; final String format; final int accent; final float glow,motion,background;
 PremiumClockConfig(boolean date,boolean seconds,String format,int accent,int glow,int motion,int background){
  this.date=date;this.seconds=seconds;this.format="12".equals(format)||"24".equals(format)?format:"system";
  this.accent=accent;this.glow=percent(glow);this.motion=percent(motion);this.background=percent(background);
 }
 private static float percent(int p){return Math.max(0,Math.min(100,p))/100f;}
 boolean use24(boolean system24){return "24".equals(format)||"system".equals(format)&&system24;}
 boolean animated(boolean reduced){return !reduced&&motion>0;}
 static float minuteProgress(int minute,int second,int millis){return Math.max(0,Math.min(1,(minute+second/60f+millis/60000f)/60f));}
 static long nextDelay(long now,boolean seconds){long period=seconds?1000:60000;return period-Math.floorMod(now,period);}
}
