package com.ansub.tvhome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Preserves image proportions, with an optional upright portrait crop. */
final class AspectWallpaperDrawable extends Drawable {
    private final Bitmap image;
    private final boolean portrait;
    private final Rect source=new Rect();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF target=new RectF();
    AspectWallpaperDrawable(Bitmap image){this(image,false);}
    AspectWallpaperDrawable(Bitmap image,boolean portrait){this.image=image;this.portrait=portrait;source.set(0,0,image.getWidth(),image.getHeight());if(portrait){int width=Math.min(image.getWidth(),Math.round(image.getHeight()*9f/16f));int height=Math.min(image.getHeight(),Math.round(width*16f/9f));source.set((image.getWidth()-width)/2,(image.getHeight()-height)/2,(image.getWidth()+width)/2,(image.getHeight()+height)/2);}}
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();if(bounds.isEmpty()||image.isRecycled())return;
        float scale=Math.min(bounds.width()/(float)source.width(),bounds.height()/(float)source.height());
        float width=source.width()*scale,height=source.height()*scale;
        target.set(bounds.exactCenterX()-width/2,bounds.exactCenterY()-height/2,
                bounds.exactCenterX()+width/2,bounds.exactCenterY()+height/2);
        canvas.save();canvas.clipRect(bounds);canvas.drawColor(Color.argb(paint.getAlpha(),0,0,0));
        canvas.drawBitmap(image,source,target,paint);canvas.restore();
    }
    @Override public void setAlpha(int value){paint.setAlpha(value);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth(){return image.getWidth();}
    @Override public int getIntrinsicHeight(){return image.getHeight();}
    @Override public ConstantState getConstantState(){
        return new ConstantState(){
            @Override public Drawable newDrawable(){return new AspectWallpaperDrawable(image,portrait);}
            @Override public int getChangingConfigurations(){return 0;}
        };
    }
}


