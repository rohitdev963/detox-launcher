package com.ansub.tvhome;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Small illustrative preview, not a screenshot or a change to saved Home preferences. */
final class HomeSettingsPreview extends View {
    static final int LAYOUT = 0, TILES = 1, MOTION = 2, HEADER = 3;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int kind;
    private int selection;
    HomeSettingsPreview(Context context, int kind, int selected) {
        super(context); this.kind = kind; selection = Math.max(0, selected);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void select(int value) { selection = Math.max(0, value); invalidate(); }
    void box(Canvas c, float x, float y, float w, float h, int color, float radius) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); rect.set(x,y,x+w,y+h); c.drawRoundRect(rect,radius,radius,paint);
    }
    @Override protected void onDraw(Canvas c) {
        c.save(); c.scale(getWidth()/600f, getHeight()/230f);
        box(c,0,0,600,230,0xFF0C192B,18);
        paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(18);paint.setColor(0xFFC8E0EF);
        c.drawText("HOME PREVIEW",20,29,paint);
        boolean compact = kind == HEADER ? selection == 0 : true;
        box(c,compact?498:450,16,compact?80:128,compact?42:66,0xFF2C4762,12);
        int cols = kind == LAYOUT ? HomeUiPolicy.columns(selection) : 6;
        float gap=10,w=(560-(cols-1)*gap)/cols,h=w*9/16;
        float top=214-h;
        for(int i=0;i<cols;i++) {
            float x=20+i*(w+gap);boolean focus=i==Math.min(2,cols-1);
            int style=kind==TILES?selection:1;
            box(c,x,top,w,h,style==0?0x222E4C65:style==2?0xAA32435B:0xFF20384F,8);
            box(c,x+w*.33f,top+h*.25f,w*.34f,h*.5f,focus?0xFF97E4D9:0xFF87ACCD,6);
            if(focus){paint.setColor(0xFF9AE9DE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);rect.set(x-2,top-2,x+w+2,top+h+2);c.drawRoundRect(rect,9,9,paint);paint.setStyle(Paint.Style.FILL);}
        }
        if(kind==MOTION){paint.setColor(0xFFB6D2E3);paint.setTextSize(16);c.drawText(selection==3?"Still focus - no zoom or movement":selection==2?"Calm transitions":selection==1?"Playful spring focus":"Soft cinematic focus",20,top-18,paint);}
        c.restore();
    }
}
