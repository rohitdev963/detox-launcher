package com.ansub.tvhome;
import java.util.*;

/** Library ordering and filters never edit membership or erase hidden apps. */
final class GamingLibraryPolicy {
 static final String[] SORT_NAMES={"Home order + pinned games","Recently played","A–Z","Favorites first"};
 static final String[] FILTER_NAMES={"All visible games","Favorites only","Played games only"};
 static int clamp(int n,int length){return Math.max(0,Math.min(length-1,n));}
 static List<String> order(Collection<String> selected,Map<String,String> names,Map<String,?> saved,List<String> home,Set<String> favorites){
  int mode=clamp(number(saved,"gamingSortOrder",0),SORT_NAMES.length),filter=clamp(number(saved,"gamingLibraryFilter",0),FILTER_NAMES.length);
  Map<String,Integer> positions=new HashMap<>();for(int i=0;i<home.size();i++)positions.put(home.get(i),i);
  List<String> result=new ArrayList<>();for(String pkg:selected)if(names.containsKey(pkg)&&(filter!=1||favorites.contains(pkg))&&(filter!=2||played(saved,pkg)>0))result.add(pkg);
  Collections.sort(result,(a,b)->{
   int compare=0;
   if(mode==1)compare=Long.compare(played(saved,b),played(saved,a));
   else if(mode==3)compare=Boolean.compare(favorites.contains(b),favorites.contains(a));
   if(compare!=0)return compare;
   if(mode==0){compare=Integer.compare(number(saved,"gamingOrder_"+a,Integer.MAX_VALUE),number(saved,"gamingOrder_"+b,Integer.MAX_VALUE));if(compare!=0)return compare;}
   if(mode==0||mode==3){compare=Integer.compare(positions.containsKey(a)?positions.get(a):Integer.MAX_VALUE,positions.containsKey(b)?positions.get(b):Integer.MAX_VALUE);if(compare!=0)return compare;}
   if(mode==0){compare=Long.compare(played(saved,b),played(saved,a));if(compare!=0)return compare;}
   compare=names.get(a).compareToIgnoreCase(names.get(b));return compare!=0?compare:a.compareTo(b);
  });return result;
 }
 private static int number(Map<String,?> values,String key,int fallback){Object n=values.get(key);return n instanceof Number?((Number)n).intValue():fallback;}
 private static long played(Map<String,?> values,String pkg){Object n=values.get("gamingPlayed_"+pkg);return n instanceof Number?((Number)n).longValue():0;}
}
