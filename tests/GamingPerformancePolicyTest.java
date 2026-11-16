package com.ansub.tvhome;

public final class GamingPerformancePolicyTest {
    public static void main(String[] args) {
        check(GamingPerformancePolicy.DEFAULT == 1, "balanced default");
        for(int hz:new int[]{60,120})for(int preset=0;preset<3;preset++){
            int expected=GamingPerformancePolicy.targetHz(preset,hz)*10,count=0;long next=0;
            for(int frame=0;frame<hz*10;frame++){
                long now=1000000000L+Math.round(frame*1000000000d/hz);
                if(now>=next){count++;next=GamingPerformancePolicy.nextFrameDeadline(next,now,GamingPerformancePolicy.frameIntervalNanos(preset,hz));}
            }
            check(Math.abs(count-expected)<=1,"preset pacing preserves target average across vsyncs");
        }
        check(GamingPerformancePolicy.targetHz(0, 60f) == 30, "smooth caps at 30");
        check(GamingPerformancePolicy.targetHz(1, 60f) == 45, "balanced caps at 45");
        check(GamingPerformancePolicy.targetHz(2, 60f) == 60, "cinematic uses 60 on a 60 Hz display");
        check(GamingPerformancePolicy.targetHz(2, 30f) == 30, "preset never exceeds display refresh");
        check(GamingPerformancePolicy.targetHz(1, Float.NaN) == 45, "invalid refresh uses safe 60 Hz fallback");
        check(GamingPerformancePolicy.targetHz(-1, 60f) == 30, "preset index clamps to smooth");
        check(GamingPerformancePolicy.targetHz(99, 60f) == 60, "preset index clamps to cinematic");
        check(GamingPerformancePolicy.targetHz(2, 120f) == 60, "cinematic preset has a bounded 60 Hz ceiling");
        check(GamingPerformancePolicy.targetHz(2, 59.94f) <= 59, "preset never rounds above a fractional display refresh rate");
        System.out.println("Gaming performance presets: refresh cap and active clock pacing passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
