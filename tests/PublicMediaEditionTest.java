package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;

public final class PublicMediaEditionTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent();
  check(BundledWallpaperCatalog.size()==3&&BuiltInLiveVideoCatalog.size()==1&&GamingThemeCatalog.NAMES.length==1,"public catalog inventory");
  check(!GamingMusicCatalog.available()&&GamingMusicCatalog.NAMES.length==0,"no bundled music");
  String activity=Files.readString(dir.resolve("GamingActivity.java")),player=Files.readString(dir.resolve("GamingMusicPlayer.java"));
  check(activity.contains("music=GamingMusicCatalog.available()?new GamingMusicPlayer(this):null;"),"silent edition must not instantiate a player or acquire audio focus");
  check(activity.contains("musicButton=null;if(GamingMusicCatalog.available())"),"silent edition must not display a nonfunctional music toggle");
  int a=activity.indexOf("private void gamingAudioSettings()"),b=activity.indexOf("@Override protected void onResume()",a);String audio=activity.substring(a,b);
  check(audio.contains("if(GamingMusicCatalog.available()){rows.add(")&&audio.contains("rows.add(new SettingsChoice(\"sounds\""),"music/volume hidden while navigation sounds remain");
  check(player.contains("if(!GamingMusicCatalog.available()||!value")&&player.contains("GamingMusicCatalog.available()&&foreground"),"empty music catalog cannot open playback");
  check(activity.contains("showVideoFallback(owner[0])")&&activity.contains("owner!=videoBackground||backgroundLayer==null"),"decoder failure uses owner-scoped still fallback");
  int fallbackStart=activity.indexOf("private void showVideoFallback("),fallbackEnd=activity.indexOf("private View gameCard(",fallbackStart);
  String fallback=activity.substring(fallbackStart,fallbackEnd);check(fallback.contains("clearBackgroundMedia()")&&fallback.contains("new GamingImageBackground")&&!fallback.contains("prefs.edit()"),"unsupported video must show a real still without changing saved selections");
  Map<String,Object> saved=new HashMap<>();saved.put("wallpaper","Photo");saved.put("photoUri","content://user/image");saved.put("liveWallpaperVideoUri","content://user/video");saved.put("gamingApps",new HashSet<>(Arrays.asList("user.game")));
  check(BundledWallpaperMigration.changes(saved).isEmpty(),"public defaults cannot reset imported media/library preferences");
  System.out.println("Public edition: original catalogs, no empty-player/toggle, preserved navigation sounds and imported settings passed");
 }
}
