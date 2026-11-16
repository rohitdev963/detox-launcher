package com.ansub.tvhome;
import java.util.Calendar;
import java.util.TimeZone;
public final class ClockMechanicsTest {
 public static void main(String[] args) {
  if(ClockFacePolicy.KEYS.length!=17)throw new AssertionError("five existing plus twelve premium clocks");
  for(String key:ClockFacePolicy.KEYS)if(!key.equals(ClockFacePolicy.normalise(key)))throw new AssertionError("selection lost");
  near(ClockMechanics.hourAngle(0,0,0),0);near(ClockMechanics.hourAngle(12,0,0),0);
  near(ClockMechanics.hourAngle(3,30,0),105);near(ClockMechanics.minuteAngle(30,30),183);
  near(ClockMechanics.secondAngle(59,500),357);
  Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));c.setTimeInMillis(0);
  if(c.get(Calendar.HOUR_OF_DAY)!=0)throw new AssertionError();
  c.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
  if(c.get(Calendar.HOUR_OF_DAY)!=5||c.get(Calendar.MINUTE)!=30)throw new AssertionError("device-zone interpretation");
  for(int h=0;h<24;h++)for(int m=0;m<60;m++){
   ClockMechanics.Words w=ClockMechanics.words(h,m);
   if(w.lit.length!=110||w.remainder!=m%5)throw new AssertionError("grid bounds/remainder");
   String expected=ClockMechanics.HOUR_WORDS[((h+(m>=35?1:0))+11)%12];
   if(!w.phrase.endsWith(expected+(m<5?" O'CLOCK":"")))throw new AssertionError(w.phrase);
   if(!w.lit[0]||!w.lit[3])throw new AssertionError("IT IS");
   for(int row=0;row<10;row++)if(ClockMechanics.GRID[row].length()!=11)throw new AssertionError("row width");
  }
  if(!ClockMechanics.words(23,59).phrase.equals("IT IS FIVE TO TWELVE"))throw new AssertionError("midnight carry");
  if(!ClockMechanics.words(12,0).phrase.equals("IT IS TWELVE O'CLOCK"))throw new AssertionError("noon");
  if(ClockMechanics.nextDelay("words",59999,false,16)!=1||ClockMechanics.nextDelay("worlddial",999,true,16)!=1)throw new AssertionError("boundary schedule");
  if(ClockMechanics.nextDelay("orbit",1,false,23)!=23)throw new AssertionError("preset cap");
  for(int fps:new int[]{30,45,60}){
   long next=0,interval=Math.round(1000000000d/fps);int frames=0;
   for(int vsync=0;vsync<600;vsync++){
    long frame=1000000000L+Math.round(vsync*1000000000d/60d);
    if(frame>=next){frames++;next=ClockMechanics.nextFrameTime(next,frame,interval);}
   }
   if(Math.abs(frames-fps*10)>1)throw new AssertionError("vsync pacing "+fps+": "+frames);
  }
  if(ClockMechanics.nextFrameTime(10,1000000000L,20)!=1000000000L)throw new AssertionError("long stall catch-up bounded");
  System.out.println("Clock mechanics: 1440 word mappings, rollover, angles, timezone and pacing passed");
 }
 private static void near(float a,float b){if(Math.abs(a-b)>.001f)throw new AssertionError(a+" != "+b);}
}
