package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;

public final class UtilityArtworkCatalogTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]).getParent(),root=source.getParent().getParent().getParent().getParent();
  Set<String> assets=new HashSet<>();
  for(String stem:UtilityArtworkCatalog.STEMS){String asset=UtilityArtworkCatalog.asset(stem);check(assets.add(asset),"duplicate artwork");check(Files.size(root.resolve("assets").resolve(asset))>100,"missing artwork "+asset);}
  check(assets.size()==13,"bounded local artwork catalogue");
  for(String control:new String[]{"quick-panel","quick:sound","quick:picture","quick:bluetooth","settings","tv-settings","add","all-apps","tv-restart","screen-sleep","com.android.vending"})check(UtilityArtworkCatalog.asset(UtilityArtworkCatalog.home(control))!=null,"unmapped existing utility "+control);
  check(UtilityArtworkCatalog.home("network")==null,"live network state must not become static artwork");
  for(String other:new String[]{"com.netflix.ninja","clock","weather","../plus","",null})check(UtilityArtworkCatalog.home(other)==null,"unrelated control mapped");
  for(String rail:new String[]{"home","add","theme","more"})check(UtilityArtworkCatalog.asset(UtilityArtworkCatalog.rail(rail))!=null,"existing right rail mapping");
  check(UtilityArtworkCatalog.rail("picture")==null&&UtilityArtworkCatalog.asset("../plus")==null&&UtilityArtworkCatalog.asset(null)==null,"unknown artwork safe fallback");
  for(String mono:new String[]{"fluent-bluetooth","fluent-restart","fluent-power","fluent-more","simple-google-play"})check(!UtilityArtworkCatalog.preserveColors(mono),"monochrome utility accent retained");
  for(String color:new String[]{"setting","plus","tool","fluent-home","fluent-apps"})check(UtilityArtworkCatalog.preserveColors(color),"native colors flattened");
  String main=Files.readString(source.resolve("MainActivity.java")),game=Files.readString(source.resolve("GamingActivity.java")),loader=Files.readString(source.resolve("BundledUtilityArtwork.java"));
  check(main.contains("if(preserveArtworkColors)iv.clearColorFilter()")&&main.contains("if(preserveArtworkColors)icon.clearColorFilter()"),"Home focus must retain full-color artwork");
  check(loader.contains("CACHE.get(stem)")&&loader.contains("bitmap.getWidth()!=256")&&loader.contains("new Artwork(context,bitmap"),"bounded shared bitmap and independent tint state");
  for(String[] route:new String[][]{{"home","finish"},{"add","addApps"},{"theme","showThemePicker"},{"more","libraryMenu"}})check(game.contains("railAction(\""+route[0]+"\",")&&game.contains("this::"+route[1]+")"),"rail action identity");
  check(game.contains("BundledUtilityArtwork.railLabel(this,id,glyph,dp(28))"),"existing rail TextView target retained");
  check(!loader.contains("SharedPreferences")&&!loader.contains("http")&&!loader.contains("animate("),"artwork does not own settings, network or animation loops");
  System.out.println("Utility artwork: 13 local assets, actual utility/rail mappings, live Network exclusion, accent policy, bounded cache and fallback contracts passed");
 }
}
