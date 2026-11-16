package com.ansub.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.view.View;
import android.view.Choreographer;
import android.animation.ValueAnimator;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Existing native faces plus twelve premium Canvas faces; no service or external renderer. */
final class NativeClockFaceView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final PremiumClockPainter premiumPainter=new PremiumClockPainter();
 private final PremiumClockTime premiumTime=new PremiumClockTime();
 private PremiumClockConfig options=new PremiumClockConfig(true,false,"system",0,45,55,85);
 private boolean system24;
 private final Rect visibleBounds=new Rect();
 private final Calendar time=Calendar.getInstance();
 private TimeZone zone=TimeZone.getDefault();
 private final Path map=new Path(),hand=new Path();
 private static final String[] DIGITS=new String[60];
 private static final float[] RADII={151,108,62};
 static {for(int i=0;i<60;i++)DIGITS[i]=i<10?"0"+i:Integer.toString(i);}
 private final float[] ringValues=new float[3];
 private String face="orbit";
 private int accent=0xFF7CE4F5,mode,preset=1;
 private boolean active,attached,pending,framePending,registered;
 private long nextFrameNanos;
 private Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
 private long wordMinute=Long.MIN_VALUE;
 private long describedMinute=Long.MIN_VALUE;
 private String zoneLabel="";
 private ClockMechanics.Words words;
 private final Runnable tick=()->{pending=false;if(running())invalidate();};
 private final Choreographer.FrameCallback frameTick=this::onFrame;
 private void onFrame(long frameTime){
  framePending=false;if(!running())return;
  float hz=getDisplay()==null?60f:getDisplay().getRefreshRate();
  long interval=GamingPerformancePolicy.frameIntervalNanos(preset,hz);
  if(frameTime>=nextFrameNanos){nextFrameNanos=ClockMechanics.nextFrameTime(nextFrameNanos,frameTime,interval);invalidate();}
  else queueFrame();
 }
 private void queueFrame(){if(!framePending){framePending=true;Choreographer.getInstance().postFrameCallback(frameTick);}}
 private final BroadcastReceiver clockChanges=new BroadcastReceiver(){
  @Override public void onReceive(Context context,Intent intent){syncZone();restart();}
 };
 NativeClockFaceView(Context context){super(context);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);buildMap();}
 void configure(String key,int color,Typeface typeface,int motionMode,int performancePreset){
  face=ClockFacePolicy.normalise(key);accent=color;font=typeface;mode=motionMode;preset=performancePreset;wordMinute=Long.MIN_VALUE;
  android.content.SharedPreferences prefs=getContext().getSharedPreferences("config",0);
  options=readOptions(prefs);system24=android.text.format.DateFormat.is24HourFormat(getContext());restart();
 }
 static PremiumClockConfig readOptions(android.content.SharedPreferences prefs){return new PremiumClockConfig(
  prefs.getBoolean("clockDateVisible",true),prefs.getBoolean("clockSecondsVisible",false),prefs.getString("clockTimeFormat","system"),
  prefs.getInt("clockAccentColor",0),prefs.getInt("clockGlowPercent",45),prefs.getInt("clockAnimationPercent",55),prefs.getInt("clockBackgroundPercent",85));}
 void setActive(boolean value){if(value&&!active)syncZone();active=value;restart();}
 void setClockTypeface(Typeface value){font=value;invalidate();}
 void motion(int motionMode,int performancePreset){mode=motionMode;preset=performancePreset;restart();}
 private boolean reducedMotion(){return mode!=0||Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled();}
 private boolean running(){return active&&attached&&getWindowVisibility()==VISIBLE&&hasWindowFocus()&&isShown()&&getAlpha()>0f&&getGlobalVisibleRect(visibleBounds);}
 private void restart(){if(pending){removeCallbacks(tick);pending=false;}if(framePending){Choreographer.getInstance().removeFrameCallback(frameTick);framePending=false;}nextFrameNanos=0;if(attached)invalidate();}
 private void syncZone(){zone=TimeZone.getDefault();time.setTimeZone(zone);system24=android.text.format.DateFormat.is24HourFormat(getContext());wordMinute=Long.MIN_VALUE;describedMinute=Long.MIN_VALUE;}
 @Override protected void onAttachedToWindow(){
  super.onAttachedToWindow();attached=true;
  IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_TIME_CHANGED);f.addAction(Intent.ACTION_TIMEZONE_CHANGED);f.addAction(Intent.ACTION_DATE_CHANGED);f.addAction(Intent.ACTION_LOCALE_CHANGED);
  if(Build.VERSION.SDK_INT>=33)getContext().registerReceiver(clockChanges,f,Context.RECEIVER_NOT_EXPORTED);else getContext().registerReceiver(clockChanges,f);
  registered=true;restart();
 }
 @Override protected void onDetachedFromWindow(){attached=false;restart();if(registered){getContext().unregisterReceiver(clockChanges);registered=false;}super.onDetachedFromWindow();}
 @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);restart();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);restart();}
 @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);restart();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);long now=System.currentTimeMillis();
  time.setTimeInMillis(now);int hour=time.get(Calendar.HOUR_OF_DAY),minute=time.get(Calendar.MINUTE),second=time.get(Calendar.SECOND),millis=reducedMotion()?0:time.get(Calendar.MILLISECOND);
  long slot=Math.floorDiv(now,60000L);
  if(describedMinute!=slot){describedMinute=slot;zoneLabel=time.getTimeZone().getDisplayName(time.get(Calendar.DST_OFFSET)!=0,TimeZone.SHORT);
   if(!"words".equals(face)&&getParent() instanceof View)((View)getParent()).setContentDescription(String.format(Locale.getDefault(),"%02d:%02d",hour,minute)+". Press OK for clock utilities");
  }
  boolean premium=ClockFacePolicy.premium(face)||"digital".equals(face);
  float designWidth=premium&&ClockFacePolicy.wide(face)?480:360,designHeight=premium&&ClockFacePolicy.wide(face)?280:360;
  int checkpoint=canvas.save();float scale=Math.min(getWidth()/designWidth,getHeight()/designHeight);canvas.translate((getWidth()-designWidth*scale)/2,(getHeight()-designHeight*scale)/2);canvas.scale(scale,scale);
  if(premium){premiumTime.update(now,zone,Locale.getDefault(),options.use24(system24),options.seconds);
   premiumPainter.draw(canvas,face,time,now,premiumTime,options,accent,font,reducedMotion());}
  else if("words".equals(face))drawWords(canvas,hour,minute,now);
  else if("orbit".equals(face))drawOrbit(canvas,hour,minute,second,millis);
  else drawDial(canvas,hour,minute,second,millis,"squareworld".equals(face));
  canvas.restoreToCount(checkpoint);
  if(running()){
   if(premium?PremiumClockSchedule.needsFrames(face,options,reducedMotion(),now,premiumTime):!reducedMotion()&&!"words".equals(face))queueFrame();
   else if(!pending){long delay=premium?PremiumClockSchedule.nextDelay(face,now,options):ClockMechanics.nextDelay(face,now,true,16);pending=true;postOnAnimationDelayed(tick,delay);}
  }
 }
 private void text(Canvas c,String value,float x,float y,float size,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTypeface(font);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);c.drawText(value,x,y,paint);}
 private void ring(Canvas c,float r,float width,int color){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);c.drawCircle(180,180,r,paint);paint.setStyle(Paint.Style.FILL);}
 private void dot(Canvas c,float x,float y,float r,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,paint);}
 private void drawOrbit(Canvas c,int hour,int minute,int second,int millis){
  dot(c,180,180,174,0xD9161D26);
  ring(c,171,1,0x60889BAD);ring(c,129,1,0x36889BAD);ring(c,83,1,0x36889BAD);
  // Fixed index at twelve; the current value rotates to this index on each ring.
  ringValues[0]=second+millis/1000f;ringValues[1]=minute+(second+millis/1000f)/60f;ringValues[2]=(hour%12)+minute/60f+second/3600f;
  for(int band=0;band<3;band++){
   int count=band==2?12:60;float step=360f/count;int selected=(int)ringValues[band];
   for(int i=0;i<count;i++){
    float angle=i*step-ringValues[band]*step;double rad=Math.toRadians(angle-90);
    float x=180+(float)Math.cos(rad)*RADII[band],y=180+(float)Math.sin(rad)*RADII[band];
    if(band<2&&i%5!=0&&i!=selected){dot(c,x,y,1,0x688DA0AF);continue;}
    int save=c.save();c.rotate(angle,x,y);
    text(c,DIGITS[band==2?(i==0?12:i):i],x,y+5,band==2?19:13,i==selected?accent:0xB8D8E1E8);c.restoreToCount(save);
   }
  }
  paint.setColor(accent);paint.setStrokeWidth(3);c.drawLine(180,1,180,12,paint);dot(c,180,180,6,accent);
 }
 private void drawWords(Canvas c,int hour,int minute,long now){
  long slot=Math.floorDiv(now,60000L);
  if(words==null||wordMinute!=slot){words=ClockMechanics.words(hour,minute);wordMinute=slot;
   // The parent is the focusable/clickable clock utility control.
   if(getParent() instanceof View)((View)getParent()).setContentDescription(words.phrase+", "+words.remainder+" additional minutes. Press OK for clock utilities");
  }
  for(int row=0;row<10;row++)for(int col=0;col<11;col++)text(c,ClockMechanics.GRID[row].substring(col,col+1),20+col*32,43+row*29,21,words.lit[row*11+col]?accent:0x507B8B9B);
  for(int i=0;i<4;i++)dot(c,153+i*18,337,3,i<words.remainder?accent:0x507B8B9B);
 }
 private void drawDial(Canvas c,int hour,int minute,int second,int millis,boolean square){
  paint.setStyle(Paint.Style.FILL);paint.setColor(square?0xDE09111A:0xE716226D);
  if(square)c.drawRoundRect(12,12,348,348,15,15,paint);else c.drawCircle(180,180,172,paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(0xA6D7E6EF);
  if(square)c.drawRoundRect(12,12,348,348,15,15,paint);else c.drawCircle(180,180,172,paint);
  paint.setStyle(Paint.Style.FILL);
  int mapSave=c.save();c.translate(103,square?215:140);c.scale(.52f,.52f);paint.setColor(0xB0D4E5F0);c.drawPath(map,paint);c.restoreToCount(mapSave);
  for(int i=0;i<60;i++){
   double a=Math.toRadians(i*6-90);float r=i%5==0?158:163;
   paint.setColor(i%5==0?0xDDE5EFF5:0x607F95AE);paint.setStrokeWidth(i%5==0?2:1);
   if(!square||i%5!=0)c.drawLine(180+(float)Math.cos(a)*r,180+(float)Math.sin(a)*r,180+(float)Math.cos(a)*168,180+(float)Math.sin(a)*168,paint);
  }
  if(square)for(int i=1;i<=12;i++){
   double a=Math.toRadians(i*30-90);int color=i%3==0?(i==12?0xFFFF755F:i==3?0xFF7FC4FF:i==6?0xFF8FE5AC:0xFFF5D96C):0xFFE8EFF5;
   text(c,Integer.toString(i),180+(float)Math.cos(a)*135,187+(float)Math.sin(a)*135,23,color);
  }
  text(c,square?"WORLD TIME":"WORLD DIAL",180,square?87:89,12,accent);
  float seconds=second+millis/1000f;
  drawHand(c,ClockMechanics.hourAngle(hour,minute,seconds),77,5,0xFFE8EFF5);
  drawHand(c,ClockMechanics.minuteAngle(minute,seconds),121,3,0xFFE8EFF5);
  paint.setColor(accent);paint.setStrokeWidth(1.4f);int save=c.save();c.rotate(ClockMechanics.secondAngle(second,millis),180,180);c.drawLine(180,198,180,28,paint);c.restoreToCount(save);dot(c,180,180,4,accent);
  if(!square)text(c,zoneLabel,180,282,11,0xBDE0EAF2);
 }
 private void drawHand(Canvas c,float angle,float length,float halfWidth,int color){
  int save=c.save();c.rotate(angle,180,180);hand.reset();hand.moveTo(180,180-length);hand.lineTo(180+halfWidth,180);hand.lineTo(180,194);hand.lineTo(180-halfWidth,180);hand.close();paint.setColor(color);paint.setStyle(Paint.Style.FILL);c.drawPath(hand,paint);c.restoreToCount(save);
 }
 private void buildMap(){
  // Original stylised continent silhouettes, intentionally not a geographic dataset.
  polygon(new float[]{0,18,25,4,52,8,72,27,62,43,39,45,27,65,17,45});
  polygon(new float[]{50,62,73,69,85,96,72,130,61,143,58,110,42,87});
  polygon(new float[]{122,20,147,11,175,21,187,9,215,20,240,12,276,27,298,54,280,70,255,57,235,84,217,68,202,73,184,54,161,51,145,40,132,44});
  polygon(new float[]{135,51,167,57,180,81,167,112,151,121,136,99,128,76});
  polygon(new float[]{248,105,272,96,291,111,284,131,258,136,241,122});
  polygon(new float[]{84,7,107,0,117,11,106,31,90,27});
 }
 private void polygon(float[] p){map.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)map.lineTo(p[i],p[i+1]);map.close();}
}
