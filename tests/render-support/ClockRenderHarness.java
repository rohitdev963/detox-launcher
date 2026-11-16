package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.Typeface;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Executes PremiumClockPainter directly. This is layout evidence, not Android GPU validation. */
public final class ClockRenderHarness {
 private static int rendered;
 public static void main(String[] args)throws Exception{
  Path out=Path.of(args[0]);Files.createDirectories(out);
  Typeface font=new Typeface(Font.createFont(Font.TRUETYPE_FONT,Path.of(args[1]).toFile()));
  Calendar time=Calendar.getInstance(TimeZone.getTimeZone("UTC"));time.clear();time.set(2026,Calendar.NOVEMBER,15,22,8,36);long instant=time.getTimeInMillis();
  BufferedImage sheet=new BufferedImage(1800,1950,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setColor(new Color(0x0A101D));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
  g.setColor(new Color(0xF4F5FA));g.setFont(new Font("SansSerif",Font.BOLD,28));g.drawString("DETOX / 12 PREMIUM CLOCK FACES",36,47);
  g.setFont(new Font("SansSerif",Font.PLAIN,16));g.setColor(new Color(0xA6B6C9));g.drawString("Exact production painter · Java2D layout preview · Android font/shadow/GPU output requires device verification",36,78);
  int at=0;
  for(int i=5;i<ClockFacePolicy.KEYS.length;i++){
   String face=ClockFacePolicy.KEYS[i];
   PremiumClockConfig options=new PremiumClockConfig(true,ClockFacePolicy.analog(face),"12",0xFF89DCE9,45,55,85);
   BufferedImage image=render(face,time,instant,font,options,1.5f,false,0xFFF1EDF8);
   int col=at%3,row=at/3,x=col*600+18,y=row*455+110;g.setColor(new Color(0x151D2C));g.fillRoundRect(x,y,564,438,24,24);
   int showWidth=ClockFacePolicy.wide(face)?510:354,showHeight=ClockFacePolicy.wide(face)?298:354;
   g.drawImage(image,x+(564-showWidth)/2,y+18,showWidth,showHeight,null);
   g.setColor(new Color(0xF4F5FA));g.setFont(new Font("SansSerif",Font.BOLD,20));g.drawString(String.format("%02d  %s",at+1,ClockFacePolicy.NAMES[i]),x+22,y+399);
   g.setColor(new Color(0x8E9CB2));g.setFont(new Font("SansSerif",Font.PLAIN,14));g.drawString(face,x+22,y+422);
   ImageIO.write(image,"png",out.resolve(face+".png").toFile());at++;
   for(float scale:new float[]{.7f,1,1.3f,2})for(boolean seconds:new boolean[]{false,true})for(boolean reduced:new boolean[]{false,true}){
    PremiumClockConfig variant=new PremiumClockConfig(at%2==0,seconds,"24",0,45,55,at%2==0?0:100);
    render(face,time,instant,font,variant,scale,reduced,at%2==0?0xFF1E2330:0xFFF3F2EB);
   }
  }
  g.dispose();ImageIO.write(sheet,"png",out.resolve("clock-faces-layout-preview.png").toFile());
  verifyFlip(out,time,instant,font);
  Files.writeString(out.resolve("render-results.txt"),"PASS: "+rendered+" painter executions; 12 faces; scales 0.7 / 1 / 1.3 / 2; date, seconds, reduced motion, transparent/opaque backgrounds and dark/light ink; no drawing exception, unbalanced Canvas or edge clipping.\nFlip transition frames additionally verify stable unchanged-digit pixels and changing flap pixels in 12/24-hour and seconds modes.\nHost graphics adapter only. Android text shadow/GPU, focus, smoothness and TV legibility are not verified.\n");
  System.out.println(Files.readString(out.resolve("render-results.txt")));
 }
 private static void verifyFlip(Path out,Calendar time,long instant,Typeface font)throws Exception{
  for(boolean use24:new boolean[]{false,true})for(boolean seconds:new boolean[]{false,true}){
   PremiumClockConfig config=new PremiumClockConfig(true,seconds,use24?"24":"12",0,45,55,100);
   PremiumClockTime strings=new PremiumClockTime();strings.update(instant,time.getTimeZone(),Locale.US,use24,seconds);
   long changed=instant+(seconds?1000:60000);strings.update(changed,time.getTimeZone(),Locale.US,use24,seconds);
   PremiumClockPainter painter=new PremiumClockPainter();BufferedImage first=null;boolean changedPixels=false;
   for(int offset:new int[]{0,60,150,240,500}){
    BufferedImage frame=new BufferedImage(480,280,BufferedImage.TYPE_INT_ARGB);Canvas canvas=new Canvas(frame);
    painter.draw(canvas,"premium_flip",time,changed+offset,strings,config,0xFFF3F2FA,font,false);canvas.close();rendered++;
    if(first==null)first=frame;else{
     // The leftmost hour card is unchanged at this minute/second boundary.
     int start=seconds?39:53,card=seconds?45:70;
     for(int y=68;y<182;y++)for(int x=start+2;x<start+card-2;x++)if(frame.getRGB(x,y)!=first.getRGB(x,y))throw new AssertionError("unchanged flap animated");
     for(int y=68;y<182;y++)for(int x=350;x<438;x++)if(frame.getRGB(x,y)!=first.getRGB(x,y))changedPixels=true;
    }
    if(!use24&&!seconds)ImageIO.write(frame,"png",out.resolve("flip-transition-"+offset+"ms.png").toFile());
   }
   if(!changedPixels)throw new AssertionError("changed flap has no visible transition");
  }
 }
 private static BufferedImage render(String face,Calendar time,long instant,Typeface font,PremiumClockConfig options,float scale,boolean reduced,int ink){
  int w=Math.round((ClockFacePolicy.wide(face)?480:360)*scale),h=Math.round((ClockFacePolicy.wide(face)?280:360)*scale);
  BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
  Canvas canvas=new Canvas(image);canvas.scale(scale,scale);PremiumClockTime strings=new PremiumClockTime();
  strings.update(instant,time.getTimeZone(),Locale.US,options.use24(false),options.seconds);
  new PremiumClockPainter().draw(canvas,face,time,instant,strings,options,ink,font,reduced);canvas.close();
  if(canvas.operations<2)throw new AssertionError("empty face: "+face);
  for(int x=0;x<w;x++)if((image.getRGB(x,0)>>>24)>8||(image.getRGB(x,h-1)>>>24)>8)throw new AssertionError("vertical clipping: "+face);
  for(int y=0;y<h;y++)if((image.getRGB(0,y)>>>24)>8||(image.getRGB(w-1,y)>>>24)>8)throw new AssertionError("horizontal clipping: "+face);
  rendered++;return image;
 }
}
