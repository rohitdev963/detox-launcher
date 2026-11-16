package com.ansub.tvhome;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Minute-paced idle clock with adjustable shading and gentle text drift. */
final class AmbientInfoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private final Typeface clockFace = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface detailFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private boolean active;
    private String time = "", date = "", nextAlarm = "";
    private Runnable shifted;

    AmbientInfoView(Context context) {
        super(context); prefs = context.getSharedPreferences("config", 0);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void active(boolean value, Runnable moved) {
        active = value; shifted = moved; handler.removeCallbacks(tick);
        if (active) tick.run();
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            Date now = new Date();
            boolean use24Hour = android.text.format.DateFormat.is24HourFormat(getContext());
            time = new SimpleDateFormat(use24Hour ? "HH:mm" : "h:mm a", Locale.getDefault()).format(now);
            date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now);
            nextAlarm = "";
            if (prefs.getBoolean("idleShowAlarm", true)) try {
                AlarmManager manager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                AlarmManager.AlarmClockInfo alarm = manager == null ? null : manager.getNextAlarmClock();
                if (alarm != null) nextAlarm = "Next alarm · " + new SimpleDateFormat(
                        use24Hour ? "EEE, HH:mm" : "EEE, h:mm a", Locale.getDefault()).format(new Date(alarm.getTriggerTime()));
            } catch (RuntimeException ignored) {}
            if (shifted != null) shifted.run();
            invalidate(); handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000);
        }
    };
    @Override protected void onDetachedFromWindow() {
        active = false; handler.removeCallbacks(tick); super.onDetachedFromWindow();
    }
    @Override protected void onDraw(Canvas canvas) {
        int shade = Math.max(0, Math.min(70, prefs.getInt("idleWallpaperShade", 54)));
        canvas.drawColor(Color.argb(Math.round(shade * 2.55f), 3, 10, 18));
        if (!prefs.getBoolean("ambientClockEnabled", true) || getWidth() == 0) return;
        float scale = getWidth() / 1920f;
        float size = Math.max(70, Math.min(130, prefs.getInt("idleClockScale", 100))) / 100f;
        int position = Math.max(0, Math.min(2, prefs.getInt("idleClockPosition", 0)));
        float logicalHeight = getHeight() / scale;
        long minute = System.currentTimeMillis() / 60000;
        canvas.save(); canvas.scale(scale, scale);
        paint.setTypeface(clockFace); paint.setTextSize(132 * size);
        float textWidth = paint.measureText(time);
        if (textWidth > 1600) { paint.setTextSize(paint.getTextSize() * 1600 / textWidth); textWidth = 1600; }
        float center = position == 0 ? 145 : position == 1 ? 960 : 1775;
        paint.setTextAlign(position == 0 ? Paint.Align.LEFT : position == 1 ? Paint.Align.CENTER : Paint.Align.RIGHT);
        float x = center + HomeUiFlow.drift(minute, true);
        float y = Math.max(170 * size, Math.min(logicalHeight * .43f, logicalHeight - 200 * size))
                + HomeUiFlow.drift(minute, false);
        paint.setColor(0xE6E2EBF2); canvas.drawText(time, x, y, paint);
        paint.setTypeface(detailFace);
        if (prefs.getBoolean("idleShowDate", true)) {
            y += 56 * size; paint.setColor(0xD6D4E0EB); drawDetail(canvas, date, x, y, 29 * size);
        }
        if (prefs.getBoolean("idleShowAlarm", true) && !nextAlarm.isEmpty()) {
            y += 47 * size; paint.setColor(0xC6C7DBEA); drawDetail(canvas, nextAlarm, x, y, 24 * size);
        }
        canvas.restore();
    }
    private void drawDetail(Canvas canvas, String value, float x, float y, float size) {
        paint.setTextSize(size);
        float width = paint.measureText(value);
        float available = paint.getTextAlign() == Paint.Align.CENTER ? 1600 : 1580;
        if (width > available) paint.setTextSize(size * available / width);
        canvas.drawText(value, x, y, paint);
    }
}
