package com.ansub.tvhome;

import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import java.util.ArrayList;

/** One display-synchronised callback for focus and panels. Retargets from current properties. */
final class TvMotion {
    private static final int FOCUS=0,PANEL=1,SURFACE=2;
    private final ArrayList<Track> tracks=new ArrayList<>();
    private final ArrayList<Track> completed=new ArrayList<>();
    private final Choreographer clock=Choreographer.getInstance();
    private boolean posted;private long lastFocus=-1000;
    private static final class Track {
        View view;int kind;long start,duration;float sx,sy,y,z,x,toScale,toY,toZ,toX,alpha,toAlpha;Runnable after,cancelled;
        void apply(float t){float p=kind==FOCUS?TvMotionPolicy.settle(t):TvMotionPolicy.progress(t);
            if(kind==FOCUS){view.setScaleX(sx+(toScale-sx)*p);view.setScaleY(sy+(toScale-sy)*p);view.setTranslationY(y+(toY-y)*p);view.setTranslationZ(z+(toZ-z)*p);}
            else if(kind==SURFACE){view.setAlpha(alpha+(toAlpha-alpha)*p);view.setScaleX(sx+(toScale-sx)*p);view.setScaleY(sy+(toScale-sy)*p);}
            else{view.setAlpha(alpha+(toAlpha-alpha)*p);view.setScaleX(sx+(toScale-sx)*p);view.setScaleY(sy+(toScale-sy)*p);view.setTranslationX(x+(toX-x)*p);}}
    }
    private final Choreographer.FrameCallback frame=timestamp->{
        posted=false;long now=timestamp/1000000;
        for(int i=tracks.size()-1;i>=0;i--){Track t=tracks.get(i);if(!t.view.isAttachedToWindow()){tracks.remove(i);if(t.cancelled!=null)completed.add(t);continue;}
            float progress=t.duration<=0?1f:Math.min(1f,Math.max(0f,(now-t.start)/(float)t.duration));t.apply(progress);
            if(progress>=1){tracks.remove(i);completed.add(t);}}
        // Completion may create the next panel. Finish collecting first so callbacks cannot
        // corrupt traversal or accidentally complete a replacement animation twice.
        while(!completed.isEmpty()){Track done=completed.remove(0);if(!done.view.isAttachedToWindow()){if(done.cancelled!=null)done.cancelled.run();}else if(done.after!=null)done.after.run();}
        schedule();
    };
    private void schedule(){if(!tracks.isEmpty()&&!posted){posted=true;clock.postFrameCallback(frame);}}
    private void remove(View view,int kind){for(int i=tracks.size()-1;i>=0;i--)if(tracks.get(i).view==view&&tracks.get(i).kind==kind)tracks.remove(i);stopIfIdle();}
    void focus(View view,boolean focused,float requested,float lift,float elevation,boolean calm,boolean reduced){
        float targetScale=focused?TvMotionPolicy.focusScale(requested,calm,reduced):1f;
        float targetY=focused&&!reduced?-lift:0f;
        float targetZ=focused&&!reduced?elevation:0f;
        // DPAD focus callbacks can repeat while a spring is already in flight. Retargeting
        // to the same endpoint restarts its clock and can make the highlight lag or stall.
        // Keep the existing track unless the actual endpoint changes.
        if(!reduced){
            for(int i=tracks.size()-1;i>=0;i--){
                Track active=tracks.get(i);
                if(active.view==view&&active.kind==FOCUS){
                    if(active.toScale==targetScale&&active.toY==targetY&&active.toZ==targetZ)
                        return;
                    break;
                }
            }
        }
        remove(view,FOCUS);
        long now=SystemClock.uptimeMillis(),duration=TvMotionPolicy.focusDuration(now-lastFocus,calm);
        if(focused)lastFocus=now;
        Track t=new Track();t.view=view;t.kind=FOCUS;t.start=now;t.duration=duration;t.sx=view.getScaleX();t.sy=view.getScaleY();t.y=view.getTranslationY();t.z=view.getTranslationZ();
        t.toScale=targetScale;t.toY=targetY;t.toZ=targetZ;
        if(reduced){t.apply(1);return;}tracks.add(t);schedule();
    }
    void panel(View view,float targetX,long duration,Runnable after){
        panel(view,targetX,duration,after,null);
    }
    void panel(View view,float targetX,long duration,Runnable after,Runnable cancelled){
        remove(view,PANEL);view.animate().cancel();Track t=new Track();t.view=view;t.kind=PANEL;t.start=SystemClock.uptimeMillis();t.duration=duration;t.x=view.getTranslationX();t.toX=targetX;t.alpha=view.getAlpha();t.toAlpha=targetX==0?1f:0f;t.sx=view.getScaleX();t.sy=view.getScaleY();t.toScale=targetX==0?1f:.985f;t.after=after;t.cancelled=cancelled;
        if(duration<=0){t.apply(1);if(after!=null)after.run();return;}tracks.add(t);schedule();
    }
    void surface(View view,float alpha,float scale,long duration,Runnable after,Runnable cancelled){
        remove(view,SURFACE);view.animate().cancel();Track t=new Track();t.view=view;t.kind=SURFACE;t.start=SystemClock.uptimeMillis();t.duration=duration;t.alpha=view.getAlpha();t.toAlpha=alpha;t.sx=view.getScaleX();t.sy=view.getScaleY();t.toScale=scale;t.after=after;t.cancelled=cancelled;if(duration<=0){t.apply(1);if(after!=null)after.run();return;}tracks.add(t);schedule();
    }
    void cancelFocus(){for(int i=tracks.size()-1;i>=0;i--)if(tracks.get(i).kind==FOCUS){View v=tracks.remove(i).view;v.setScaleX(1);v.setScaleY(1);v.setTranslationY(0);v.setTranslationZ(0);}stopIfIdle();}
    void cancelAll(){
        // Snap focus to its intended state, and close an interrupted exiting menu without
        // executing its selected action after the activity has gone into the background.
        // Detach the active set before invoking callbacks. A cancellation callback can
        // dismiss a dialog and synchronously schedule another transition; mutating the
        // same list during iteration would otherwise skip tracks or throw.
        ArrayList<Track> abandoned=new ArrayList<>(tracks);
        tracks.clear();completed.clear();clock.removeFrameCallback(frame);posted=false;
        for(int i=abandoned.size()-1;i>=0;i--){
            Track t=abandoned.get(i);
            if(t.kind==FOCUS){if(t.view.isAttachedToWindow())t.apply(1);}
            else if(t.cancelled!=null)t.cancelled.run();
        }
    }
    private void stopIfIdle(){if(tracks.isEmpty()&&posted){clock.removeFrameCallback(frame);posted=false;}}
}
