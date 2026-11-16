package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

// Tiny white outline icons on a 24x24 grid, so the app needs no image resources.
// Each icon is a list of ops: 0=moveTo x y, 1=lineTo x y, 2=close, 3=circle cx cy r,
// 4=arc cx cy r startDegrees sweepDegrees (0 degrees is 3 o'clock, going clockwise).
final class LineIcon extends Drawable {
    static final int[] PLAY = {0, 60, 45, 1, 60, 195, 1, 190, 120, 2};
    static final int[] HDMI = {0, 30, 90, 1, 210, 90, 1, 210, 130, 1, 185, 160, 1, 55, 160, 1, 30, 130, 2,
            0, 80, 120, 1, 160, 120};
    static final int[] SETTINGS = {0, 40, 70, 1, 140, 70, 0, 180, 70, 1, 200, 70, 0, 40, 170, 1, 60, 170,
            0, 100, 170, 1, 200, 170, 3, 160, 70, 20, 3, 80, 170, 20};
    static final int[] CONTROL_PANEL = {0, 35, 58, 1, 205, 58, 0, 35, 120, 1, 205, 120,
            0, 35, 182, 1, 205, 182, 3, 92, 58, 16, 3, 154, 120, 16, 3, 111, 182, 16};
    static final int[] SCREEN_OFF = {4, 120, 130, 76, 220, 280, 0, 120, 34, 1, 120, 130};
    static final int[] RESTART = {4, 120, 120, 82, 45, 300, 0, 200, 96, 1, 198, 54, 1, 163, 82};
    static final int[] HOME = {0, 20, 108, 1, 120, 24, 1, 220, 108, 0, 48, 100, 1, 48, 212, 1, 104, 212, 1, 104, 148, 1, 136, 148, 1, 136, 212, 1, 192, 212, 1, 192, 100};
    static final int[] ALL_APPS = {0, 80, 40, 1, 160, 120, 1, 80, 200};
    static final int[] FOLDER = {0, 20, 65, 1, 88, 65, 1, 112, 88, 1, 220, 88, 1, 220, 190,
            1, 20, 190, 2, 0, 20, 65, 1, 20, 180, 1, 220, 180};
    static final int[] MICROPHONE = {4, 120, 92, 38, 180, 180, 0, 120, 32, 1, 120, 132,
            4, 120, 92, 65, 0, 180, 0, 120, 157, 1, 120, 188, 0, 80, 210, 1, 160, 210};

    static final int[] PLUS = {0, 120, 50, 1, 120, 190, 0, 50, 120, 1, 190, 120};
    static final int[] SEARCH = {3, 100, 100, 60, 0, 143, 143, 1, 207, 207};
    static final int[] WIFI = {4, 120, 195, 135, 225, 90, 4, 120, 195, 92, 225, 90, 4, 120, 195, 50, 225, 90,
            3, 120, 195, 4};
    static final int[] WIFI_2 = {4, 120, 195, 92, 225, 90, 4, 120, 195, 50, 225, 90, 3, 120, 195, 4};
    static final int[] WIFI_1 = {4, 120, 195, 50, 225, 90, 3, 120, 195, 4};
    static final int[] SLASH = {0, 50, 45, 1, 190, 205};
    static final int[] BLUETOOTH = {0, 70, 80, 1, 170, 160, 1, 120, 205, 1, 120, 35, 1, 170, 80, 1, 70, 160};
    static final int[] SOUND = {0, 35, 95, 1, 75, 95, 1, 120, 55, 1, 120, 185, 1, 75, 145, 1, 35, 145, 2,
            4, 120, 120, 40, -45, 90, 4, 120, 120, 78, -45, 90};
    static final int[] ETHERNET = {0, 40, 60, 1, 200, 60, 1, 200, 165, 1, 160, 165, 1, 160, 195, 1, 80, 195,
            1, 80, 165, 1, 40, 165, 2, 0, 85, 60, 1, 85, 100, 0, 120, 60, 1, 120, 100, 0, 155, 60, 1, 155, 100};
    static final int[] PICTURE ={0, 30, 50, 1, 210, 50, 1, 210, 165, 1, 30, 165, 2, 0, 120, 165, 1, 120, 200,
            0, 80, 200, 1, 160, 200};

    private final int[] ops;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF arc = new RectF();
    private final RectF content = new RectF();
    private int color = 0xEBFFFFFF;
    private float strokeScale = 1f;
    private boolean fitToContent;private float fillFraction=.86f;private RectF fitReference;

    LineIcon(int[] ops) {
        this.ops = ops;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
    }

    void setColor(int color) {
        this.color = color;
        paint.setColor(color);
        invalidateSelf();
    }

    void setStrokeScale(float scale) {
        strokeScale = Math.max(0.65f, scale);
        invalidateSelf();
    }

    void setFitToContent(boolean fit) {
        fitToContent = fit;
        invalidateSelf();
    }

    void setFillFraction(float value){fillFraction=Math.max(.5f,Math.min(.98f,value));invalidateSelf();}
    void setReferenceBounds(float left,float top,float right,float bottom){fitReference=new RectF(left,top,right,bottom);invalidateSelf();}
    @Override
    public void draw(Canvas c) {
        if (ops == null) return;
        float s = getBounds().width() / 240f; // ops are in tenths of the 24-unit grid
        paint.setStrokeWidth(22 * s * strokeScale);
        path.reset();
        for (int i = 0; i < ops.length; ) {
            switch (ops[i]) {
                case 0: path.moveTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 1: path.lineTo(ops[i + 1] * s, ops[i + 2] * s); i += 3; break;
                case 2: path.close(); i += 1; break;
                case 3: path.addCircle(ops[i + 1] * s, ops[i + 2] * s, ops[i + 3] * s, Path.Direction.CW); i += 4; break;
                default:
                    float cx = ops[i + 1] * s, cy = ops[i + 2] * s, r = ops[i + 3] * s;
                    arc.set(cx - r, cy - r, cx + r, cy + r);
                    path.addArc(arc, ops[i + 4], ops[i + 5]);
                    i += 6;
            }
        }
        c.save();
        c.translate(getBounds().left, getBounds().top);
        if (fitToContent) {
            if(fitReference==null)path.computeBounds(content,true);else content.set(fitReference.left*s,fitReference.top*s,fitReference.right*s,fitReference.bottom*s);
            float available=Math.min(getBounds().width(),getBounds().height())*fillFraction;float stroke=22*s*strokeScale;
            float scale=Math.min(available/Math.max(1,content.width()+stroke),available/Math.max(1,content.height()+stroke));
            c.translate(getBounds().width() / 2f, getBounds().height() / 2f);
            c.scale(scale, scale);
            c.translate(-content.centerX(), -content.centerY());
        }
        c.drawPath(path, paint);
        c.restore();
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 44; }
    @Override public int getIntrinsicHeight() { return 44; }
}
