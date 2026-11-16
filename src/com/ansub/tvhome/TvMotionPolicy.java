package com.ansub.tvhome;

/** Shared motion timing. Exact endpoints avoid tiny residual offsets after a transition. */
final class TvMotionPolicy {
    static float progress(float time){float t=Math.max(0,Math.min(1,time));return 1-(float)Math.pow(1-t,4);}
    static float settle(float time){float t=Math.max(0,Math.min(1,time));if(t==0||t==1)return t;float end=1-9*(float)Math.exp(-8);return (1-(1+8*t)*(float)Math.exp(-8*t))/end;}
    static long focusDuration(long sincePrevious,boolean calm){return sincePrevious>=0&&sincePrevious<140?130:calm?190:230;}
    static float focusScale(float requested,boolean calm,boolean reduced){return reduced?1:1+(Math.min(1.065f,Math.max(1,requested))-1)*(calm?.55f:1);}
}
