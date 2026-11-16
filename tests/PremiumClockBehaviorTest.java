package com.ansub.tvhome;

import java.util.*;
import java.nio.file.*;

/** Actual shared time/preview/scheduling policies executed on the host JVM. */
public final class PremiumClockBehaviorTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void near(float a,float b){check(Math.abs(a-b)<.001f,a+" != "+b);}
 public static void main(String[] args)throws Exception{
  String[] existing={"digital","orbit","words","worlddial","squareworld"};
  String[] premium={"premium_glass","premium_minimal_digital","premium_neo","premium_anime","premium_classic","premium_anime_analog","premium_hud","premium_flip","premium_weather","premium_progress","premium_neon","premium_minimal_analog"};
  check(ClockFacePolicy.KEYS.length==17&&ClockFacePolicy.NAMES.length==17,"17 persisted styles");
  Set<String> ids=new HashSet<>();for(String id:ClockFacePolicy.KEYS)check(ids.add(id)&&id.equals(ClockFacePolicy.normalise(id)),"stable unique ID");
  for(int i=0;i<5;i++)check(ClockFacePolicy.KEYS[i].equals(existing[i]),"existing style order/ID");
  for(String id:premium){check(ClockFacePolicy.premium(id),"new ID "+id);check(HomeProfilePolicy.isProfileSetting("clockStyle"),"profile retains style");
   ClockPreviewState preview=new ClockPreviewState("orbit");preview.select(id);check(preview.candidate().equals(id)&&preview.saved.equals("orbit"),"browse preserves saved");
   check(preview.cancel().equals("orbit"),"cancel restores existing face");preview.select(id);check(preview.candidate().equals("orbit"),"dismissed preview stays closed");
   preview=new ClockPreviewState("digital");preview.select(id);String saved=preview.save();check(new ClockPreviewState(saved).candidate().equals(id),"save/reopen");}
  check(ClockFacePolicy.normalise(null).equals("digital")&&ClockFacePolicy.normalise("old-unknown").equals("digital"),"existing fallback");
  for(String id:premium)check(ClockFacePolicy.analog(id)==(id.endsWith("analog")||id.equals("premium_classic")),"analog classification "+id);
  PremiumClockConfig defaults=new PremiumClockConfig(true,false,"system",0,45,55,85);
  PremiumClockConfig stopped=new PremiumClockConfig(false,true,"12",0,-10,0,150);
  check(defaults.use24(true)&&!defaults.use24(false)&&!stopped.use24(true),"system / forced12");
  check(new PremiumClockConfig(true,false,"24",0,0,0,0).use24(false),"forced24");near(stopped.glow,0);near(stopped.background,1);
  check(!defaults.animated(true)&&!stopped.animated(false)&&defaults.animated(false),"reduced/intensity0");
  PremiumClockTime time=new PremiumClockTime();TimeZone utc=TimeZone.getTimeZone("UTC");Locale locale=Locale.US;
  time.update(0,utc,locale,false,false);check(time.digits.equals("12:00")&&time.meridiem.equals("AM"),"midnight12");
  time.update(43200000,utc,locale,false,true);check(time.digits.equals("12:00:00")&&time.meridiem.equals("PM"),"noon12");
  time.update(86399000,utc,locale,true,true);check(time.digits.equals("23:59:59")&&time.meridiem.isEmpty(),"24-hour seconds");
  time.update(86400000,utc,locale,true,true);check(time.digits.equals("00:00:00"),"midnight rollover");
  time.update(0,TimeZone.getTimeZone("Asia/Kolkata"),locale,true,false);check(time.digits.equals("05:30"),"timezone change invalidates cached time");
  time.update(0,TimeZone.getTimeZone("America/Los_Angeles"),locale,true,false);check(time.digits.equals("16:00")&&time.dateText.contains("31 Dec"),"previous calendar day in local zone");
  Calendar c=Calendar.getInstance(utc);c.clear();c.set(2026,Calendar.MARCH,8,6,59,59);
  time.update(c.getTimeInMillis(),TimeZone.getTimeZone("America/New_York"),locale,true,true);check(time.digits.equals("01:59:59"),"pre DST");
  time.update(c.getTimeInMillis()+1000,TimeZone.getTimeZone("America/New_York"),locale,true,true);check(time.digits.equals("03:00:00"),"DST jump");
  time.update(0,utc,Locale.JAPAN,true,false);check(!time.dateText.isEmpty(),"localized date");
  time.update(0,utc,locale,true,false);time.update(60000,utc,locale,true,false);
  check(!time.digitChanged(0)&&!time.digitChanged(1)&&!time.digitChanged(2)&&!time.digitChanged(3)&&time.digitChanged(4),"only changed flip digit");
  check(time.glyph(4,false).equals("1")&&time.glyph(4,true).equals("0"),"cached flap glyphs");
  check(PremiumClockSchedule.needsFrames("premium_flip",defaults,false,60100,time),"flip animates change");
  check(!PremiumClockSchedule.needsFrames("premium_flip",defaults,false,61000,time),"flip stops after transition");
  for(String id:premium){check(!PremiumClockSchedule.needsFrames(id,defaults,true,60100,time),"reduced no frames "+id);check(!PremiumClockSchedule.needsFrames(id,stopped,false,60100,time),"intensity0 no frames "+id);}
  check(!PremiumClockSchedule.needsFrames("premium_minimal_digital",defaults,false,60100,time),"static digital no frames");
  check(!PremiumClockSchedule.needsFrames("premium_classic",defaults,false,60100,time),"hidden seconds no frames");
  PremiumClockConfig seconds=new PremiumClockConfig(true,true,"system",0,45,55,85);
  check(PremiumClockSchedule.needsFrames("premium_classic",seconds,false,60100,time),"visible sweep frame loop");
  check(PremiumClockSchedule.nextDelay("premium_minimal_digital",1234,defaults)==58766,"minute alignment");
  check(PremiumClockSchedule.nextDelay("premium_classic",1234,seconds)==766,"second alignment");
  check(PremiumClockSchedule.nextDelay("premium_progress",1234,defaults)==766,"real progress without smooth motion");
  near(PremiumClockConfig.minuteProgress(0,0,0),0);near(PremiumClockConfig.minuteProgress(30,0,0),.5f);check(PremiumClockConfig.minuteProgress(59,59,999)<1,"hour boundary");
  near(ClockMechanics.hourAngle(3,30,30),105.25f);near(ClockMechanics.minuteAngle(30,30),183);near(ClockMechanics.secondAngle(59,999),359.994f);
  for(String key:new String[]{"clockDateVisible","clockSecondsVisible","clockTimeFormat","clockAccentColor","clockGlowPercent","clockAnimationPercent","clockBackgroundPercent"})check(HomeProfilePolicy.isProfileSetting(key),"profile ownership "+key);
  System.out.println("Premium clocks: 12 stable IDs + 5 preserved, preview transactions, timezone/DST/format, flip digits, progress, reduced motion and scheduling passed");
 }
}
