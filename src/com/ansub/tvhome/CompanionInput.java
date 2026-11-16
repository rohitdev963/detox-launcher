package com.ansub.tvhome;

import android.app.Activity;
import android.view.KeyEvent;
import java.lang.ref.WeakReference;

/** Routes paired input to the currently resumed Detox activity, never an arbitrary external app. */
final class CompanionInput {
    private static WeakReference<Activity> foreground = new WeakReference<>(null);
    private static WeakReference<android.app.Dialog> dialog = new WeakReference<>(null);
    static void dialog(android.app.Dialog value){dialog=new WeakReference<>(value);}
    static void clearDialog(android.app.Dialog owner){if(dialog.get()==owner)dialog.clear();}
    static void resume(Activity activity) { foreground = new WeakReference<>(activity); }
    static void pause(Activity activity) { if(foreground.get()==activity)foreground.clear(); }
    static boolean ready() { Activity activity=foreground.get(); return activity!=null&&!activity.isFinishing(); }
    static boolean key(int code) {
        Activity activity=foreground.get();if(activity==null||activity.isFinishing())return false;
        android.app.Dialog modal=dialog.get();
        long at=android.os.SystemClock.uptimeMillis();
        if(modal!=null&&modal.isShowing()&&modal.getWindow()!=null&&modal.getWindow().getDecorView().hasWindowFocus()){boolean down=modal.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_DOWN,code,0));boolean up=modal.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_UP,code,0));return down||up;}
        boolean down=activity.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_DOWN,code,0));
        boolean up=activity.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_UP,code,0));return down||up;
    }
}
