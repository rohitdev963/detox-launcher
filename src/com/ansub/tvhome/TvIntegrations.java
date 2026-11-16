package com.ansub.tvhome;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.Bundle;
import android.provider.Settings;
import android.media.tv.TvContract;
import java.util.*;

/** On-demand system integrations; no polling or background network requests. */
final class TvIntegrations {
 static void show(Activity host,Runnable phone){show(host,phone,null);}
 static void show(Activity host,Runnable phone,Runnable parent){
  String[] ids={"watch","media","phone","recovery"};
  String[] choices={"Continue watching","Now playing","Send wallpaper from phone","Launcher recovery"};
  Runnable back=()->show(host,phone,parent);
  DetoxDialogStyle.builder(host,parent).setTitle("TV integrations").setItems(choices,(d,i)->{
   switch(ids[i]){case "watch":watch(host,back);break;case "media":media(host,back);break;case "phone":phone.run();break;case "recovery":recovery(host,back);break;}
  }).setNegativeButton("Close",(d,i)->{if(parent!=null)parent.run();}).show();
 }
 static void watch(Activity host){watch(host,null);}
 static void watch(Activity host,Runnable parent){
  List<String> labels=new ArrayList<>();List<Intent> actions=new ArrayList<>();
  try(Cursor c=host.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI,new String[]{"title","intent_uri","last_playback_position_millis","duration_millis"},null,null,null)){
   if(c!=null)while(c.moveToNext()&&labels.size()<20){String uri=c.getString(1);if(uri==null)continue;try{Intent action=Intent.parseUri(uri,Intent.URI_INTENT_SCHEME);action.setComponent(null);action.setSelector(null);if(!Intent.ACTION_VIEW.equals(action.getAction()))continue;int duration=c.getInt(3),position=c.getInt(2);labels.add(c.getString(0)+(duration>0?" · "+Math.min(100,(int)(100L*position/duration))+"%":""));actions.add(action);}catch(Exception ignored){}}
  }catch(Exception error){message(host,"Continue watching","Android did not allow access to Watch Next on this TV. Availability depends on the TV and participating apps.",parent);return;}
  if(labels.isEmpty()){message(host,"Continue watching","No Watch Next titles are available from your streaming apps.",parent);return;}
  DetoxDialogStyle.builder(host,parent).setTitle("Continue watching").setItems(labels.toArray(new String[0]),(d,i)->{try{host.startActivity(actions.get(i));}catch(Exception e){message(host,"Continue watching","This app cannot open the saved title.",parent);}}).setNegativeButton("Close",(d,i)->{if(parent!=null)parent.run();}).show();
 }
 static void media(Activity host){media(host,null);}
 static void media(Activity host,Runnable parent){
  try{
   MediaSessionManager manager=(MediaSessionManager)host.getSystemService(Context.MEDIA_SESSION_SERVICE);
   List<MediaController> sessions=manager.getActiveSessions(new ComponentName(host,DetoxMediaAccess.class));
   if(sessions.isEmpty()){message(host,"Now playing","No active media sessions. Start music or video in another app, then return here.",parent);return;}
   String[] labels=new String[sessions.size()];for(int i=0;i<labels.length;i++){MediaMetadata m=sessions.get(i).getMetadata();String title=m==null?null:m.getString(MediaMetadata.METADATA_KEY_TITLE);labels[i]=(title==null?sessions.get(i).getPackageName():title);}
   DetoxDialogStyle.builder(host,parent).setTitle("Now playing").setItems(labels,(d,i)->{MediaController controller=sessions.get(i);MediaMetadata m=controller.getMetadata();DetoxDialogStyle.builder(host,()->media(host,parent)).setTitle(labels[i]).setMessage(m==null?"":m.getString(MediaMetadata.METADATA_KEY_ARTIST)).setItems(new String[]{"Previous","Play / pause","Next"},(x,j)->{MediaController.TransportControls t=controller.getTransportControls();if(j==0)t.skipToPrevious();else if(j==2)t.skipToNext();else if(controller.getPlaybackState()!=null&&controller.getPlaybackState().getState()==PlaybackState.STATE_PLAYING)t.pause();else t.play();}).setNegativeButton("Close",(controls,j)->media(host,parent)).show();}).setNegativeButton("Close",(d,i)->{if(parent!=null)parent.run();}).show();
  }catch(SecurityException denied){DetoxDialogStyle.builder(host,parent).setTitle("Enable media access").setMessage("Allow Detox notification access in Android settings to read active media sessions. This is optional.").setPositiveButton("Open settings",(d,i)->{try{host.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(Exception e){message(host,"Media access","This TV does not expose notification access settings.",parent);}}).setNegativeButton("Cancel",(d,i)->{if(parent!=null)parent.run();}).show();}
 }
 static void recovery(Activity host){recovery(host,null);}
 static void recovery(Activity host,Runnable parent){DetoxDialogStyle.builder(host,parent).setTitle("Launcher recovery").setMessage("Choose another Home app in Android settings if Detox has problems. Your Detox settings will be kept.").setPositiveButton("Home settings",(d,i)->{try{host.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}catch(Exception e){host.startActivity(new Intent(Settings.ACTION_SETTINGS));}}).setNegativeButton("Close",(d,i)->{if(parent!=null)parent.run();}).show();}
 static void message(Activity host,String title,String message){message(host,title,message,null);}
 static void message(Activity host,String title,String message,Runnable parent){DetoxDialogStyle.builder(host,parent).setTitle(title).setMessage(message).setPositiveButton("OK",(d,i)->{if(parent!=null)parent.run();}).show();}
}
