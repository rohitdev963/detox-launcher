package com.ansub.tvhome;
import android.view.*;import android.os.SystemClock;import java.util.*;
/** A short, skippable foreground reveal; wallpaper itself stays visible. */
final class HomeEntrance {
 private static final class Item {View view;float alpha,y;long delay;}
 private final ArrayList<Item> items=new ArrayList<>();private long started;private boolean running;private Runnable done;
 private final Choreographer clock=Choreographer.getInstance();
 void add(View view,long delay){if(view==null||view.getVisibility()!=View.VISIBLE)return;for(Item existing:items)if(existing.view==view)return;Item i=new Item();i.view=view;i.alpha=view.getAlpha();i.y=view.getTranslationY();i.delay=delay;items.add(i);}
 void start(Runnable callback){done=callback;started=SystemClock.uptimeMillis();running=true;for(Item i:items){i.view.setAlpha(0);i.view.setTranslationY(i.y+12*i.view.getResources().getDisplayMetrics().density);}clock.postFrameCallback(frame);}
 boolean running(){return running;}
 private final Choreographer.FrameCallback frame=nanos->{if(!running)return;long elapsed=nanos/1000000-started;for(Item i:items){float p=HomeUiFlow.reveal(elapsed,i.delay);i.view.setAlpha(i.alpha*p);i.view.setTranslationY(i.y+(1-p)*12*i.view.getResources().getDisplayMetrics().density);}if(elapsed>=1000)finish();else clock.postFrameCallback(this.frame);};
 void finish(){if(!running)return;running=false;clock.removeFrameCallback(frame);for(Item i:items){i.view.setAlpha(i.alpha);i.view.setTranslationY(i.y);}items.clear();Runnable callback=done;done=null;if(callback!=null)callback.run();}
}
