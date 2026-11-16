package com.ansub.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import java.util.Date;

/** Read-only, local app/device information. No identifiers, network requests or preference writes. */
final class DetoxAboutInfo {
 static String summary(Context context){
  PackageManager manager=context.getPackageManager();
  StringBuilder text=new StringBuilder("Detox\nAndroid TV home launcher");
  try{
   PackageInfo info=manager.getPackageInfo(context.getPackageName(),0);
   field(text,"Version",info.versionName);
   long code=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
   field(text,"Build",Long.toString(code)+((context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0?" · Debug":" · Release"));
   if(info.lastUpdateTime>0)field(text,"Last updated",android.text.format.DateFormat.getDateFormat(context).format(new Date(info.lastUpdateTime)));
  }catch(PackageManager.NameNotFoundException unavailable){field(text,"Version","Unavailable");}
  field(text,"Package",context.getPackageName());
  ResolveInfo home=manager.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
  field(text,"Default Home launcher",home==null||home.activityInfo==null?"Not reported by this TV":context.getPackageName().equals(home.activityInfo.packageName)?"Detox":"Another launcher");
  field(text,"Android version",Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT);
  field(text,"TV device",Build.MANUFACTURER+" · "+Build.MODEL);
  if(Build.VERSION.SDK_INT>=23&&!Build.VERSION.SECURITY_PATCH.isEmpty())field(text,"Android security update",Build.VERSION.SECURITY_PATCH);
  field(text,"Utility artwork","3Dicons · CC0\nMicrosoft Fluent System Icons · MIT\nSimple Icons · CC0");
  return text.toString();
 }
 private static void field(StringBuilder text,String label,String value){text.append("\n\n").append(Strings.t(label)).append("\n").append(value==null||value.trim().isEmpty()?Strings.t("Unavailable"):value);}
 private DetoxAboutInfo(){}
}
