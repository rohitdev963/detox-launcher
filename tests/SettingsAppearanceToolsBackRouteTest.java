import java.nio.file.*;
public class SettingsAppearanceToolsBackRouteTest {
  static String section(String source, String method, String next) {
    int a=source.indexOf("private void "+method+"() {");
    if(a<0)throw new AssertionError("Missing "+method);
    int b=source.indexOf("private ",a+15);
    return source.substring(a,b<0?source.length():b);
  }
  static void check(String source,String method,String route) {
    String body=section(source,method,"");
    if(!body.contains("routeNextDialogBack("+route+")"))throw new AssertionError(method+" missing route "+route);
  }
  public static void main(String[] args)throws Exception {
    String s=Files.readString(Path.of(args[0]));
    check(s,"showBackupRestoreOptions","() -> showSettingsGroup(SettingsMenuRoutes.BACKUP)");
    check(s,"showRemoteShortcutOptions","() -> returnToSettingsCategory(SettingsMenuRoutes.SYSTEM)");
    check(s,"showSleepTimerOptions","() -> returnToSettingsCategory(SettingsMenuRoutes.SYSTEM)");
    check(s,"showHighContrastFocusOptions","this::showInterfaceMotionOptions");
    check(s,"showAppArtworkVisibilityOptions","() -> showSettingsGroup(SettingsMenuRoutes.ICONS)");
    check(s,"showIconPackOptions","() -> showSettingsGroup(SettingsMenuRoutes.ICONS)");
    if(!section(s,"showAppArtworkVisibilityOptions","").contains("showSettingsGroup(SettingsMenuRoutes.ICONS);")) throw new AssertionError("Idle artwork save route");
    System.out.println("Appearance/Tools BACK route checks: 7 passed");
  }
}
