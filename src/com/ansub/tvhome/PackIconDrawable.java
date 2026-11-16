package com.ansub.tvhome;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Marks pack-authored artwork so home surfaces do not add a second plate or shrink it again. */
final class PackIconDrawable extends Drawable {
 private final Drawable art;private final float factor;
 PackIconDrawable(Drawable source,boolean mapped,float fallbackScale){Drawable.ConstantState state=source.getConstantState();art=state==null?source.mutate():state.newDrawable().mutate();factor=mapped?1:IconSizingPolicy.factor(fallbackScale);}
 @Override public void draw(Canvas canvas){Rect b=getBounds();int iw=Math.max(1,art.getIntrinsicWidth()),ih=Math.max(1,art.getIntrinsicHeight());float fit=IconSizingPolicy.scale(b.width(),b.height(),iw,ih,factor);int w=Math.max(1,Math.round(iw*fit)),h=Math.max(1,Math.round(ih*fit));art.setBounds(b.centerX()-w/2,b.centerY()-h/2,b.centerX()+(w+1)/2,b.centerY()+(h+1)/2);art.draw(canvas);}
 @Override public void setAlpha(int value){art.setAlpha(value);invalidateSelf();}@Override public void setColorFilter(ColorFilter filter){art.setColorFilter(filter);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}@Override public int getIntrinsicWidth(){return 128;}@Override public int getIntrinsicHeight(){return 128;}
}
