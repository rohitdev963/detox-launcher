package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class GamingBackgroundRemovalTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent();String game=Files.readString(dir.resolve("GamingActivity.java"));
  int a=game.indexOf("private void gamingBackgroundSettings()"),b=game.indexOf("private void gamingAudioSettings()",a);check(a>=0&&b>a,"Background menu");String menu=game.substring(a,b);
  List<String> ids=new ArrayList<>();Matcher choices=Pattern.compile("new SettingsChoice\\(\"([^\"]+)\"").matcher(menu);while(choices.find())ids.add(choices.group(1));
  check(ids.equals(Arrays.asList("theme","contrast")),"exactly two retained Background controls");
  for(String removed:new String[]{"Interface accent","Ambient decoration","Generated backgrounds","Game lighting","scenePicker(","GamingLivingAtmosphere","DetoxShaderView","DetoxGpuSupport","postFrameCallback","drawLine(","drawArc(","class Atmosphere"})check(!game.contains(removed),"retired behavior still wired: "+removed);
  check(game.contains("showSelectedBackground();")&&game.contains("GamingImageBackground")&&game.contains("createVideoBackground(choice.source)")&&game.contains("Background contrast"),"real image/video and contrast preserved");
  check(game.contains("clearRetiredGamingBackgroundSettings();appArtworkResolver=")&&!game.contains("getBoolean(\"gamingShaderBackground\"")&&!game.contains("getInt(\"gamingTheme\""),"old preferences cannot activate a removed renderer/accent");
  check(game.contains("GamingIconGlow")&&game.contains("DetoxPointerFocus.install")&&game.contains("Menu motion")&&game.contains("gamingMusicEnabled"),"focus/pointer/motion/music remain functional");
  for(String removed:new String[]{"GamingLivingAtmosphere.java","graphics/DetoxShaderView.java","graphics/DetoxGpuSupport.java"})check(!Files.exists(dir.resolve(removed)),"unused renderer file remains");
  Map<String,Object> prefs=new HashMap<>();
  for(String key:new String[]{"gamingTheme","gamingIntensity","gamingLivingScenes","gamingLivingMood","gamingLivingEffect","gamingLivingIntensity","gamingShaderBackground","gamingShaderScene","gamingScene_com.example.game","detoxExtensionsShader","detoxShaderScene"})prefs.put(key,1);
  Map<String,Object> retained=new HashMap<>();for(String key:new String[]{"gamingApps","gamingHidden","gamingFavorites","gamingBackgroundSource","gamingBackgroundReadability","gamingBackgroundFill","gamingMusicEnabled","gamingMotion","gamingSortOrder","gamingSceneDescription","gamingThemeVideo","wallpaper","fontColor","themePreset","clockStyle","profileStore"})retained.put(key,"saved "+key);prefs.putAll(retained);
  prefs.put("gamingNight",true);
  Set<String> removed=GamingBackgroundRetirementPolicy.keysToRemove(prefs.keySet());check(removed.size()==12,"exact retirement boundaries");for(String key:removed)prefs.remove(key);check(prefs.equals(retained),"unrelated media, library or launcher data changed");check(GamingBackgroundRetirementPolicy.keysToRemove(prefs.keySet()).isEmpty(),"repeat cleanup not idempotent");
  check(!game.contains("getBoolean(\"gamingNight\"")&&!game.contains("putBoolean(\"gamingNight\""),"retired Game Night cannot silently filter the library");
  for(String key:removed)check(!HomeProfilePolicy.isProfileSetting(key),"saved profiles could revive removed effects");
  check(HomeProfilePolicy.isProfileSetting("gamingBackgroundSource")&&HomeProfilePolicy.isProfileSetting("gamingMusicEnabled"),"legitimate profile settings lost");
  String cleanup=Files.readString(dir.resolve("RetiredPluginDataCleanup.java"));check(!cleanup.contains("putBoolean(\"gamingShaderBackground\"")&&!cleanup.contains("putInt(\"gamingShaderScene\""),"old plugin migration could restore retired flags");
  System.out.println("Gaming Background removal: exactly two controls, no grid/particle/shader loops, preserved media/focus/audio, scoped idempotent preference cleanup and profile isolation passed");
 }
}
