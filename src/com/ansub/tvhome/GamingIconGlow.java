package com.ansub.tvhome;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.view.*;

/** One focused-card halo; pauses with its window and releases on detach. */
final class GamingIconGlow implements View.OnAttachStateChangeListener,ViewTreeObserver.OnWindowFocusChangeListener {
 private final View owner,target;private final Drawable glow;private ValueAnimator animator;private boolean focused,animated,breathing;
 GamingIconGlow(View owner,View target,Drawable glow){this.owner=owner;this.target=target;this.glow=glow;owner.addOnAttachStateChangeListener(this);}
 void focus(boolean focused,boolean animated,boolean breathing){this.focused=focused;this.animated=animated;this.breathing=breathing;refresh();}
 private void stop(){if(animator!=null){animator.cancel();animator=null;}}
 private void refresh(){stop();
  if(!focused){glow.setAlpha(0);target.invalidate();return;}
  if(!animated||!owner.isAttachedToWindow()||!owner.hasWindowFocus()||!breathing){glow.setAlpha(220);target.invalidate();return;}
  animator=ValueAnimator.ofInt(155,225);animator.setDuration(DetoxFocusMotion.frameAlignedDuration(owner,1600));animator.setRepeatCount(ValueAnimator.INFINITE);animator.setRepeatMode(ValueAnimator.REVERSE);
  animator.addUpdateListener(a->{glow.setAlpha((Integer)a.getAnimatedValue());target.invalidate();});animator.start();
 }
 public void onViewAttachedToWindow(View v){v.getViewTreeObserver().addOnWindowFocusChangeListener(this);refresh();}
 public void onViewDetachedFromWindow(View v){stop();if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnWindowFocusChangeListener(this);}
 public void onWindowFocusChanged(boolean hasFocus){if(!hasFocus){stop();glow.setAlpha(focused?220:0);target.invalidate();}else refresh();}
}
