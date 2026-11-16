package com.ansub.tvhome;
import java.nio.file.*;

/** Source contracts complement the executed pure policies; these are not device UI tests. */
public final class PremiumClockIntegrationTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static String method(String s,String name){int a=s.indexOf("private void "+name+"()");check(a>=0,"missing "+name);int b=s.indexOf("\n    private ",a+20);return s.substring(a,b<0?s.length():b);}
 public static void main(String[] args)throws Exception{
  Path main=Path.of(args[0]);Path dir=main.getParent();String s=Files.readString(main);
  String style=method(s,"showClockOptions");
  check(style.contains("ClockStylePreview.show")&&style.contains("putString(\"clockStyle\",style)"),"actual selector save binding");
  check(style.contains("routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP))"),"BACK immediate parent");
  check(SettingsMenuRoutes.find("clock.date").parent.equals(SettingsMenuRoutes.CLOCK_GROUP)&&SettingsMenuRoutes.find("clock.display").parent.equals(SettingsMenuRoutes.CLOCK_GROUP),"clock settings routing");
  String visibility=method(s,"applyClockVisibility");
  check(visibility.contains("percent==0?View.INVISIBLE:View.VISIBLE")&&visibility.contains("nativeClockFace.setAlpha(alpha)"),"hidden clock cannot retain invisible focus/frame work");
  String apply=method(s,"applyClockStyle");
  for(String token:new String[]{"clockSizeScale()","ClockFacePolicy.wide(style)","faceWidth,faceHeight","clockOptions.date","clockOptions.seconds","setFormat12Hour","setFormat24Hour"})check(apply.contains(token),"clock layout/format integration "+token);
  String position=method(s,"applyClockPosition");check(position.contains("clockLayout.getWidth()")&&position.contains("clockLayout.getHeight()")&&position.contains("clockPosition"),"measured clock placement retained");
  String display=method(s,"showClockDisplayOptions");
  check(display.contains("ClockFacePolicy.accent(face)")&&display.contains("ClockFacePolicy.glow(face)")&&display.contains("ClockFacePolicy.background(face)"),"capability-gated controls");
  String preview=Files.readString(dir.resolve("ClockStylePreview.java"));
  check(!preview.contains(".edit()")&&preview.contains(".setPositiveButton(\"Save\"")&&preview.contains(".setNegativeButton(\"Cancel\""),"preview is nonpersisting until Save");
  check(preview.contains("state.cancel()")&&preview.contains("commit.accept(state.save())")&&preview.contains("face.setActive(false)"),"preview transaction cleanup");
  check(preview.contains("MenuNavigationPolicy")&&preview.contains("ACTION_HOVER_ENTER")&&preview.contains("dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus()"),"remote/mouse/save focus routes");
  String nativeView=Files.readString(dir.resolve("NativeClockFaceView.java"));
  for(String token:new String[]{"Intent.ACTION_TIME_CHANGED","Intent.ACTION_TIMEZONE_CHANGED","Intent.ACTION_LOCALE_CHANGED","unregisterReceiver","removeFrameCallback","removeCallbacks(tick)","hasWindowFocus()","isShown()","getAlpha()>0f","areAnimatorsEnabled()","PremiumClockSchedule.needsFrames","GamingPerformancePolicy.frameIntervalNanos"})check(nativeView.contains(token),"clock lifecycle "+token);
  String painter=Files.readString(dir.resolve("PremiumClockPainter.java"));
  for(String id:ClockFacePolicy.KEYS)if(ClockFacePolicy.premium(id))check(painter.contains("case \""+id+"\":"),"missing live face "+id);
  check(painter.contains("t.digitChanged(i)")&&painter.contains("minuteProgress("),"flip/progress real data");
  check(!painter.contains("Bitmap")&&!painter.substring(painter.indexOf("void draw(")).contains("new LinearGradient"),"no bitmap or gradient allocation in drawing");
  check(!painter.contains("temperature")&&!painter.contains("batteryPercent(")&&!painter.contains("weatherText("),"no invented weather or unreliable battery rendered");
  check(!painter.contains("MediaPlayer")&&!painter.contains("Visualizer"),"ambient bars do not imply playback measurement");
  System.out.println("Premium clock integration: all live face cases, real save binding, parent Back, visibility, measured sizing, gated controls, lifecycle, remote/mouse and truthful data fallbacks passed (source contracts)");
 }
}
