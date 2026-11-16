package com.ansub.tvhome;
import java.util.*;
public final class SettingsCatalogueTest {
 public static void main(String[] args){
  String[] expected={"Wallpaper & Ambient","Appearance & Fonts","Apps & Home Layout","Animations & Focus","Clock & Utilities","System & Tools","Phone Companion"};
  if(!Arrays.equals(expected,SettingsMenuRoutes.categoryNames()))throw new AssertionError("exactly seven requested categories");
  Set<String> ids=new HashSet<>(),handlers=new HashSet<>();int leaves=0;
  for(SettingsMenuRoutes.Entry e:SettingsMenuRoutes.all()){
   if(!ids.add(e.id)||e.label.isEmpty())throw new AssertionError("duplicate/empty entry");
   if(e.group()&&SettingsMenuRoutes.children(e.id).length==0)throw new AssertionError("placeholder group "+e.id);
   if(!e.group()){leaves++;if(!handlers.add(e.handler))throw new AssertionError("duplicate canonical editor "+e.handler);}
   String category=SettingsMenuRoutes.categoryOf(e.id);if(SettingsMenuRoutes.categoryIndex(category)<0)throw new AssertionError("unreachable "+e.id);
   SettingsMenuRoutes.Entry[] rows=SettingsMenuRoutes.children(e.id);Collections.shuffle(Arrays.asList(rows),new Random(9));
   for(SettingsMenuRoutes.Entry row:rows)if(SettingsMenuRoutes.find(row.id)!=row||!row.parent.equals(e.id))throw new AssertionError("reorder changed dispatch/parent");
  }
  if(!SettingsMenuRoutes.find("apps.down").handler.equals("showHomeDownLaunchOptions"))throw new AssertionError("DOWN mapping");
  if(!SettingsMenuRoutes.find("style.color").parent.equals(SettingsMenuRoutes.STYLE))throw new AssertionError("global theme owner");
  if(!SettingsMenuRoutes.find("utility.opacity").parent.equals(SettingsMenuRoutes.UTILITIES))throw new AssertionError("opacity owner");
  if(SettingsMenuRoutes.find("tools.extensions")!=null||SettingsMenuRoutes.find("aquarium")!=null)throw new AssertionError("removed features restored");
  System.out.println("Settings catalogue: seven categories, "+leaves+" unique canonical actions, complete parents, reorder-safe IDs passed");
 }
}
