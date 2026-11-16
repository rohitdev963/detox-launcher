package com.ansub.tvhome;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.FocusFinder;
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
        if(modal!=null&&modal.isShowing()&&modal.getWindow()!=null&&modal.getWindow().getDecorView().hasWindowFocus()){
            boolean down=modal.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_DOWN,code,0));
            boolean navigation=!down&&navigate(modal.getWindow().getDecorView(),code);
            boolean up=modal.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_UP,code,0));return down||navigation||up;
        }
        boolean down=activity.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_DOWN,code,0));
        boolean navigation=!down&&activity.getWindow()!=null&&navigate(activity.getWindow().getDecorView(),code);
        boolean up=activity.dispatchKeyEvent(new KeyEvent(at,at,KeyEvent.ACTION_UP,code,0));return down||navigation||up;
    }
    /** Direct Activity/Dialog dispatch bypasses ViewRootImpl's normal D-pad focus fallback. */
    private static boolean navigate(View root,int code){
        int direction=code==KeyEvent.KEYCODE_DPAD_UP?View.FOCUS_UP:code==KeyEvent.KEYCODE_DPAD_DOWN?View.FOCUS_DOWN
                :code==KeyEvent.KEYCODE_DPAD_LEFT?View.FOCUS_LEFT:code==KeyEvent.KEYCODE_DPAD_RIGHT?View.FOCUS_RIGHT:0;
        if(direction==0||!(root instanceof ViewGroup)||!root.hasWindowFocus())return false;
        View current=root.findFocus();
        View next=current==null?FocusFinder.getInstance().findNextFocus((ViewGroup)root,null,direction):current.focusSearch(direction);
        if(next!=null&&next!=current){
            // Remote commands may arrive after phone/air-mouse touches put the TV in touch mode.
            boolean focused=next.isInTouchMode()?next.requestFocusFromTouch():next.requestFocus(direction);
            if(focused){next.requestRectangleOnScreen(new android.graphics.Rect(0,0,next.getWidth(),next.getHeight()),false);return true;}
        }
        // Reaching a valid focus boundary is an accepted key, not a connection failure.
        return current!=null;
    }
}
