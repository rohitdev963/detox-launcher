package com.ansub.tvhome;
import java.util.*;
import java.nio.file.*;

public final class GamingSettingsRedesignTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void order(List<String> actual,String... expected){check(actual.equals(Arrays.asList(expected)),"order "+actual);}
 public static void main(String[] args)throws Exception{
  Map<String,String> names=new HashMap<>();names.put("a","Zelda");names.put("b","Batman");names.put("c","Call of Duty");
  List<String> selected=Arrays.asList("a","b","c","removed.package"),home=Arrays.asList("c","b","a");Set<String> favorites=new HashSet<>(Arrays.asList("a"));Map<String,Object> prefs=new HashMap<>();
  order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"c","b","a");
  prefs.put("gamingOrder_a",-1);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"a","c","b");
  prefs.put("gamingSortOrder",1);prefs.put("gamingPlayed_b",9000000000L);prefs.put("gamingPlayed_c",7000000000L);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"b","c","a");
  prefs.put("gamingSortOrder",2);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"b","c","a");
  prefs.put("gamingSortOrder",3);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"a","c","b");
  prefs.put("gamingLibraryFilter",1);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"a");
  prefs.put("gamingLibraryFilter",2);order(GamingLibraryPolicy.order(selected,names,prefs,home,favorites),"c","b");
  Map<String,Object> before=new HashMap<>(prefs);GamingLibraryPolicy.order(selected,names,prefs,home,favorites);check(before.equals(prefs)&&selected.size()==4,"sorting/filtering cannot modify preferences/membership");
  favorites.clear();prefs.put("gamingLibraryFilter",1);check(GamingLibraryPolicy.order(selected,names,prefs,home,favorites).isEmpty(),"empty favorite filter safe");
  String g=Files.readString(Path.of(args[0]).getParent().resolve("GamingActivity.java"));
  for(String id:new String[]{"library","icons","background","audio","performance"})check(g.contains("case \""+id+"\":"),"named root dispatch "+id);
  for(String obsolete:new String[]{"Visual effects","Motion, sound & accent","Living Atmosphere","2D companions","Game Night ·"})check(!g.contains("\""+obsolete),"obsolete visible menu "+obsolete);
  check(g.contains("settingsFocus.put(title,id)")&&g.contains("KEYCODE_BUTTON_B")&&g.contains("BackGate"),"named focus restore, controller B and Back gate");
  check(g.contains("motionAllowed()&&prefs.getBoolean(\"gamingLaunchAnimation\",true)"),"launch animation toggle reaches actual transition");
  check(g.contains("case \"uninstall\":")&&!g.contains("scenePicker("),"Uninstall preserved; removed scene controls cannot remain as game-option aliases");
  check(!g.contains("java.util.function")&&!g.contains(".removeIf("),"new menus compatible with API 21 without core library desugaring");
  check(g.contains("setNegativeButton(\"Cancel\"")&&g.contains("putStringSet(\"gamingApps\",chosen).putStringSet(\"gamingHidden\",hidden)"),"visibility explicitly saves or cancels");
  for(String key:new String[]{"gamingSortOrder","gamingLibraryFilter","gamingLaunchAnimation"})check(HomeProfilePolicy.isProfileSetting(key),"new Gaming settings in profiles "+key);
  System.out.println("Gaming settings: real sorting/filter policies, retained membership, named menu/actions, launch toggle, Back/controller guards and API21 contracts passed");
 }
}
