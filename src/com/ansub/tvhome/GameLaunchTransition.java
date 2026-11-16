package com.ansub.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;

/** A short launch handoff. Leaving Gaming cancels every pending callback. */
final class GameLaunchTransition {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final FrameLayout parent,overlay;
    private final GamingMusicPlayer music;
    private final View previousFocus;
    private boolean running=true;

    GameLaunchTransition(Activity activity,FrameLayout parent,GamingMusicPlayer music,String title,boolean animate,int preset,Runnable launch){
        this.parent=parent;this.music=music;previousFocus=activity.getCurrentFocus();
        overlay=new FrameLayout(activity);overlay.setBackgroundColor(0xF2080B12);
        overlay.setFocusable(true);overlay.setFocusableInTouchMode(true);overlay.setClickable(true);
        overlay.setContentDescription("Opening "+title);
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);
        float density=activity.getResources().getDisplayMetrics().density;
        content.setPadding((int)(48*density),0,(int)(48*density),0);
        TextView name=new TextView(activity);name.setText(title);name.setTextColor(Color.WHITE);name.setTextSize(36);
        name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));name.setGravity(Gravity.CENTER);name.setMaxLines(2);
        content.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView status=new TextView(activity);status.setText("Opening game…");status.setTextColor(0xFFC6D4E2);status.setTextSize(16);
        status.setGravity(Gravity.CENTER);status.setPadding(0,(int)(16*density),0,0);content.addView(status);
        overlay.addView(content,new FrameLayout.LayoutParams(-1,-1));
        overlay.setOnKeyListener((v,key,event)->{
            if((key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_BUTTON_B)&&event.getAction()==KeyEvent.ACTION_UP)cancel(true);
            return true;
        });
        parent.addView(overlay,new FrameLayout.LayoutParams(-1,-1));overlay.requestFocus();
        int mode=GamingPerformancePolicy.normalize(preset);
        long duration=!animate?0:mode==0?280:mode==2?680:470;
        if(animate){overlay.setAlpha(0);overlay.animate().alpha(1).setDuration(Math.max(120,duration*2/5)).start();content.setTranslationY(12*density);content.animate().translationY(0).setDuration(Math.max(160,duration*3/5)).start();}
        if(music!=null)music.fadeForLaunch(animate?(int)Math.max(0,duration-80):0);
        handler.postDelayed(()->{if(running&&!activity.isFinishing()&&!activity.isDestroyed())launch.run();},duration);
    }
    boolean running(){return running;}
    void cancel(boolean resumeMusic){
        running=false;handler.removeCallbacksAndMessages(null);overlay.animate().cancel();parent.removeView(overlay);
        if(music!=null){music.cancelLaunchFade();if(resumeMusic)music.active(true);}
        if(previousFocus!=null&&previousFocus.isAttachedToWindow())previousFocus.requestFocus();
    }
}
