package com.ansub.tvhome;
import android.content.SharedPreferences;
import java.util.Map;

/** Repair only retired bundled selections; do not clear media, layouts or preference stores. */
final class WallpaperCatalogPreferences {
 static void repair(SharedPreferences prefs){
  Map<String,String> changes=BundledWallpaperMigration.changes(prefs.getAll());
  if(changes.isEmpty())return;
  SharedPreferences.Editor edit=prefs.edit();
  for(Map.Entry<String,String> entry:changes.entrySet())edit.putString(entry.getKey(),entry.getValue());
  edit.apply();
 }
}
