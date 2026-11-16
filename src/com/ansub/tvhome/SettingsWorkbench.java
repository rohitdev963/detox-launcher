package com.ansub.tvhome;
import android.app.*;import android.content.*;import android.widget.*;import java.util.*;import org.json.*;
/** Search routes and an explicit settings checkpoint, stored outside launcher configuration. */
final class SettingsWorkbench {
 static void checkpoint(Activity a){try{String json=LauncherProfileCodec.encode(a.getSharedPreferences("config",0).getAll(),false).toString();a.getSharedPreferences("settingsUndo",0).edit().putString("snapshot",json).apply();}catch(Exception e){TvIntegrations.message(a,"Undo","Could not save a settings checkpoint.");}}
 static void undo(Activity a){undo(a,null);}
 static void undo(Activity a,Runnable parent){String snapshot=a.getSharedPreferences("settingsUndo",0).getString("snapshot","");if(snapshot.isEmpty()){TvIntegrations.message(a,"Undo","Save a checkpoint before changing settings.",parent);return;}DetoxDialogStyle.builder(a,parent).setTitle("Restore settings checkpoint?").setMessage("Restores saved preferences. Imported or deleted files cannot be restored.").setNegativeButton("Cancel",(d,w)->{if(parent!=null)parent.run();}).setPositiveButton("Restore",(d,w)->{try{android.content.SharedPreferences p=a.getSharedPreferences("config",0);Map<String,Object> values=LauncherProfileCodec.decode(new JSONObject(snapshot),p.getAll(),false);android.content.SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,Object> entry:values.entrySet()){String k=entry.getKey();Object v=entry.getValue();if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);}e.apply();a.recreate();}catch(Exception error){TvIntegrations.message(a,"Undo","Checkpoint could not be restored.",parent);}}).show();}
 static void search(Activity a,String[] names,Runnable[] routes){search(a,names,routes,null);}
 static void search(Activity a,String[] names,Runnable[] routes,Runnable parent){EditText input=new EditText(a);input.setSingleLine(true);input.setHint("Search settings");DetoxDialogStyle.builder(a,parent).setTitle("Find a setting").setView(input).setNegativeButton("Cancel",(d,w)->{if(parent!=null)parent.run();}).setPositiveButton("Search",(d,w)->{String q=input.getText().toString().trim().toLowerCase(Locale.ROOT);List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();for(int i=0;i<names.length;i++)if(names[i].toLowerCase(Locale.ROOT).contains(q)){labels.add(names[i]);actions.add(routes[i]);}if(labels.isEmpty()){DetoxDialogStyle.builder(a,()->search(a,names,routes,parent)).setTitle("Settings search").setMessage("No matching settings. Try clock, wallpaper or apps.").setPositiveButton("OK",(x,y)->search(a,names,routes,parent)).show();return;}DetoxDialogStyle.builder(a,()->search(a,names,routes,parent)).setTitle("Settings results").setItems(labels.toArray(new String[0]),(x,i)->actions.get(i).run()).setNegativeButton("Close",(x,y)->search(a,names,routes,parent)).show();}).show();}
 static void frame(Activity a){frame(a,null);}
 private static final WeakHashMap<SharedPreferences,WallpaperFramingState> FRAME_PREVIEWS=new WeakHashMap<>();
 static WallpaperFramingState previewFrame(SharedPreferences prefs){return FRAME_PREVIEWS.get(prefs);}
 static void clearFramePreview(Activity host){FRAME_PREVIEWS.remove(host.getSharedPreferences("config",0));}
 static void frame(Activity a,Runnable parent){
  SharedPreferences p=a.getSharedPreferences("config",0);
  WallpaperFramingState state=new WallpaperFramingState(p.getFloat("wallpaperFrameZoom",1f),p.getFloat("wallpaperFrameX",.5f),p.getFloat("wallpaperFrameY",.5f));
  SettingsChoice[] rows={new SettingsChoice("zoomIn","Zoom in"),new SettingsChoice("zoomOut","Zoom out"),new SettingsChoice("left","Move left"),new SettingsChoice("right","Move right"),new SettingsChoice("up","Move up"),new SettingsChoice("down","Move down"),new SettingsChoice("reset","Reset framing")};
  String[] labels=new String[rows.length];for(int i=0;i<rows.length;i++)labels[i]=rows[i].label;
  Runnable clear=()->{if(FRAME_PREVIEWS.get(p)==state)FRAME_PREVIEWS.remove(p);a.getWindow().getDecorView().invalidate();};
  AlertDialog dialog=DetoxDialogStyle.builder(a,parent).setTitle("Frame wallpaper · preview on Home")
   .setItems(labels,null).setNegativeButton("Done",(d,w)->{
     p.edit().putFloat("wallpaperFrameZoom",state.zoom).putFloat("wallpaperFrameX",state.x).putFloat("wallpaperFrameY",state.y).apply();clear.run();if(parent!=null)parent.run();
   }).setNeutralButton("Cancel",(d,w)->{clear.run();if(parent!=null)parent.run();}).create();
  dialog.setOnCancelListener(d->clear.run());dialog.setOnDismissListener(d->clear.run());
  dialog.setOnShowListener(d->{
   DetoxDialogStyle.apply(dialog,p.getBoolean("menuBlur",false),Math.max(1,Math.round(a.getResources().getDisplayMetrics().density)));
   dialog.getListView().setOnItemClickListener((list,view,i,id)->{state.adjust(rows[i].id);a.getWindow().getDecorView().invalidate();});
   dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
   dialog.getWindow().setGravity(android.view.Gravity.RIGHT|android.view.Gravity.BOTTOM);
  });
  FRAME_PREVIEWS.put(p,state);dialog.show();
 }
}
