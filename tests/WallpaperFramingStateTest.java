package com.ansub.tvhome;
public final class WallpaperFramingStateTest {
 static void near(float expected,float value){if(Math.abs(expected-value)>.00001f)throw new AssertionError(expected+" != "+value);}
 public static void main(String[] args){
  WallpaperFramingState state=new WallpaperFramingState(1,.5f,.5f);
  state.adjust("zoomIn");state.adjust("left");state.adjust("down");near(1.1f,state.zoom);near(.4f,state.x);near(.6f,state.y);
  for(int i=0;i<40;i++){state.adjust("zoomIn");state.adjust("left");state.adjust("down");}near(2,state.zoom);near(0,state.x);near(1,state.y);
  for(int i=0;i<40;i++){state.adjust("zoomOut");state.adjust("right");state.adjust("up");}near(1,state.zoom);near(1,state.x);near(0,state.y);
  state.adjust("reset");near(1,state.zoom);near(.5f,state.x);near(.5f,state.y);
  WallpaperFramingState reopened=new WallpaperFramingState(1.3f,.2f,.7f);reopened.adjust("right");near(.3f,reopened.x);
  try{state.adjust("unknown");throw new AssertionError("unknown action accepted");}catch(IllegalArgumentException expected){}
  System.out.println("Wallpaper framing session: pan/zoom, bounds, reset, saved-state loading and named actions passed");
 }
}
