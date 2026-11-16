package com.ansub.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Redirects only Google TV's HOME window to Detox; it does not read window contents. */
public final class LauncherAccessibilityService extends AccessibilityService {
    private static volatile LauncherAccessibilityService connectedService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long lastSettingsKeyAt;

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        connectedService = this;
    }

    static boolean lockScreen() {
        LauncherAccessibilityService service = connectedService;
        return service != null && android.os.Build.VERSION.SDK_INT >= 28
                && service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    static boolean showPowerDialog() {
        LauncherAccessibilityService service = connectedService;
        return service != null && service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
    }
    static boolean companionBack(){LauncherAccessibilityService service=connectedService;return service!=null&&service.performGlobalAction(GLOBAL_ACTION_BACK);}

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (DetoxApplication.blocked(this)) return;
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !"com.google.android.apps.tv.launcherx".contentEquals(pkg)) return;
        Intent home = new Intent(this, MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(home); } catch (RuntimeException ignored) {}
    }

    @Override public boolean onKeyEvent(KeyEvent event) {
        if (DetoxApplication.blocked(this)) return false;
        if (event == null || !isTclSettingsKey(event)) return false;
        // TCL RC902V remotes report the dedicated Settings key as KEY_FORWARDMAIL
        // (Linux key 233; raw scan 0x000c0055). Consume both edges so Android cannot
        // open and immediately dismiss its own Settings screen.
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            long now = android.os.SystemClock.uptimeMillis();
            if (now - lastSettingsKeyAt > 500L) {
                lastSettingsKeyAt = now;
                mainHandler.post(() -> {
                    Intent open = new Intent(this, MainActivity.class)
                            .setAction(MainActivity.ACTION_OPEN_DETOX_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    try { startActivity(open); } catch (RuntimeException ignored) {}
                });
            }
        }
        return true;
    }

    private static boolean isTclSettingsKey(KeyEvent event) {
        int code = event.getKeyCode();
        int scan = event.getScanCode();
        return code == KeyEvent.KEYCODE_SETTINGS
                || scan == 233 || scan == 786517;
    }
    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        if (connectedService == this) connectedService = null;
        super.onDestroy();
    }
}
