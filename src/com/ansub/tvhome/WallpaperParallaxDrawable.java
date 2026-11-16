package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Adds a very small focus-driven drift while overscanning enough to keep screen edges covered. */
final class WallpaperParallaxDrawable extends Drawable {
    private final Drawable content;
    private float offsetX,offsetY;

    WallpaperParallaxDrawable(Drawable content){this.content=content;}
    float offsetX(){return offsetX;}
    float offsetY(){return offsetY;}
    void offset(float x,float y){offsetX=x;offsetY=y;invalidateSelf();}

    @Override protected void onBoundsChange(Rect bounds){content.setBounds(bounds);}
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();int save=canvas.save();
        canvas.translate(offsetX,offsetY);
        canvas.scale(1.035f,1.035f,bounds.exactCenterX(),bounds.exactCenterY());
        content.setBounds(bounds);content.draw(canvas);canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){content.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){content.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return content.getIntrinsicWidth();}
    @Override public int getIntrinsicHeight(){return content.getIntrinsicHeight();}
}
