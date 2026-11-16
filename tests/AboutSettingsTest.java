package com.ansub.tvhome;
import java.nio.file.*;

public final class AboutSettingsTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  check(SettingsMenuRoutes.categoryCount()==7,"existing hierarchy preserved");
  check(SettingsMenuRoutes.find("system.storage")==null&&SettingsMenuRoutes.find("system.integrations")==null,"removed options must not remain searchable");
  SettingsMenuRoutes.Entry about=SettingsMenuRoutes.find("system.about");
  check(about!=null&&about.parent.equals(SettingsMenuRoutes.SYSTEM)&&about.handler.equals("showAbout")&&about.label.equals("About"),"About canonical route");
  String main=Files.readString(Path.of(args[0]));
  check(main.contains("case \"system.about\": showAbout(); break;"),"named dispatch");
  check(!main.contains("showTvIntegrations()")&&!main.contains("openLauncherStorage()")&&!main.contains("\"TV integrations\""),"no stale entry/wrapper/Quick Controls alias");
  int a=main.indexOf("private void showAbout()"),b=main.indexOf("private void showSettingsSearch()",a);String dialog=main.substring(a,b);
  check(dialog.contains("detoxAlertBuilder()")&&dialog.contains("DetoxAboutInfo.summary(this)")&&dialog.contains("returnToSettingsCategory(SettingsMenuRoutes.SYSTEM)"),"glass style and explicit Back destination");
  check(main.contains("routeNextDialogBack(()->showSettingsGroup(entry.parent))"),"hardware Back inherits immediate Settings parent");
  check(main.contains("visible.size() + 3")&&main.contains("which == visible.size()+2) { openGamingScreen()")&&main.contains("which == visible.size()+1) { showHomeTools()"),"Quick Controls indexes remain valid after removal");
  String info=Files.readString(Path.of(args[0]).getParent().resolve("DetoxAboutInfo.java"));
  check(info.contains("getPackageInfo(context.getPackageName(),0)")&&info.contains("info.versionName")&&info.contains("info.getLongVersionCode()")&&info.contains("Build.VERSION.SDK_INT>=28"),"real version metadata and old Android compatibility");
  check(info.contains("info.lastUpdateTime>0")&&info.contains("Intent.CATEGORY_HOME")&&info.contains("home.activityInfo==null"),"real update/default Home data with unavailable fallback");
  check(!info.contains("SharedPreferences")&&!info.contains("startActivity(")&&!info.contains("getSerial(")&&!info.contains("https://"),"About is read only, local and does not collect identifiers");
  System.out.println("About Settings: removed routes/alias, seven categories, real local metadata, glass dialog, Back and Quick Controls dispatch contracts passed (host/source checks)");
 }
}
