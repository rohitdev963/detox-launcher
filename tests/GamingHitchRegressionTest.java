package com.ansub.tvhome;
import java.nio.file.*;

public final class GamingHitchRegressionTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 static String section(String source,String from,String to){int a=source.indexOf(from),b=source.indexOf(to,a+from.length());check(a>=0&&b>a,"missing section "+from);return source.substring(a,b);}
 public static void main(String[] args)throws Exception{
  int legacy=0,fixed=0;long last=0,next=0,interval=GamingPerformancePolicy.frameIntervalNanos(1,60);
  for(int i=0;i<600;i++){long now=1000000000L+Math.round(i*1000000000d/60);if(last==0||now-last>=interval){legacy++;last=now;}if(now>=next){fixed++;next=GamingPerformancePolicy.nextFrameDeadline(next,now,interval);}}
  check(legacy==300&&Math.abs(fixed-450)<=1,"reproduce and fix Balanced collapsing from 45 to 30 draws/sec");
  for(double hz:new double[]{24,30,50,59.94,60,90,120})for(int preset=0;preset<3;preset++){
   next=0;int count=0;for(int i=0;i<(int)(hz*10);i++){long now=1000000000L+Math.round(i*1000000000d/hz);if(now>=next){count++;next=GamingPerformancePolicy.nextFrameDeadline(next,now,GamingPerformancePolicy.frameIntervalNanos(preset,(float)hz));}}
   check(Math.abs(count-GamingPerformancePolicy.targetHz(preset,(float)hz)*10)<=2,"average cadence "+hz+" preset "+preset);
  }
  check(GamingPerformancePolicy.nextFrameDeadline(100,9000000000L,interval)==9000000000L,"long stalls skip missed deadlines without catch-up loop");
  Path dir=Path.of(args[0]).getParent();String g=Files.readString(dir.resolve("GamingActivity.java")),preview=Files.readString(dir.resolve("GamingThemePreview.java"));
  check(!g.contains("Choreographer.FrameCallback")&&!g.contains("postFrameCallback")&&!g.contains("GamingLivingAtmosphere")&&!g.contains("DetoxShaderView"),"retired background loops no longer run or restart behind menus");
  check(!Files.exists(dir.resolve("DisplayRefreshRate.java")),"unused retired background refresh helper removed");
  String card=section(g,"private View gameCard(","private void invalidateGamingArtwork");check(card.contains("focusedPackage=pkg;")&&!card.contains("applyLivingScene("),"game focus remains without removed scene effects");
  String focus=section(g,"private void focusScale(","private long motionDuration(");check(focus.contains("target.animate().cancel()")&&focus.contains("target.setScaleX(1f);target.setScaleY(1f);return;"),"reduced motion cancels and resets focus scaling");
  check(g.contains("railAction(\"theme\",\"◈\",\"Theme\",this::showThemePicker)")&&g.contains("showThemePicker(){showThemePicker(null);}"),"quick Theme resets stale Settings parent");
  check(preview.contains("KEYCODE_BUTTON_B")&&preview.contains("gate.accept(KeyEvent.KEYCODE_BACK"),"controller B cancels preview rather than leaving Gaming");
  System.out.println("Gaming hitch regressions: policy simulation reproduced 300 vs corrected "+fixed+" draws/10s; 21 cadence combinations, retired-loop absence, focus, reduced motion and preview Back contracts passed");
 }
}
