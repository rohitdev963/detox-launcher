package com.ansub.tvhome;
import java.util.*;

/** Actual catalog lookups reuse immutable choices, while menu arrays remain independently mutable. */
public final class GamingCatalogCacheTest {
 static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
 public static void main(String[] args){
  Set<String> ids=new HashSet<>();int count=0;
  for(String group:new String[]{GamingBackgroundCatalog.IMAGES,GamingBackgroundCatalog.LIVE,GamingBackgroundCatalog.GAMING}){
   GamingBackgroundCatalog.Choice[] first=GamingBackgroundCatalog.entries(group),second=GamingBackgroundCatalog.entries(group);
   check(first!=second,"menu arrays must be caller-owned");
   for(int i=0;i<first.length;i++){
    GamingBackgroundCatalog.Choice choice=first[i];count++;
    check(ids.add(choice.id),"duplicate background "+choice.id);
    check(choice==second[i],"immutable catalog choice need not be rebuilt");
    for(int repeat=0;repeat<1000;repeat++)check(GamingBackgroundCatalog.find(choice.id)==choice,"lookup must reuse the same choice");
    check(GamingBackgroundCatalog.groupOf(choice.id).equals(group),"correct group");
   }
   if(first.length>0){first[0]=null;check(GamingBackgroundCatalog.entries(group)[0]!=null,"caller mutation leaked into catalog");}
  }
  check(count==5,"3 original images, 1 live video and 1 Gaming video");
  check(GamingBackgroundCatalog.find(null)==null&&GamingBackgroundCatalog.find("missing")==null,"unknown lookup");
  check(GamingBackgroundCatalog.groupOf(null).isEmpty()&&GamingBackgroundCatalog.entries("missing").length==0,"unknown group");
  check(GamingBackgroundCatalog.groupOf(GamingBackgroundCatalog.SAME_HOME).equals("home"),"Home remains dynamic");
  Map<String,Object> saved=new HashMap<>();saved.put("gamingBackgroundSource",GamingBackgroundCatalog.SAME_HOME);saved.put("wallpaper",BundledWallpaperCatalog.NAMES[0]);
  String before=GamingBackgroundCatalog.resolve(saved,null).source;saved.put("wallpaper",BundledWallpaperCatalog.NAMES[1]);
  check(!before.equals(GamingBackgroundCatalog.resolve(saved,null).source),"cached built-ins must not freeze Home-follow resolution");
  System.out.println("Public Gaming catalog: 5 unique cached choices, 5000 identity checks, isolated menu arrays and dynamic Home-follow passed");
 }
}
