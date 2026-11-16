package com.ansub.tvhome;
import java.nio.file.*;

public final class GamingSharedBackgroundIntegrationTest {
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent();String activity=Files.readString(dir.resolve("GamingActivity.java"));
  String image=Files.readString(dir.resolve("GamingImageBackground.java")),preview=Files.readString(dir.resolve("GamingThemePreview.java")),video=Files.readString(dir.resolve("GamingVideoBackground.java"));
  for(String key:new String[]{"images","live","gaming","home","framing"})require(activity.contains("new SettingsChoice(\""+key+"\""),"named menu destination "+key);
  require(activity.contains("putString(\"gamingBackgroundSource\",selected)")&&activity.contains("replaceSelectedBackground()")&&!activity.contains("gamingShaderBackground"),"Apply persists Gaming media without a retired renderer override");
  require(!activity.contains("putString(\"wallpaper\"")&&!activity.contains("putString(\"photoUri\"")&&!activity.contains("putString(\"liveWallpaperVideoAsset\""),"Gaming must not change Home choice");
  require(!activity.contains("putString(\"gamingVideoSource\",replacement)"),"temporary fallback must not replace saved selection");
  require(activity.contains("registerOnSharedPreferenceChangeListener(homeBackgroundListener)")&&activity.contains("unregisterOnSharedPreferenceChangeListener(homeBackgroundListener)")&&activity.contains("removeCallbacks(refreshHomeBackground)"),"live Home following listener lifecycle");
  require(activity.contains("themePreview.close()")&&activity.contains("clearBackgroundMedia()"),"preview and backgrounds cleaned on lifecycle changes");
  require(activity.contains("themeBrowseSelections.put(group,choices[index].id)")&&activity.contains("choices[i].id.equals(browse)"),"preview Cancel must restore focus by named candidate, not changed menu index");
  int start=activity.indexOf("private void previewTheme("),end=activity.indexOf("private void showGamingFraming",start);String open=activity.substring(start,end);
  require(open.indexOf("videoBackground.close()")<open.indexOf("new GamingThemePreview(")&&open.contains("videoBackground.close()"),"release main hardware decoder before preview starts");
  require(activity.contains("focus&&resumed&&themePreview==null"),"main video cannot restart while preview owns decoder");
  require(activity.contains("GamingImageBackground")&&activity.contains("createVideoBackground(choice.source)"),"both media render paths bound");
  require(preview.contains(".setPositiveButton(\"Apply\"")&&preview.contains(".setNegativeButton(\"Cancel\"")&&preview.contains("state.apply()")&&preview.contains("state.cancel()"),"preview transaction controls");
  require(!preview.contains(".edit()"),"preview cannot save Home/Gaming preferences while browsing");
  require(preview.contains("if(ready&&!failed&&!closed)")&&preview.contains("setEnabled(false)"),"bad/unrendered previews cannot be applied");
  require(preview.contains("BackGate")&&preview.contains("getRepeatCount()>0")&&activity.contains("getRepeatCount()>0"),"Back and held OK guards");
  require(video.contains("setVolume(0f, 0f)")&&video.contains("setLooping(true)")&&video.contains("MEDIA_INFO_VIDEO_RENDERING_START")&&video.contains("old.setOnInfoListener(null)"),"muted looping video and actual first-frame readiness cleanup");
  require(!preview.contains("GamingMusicPlayer")&&!image.contains("GamingMusicPlayer"),"wallpaper preview cannot commandeer music");
  for(String token:new String[]{"LruCache","12*1024*1024","Executors.newSingleThreadExecutor","inSampleSize","isAttachedToWindow()","request!=generation","shutdownNow()","PhotoImageTools.orient","BundledWallpaperCatalog.landscapeRotation"})require(image.contains(token),"bounded background image decode contract "+token);
  require(!image.contains("postFrameCallback")&&!image.contains("postDelayed"),"static backgrounds do not run frame loops");
  require(!activity.contains("Generated backgrounds")&&!activity.contains("DetoxShaderView")&&!activity.contains("GPU effect"),"retired GPU choices cannot override selected image/video");
  System.out.println("Gaming shared background integration: named menus, isolated Apply, first-frame preview readiness, parent Back, held-key guards, muted looping media, image cache and lifecycle cleanup passed (source contracts)");
 }
}
