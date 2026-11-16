package com.ansub.tvhome;
/** Runs only in the updated app, never through ADB or against other apps' storage. */
final class RetiredPluginDataCleanup {
 static void schedule(android.content.Context context){
  android.content.Context app=context.getApplicationContext();
  Thread cleanup=new Thread(()->{
   android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
   try{
    android.content.SharedPreferences config=app.getSharedPreferences("config",0);
    removeRetiredVisualSetting(config);
    RetiredPluginFiles.remove(app.getFilesDir(),app.getCacheDir());
    app.getSharedPreferences("detox_extension_states",0).edit().clear().apply();
    app.getSharedPreferences("detox_extension_options",0).edit().clear().apply();
    android.content.SharedPreferences.Editor editor=config.edit().remove("trustedModulesEnabled");
    editor.remove("detoxExtensionsShader").remove("detoxShaderScene").apply();
   }catch(Exception error){DetoxDiagnostics.recordFailure("DetoxCleanup","Retired plugin store","Private cleanup will retry next launch",error);}
  },"DetoxPluginRetirement");cleanup.setDaemon(true);cleanup.start();
 }
 private static void removeRetiredVisualSetting(android.content.SharedPreferences config){
  final String retired="homeCompanionMode";
  android.content.SharedPreferences.Editor editor=config.edit().remove(retired);
  Object saved=config.getAll().get("profileStore");
  if(saved instanceof String)try{
   org.json.JSONObject store=new org.json.JSONObject((String)saved);boolean changed=false;
   java.util.Iterator<String> names=store.keys();
   while(names.hasNext()){
    org.json.JSONObject snapshot=store.optJSONObject(names.next());if(snapshot==null)continue;
    if(snapshot.has(retired)){snapshot.remove(retired);changed=true;}
    org.json.JSONObject types=snapshot.optJSONObject(LauncherProfileCodec.TYPES);
    if(types!=null&&types.has(retired)){types.remove(retired);changed=true;}
   }
   if(changed)editor.putString("profileStore",store.toString());
  }catch(org.json.JSONException malformed){
   DetoxDiagnostics.recordFailure("DetoxCleanup","Retired visual setting","Kept unreadable profile store unchanged",malformed);
  }
  editor.apply();
 }
 private RetiredPluginDataCleanup(){}
}
