package com.ansub.tvhome;
import java.util.*;import org.json.*;
/** Versioned launcher settings transfer envelope. */
final class LauncherBackupCodec {
 static final int FORMAT=4;
 static JSONObject create(Map<String,?> settings,String pkg,long now)throws JSONException{
  return new JSONObject().put("format",FORMAT).put("package",pkg).put("createdAt",now).put("settings",LauncherProfileCodec.encode(settings,false));
 }
 static int version(JSONObject document,String pkg)throws JSONException{
  int format=document.optInt("format",1);if(format<1||format>FORMAT)throw new IllegalArgumentException("Unsupported backup version");
  if(format>=4&&(!pkg.equals(document.optString("package"))||document.optJSONObject("settings")==null))throw new IllegalArgumentException("Invalid backup envelope");
  if(format==3&&(!pkg.equals(document.optString("package"))||document.optJSONObject("settings")==null||document.optJSONObject("gameSettings")==null))throw new IllegalArgumentException("Invalid legacy backup envelope");
  if(format<3&&(document.length()==0||document.has("settings")&&document.optJSONObject("settings")==null||format==2&&document.optJSONObject("settings")==null))throw new IllegalArgumentException("Backup has no settings");return format;
 }
 private LauncherBackupCodec(){}
}
