package com.ansub.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Cached gradients over photos, video and wallpaper lighting; never changes TV picture settings. */
final class HomeReadabilityView extends View {
    private final Paint paint = new Paint();
    private LinearGradient edge;
    private int brightness = 100, strength = 18;
    HomeReadabilityView(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void tune(int brightness, int strength) {
        this.brightness = HomeUiPolicy.bounded(brightness, 40, 100);
        this.strength = HomeUiPolicy.bounded(strength, 0, 60);
        rebuild(); invalidate();
    }
    private void rebuild() {
        float height=Math.max(1,getHeight());
        int alpha = HomeUiPolicy.shadeAlpha(strength);
        edge = new LinearGradient(0, 0, 0, height,
                new int[] {((alpha * 2 / 3) << 24) | 0x07101E, 0x0007101E, (alpha << 24) | 0x07101E},
                new float[] {0, .34f, 1}, Shader.TileMode.CLAMP);
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { rebuild(); }
    @Override protected void onDraw(Canvas canvas) {
        if (edge == null) rebuild();
        paint.setShader(null); paint.setColor(HomeUiPolicy.brightnessAlpha(brightness) << 24);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(Color.WHITE); paint.setShader(edge);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint); paint.setShader(null);
    }
}


