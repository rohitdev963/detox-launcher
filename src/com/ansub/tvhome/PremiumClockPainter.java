package com.ansub.tvhome;

import android.graphics.*;
import java.util.Calendar;

/** Original resolution-independent vector faces. All paths/shaders are retained, no bitmap/blur loop. */
final class PremiumClockPainter {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
 private final RectF bounds=new RectF();
 private final Path ridge=new Path(),branch=new Path(),petal=new Path(),hand=new Path();
 private Shader spectrum,sky,landscape,glass;
 private int cachedAccent;private boolean shadersReady;
 private Typeface font;private PremiumClockConfig options;private int ink,accent;
 private float phase;private boolean moving;private String dateText;
 private static final int WHITE=0xFFF5F4FA;
 PremiumClockPainter(){
  ridge.moveTo(0,210);ridge.lineTo(70,140);ridge.lineTo(110,174);ridge.lineTo(205,71);ridge.lineTo(299,178);ridge.lineTo(352,130);ridge.lineTo(480,219);ridge.lineTo(480,280);ridge.lineTo(0,280);ridge.close();
  branch.moveTo(0,18);branch.cubicTo(63,14,92,43,148,63);branch.moveTo(70,29);branch.cubicTo(93,5,137,12,168,10);branch.moveTo(103,48);branch.cubicTo(116,76,155,86,174,82);
  petal.moveTo(0,-5);petal.cubicTo(7,-6,8,4,0,7);petal.cubicTo(-8,4,-7,-6,0,-5);petal.close();
 }
 private void shaders(int color){if(shadersReady&&cachedAccent==color)return;cachedAccent=color;shadersReady=true;
  spectrum=new SweepGradient(180,180,new int[]{color,0xFFEAB4EC,0xFFAA95F1,color},null);
  sky=new LinearGradient(0,0,480,280,new int[]{0xFF101D42,0xFF404773,0xFFB78EA7},null,Shader.TileMode.CLAMP);
  landscape=new LinearGradient(0,0,0,280,new int[]{0xFF274D73,0xFF82A1AE,0xFF152D46},null,Shader.TileMode.CLAMP);
  glass=new LinearGradient(0,0,480,280,new int[]{color,0xFFB59BE8,0xFFF2B8D7},null,Shader.TileMode.CLAMP);
 }
 void draw(Canvas c,String face,Calendar time,long now,PremiumClockTime strings,PremiumClockConfig config,int color,Typeface typeface,boolean reduced){
  options=config;dateText=strings.dateText;font=typeface;ink=color;accent=config.accent==0?color:config.accent;shaders(accent);
  moving=config.animated(reduced);phase=moving?(float)((now*(double)(.3f+.7f*config.motion)%12000d)/12000d):0;
  p.setShader(null);p.clearShadowLayer();p.setStrokeCap(Paint.Cap.ROUND);
  switch(face){
   case "premium_glass":
    panel(c,18,30,462,250,28);reflection(c,24,36,456,95);
    digital(c,strings,240,150,78,412,207);line(c,184,224,296,224,2,tint(accent,130));break;
   case "premium_minimal_digital":
    digital(c,strings,240,147,89,456,214);line(c,192,177,288,177,2,accent);break;
   case "premium_neo":
    disc(c,180,180,159,opacity(0xE5101826));luminousRing(c,158,3);
    arc(c,146,-90,360,1,0x287ADFE5);digital(c,strings,180,184,64,260,229);break;
   case "premium_anime":
    scene(c,false,false);digital(c,strings,240,154,77,412,208);break;
   case "premium_classic":
    disc(c,180,180,169,opacity(0xE8151B27));luminousRing(c,167,1.4f);analog(c,time,false,false,now);break;
   case "premium_anime_analog":
    scene(c,true,false);luminousRing(c,166,2);analog(c,time,false,true,now);break;
   case "premium_hud":
    disc(c,180,180,163,opacity(0xDD091B26));
    for(int i=0;i<12;i++)arc(c,166,i*30-90,22,2,tint(accent,i%3==0?240:115));
    arc(c,154,-90+phase*360,82,2,accent);arc(c,154,90+phase*360,65,2,tint(accent,100));
    for(int i=0;i<60;i++)tick(c,i*6,144,i%5==0?137:141,i%5==0?2:1,tint(accent,130));
    text(c,"LOCAL TIME",180,114,12,accent,200);digital(c,strings,180,183,61,256,228);
    for(int i=0;i<5;i++)disc(c,156+i*12,266,2.2f,tint(accent,moving?110+(int)(100*(.5+.5*Math.sin(phase*6.28+i))):165));break;
   case "premium_flip":flip(c,strings,now);break;
   case "premium_weather":
    scene(c,false,true);digital(c,strings,240,151,78,412,213);break;
   case "premium_progress":
    disc(c,180,180,160,opacity(0xD5111D2A));arc(c,157,-90,360,6,0x284F6577);
    float seconds=time.get(Calendar.SECOND)+(moving?time.get(Calendar.MILLISECOND)/1000f:0);
    float progress=PremiumClockConfig.minuteProgress(time.get(Calendar.MINUTE),(int)seconds,moving?time.get(Calendar.MILLISECOND):0);
    p.setShader(spectrum);arc(c,157,-90,progress*360,6,WHITE);p.setShader(null);
    digital(c,strings,180,180,62,265,224);text(c,"MINUTES THIS HOUR",180,267,11,tint(ink,190),256);break;
   case "premium_neon":
    panel(c,18,22,462,258,27);digital(c,strings,240,128,76,410,177);
    // Decorative ambient bars, explicitly not playback/amplitude data.
    for(int i=0;i<19;i++){float h=8+(float)(.5+.5*Math.sin(i*.83+phase*6.28))*24*(moving?config.motion:0.45f);
     round(c,126+i*12,232-h,132+i*12,232,3,tint(accent,155));}break;
   case "premium_minimal_analog":analog(c,time,true,false,now);break;
   default:digital(c,strings,240,151,85,450,211);
  }
  p.setShader(null);p.clearShadowLayer();
 }
 private int opacity(int color){return tint(color,Math.round(Color.alpha(color)*options.background));}
 private static int tint(int color,int alpha){return (color&0xFFFFFF)|(Math.max(0,Math.min(255,alpha))<<24);}
 private void fill(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);}
 private void disc(Canvas c,float x,float y,float r,int color){fill(color);c.drawCircle(x,y,r,p);}
 private void round(Canvas c,float l,float t,float r,float b,float radius,int color){fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);}
 private void line(Canvas c,float x,float y,float x2,float y2,float weight,int color){p.setColor(color);p.setStrokeWidth(weight);c.drawLine(x,y,x2,y2,p);}
 private void arc(Canvas c,float r,float start,float sweep,float weight,int color){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(weight);p.setColor(color);bounds.set(180-r,180-r,180+r,180+r);c.drawArc(bounds,start,sweep,false,p);p.setStyle(Paint.Style.FILL);}
 private void luminousRing(Canvas c,float r,float weight){
  float breath=moving?1-options.motion*.15f+options.motion*.15f*(float)Math.sin(phase*6.28):1;
  for(int i=3;i>0;i--)arc(c,r,-90,360,weight+i*2,tint(accent,Math.round((10-i*2)*options.glow*breath)));
  p.setShader(spectrum);arc(c,r,-90,360,weight,tint(WHITE,220));p.setShader(null);
 }
 private void panel(Canvas c,float l,float t,float r,float b,float radius){
  round(c,l,t+4,r,b+4,radius,opacity(0x44000000));round(c,l,t,r,b,radius,opacity(0xD9172134));
  p.setStyle(Paint.Style.STROKE);p.setShader(glass);
  for(int i=3;i>=0;i--){p.setStrokeWidth(i==0?1.5f:2+i*2);p.setAlpha(i==0?210:Math.round(9*options.glow));c.drawRoundRect(l,t,r,b,radius,radius,p);}
  p.setShader(null);p.setAlpha(255);p.setStyle(Paint.Style.FILL);
 }
 private void reflection(Canvas c,float l,float t,float r,float b){round(c,l,t,r,b,23,opacity(tint(WHITE,moving?11+(int)(5*options.motion*Math.sin(phase*6.28)):11)));}
 private void text(Canvas c,String value,float x,float y,float size,int color,float maxWidth){
  fill(color);p.setTypeface(font);p.setFakeBoldText(size>=18);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
  float measured=p.measureText(value);if(measured>maxWidth)p.setTextSize(size*maxWidth/measured);
  // A small fixed text shadow protects transparency without offscreen bitmap blurs.
  p.setShadowLayer(1.5f,0,1,0xC0000000);c.drawText(value,x,y,p);p.clearShadowLayer();
 }
 private void digital(Canvas c,PremiumClockTime t,float x,float y,float size,float maxWidth,float dateY){
  text(c,t.digits,x,y,size,ink,maxWidth);
  if(!t.meridiem.isEmpty())text(c,t.meridiem,x,y+23,14,tint(ink,220),maxWidth);
  if(options.date)text(c,t.dateText,x,dateY,18,tint(ink,220),maxWidth);
 }
 private void tick(Canvas c,float angle,float outer,float inner,float stroke,int color){
  int save=c.save();c.rotate(angle,180,180);line(c,180,180-inner,180,180-outer,stroke,color);c.restoreToCount(save);
 }
 private void analog(Canvas c,Calendar time,boolean minimal,boolean ornate,long now){
  for(int i=0;i<60;i++)if(!minimal||i%5==0)tick(c,i*6,151,i%5==0?139:147,i%5==0?2.6f:1,tint(ink,i%5==0?235:95));
  if(!minimal){text(c,"12",180,61,18,ink,50);text(c,"3",309,187,18,ink,40);text(c,"6",180,311,18,ink,40);text(c,"9",51,187,18,ink,40);}
  if(options.date)text(c,dateText,180,247,14,tint(ink,220),180);
  float seconds=time.get(Calendar.SECOND)+(moving?time.get(Calendar.MILLISECOND)/1000f:0);
  float hour=ClockMechanics.hourAngle(time.get(Calendar.HOUR_OF_DAY),time.get(Calendar.MINUTE),seconds);
  float minute=ClockMechanics.minuteAngle(time.get(Calendar.MINUTE),seconds);
  clockHand(c,hour,83,4.5f,minimal?ink:WHITE,ornate);clockHand(c,minute,125,2.8f,ink,ornate);
  if(options.seconds){int save=c.save();c.rotate(seconds*6,180,180);line(c,180,200,180,42,1.3f,accent);c.restoreToCount(save);}
  disc(c,180,180,6,0xFF101926);disc(c,180,180,3.5f,accent);
 }
 private void clockHand(Canvas c,float angle,float length,float half,int color,boolean ornate){
  int save=c.save();c.rotate(angle,180,180);hand.reset();hand.moveTo(180,180-length);hand.lineTo(180+half,166);hand.lineTo(180+half,190);hand.lineTo(180-half,190);hand.lineTo(180-half,166);hand.close();
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xA0000000);c.drawPath(hand,p);fill(color);c.drawPath(hand,p);
  if(ornate){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(accent);c.drawCircle(180,180-length*.66f,5,p);p.setStyle(Paint.Style.FILL);}c.restoreToCount(save);
 }
 private void scene(Canvas c,boolean circle,boolean weather){
  int save=c.save();Path clip=hand;clip.reset();if(circle)clip.addCircle(180,180,165,Path.Direction.CW);else{bounds.set(18,22,462,258);clip.addRoundRect(bounds,26,26,Path.Direction.CW);}c.clipPath(clip);
  if(circle){c.translate((360-480*(360f/280f))/2,0);c.scale(360f/280f,360f/280f);}
  p.setShader(weather?landscape:sky);fill(opacity(WHITE));c.drawRect(0,0,480,280,p);p.setShader(null);
  disc(c,weather?359:circle?300:365,58,weather?24:22,opacity(weather?0xFFEED5A7:0xFFF3E8EC));
  fill(opacity(weather?0xFF48667D:0xFF354163));c.drawPath(ridge,p);
  int layer=c.save();c.translate(-55,67);c.scale(1.3f,.85f);fill(opacity(0xFF192C46));c.drawPath(ridge,p);c.restoreToCount(layer);
  if(!weather){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(opacity(0xFF25223D));c.drawPath(branch,p);p.setStyle(Paint.Style.FILL);
   for(int i=0;i<16;i++){float x=22+i*9,y=20+(float)Math.sin(i*.76)*24+i*.28f;blossom(c,x,y,opacity(0xFFE3B1CF));}
   if(moving)for(int i=0;i<9;i++){float x=(i*67+phase*480)%480,y=(i*37+phase*280)%280;int petalSave=c.save();c.translate(x,y);c.rotate(i*31+phase*360);fill(opacity(0xB8F2BACE));c.drawPath(petal,p);c.restoreToCount(petalSave);}
  }
  // Local scrim keeps numerals legible over the scene, also respects panel transparency.
  round(c,49,81,431,227,30,opacity(0x70111B30));c.restoreToCount(save);
 }
 private void blossom(Canvas c,float x,float y,int color){for(int j=0;j<5;j++){double a=j*Math.PI*.4;disc(c,x+(float)Math.cos(a)*4,y+(float)Math.sin(a)*4,3.8f,color);}disc(c,x,y,1.4f,opacity(0xFFFFE2DC));}
 private void flip(Canvas c,PremiumClockTime t,long now){
  int count=t.digits.length();float card=count>5?45:70,gap=6,total=count*(card+gap)-gap,start=(480-total)/2;
  float progress=moving?Math.max(0,Math.min(1,(now-t.changedAt)/PremiumClockSchedule.flipDuration(options))):1;
  for(int i=0;i<count;i++){
   float x=start+i*(card+gap),center=x+card/2;
   if(t.digits.charAt(i)==':'){text(c,":",center,157,51,ink,card);continue;}
   round(c,x,72,x+card,188,10,opacity(0x55000000));round(c,x,68,x+card,182,10,opacity(0xFF18212E));
   round(c,x+1,69,x+card-1,123,9,opacity(0xFF2A3545));
   String digit=t.glyph(i,false);
   text(c,digit,center,154,count>5?58:76,ink,card-6);
   if(progress<1&&t.digitChanged(i)){
    int save=c.save();c.clipRect(x,125,x+card,182);round(c,x,68,x+card,182,10,opacity(0xFF18212E));text(c,t.glyph(i,true),center,154,count>5?58:76,ink,card-6);c.restoreToCount(save);
    if(progress<.5f){save=c.save();c.clipRect(x,68,x+card,125);round(c,x+1,69,x+card-1,123,9,opacity(0xFF2A3545));text(c,t.glyph(i,true),center,154,count>5?58:76,ink,card-6);c.restoreToCount(save);}
    save=c.save();c.clipRect(x,68,x+card,182);float sy=Math.abs(1-progress*2);c.scale(1,Math.max(.02f,sy),center,125);
    round(c,x,progress<.5f?68:125,x+card,progress<.5f?125:182,7,opacity(0xFF344052));
    text(c,progress<.5f?t.glyph(i,true):digit,center,154,count>5?58:76,ink,card-6);c.restoreToCount(save);
   }
   line(c,x+2,125,x+card-2,125,1.2f,0xCC060A10);
  }
  if(!t.meridiem.isEmpty())text(c,t.meridiem,240,210,14,ink,400);
  if(options.date)text(c,t.dateText,240,243,18,tint(ink,220),430);
 }
}
