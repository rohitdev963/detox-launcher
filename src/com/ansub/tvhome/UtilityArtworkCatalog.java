package com.ansub.tvhome;

/** Utility artwork ownership only; never replaces installed-app artwork or live indicators. */
final class UtilityArtworkCatalog {
 static final String[] STEMS={"tool","headphone","picture","setting","plus","color-palette","fluent-bluetooth","fluent-apps","fluent-home","fluent-restart","fluent-power","fluent-more","simple-google-play"};
 static String home(String control){
  if(control==null)return null;
  switch(control){
   case "quick-panel":return "tool";
   case "quick:sound":return "headphone";
   case "quick:picture":return "picture";
   case "quick:bluetooth":return "fluent-bluetooth";
   case "settings":case "tv-settings":return "setting";
   case "add":return "plus";
   case "all-apps":return "fluent-apps";
   case "tv-restart":return "fluent-restart";
   case "screen-sleep":return "fluent-power";
   case "com.android.vending":return "simple-google-play";
   default:return null;
  }
 }
 static String rail(String control){
  if("home".equals(control))return "fluent-home";
  if("add".equals(control))return "plus";
  if("theme".equals(control))return "color-palette";
  if("more".equals(control))return "fluent-more";
  return null;
 }
 static boolean preserveColors(String stem){return stem!=null&&(!stem.startsWith("fluent-")&&!stem.startsWith("simple-")||"fluent-apps".equals(stem)||"fluent-home".equals(stem));}
 static String asset(String stem){
  for(String name:STEMS)if(name.equals(stem)){
   if(name.startsWith("fluent-"))return "ui/utility-fluent/"+name.substring(7)+".webp";
   if(name.startsWith("simple-"))return "ui/utility-simpleicons/"+name.substring(7)+".webp";
   return "ui/3dicons/"+name+"-dynamic-color.webp";
  }
  return null;
 }
 private UtilityArtworkCatalog(){}
}
