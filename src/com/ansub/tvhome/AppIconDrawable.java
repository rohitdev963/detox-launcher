package com.ansub.tvhome;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Fits the app's own artwork without adding another plate, border or inset. */
final class AppIconDrawable extends Drawable {
    private final Drawable icon;private int alpha=255;
    AppIconDrawable(Drawable source){Drawable.ConstantState state=source==null?null:source.getConstantState();icon=state==null?source:state.newDrawable().mutate();}
    @Override public void draw(Canvas canvas){
        if(icon==null)return;Rect b=getBounds();
        if(icon instanceof PackIconDrawable){icon.setBounds(b);}
        else {float iw=Math.max(1,icon.getIntrinsicWidth()),ih=Math.max(1,icon.getIntrinsicHeight());float fit=Math.min(b.width()/iw,b.height()/ih);int w=Math.max(1,Math.round(iw*fit)),h=Math.max(1,Math.round(ih*fit));icon.setBounds(b.centerX()-w/2,b.centerY()-h/2,b.centerX()+(w+1)/2,b.centerY()+(h+1)/2);}
        icon.setAlpha(alpha);icon.draw(canvas);
    }
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){if(icon!=null)icon.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return 128;}
    @Override public int getIntrinsicHeight(){return 128;}
}
