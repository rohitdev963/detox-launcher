package com.ansub.tvhome;
/** Local crash-loop guard. Does not disable or change the system launcher. */
public final class DetoxApplication extends android.app.Application {
 public void onCreate(){super.onCreate();Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();Thread.setDefaultUncaughtExceptionHandler((thread,error)->{android.content.SharedPreferences p=getSharedPreferences("recovery",0);long now=System.currentTimeMillis();int count=now-p.getLong("lastCrash",0)<120000?p.getInt("crashes",0)+1:1;p.edit().putInt("crashes",count).putLong("lastCrash",now).commit();if(previous!=null)previous.uncaughtException(thread,error);else android.os.Process.killProcess(android.os.Process.myPid());});RetiredPluginDataCleanup.schedule(this);}
 static boolean blocked(android.content.Context c){return c.getSharedPreferences("recovery",0).getInt("crashes",0)>=3;}
}
