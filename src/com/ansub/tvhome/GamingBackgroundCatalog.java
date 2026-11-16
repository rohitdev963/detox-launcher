package com.ansub.tvhome;
import java.util.*;

/** Shared media references only; no copied assets or writes to Home preferences. */
final class GamingBackgroundCatalog {
 static final String SAME_HOME="@home",IMAGES="images",LIVE="live",GAMING="gaming";
 static String canonical(String id){return "asset:gaming/videos/call-of-duty-soldier.mp4".equals(id)?"asset:wallpapers/live-videos/call-of-duty-soldier.mp4":id;}
 static final class Choice {
  final String id,name,kind,source;final boolean followsHome;
  Choice(String id,String name,String kind,String source,boolean home){this.id=id;this.name=name;this.kind=kind;this.source=source;followsHome=home;}
  boolean video(){return "video".equals(kind);}
 }
 private static final Map<String,Choice[]> GROUPS=new LinkedHashMap<>();
 private static final Map<String,Choice> CHOICES=new HashMap<>();
 private static final Map<String,String> CHOICE_GROUPS=new HashMap<>();
 static {
  for(String group:new String[]{IMAGES,LIVE,GAMING}){
   Choice[] choices=createEntries(group);GROUPS.put(group,choices);
   for(Choice choice:choices){CHOICES.put(choice.id,choice);CHOICE_GROUPS.put(choice.id,group);}
  }
 }
 // Choices are immutable and built once. Callers can reorder their own returned array safely.
 static Choice[] entries(String group){Choice[] choices=GROUPS.get(group);return choices==null?new Choice[0]:choices.clone();}
 private static Choice[] createEntries(String group){
  ArrayList<Choice> list=new ArrayList<>();
  if(IMAGES.equals(group))for(int i=0;i<BundledWallpaperCatalog.size();i++){
   String source="asset:"+BundledWallpaperCatalog.assetAt(i);list.add(new Choice("image:"+source,BundledWallpaperCatalog.NAMES[i],"image",source,false));
  }
  else if(LIVE.equals(group))for(int i=0;i<BuiltInLiveVideoCatalog.size();i++){
   String source="asset:"+BuiltInLiveVideoCatalog.assetAt(i);list.add(new Choice(source,BuiltInLiveVideoCatalog.nameAt(i),"video",source,false));
  }
  else if(GAMING.equals(group))for(int i=0;i<GamingThemeCatalog.NAMES.length;i++){
   String source=GamingThemeCatalog.source(i);list.add(new Choice(source,GamingThemeCatalog.NAMES[i],"video",source,false));
  }
  return list.toArray(new Choice[0]);
 }
 static Choice find(String id){return CHOICES.get(id);}
 static String groupOf(String id){if(SAME_HOME.equals(id))return "home";String group=CHOICE_GROUPS.get(id);return group==null?"":group;}
 static String savedId(Map<String,?> prefs){
  String current=canonical(string(prefs,"gamingBackgroundSource",""));
  if(SAME_HOME.equals(current)||find(current)!=null)return current;
  if(!current.isEmpty())return GamingThemeCatalog.source(0);
  String legacy=canonical(string(prefs,"gamingVideoSource",""));
  if(legacy.isEmpty())return GamingThemeCatalog.source(0);
  if(legacy.startsWith("asset:")&&find(legacy)==null)return GamingThemeCatalog.source(0);
  return legacy;
 }
 static Choice resolve(Map<String,?> prefs,String override){
  String id=override==null?savedId(prefs):canonical(override);
  if(SAME_HOME.equals(id))return home(prefs);
  Choice c=find(id);if(c!=null)return c;
  // Preserve a legacy imported video URI/path, without adding an unrelated import menu.
  if(id!=null&&!id.isEmpty()&&!id.startsWith("asset:")&&!id.startsWith("image:"))return new Choice(id,"Imported Gaming video","video",id,false);
  return entries(GAMING)[0];
 }
 static Choice home(Map<String,?> prefs){
  String asset=string(prefs,"liveWallpaperVideoAsset","");
  String file=string(prefs,"liveWallpaperVideoPath","");String uri=string(prefs,"liveWallpaperVideoUri","");
  if(bool(prefs,"liveWallpaperEnabled",false)){
   if(BuiltInLiveVideoCatalog.containsAsset(asset))return new Choice(SAME_HOME,"Same as Home · "+BuiltInLiveVideoCatalog.nameForAsset(asset),"video","asset:"+asset,true);
   if(readableFile(file))return new Choice(SAME_HOME,"Same as Home · Imported video","video","file:"+file,true);
   if(!uri.isEmpty())return new Choice(SAME_HOME,"Same as Home · Imported video","video",uri,true);
  }
  int index=BundledWallpaperCatalog.selected(string(prefs,"wallpaper",BundledWallpaperCatalog.NAMES[0]));
  if(index==BundledWallpaperCatalog.size()){
   file=string(prefs,"photoFile","");uri=string(prefs,"photoUri","");
   if(readableFile(file))return new Choice(SAME_HOME,"Same as Home · Photo","image","file:"+file,true);
   if(!uri.isEmpty())return new Choice(SAME_HOME,"Same as Home · Photo","image",uri,true);
   index=0;
  }
  return new Choice(SAME_HOME,"Same as Home · "+BundledWallpaperCatalog.NAMES[index],"image","asset:"+BundledWallpaperCatalog.assetAt(index),true);
 }
 static boolean fill(Map<String,?> prefs,Choice c){return c.followsHome?(c.video()?bool(prefs,"liveWallpaperCrop",true):false):bool(prefs,"gamingBackgroundFill",true);}
 static float zoom(Map<String,?> prefs,Choice c){return c.followsHome?number(prefs,c.video()?"liveWallpaperCropScale":"wallpaperFrameZoom",1,1,c.video()?2.5f:2f):1;}
 static float position(Map<String,?> prefs,Choice c,boolean x){return c.followsHome&&!c.video()?number(prefs,x?"wallpaperFrameX":"wallpaperFrameY",.5f,0,1):.5f;}
 private static boolean readableFile(String value){if(value.isEmpty())return false;java.io.File file=new java.io.File(value);return file.isFile()&&file.length()>0;}
 static boolean homeKey(String key){return "wallpaper".equals(key)||"photoFile".equals(key)||"photoUri".equals(key)||key!=null&&(key.startsWith("wallpaperFrame")||"liveWallpaperEnabled".equals(key)||"liveWallpaperVideoAsset".equals(key)||"liveWallpaperVideoPath".equals(key)||"liveWallpaperVideoUri".equals(key)||"liveWallpaperCrop".equals(key)||"liveWallpaperCropScale".equals(key));}
 static String string(Map<String,?> p,String key,String fallback){Object value=p.get(key);return value instanceof String?(String)value:fallback;}
 static boolean bool(Map<String,?> p,String key,boolean fallback){Object value=p.get(key);return value instanceof Boolean?(Boolean)value:fallback;}
 static float number(Map<String,?> p,String key,float fallback,float min,float max){Object value=p.get(key);float n=value instanceof Number?((Number)value).floatValue():fallback;return Float.isNaN(n)||Float.isInfinite(n)?fallback:Math.max(min,Math.min(max,n));}
}
