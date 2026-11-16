package com.ansub.tvhome;
import java.nio.file.*;

public final class BottomAppRowsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  int cases=0;
  for(int width:new int[]{1280,1920,3840})for(int mode=0;mode<3;mode++)for(int count:new int[]{0,1,4,6,8,12,25,60})for(boolean headings:new boolean[]{false,true})for(boolean below:new boolean[]{false,true}){
   float scale=width/1920f;int height=Math.round(width*9/16f),reserve=below?Math.round(116*scale):0;
   HomeUiPolicy.Layout layout=HomeUiPolicy.layout(width,height,scale,mode,count,Math.round(32*scale),0,Math.round(320*scale),reserve,headings);
   int last=layout.y((layout.rows-1)*layout.columns),edge=Math.max(1,Math.round(8*scale));
   check(layout.contentHeight-last-layout.tileHeight-reserve==edge,"unnecessary footer: "+width+"/"+mode+"/"+count);
   for(int labels=0;labels<3;labels++){
    check(HomeUiPolicy.focusTop(labels,scale)==-Math.max(1,Math.round((labels==2?5:58)*scale)),"focus must include captions above cards");
    check(HomeUiPolicy.focusBottom(layout.tileHeight,scale)-layout.tileHeight<=edge,"focus rectangle cannot invent a footer below the viewport");
   }
   check(layout.top>=Math.round(320*scale)&&layout.contentHeight>=height,"clock/minimum viewport clearance");
   for(int row=1;row<layout.rows;row++)check(layout.y(row*layout.columns)-layout.y((row-1)*layout.columns)>=layout.tileHeight+Math.round(90*scale),"captions must not overlap the previous row");
   for(int i=0;i<count;i++){int next=HomeUiPolicy.downIndex(i,count,layout.columns);check(next==-1||next>i&&next<count,"DPAD still targets a valid app row");}
   cases++;
  }
  Path dir=Path.of(args[0]).getParent();String home=Files.readString(Path.of(args[0])),game=Files.readString(dir.resolve("GamingActivity.java"));
  check(!home.contains("dockPanel")&&!home.contains("appGlassPanel"),"dock surface remnants");
  check(home.contains("wrap.addView(caption,at(0,-px(58),w,px(46)))")&&home.contains("parent.addView(wrap, at(x, y, w, h))"),"Home captions above bottom-anchored cards");
  check(home.contains("requestRectangleOnScreen(homeFocusBounds(view)")&&home.contains("requestRectangleOnScreen(homeFocusBounds(card))")&&!home.contains("view.getHeight()+(rowApps.contains(view.getTag())?px(62)"),"focus/reorder scrolling cannot retain old below-card caption bounds");
  check(game.contains("artPanel,new FrameLayout.LayoutParams(-1,artHeight,Gravity.BOTTOM)")&&game.contains("nameLp=new FrameLayout.LayoutParams(-1,captionHeight,Gravity.TOP)"),"Gaming captions above bottom artwork");
  check(!game.contains("View floor=new View")&&!game.contains("h-dp(7)")&&game.contains("SYSTEM_UI_FLAG_HIDE_NAVIGATION")&&game.contains("if(focus)configureGamingWindow()"),"decorative bottom strip removed; immersive mode restored on return");
  check(SettingsMenuRoutes.find("utility.style")==null,"removed style editor must not remain searchable");
  check(SettingsMenuRoutes.find("utility.size")!=null&&SettingsMenuRoutes.find("utility.visibility")!=null&&SettingsMenuRoutes.find("utility.motion")!=null,"independent utility settings retained");
  check(home.contains("BundledUtilityArtwork.home(this,key)")&&home.contains("utilityFocusScale(officialArtwork)"),"authentic artwork and remote focus retained");
  check(!home.contains("getString(\"utilityAppearanceStyle\"")&&!home.contains("utilityIconSurface("),"saved retired style cannot recolor or frame icons");
  for(String retired:new String[]{"UtilityIconAppearance","UtilityIconSurfaceDrawable","UtilityGlyphDrawable","UtilityFocusTransferOverlay"})check(!Files.exists(dir.resolve(retired+".java")),"retired style renderer still present");
  System.out.println("Bottom app rows: "+cases+" Home geometry cases at 720p/1080p/4K, caption/DPAD clearance, Gaming bottom placement, dock removal and utility-style retirement passed (host/source checks)");
 }
}
