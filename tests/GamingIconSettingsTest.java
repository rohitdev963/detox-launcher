package com.ansub.tvhome;
import java.util.*;
import java.nio.file.*;

public final class GamingIconSettingsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Map<String,Object> saved=new HashMap<>();check(AppIconSettingsPolicy.read(saved,AppIconSettingsPolicy.VISIBILITY)==85,"existing opacity default");
  check(AppIconSettingsPolicy.read(saved,AppIconSettingsPolicy.SIZE)==100,"existing size default");
  saved.put("appArtworkIdlePercent",10);check(AppIconSettingsPolicy.opacity(saved,false)==.2f&&AppIconSettingsPolicy.opacity(saved,true)==1f,"focused always fully visible, idle clamps");
  saved.put("appArtworkIdlePercent",140);check(AppIconSettingsPolicy.opacity(saved,false)==1f,"upper clamp");
  saved.put("homeFocusPulse",false);check(!AppIconSettingsPolicy.breathing(saved),"boolean false preserved");saved.put("homeFocusPulse",true);
  saved.put("launcherAnimationMode",2);check(!AppIconSettingsPolicy.breathing(saved),"legacy reduced motion");saved.put("launcherMotionPreset",0);check(AppIconSettingsPolicy.breathing(saved),"new motion preset owns precedence");
  saved.put("launcherMotionPreset",2);check(!AppIconSettingsPolicy.breathing(saved),"calm uses steady halo");saved.put("launcherMotionPreset",3);check(AppIconSettingsPolicy.motionMode(true,3,0)==2,"reduced suppresses animations");
  String before=AppIconSettingsPolicy.signature(saved);saved.put("gamingMusicPosition",40L);check(before.equals(AppIconSettingsPolicy.signature(saved)),"music must not rebuild Home icons");saved.put("iconPackPackage","pack.test");check(!before.equals(AppIconSettingsPolicy.signature(saved)),"pack changes rebuild Home on return");
  for(String id:new String[]{"style.iconPack","apps.iconSize","apps.tiles","font.appNames","animation.artwork"})check(SettingsMenuRoutes.find(id).parent.equals(SettingsMenuRoutes.ICONS),"canonical Icons destination "+id);
  check(SettingsMenuRoutes.find(SettingsMenuRoutes.ICONS).parent.equals(SettingsMenuRoutes.APPS),"Icons reachable under Apps");
  Path dir=Path.of(args[0]).getParent();String g=Files.readString(dir.resolve("GamingActivity.java")),home=Files.readString(Path.of(args[0])),glow=Files.readString(dir.resolve("GamingIconGlow.java"));
  check(g.contains("Map<String,?> iconSettings=prefs.getAll();")&&g.contains("image.setAlpha(AppIconSettingsPolicy.opacity(iconSettings,false))"),"new cached cards respect opacity using one shared settings snapshot");
  check(g.contains("generation!=gameArtworkGeneration")&&g.contains("appArtworkResolver.invalidate()"),"old pack callbacks cannot overwrite new pack");
  check(g.contains("registerOnSharedPreferenceChangeListener(iconPreferencesListener)")&&g.contains("unregisterOnSharedPreferenceChangeListener(iconPreferencesListener)"),"preference listener lifecycle");
  int a=g.indexOf("private void refreshGamingIcons("),b=g.indexOf("private void requestGameArtwork(",a);String row=g.substring(a,b);check(!row.contains("render();")&&!row.contains("replaceSelectedBackground()"),"icon settings cannot restart video or music");
  check(home.contains("lastHomeIconSettings.equals(AppIconSettingsPolicy.signature")&&home.contains("lastHomeIconSettings = AppIconSettingsPolicy.signature"),"Home applies shared settings on resume");
  check(glow.contains("onViewDetachedFromWindow")&&glow.contains("onWindowFocusChanged")&&glow.contains("animator.cancel()"),"halo lifecycle releases animation");
  check(!Files.exists(dir.resolve("../../../../assets/gaming/videos/call-of-duty-soldier.mp4").normalize()),"duplicate COD asset removed");
  System.out.println("Gaming icons: existing defaults/keys, opacity, motion precedence, Home resume, pack generations, lifecycle and dedup source contracts passed");
 }
}
