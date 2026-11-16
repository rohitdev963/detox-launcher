package com.ansub.tvhome;

import android.media.MediaPlayer;
import android.media.SyncParams;
import android.os.Build;
import android.view.Surface;

/** Follow the current TV mode without forced 60Hz modes or pretending to generate video frames. */
final class VideoDisplayPacing {
 static void followDisplay(Surface surface){
  if(Build.VERSION.SDK_INT<30||surface==null||!surface.isValid())return;
  try{
   // 0 accepts the system's current display choice. TextureView is composited by its window.
   if(Build.VERSION.SDK_INT>=31)surface.setFrameRate(0f,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
   else surface.setFrameRate(0f,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
  }catch(RuntimeException ignored){/* Older/vendor compositor: keep normal timestamped playback. */}
 }
 static void prepared(MediaPlayer player,Surface surface,boolean directlyDisplayed){
  followDisplay(surface);
  // Explicit VSYNC is only supported for directly displayed surfaces, not a TextureView sink.
  if(!directlyDisplayed||Build.VERSION.SDK_INT<23||player==null)return;
  try{player.setSyncParams(new SyncParams().allowDefaults().setSyncSource(SyncParams.SYNC_SOURCE_VSYNC).setTolerance(.001f));}
  catch(RuntimeException ignored){/* Unsupported decoder: retain platform A/V sync instead of failing video. */}
 }
}
