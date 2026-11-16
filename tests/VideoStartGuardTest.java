package com.ansub.tvhome;
import java.nio.file.*;

public final class VideoStartGuardTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  VideoStartGuard guard=new VideoStartGuard();long first=guard.begin();check(guard.pending(first)&&!guard.rendered(),"new selection pending, not falsely applied");
  long second=guard.begin();check(!guard.ready(first)&&!guard.timeout(first)&&guard.pending(second),"old decoder cannot complete or fail current selection");
  check(guard.ready(second)&&guard.rendered()&&!guard.timeout(second)&&!guard.ready(second),"first frame exactly once, no late timeout");
  long third=guard.begin();check(guard.timeout(third)&&!guard.ready(third),"stalled decoder cannot later confirm success");
  long fourth=guard.begin();guard.cancel();check(!guard.pending(fourth)&&!guard.ready(fourth)&&!guard.rendered(),"stop cancels pending callbacks");
  String s=Files.readString(Path.of(args[0]));
  check(s.contains("MEDIA_INFO_VIDEO_RENDERING_START&&videoStartGuard.ready(startToken)")&&s.contains("cancelVideoWallpaperStartTimeout();"),"actual first-render callback owns readiness");
  check(s.contains("player==videoWallpaperPlayer&&videoStartGuard.timeout(token)")&&s.contains("postDelayed(videoWallpaperStartTimeout,12000L)"),"generation-scoped first-frame timeout is scheduled");
  check(s.contains("onSurfaceTextureUpdated(SurfaceTexture surface) {if(videoWallpaperPlayer!=null&&videoWallpaperPrepared)videoWallpaperFrameAt=SystemClock.uptimeMillis();}")&&s.contains("Video stopped producing frames for 8 seconds"),"TextureView frame arrival monitors stalls without reading decoder state each frame");
  int start=s.indexOf("private void stopVideoWallpaper()"),end=s.indexOf("private void showVideoWallpaperPicker()",start);String stop=s.substring(start,end);
  check(stop.contains("videoStartGuard.cancel()")&&stop.contains("setOnInfoListener(null)")&&!stop.contains("player.reset()")&&!stop.contains("player.stop()"),"release cancels callbacks without redundant decoder transitions");
  check(s.contains("if(videoWallpaperPlayer!=mp)return;"),"stale video-size callback guarded");
  int apply=s.indexOf("private void applyBuiltInLiveVideoWallpaper(int index)"),next=s.indexOf("private void showInterfaceMotionOptions()",apply);String body=s.substring(apply,next);
  check(body.contains("Loading video: ")&&!body.contains("Live wallpaper applied: "),"selection cannot report success before rendering");
  System.out.println("Video startup: first-frame confirmation, timeout, stale callback rejection, cancellation and actual Home integration contracts passed");
 }
}
