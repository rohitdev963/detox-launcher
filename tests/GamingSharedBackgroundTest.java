package com.ansub.tvhome;
import java.util.*;
import java.nio.file.*;

/** Executes the real shared catalogue/selection/geometry/preview policies on the host JVM. */
public final class GamingSharedBackgroundTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void near(float a,float b){check(Math.abs(a-b)<.001f,a+" != "+b);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent(),root=dir.resolve("../../../..").normalize();
  Set<String> ids=new HashSet<>(),paths=new HashSet<>();int count=0;
  for(String group:new String[]{GamingBackgroundCatalog.IMAGES,GamingBackgroundCatalog.LIVE,GamingBackgroundCatalog.GAMING}){
   GamingBackgroundCatalog.Choice[] choices=GamingBackgroundCatalog.entries(group);
   check(choices.length==(group.equals("images")?3:1),"group count "+group);
   List<GamingBackgroundCatalog.Choice> shuffled=new ArrayList<>(Arrays.asList(choices));Collections.shuffle(shuffled,new Random(42));
   for(GamingBackgroundCatalog.Choice c:shuffled){count++;check(ids.add(c.id)&&paths.add(c.source),"duplicate media/id");
    check(Files.isRegularFile(root.resolve("assets").resolve(c.source.substring(6))),"shared asset missing");
    check(GamingBackgroundCatalog.find(c.id).source.equals(c.source),"named lookup survives reorder");
    check(GamingBackgroundCatalog.groupOf(c.id).equals(group),"current category checkmark follows saved ID");
    Map<String,Object> p=home();Map<String,Object> before=new HashMap<>(p);p.put("gamingBackgroundSource",c.id);
    check(GamingBackgroundCatalog.savedId(p).equals(c.id),"persisted ID roundtrip");
    check(GamingBackgroundCatalog.resolve(p,null).source.equals(c.source),"selection resolves correct media");
    p.remove("gamingBackgroundSource");check(p.equals(before),"resolver modified Home/settings");
   }
  }
  check(count==5,"5 original background files plus Same as Home mode");
  Map<String,Object> p=home();check(GamingBackgroundCatalog.savedId(p).equals(GamingThemeCatalog.source(0)),"old default");
  for(int i=0;i<GamingThemeCatalog.NAMES.length;i++){p.put("gamingVideoSource",GamingThemeCatalog.source(i));check(GamingBackgroundCatalog.savedId(p).equals(GamingThemeCatalog.source(i)),"old Gaming selection preserved");}
  p.put("gamingVideoSource","asset:gaming/videos/call-of-duty-soldier.mp4");check(GamingBackgroundCatalog.savedId(p).equals(GamingThemeCatalog.source(0)),"removed personal clip resolves public fallback");
  p.put("gamingBackgroundSource","asset:gaming/videos/call-of-duty-soldier.mp4");check(GamingBackgroundCatalog.resolve(p,null).source.equals(GamingThemeCatalog.source(0)),"retired source uses public fallback");p.remove("gamingBackgroundSource");
  p.put("gamingVideoSource","content://saved-game-video/1");check(GamingBackgroundCatalog.resolve(p,null).source.equals("content://saved-game-video/1"),"legacy imported video preserved");
  p.put("gamingBackgroundSource","invalid-removed-id");check(GamingBackgroundCatalog.savedId(p).equals(GamingThemeCatalog.source(0)),"unknown new selection has safe fallback");
  p.put("gamingBackgroundSource",GamingBackgroundCatalog.SAME_HOME);Map<String,Object> saved=new HashMap<>(p);
  GamingBackgroundCatalog.Choice c=GamingBackgroundCatalog.resolve(p,null);check(c.followsHome&&!c.video()&&c.name.contains("Aurora Veil"),"follow static Home");
  check(!GamingBackgroundCatalog.fill(p,c),"Home image uses Home fit behavior");
  p.put("wallpaperFrameZoom",1.5f);p.put("wallpaperFrameX",.25f);near(GamingBackgroundCatalog.zoom(p,c),1.5f);near(GamingBackgroundCatalog.position(p,c,true),.25f);
  p.put("liveWallpaperEnabled",true);p.put("liveWallpaperVideoAsset",BuiltInLiveVideoCatalog.assetAt(0));p.put("liveWallpaperCrop",false);p.put("liveWallpaperCropScale",1.25f);
  c=GamingBackgroundCatalog.resolve(p,null);check(c.video()&&c.source.equals("asset:"+BuiltInLiveVideoCatalog.assetAt(0))&&c.id.equals("@home"),"follow current Home video");
  check(!GamingBackgroundCatalog.fill(p,c),"Home video framing followed");near(GamingBackgroundCatalog.zoom(p,c),1.25f);
  p.put("liveWallpaperVideoAsset","");p.put("liveWallpaperVideoPath","/missing/file.mp4");p.put("liveWallpaperVideoUri","content://home-video/7");
  check(GamingBackgroundCatalog.resolve(p,null).source.equals("content://home-video/7"),"unavailable file falls through to Home URI");
  p.put("liveWallpaperEnabled",false);p.put("wallpaper","Photo");p.put("photoFile","/missing/photo.jpg");p.put("photoUri","content://home-photo/8");
  check(GamingBackgroundCatalog.resolve(p,null).source.equals("content://home-photo/8"),"follow imported Home photo URI");
  p.put("photoFile",root.resolve("assets").resolve(BundledWallpaperCatalog.assetAt(0)).toString());check(GamingBackgroundCatalog.resolve(p,null).source.startsWith("file:"),"readable file has Home precedence");
  p.put("photoFile","");p.put("photoUri","");check(GamingBackgroundCatalog.resolve(p,null).source.equals("asset:"+BundledWallpaperCatalog.assetAt(0)),"missing Photo falls back to real bundled image");
  String image=GamingBackgroundCatalog.entries("images")[2].id;p.put("gamingBackgroundSource",image);p.put("gamingBackgroundFill",false);
  check(!GamingBackgroundCatalog.resolve(p,null).followsHome&&!GamingBackgroundCatalog.fill(p,GamingBackgroundCatalog.resolve(p,null)),"independent image/framing mode");
  p.put("wallpaper","Krishna");check(GamingBackgroundCatalog.resolve(p,null).id.equals(image),"Home changes do not override independent Gaming choice");
  for(String id:ids){GamingBackgroundPreviewState preview=new GamingBackgroundPreviewState("@home",id);check(preview.cancel().equals("@home"),"Cancel restores prior mode");
   preview=new GamingBackgroundPreviewState("@home",id);check(preview.apply().equals(id),"Apply returns candidate");try{preview.apply();throw new AssertionError("duplicate Apply accepted");}catch(IllegalStateException expected){}}
  check(HomeProfilePolicy.isProfileSetting("gamingBackgroundSource")&&HomeProfilePolicy.isProfileSetting("gamingBackgroundFill"),"existing profile ownership includes new settings");
  for(String key:new String[]{"wallpaper","photoFile","photoUri","liveWallpaperEnabled","liveWallpaperVideoAsset","liveWallpaperCrop","wallpaperFrameX"})check(GamingBackgroundCatalog.homeKey(key),"follow update key "+key);
  check(!GamingBackgroundCatalog.homeKey("gamingMusicPosition")&&!GamingBackgroundCatalog.homeKey("liveWallpaperMuted"),"unrelated/music/mute writes must not restart Gaming background");
  float[] fit=GamingBackgroundGeometry.matrix(3840,2160,1920,1080,true,1,.5f,.5f);near(fit[0],.5f);near(fit[1],0);near(fit[2],0);
  fit=GamingBackgroundGeometry.matrix(1000,2000,1920,1080,false,1,.5f,.5f);near(fit[0],.54f);near(fit[1],690);near(fit[2],0);
  fit=GamingBackgroundGeometry.matrix(1000,2000,1920,1080,true,1,.5f,.5f);near(fit[0],1.92f);near(fit[1],0);near(fit[2],-1380);
  fit=GamingBackgroundGeometry.matrix(1000,2000,3840,2160,true,1,.5f,.5f);near(fit[0],3.84f);
  fit=GamingBackgroundGeometry.matrix(1000,2000,1920,1080,true,Float.NaN,Float.NaN,Float.NaN);for(float f:fit)check(Float.isFinite(f),"corrupt framing safe");
  System.out.println("Gaming shared backgrounds: 5 unique original assets, stable save/restore, legacy sources, independent/follow-Home modes, preview Apply/Cancel, profile ownership and 1080p/4K fit/crop passed");
 }
 static Map<String,Object> home(){Map<String,Object> p=new HashMap<>();p.put("wallpaper","Aurora Veil");p.put("liveWallpaperEnabled",false);p.put("gamingMusicEnabled",true);p.put("clockStyle","premium_glass");return p;}
}
