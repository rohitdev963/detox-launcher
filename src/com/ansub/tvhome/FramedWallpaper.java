package com.ansub.tvhome;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.content.SharedPreferences;
/** Applies framing at draw time so the live home is the preview. */
final class FramedWallpaper extends Drawable {
 private final Drawable source;private final SharedPreferences prefs;
 FramedWallpaper(Drawable source,SharedPreferences prefs){this.source=source;this.prefs=prefs;}
 public void draw(Canvas c){Rect b=getBounds();source.setBounds(b);WallpaperFramingState preview=SettingsWorkbench.previewFrame(prefs);float zoom=preview==null?Math.max(1f,Math.min(2f,prefs.getFloat("wallpaperFrameZoom",1f))):preview.zoom;float x=preview==null?Math.max(0f,Math.min(1f,prefs.getFloat("wallpaperFrameX",.5f))):preview.x,y=preview==null?Math.max(0f,Math.min(1f,prefs.getFloat("wallpaperFrameY",.5f))):preview.y;c.save();c.clipRect(b);c.translate(b.left-(zoom-1)*b.width()*x,b.top-(zoom-1)*b.height()*y);c.scale(zoom,zoom);source.draw(c);c.restore();}
 public void setAlpha(int a){source.setAlpha(a);}public void setColorFilter(ColorFilter f){source.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
