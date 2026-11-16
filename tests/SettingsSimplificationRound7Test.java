import java.nio.file.*;
public class SettingsSimplificationRound7Test {
  static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
  public static void main(String[] args)throws Exception{
    String s=Files.readString(Path.of(args[0]));
    String catalogue=Files.readString(Path.of(args[0]).getParent().resolve("SettingsMenuRoutes.java"));
    check(catalogue.contains("\"apps.down\",\"App Launch on DOWN\",APPS,\"showHomeDownLaunchOptions\""),"DOWN named route");
    check(s.contains("case \"apps.down\": showHomeDownLaunchOptions(); break;"),"DOWN dispatch");
    check(s.contains("putString(HOME_DOWN_LAUNCH_PACKAGE,packages.get(which))"),"DOWN saved app selection");
    check(s.contains("SettingsPreferencePolicy.utilityResetKeys(prefs.getAll().keySet())"),"reset ownership policy");
    check(!s.contains(".remove(\"themePreset\")"),"Utilities reset must keep global theme");
    int c=s.indexOf("private void showHighContrastFocusOptions()"),d=s.indexOf("private void showIconPackOptions()",c);
    check(c>=0&&d>c,"contrast method");
    String contrast=s.substring(c,d);
    check(contrast.contains("showInterfaceMotionOptions();"),"contrast return route");
    check(!contrast.contains("returnToSettingsCategory(1);"),"contrast incorrectly exits parent");
    check(catalogue.contains("Animations & Focus"),"focus category title");
    check(s.contains("choices.add(\"Match utilities colour\");"),"clock color label");
    System.out.println("Settings simplification: named DOWN routing, isolated reset, focus and clock matching passed");
  }
}
