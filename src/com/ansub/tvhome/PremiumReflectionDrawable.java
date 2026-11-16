package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Static GPU-friendly shader sheen for premium Home tiles; no bitmap allocation or frame loop. */
final class PremiumReflectionDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final int preset;
    private final float density;
    private final RectF rect = new RectF(), borderRect = new RectF();
    private LinearGradient sheen;
    private float radius;
    private int alpha=255;
    PremiumReflectionDrawable(int preset, float density) {
        this.preset = Math.max(0,Math.min(HomePremiumThemes.IDS.length-1,preset));
        this.density = Float.isNaN(density)||Float.isInfinite(density)?1f:Math.max(.1f,density);
    }
    @Override protected void onBoundsChange(Rect bounds) {
        rect.set(bounds);borderRect.set(bounds.left+density,bounds.top+density,bounds.right-density,bounds.bottom-density);
        radius = HomePremiumThemes.RADII[preset] * density;
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        if(bounds.isEmpty()||preset<=0){sheen=null;return;}
        int accent = HomePremiumThemes.ACCENTS[preset] & 0x00FFFFFF;
        // Restrained directional reflection: brighter near the upper edge, fading into transparency.
        int highlightAlpha = preset == 6 ? 0x66 : preset == 1 ? 0x58 : 0x38;
        sheen = new LinearGradient(bounds.left, bounds.top, bounds.right,
                bounds.top + bounds.height() * 0.8f,
                new int[] {(highlightAlpha << 24) | 0x00FFFFFF, 0x18000000 | accent, Color.TRANSPARENT},
                new float[] {0f, 0.28f, 1f}, Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas canvas) {
        if(getBounds().isEmpty()||preset<=0||alpha==0)return;
        if(sheen==null)onBoundsChange(getBounds());
        int saved = canvas.save();canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(alpha);paint.setShader(sheen);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(0x55FFFFFF);
        paint.setAlpha(0x55*alpha/255);
        canvas.drawRoundRect(borderRect,
                Math.max(0, radius - density), Math.max(0, radius - density), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(saved);
    }
    @Override public void setAlpha(int alpha) { this.alpha=Math.max(0,Math.min(255,alpha)); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
