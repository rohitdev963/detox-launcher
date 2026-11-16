package com.ansub.tvhome;
import java.util.Map;

/** Shared Home/Gaming icon preference ownership; no parallel Gaming-only icon keys. */
final class AppIconSettingsPolicy {
 static final class Option {
  final String id,title,key;final int fallback;final String[] labels;final int[] values;
  Option(String id,String title,String key,int fallback,String[] labels,int[] values){this.id=id;this.title=title;this.key=key;this.fallback=fallback;this.labels=labels;this.values=values;}
  int selected(int saved){int best=0;for(int i=0;i<values.length;i++)if(Math.abs(values[i]-saved)<Math.abs(values[best]-saved))best=i;return best;}
 }
 static final Option SIZE=new Option("size","Icon Size","appIconScalePercent",100,new String[]{"Compact · 60%","Small · 75%","Medium · 90%","Full · 100%"},new int[]{60,75,90,100});
 static final Option VISIBILITY=new Option("visibility","Icon Visibility","appArtworkIdlePercent",85,new String[]{"20% while idle","25% while idle","30% while idle","40% while idle","55% while idle","70% while idle","85% while idle","100% while idle"},new int[]{20,25,30,40,55,70,85,100});
 static final Option LABELS=new Option("labels","App Labels","homeAppLabels",0,new String[]{"On focus","Always visible","Hidden"},new int[]{0,1,2});
 static final Option TILES=new Option("tiles","Tile Style","homeTileStyle",1,new String[]{"Floating artwork","Framed tiles","Soft glass"},new int[]{0,1,2});
 static final Option LAYOUT=new Option("layout","Icon Layout","appLayoutMode",0,new String[]{"Balanced · 6 across","Comfortable · 4 larger tiles","More apps · 8 across"},new int[]{0,1,2});
 static final Option CONTRAST=new Option("contrast","High-contrast Focus","highContrastFocus",0,new String[]{"Off","On · bright outline"},new int[]{0,1});
 static final Option GLOW=new Option("glow","Focus Glow","homeFocusPulse",1,new String[]{"Steady","Breathing"},new int[]{0,1});
 static Option find(String id){for(Option o:new Option[]{SIZE,VISIBILITY,LABELS,TILES,LAYOUT,CONTRAST,GLOW})if(o.id.equals(id))return o;return null;}
 static int read(Map<String,?> saved,Option option){Object value=saved.get(option.key);int n=value instanceof Number?((Number)value).intValue():value instanceof Boolean?((Boolean)value?1:0):option.fallback;int min=option.values[0],max=option.values[option.values.length-1];return Math.max(min,Math.min(max,n));}
 static float opacity(Map<String,?> saved,boolean focused){return focused?1:read(saved,VISIBILITY)/100f;}
 static boolean artworkKey(String key){return "iconPackPackage".equals(key)||key!=null&&(key.startsWith("customAppIcon:")||key.startsWith("customAppIconFile:"));}
 static boolean presentationKey(String key){if(artworkKey(key))return true;for(Option o:new Option[]{SIZE,VISIBILITY,LABELS,TILES,LAYOUT,CONTRAST,GLOW})if(o.key.equals(key))return true;return false;}
 static String signature(Map<String,?> saved){java.util.TreeMap<String,Object> values=new java.util.TreeMap<>();for(Map.Entry<String,?> e:saved.entrySet())if(presentationKey(e.getKey()))values.put(e.getKey(),e.getValue());return values.toString();}
 static int motionMode(boolean hasPreset,int preset,int legacy){if(hasPreset){preset=Math.max(0,Math.min(3,preset));return preset==3?2:preset==2?1:0;}return Math.max(0,Math.min(2,legacy));}
 static boolean breathing(Map<String,?> saved){return read(saved,GLOW)==1&&motionMode(saved.containsKey("launcherMotionPreset"),integer(saved,"launcherMotionPreset",0),integer(saved,"launcherAnimationMode",0))==0;}
 private static int integer(Map<String,?> saved,String key,int fallback){Object n=saved.get(key);return n instanceof Number?((Number)n).intValue():fallback;}
}
