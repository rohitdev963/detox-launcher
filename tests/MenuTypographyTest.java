package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;

public final class MenuTypographyTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static String section(String source,String start,String end){int a=source.indexOf(start),b=source.indexOf(end,a+start.length());check(a>=0&&b>a,"missing section "+start);return source.substring(a,b);}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of(args[0]).getParent(),root=dir.getParent().getParent().getParent().getParent();
  Set<String> files=new HashSet<>();
  for(String key:new String[]{"nunito","orbitron","playfair","pacifico","poppins"}){String asset=MenuTextPolicy.fontAsset(key);check(asset!=null&&files.add(asset)&&Files.size(root.resolve("assets").resolve(asset))>1000,"actual existing font "+key);}
  check(MenuTextPolicy.fontAsset("../other.ttf")==null&&MenuTextPolicy.fontAsset(null)==null,"font loader allowlist/fallback");
  for(int color:new int[]{0xFFFFFFFF,0xFF000000,0xFFFFD28A,0xFF9ADCF2,0xFF9B8CFF,0x7F123456})check(MenuTextPolicy.ink(color,0xFFAAE4C1)==color,"solid/alpha color must not become white");
  check(MenuTextPolicy.ink(MenuTextPolicy.WALLPAPER,0xFFAABBCC)==0xFFAABBCC,"wallpaper match");
  check(MenuTextPolicy.ink(MenuTextPolicy.RAINBOW,0)==0xFFFFFFFF&&MenuTextPolicy.RAINBOW_COLORS.length==8,"rainbow uses a shader instead of treating sentinel as near-transparent RGB");
  check(MenuTextPolicy.shadow(0xFF000000)==0xC0FFFFFF&&MenuTextPolicy.shadow(0xFFFFFFFF)==0xD9000000,"dark and light selections have contrasting shadows");
  check(MenuTextPolicy.scale(-1)==.8f&&MenuTextPolicy.scale(900)==1.4f,"invalid scale safety");
  for(int percent:new int[]{90,100,110,120,130,140})check(Math.abs(MenuTextPolicy.scale(percent)-percent/100f)<.0001,"supported scale preserved");
  check(MenuTextPolicy.headingSize(18)==24&&MenuTextPolicy.headingSize(18)/18>=1.25,"clear heading/body hierarchy");
  String home=Files.readString(Path.of(args[0])),shared=Files.readString(dir.resolve("MenuTypography.java")),dialog=Files.readString(dir.resolve("DetoxDialogStyle.java")),clock=Files.readString(dir.resolve("ClockUtilities.java")),preview=Files.readString(dir.resolve("ClockStylePreview.java"));
  String homeStyle=section(home,"private void styleMenuText(","private void styleUtilityTitle(");
  check(!homeStyle.contains("setTextColor(")&&homeStyle.contains("MenuTypography.set(text,true,menuPx(34))")&&homeStyle.contains("MenuTypography.adoptScaled(text,false)"),"Home has distinct heading/option roles without fixed-white overrides");
  String values=section(home,"private void setMenuLabel(","private void styleMenuText(");check(!values.contains("ForegroundColorSpan"),"option values must inherit selected color");
  check(!dialog.contains("sans-serif-medium")&&!dialog.contains("setTextColor(0xFFF5F8FF)")&&dialog.contains("MenuTypography.tree(window.getDecorView())"),"all standard nested menus share font/color including custom content");
  check(shared.contains("style.heading?Typeface.BOLD:Typeface.NORMAL")&&shared.contains("WeakHashMap<TextView,Style>")&&shared.contains("WeakReference<MainActivity>"),"cached family roles without retained Activity/view owners");
  check(shared.contains("style.shaderWidth!=width")&&shared.contains("setShader(null)")&&!shared.contains("postDelayed")&&!shared.contains(".edit()"),"rainbow width cache, solid restoration, no timers or preference writes");
  check(home.contains("MenuTypography.managed(view))return")&&home.contains("previewFontColorOverride = which == 0 ? wallpaperAccentColor()")&&home.contains("MenuTypography.refresh(this)"),"menu color preview cannot be overwritten by older rainbow callbacks");
  String restore=section(home,"private void restorePreviewSettings()","private int px(");check(restore.contains("previewFontKey = null")&&restore.contains("previewFontColorOverride = null")&&restore.contains("loadFonts()"),"Cancel restores saved family/color");
  check(clock.contains("MenuTypography.set(view,false,px(size))")&&clock.contains("MenuTypography.set(title,true,px(36))")&&clock.contains("MenuTypography.refresh(view)"),"Clock Utilities including focus obey typography");
  check(preview.contains("MenuTypography.set(row,false,"),"clock selector reused rows cannot force a fixed font/color");
  System.out.println("Menu typography: five bundled font files, solid/wallpaper/rainbow colors, scale/contrast, heading hierarchy, nested-dialog integration and preview rollback contracts passed (host/source checks)");
 }
}
