package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

/** Quiet photographic lighting and readability treatments; no particle or cartoon renderer. */
class ProfessionalWallpaperView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private RadialGradient light;
    private int lightColor=0xFFC3D1DC;
    private LinearGradient sweep, edges;
    private String style = "ambient";
    private boolean active, enabled = true, videoAware;
    private int performance = 1;
    private float presetSpeed = 1f;
    private float speed = 1f, amount = 1f, brightness = 1f;
    private final long epoch = SystemClock.uptimeMillis();
    private long sampleStart;
    private int frames;
    private float fps;

    ProfessionalWallpaperView(Context context) {
        super(context); setFocusable(false); setClickable(false); setWillNotDraw(false);
    }

    void setWallpaperTone(int color){int r=Color.red(color),g=Color.green(color),b=Color.blue(color);int next=Color.rgb(175+r/4,175+g/4,175+b/4);if(next==lightColor)return;lightColor=next;onSizeChanged(getWidth(),getHeight(),getWidth(),getHeight());invalidate();}
    void setMotionEnabled(boolean value) { enabled = value; setVisibility(value ? VISIBLE : GONE); invalidate(); }
    void setMotionStyle(String value) { style = WallpaperEffects.normalize(value); invalidate(); }
    void setEffectTuning(int tempo, int luminance, int intensity) {
        speed = tempo == 0 ? .7f : tempo == 2 ? 1.2f : 1f;
        brightness = luminance == 0 ? .65f : luminance == 2 ? 1.2f : 1f;
        amount = intensity == 0 ? .65f : intensity == 2 ? 1.1f : .85f;
        invalidate();
    }
    void setMotionTuning(int preset) { presetSpeed = preset == 0 ? .7f : preset == 2 ? 1.2f : 1f; invalidate(); }
    void setPerformanceMode(int mode) { performance = Math.max(0, Math.min(2, mode)); invalidate(); }
    void setVideoAwareLoad(boolean value) { videoAware = value; invalidate(); }
    void setActive(boolean value) { active = value; sampleStart = 0L; frames = 0; fps = 0f; if (value) invalidate(); }
    float observedFps() { return fps; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w <= 0 || h <= 0) return;
        light = new RadialGradient(0, 0, Math.max(w, h) * .65f,
                new int[] {lightColor, lightColor & 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
        sweep = new LinearGradient(-w * .42f, 0, w * .42f, h * .3f,
                new int[] {lightColor & 0x00FFFFFF, lightColor, lightColor & 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
        edges = new LinearGradient(0, 0, w, 0,
                new int[] {0x90040A12, 0x00040A12, 0x00040A12, 0x90040A12},
                new float[] {0f, .3f, .7f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override protected void onDraw(Canvas c) {
        if (!active || !isShown() || !enabled || "off".equals(style) || light == null) return;
        float w = getWidth(), h = getHeight();
        float time = (SystemClock.uptimeMillis() - epoch) * .001f * speed * presetSpeed;
        float strength = amount * brightness * (videoAware ? .45f : 1f);
        paint.setAlpha(255);
        if ("shade".equals(style)) {
            paint.setShader(edges); paint.setAlpha(Math.min(255, (int)(180f * strength))); c.drawRect(0, 0, w, h, paint);
        } else if ("evening".equals(style)) {
            paint.setShader(null); paint.setColor(Color.argb(Math.min(85, (int)(48f * strength)), 24, 17, 19)); c.drawRect(0, 0, w, h, paint);
        } else {
            if ("studio".equals(style)) {
                matrix.setTranslate(w * (.5f + .72f * (float)Math.sin(time * .035f)), 0f);
                sweep.setLocalMatrix(matrix); paint.setShader(sweep); paint.setAlpha(Math.min(22, (int)(16f * strength)));
                c.drawRect(0, 0, w, h, paint);
            } else {
                matrix.setTranslate(w * (.55f + .16f * (float)Math.sin(time * .035f)), h * (.18f+.035f*(float)Math.cos(time*.023f)));
                light.setLocalMatrix(matrix); paint.setShader(light); paint.setAlpha(Math.min(24, (int)(16f * strength)));
                c.drawRect(0, 0, w, h, paint);
            }
            long now = SystemClock.uptimeMillis(); if (sampleStart == 0L) sampleStart = now;
            frames++; if (now - sampleStart >= 1000L) { fps = frames * 1000f / (now - sampleStart); sampleStart = now; frames = 0; }
            // Slow photographic lighting does not need to redraw the entire home at game frame rates.
            postInvalidateDelayed(videoAware || performance == 0 ? 100L : performance == 2 ? 33L : 66L);
        }
        paint.setShader(null); paint.setAlpha(255);
    }
}
