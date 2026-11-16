package com.ansub.tvhome;
import java.util.*;

/** Idempotent exact-selection repair; never deletes imported paths, URIs or other user settings. */
final class BundledWallpaperMigration {
 static Map<String,String> changes(Map<String,?> saved){
  Map<String,String> changes=new LinkedHashMap<>();
  Object still=saved.get("wallpaper");
  if(still instanceof String&&BundledWallpaperCatalog.selected((String)still)==0&&!BundledWallpaperCatalog.NAMES[0].equals(still))
   changes.put("wallpaper",BundledWallpaperCatalog.NAMES[0]);
  Object video=saved.get("liveWallpaperVideoAsset");
  if(video instanceof String&&!((String)video).isEmpty()&&!BuiltInLiveVideoCatalog.containsAsset((String)video)){
   // Imported media wins over an obsolete bundled selection; never erase its path/URI.
   boolean imported=nonempty(saved.get("liveWallpaperVideoPath"))||nonempty(saved.get("liveWallpaperVideoUri"));
   String fallback=imported?"":BuiltInLiveVideoCatalog.assetAt(0);
   changes.put("liveWallpaperVideoAsset",fallback);changes.put("liveWallpaperVideoName",BuiltInLiveVideoCatalog.nameForAsset(fallback));
  }
  return changes;
 }
 private static boolean nonempty(Object value){return value instanceof String&&!((String)value).isEmpty();}
}
