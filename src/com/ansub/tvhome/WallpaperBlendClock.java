package com.ansub.tvhome;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.TextClock;
/** Wallpaper sampled through the clock glyphs, with a restrained readable highlight. */
final class WallpaperBlendClock extends TextClock {
 private final Paint mask=new Paint();
 private final int[] location=new int[2];
 WallpaperBlendClock(Context context){super(context);mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));}
 @Override protected void onDraw(Canvas canvas){
  if(!getContext().getSharedPreferences("config",0).getBoolean("clockWallpaperBlend",false)||!(getContext() instanceof Activity)){super.onDraw(canvas);return;}
  Activity host=(Activity)getContext();Drawable wallpaper=host.getWindow().getDecorView().getBackground();
  if(wallpaper==null){super.onDraw(canvas);return;}
  int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
  getLocationInWindow(location);canvas.save();canvas.translate(-location[0]+12,-location[1]+8);wallpaper.draw(canvas);canvas.restore();
  int glyph=canvas.saveLayer(0,0,getWidth(),getHeight(),mask);super.onDraw(canvas);canvas.restoreToCount(glyph);canvas.restoreToCount(layer);
  int alpha=getPaint().getAlpha();getPaint().setAlpha(75);super.onDraw(canvas);getPaint().setAlpha(alpha);
 }
}
