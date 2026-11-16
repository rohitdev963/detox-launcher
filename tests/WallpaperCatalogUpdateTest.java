package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/** Public samples and preference preservation across edition changes. */
public final class WallpaperCatalogUpdateTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent(),root=dir.resolve("../../../..").normalize();
  check(BundledWallpaperCatalog.size()==3&&BundledWallpaperCatalog.NAMES.length==4,"three original stills plus Photo");
  Set<String> paths=new HashSet<>();
  for(int i=0;i<BundledWallpaperCatalog.size();i++){
   String name=BundledWallpaperCatalog.NAMES[i],asset=BundledWallpaperCatalog.assetAt(i);
   check(paths.add(asset)&&asset.startsWith("public/wallpapers/"),"only unique public still paths");
   check(BundledWallpaperCatalog.selected(name)==i,"stable saved-name roundtrip");
   BufferedImage image=ImageIO.read(root.resolve("assets").resolve(asset).toFile());
   check(image!=null&&image.getWidth()==1920&&image.getHeight()==1080,"original still decodes as landscape 1080p");
   check(!BundledWallpaperCatalog.landscapeRotation(i),"public originals need no rotation");
  }
  check(BundledWallpaperCatalog.selected("Photo")==BundledWallpaperCatalog.size(),"Photo import remains distinct");
  for(String previous:new String[]{"Krishna","Manarola Blue Hour","Midnight Mountain","Spider-Man Midnight","unknown"}){
   Map<String,Object> prefs=sample();prefs.put("wallpaper",previous);Map<String,Object> before=new HashMap<>(prefs);
   Map<String,String> changes=BundledWallpaperMigration.changes(prefs);
   check(changes.size()==1&&changes.get("wallpaper").equals(BundledWallpaperCatalog.NAMES[0]),"old still falls back to original sample only");
   prefs.putAll(changes);check(BundledWallpaperMigration.changes(prefs).isEmpty(),"still repair idempotent");preserved(before,prefs,changes.keySet());
  }
  for(boolean imported:new boolean[]{false,true}){
   Map<String,Object> prefs=sample();if(!imported){prefs.remove("liveWallpaperVideoPath");prefs.remove("liveWallpaperVideoUri");}
   prefs.put("liveWallpaperVideoAsset","wallpapers/live-videos/earth-orbital-view.mp4");Map<String,Object> before=new HashMap<>(prefs);
   Map<String,String> changes=BundledWallpaperMigration.changes(prefs);check(changes.size()==2,"repair only bundled video identity/name");prefs.putAll(changes);
   if(imported)check("".equals(prefs.get("liveWallpaperVideoAsset")),"imported video keeps precedence over retired bundled asset");
   else check(BuiltInLiveVideoCatalog.containsAsset((String)prefs.get("liveWallpaperVideoAsset")),"retired bundled video uses actual public sample");
   check(BundledWallpaperMigration.changes(prefs).isEmpty(),"video repair idempotent");preserved(before,prefs,changes.keySet());
  }
  check(BundledWallpaperMigration.changes(sample()).isEmpty(),"Photo and imported video unchanged");
  String main=Files.readString(Path.of(args[0])),service=Files.readString(dir.resolve("DetoxLiveWallpaperService.java")),gaming=Files.readString(dir.resolve("GamingVideoBackground.java")),pacing=Files.readString(dir.resolve("VideoDisplayPacing.java"));
  check(main.contains("WallpaperCatalogPreferences.repair(")&&service.contains("WallpaperCatalogPreferences.repair("),"both runtime entry points repair selections");
  check(main.contains("VideoDisplayPacing.prepared(mp,videoWallpaperSurface,false)")&&service.contains("VideoDisplayPacing.prepared(mp,getSurfaceHolder().getSurface(),true)")&&gaming.contains("VideoDisplayPacing.prepared(p,getHolder().getSurface(),true)"),"shared display pacing preserved");
  check(pacing.contains("Build.VERSION.SDK_INT<30")&&pacing.contains("Build.VERSION.SDK_INT<23")&&pacing.contains("!directlyDisplayed")&&pacing.contains("SYNC_SOURCE_VSYNC")&&pacing.contains("setFrameRate(0f,")&&pacing.contains("catch(RuntimeException"),"API guards and current-display pacing preserved");
  check(!pacing.contains("setSpeed(")&&!pacing.contains("preferredDisplayModeId")&&!pacing.contains("postFrameCallback"),"no acceleration or forced refresh mode");
  System.out.println("Public wallpapers: landscape decode, stable names, isolated/idempotent repair, user-media preservation and guarded pacing passed");
 }
 static void preserved(Map<String,Object> before,Map<String,Object> after,Set<String> changed){for(String key:before.keySet())if(!changed.contains(key))check(Objects.equals(before.get(key),after.get(key)),"unrelated preference changed: "+key);}
 static Map<String,Object> sample(){Map<String,Object> p=new HashMap<>();p.put("wallpaper","Photo");p.put("photoFile","/user/pictures/private.jpg");p.put("photoUri","content://photos/42");p.put("liveWallpaperVideoPath","/user/videos/imported.mp4");p.put("liveWallpaperVideoUri","content://videos/3");p.put("liveWallpaperEnabled",true);p.put("clockStyle","premium_glass");p.put("apps","user.apps");return p;}
}
