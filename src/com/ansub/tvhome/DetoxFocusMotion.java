package com.ansub.tvhome;

import android.view.View;
import android.view.WindowManager;
import android.content.Context;

/** One-shot focus animation; no per-frame polling or persistent animation loop. */
public final class DetoxFocusMotion {
    private DetoxFocusMotion() {}

    /** Align duration to display frames, but do not change Android's vsync-driven animator. */
    public static long frameAlignedDuration(View view, long desiredMs) {
        float hz = 60f;
        try {
            WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm != null && wm.getDefaultDisplay() != null) {
                float reported = wm.getDefaultDisplay().getRefreshRate();
                if (reported >= 24f && reported <= 240f) hz = reported;
            }
        } catch (RuntimeException ignored) { }
        double frameMs = 1000d / hz;
        return Math.max(1L, Math.round(Math.max(1d, Math.round(desiredMs / frameMs)) * frameMs));
    }

    public static void scale(View view, boolean focused, boolean enabled) {
        view.animate().cancel();
        float target = focused ? 1.035f : 1f;
        if (!enabled || !view.isAttachedToWindow()) {
            view.setScaleX(target);
            view.setScaleY(target);
            return;
        }
        view.animate().scaleX(target).scaleY(target)
                .setDuration(frameAlignedDuration(view, 160L)).start();
    }
}
