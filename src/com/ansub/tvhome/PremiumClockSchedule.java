package com.ansub.tvhome;

/** Pure scheduling contract shared by renderer and regression tests. */
final class PremiumClockSchedule {
 static float flipDuration(PremiumClockConfig options){return 180+240*options.motion;}
 static boolean needsFrames(String face,PremiumClockConfig options,boolean reduced,long now,PremiumClockTime time){
  if(!options.animated(reduced))return false;
  if("premium_flip".equals(face))return !time.digits.equals(time.previous)&&now>=time.changedAt&&now-time.changedAt<flipDuration(options);
  if(ClockFacePolicy.analog(face))return options.seconds;
  return "premium_neo".equals(face)||"premium_anime".equals(face)||"premium_hud".equals(face)||"premium_progress".equals(face)||"premium_neon".equals(face)||"premium_glass".equals(face);
 }
 static long nextDelay(String face,long now,PremiumClockConfig options){
  return PremiumClockConfig.nextDelay(now,options.seconds||"premium_progress".equals(face));
 }
}
