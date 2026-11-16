import java.nio.file.*;
public class SettingsShortcutBackRouteTest {
  static void check(String s,String method,String route){
    int start=s.indexOf("private void "+method+"(");
    if(start<0)throw new AssertionError("Missing "+method);
    int end=s.indexOf("\n    private ",start+15);
    String body=s.substring(start,end<0?s.length():end);
    if(!body.contains("routeNextDialogBack("+route+")"))throw new AssertionError("Missing back route: "+method);
  }
  public static void main(String[] args)throws Exception{
    String s=Files.readString(Path.of(args[0]));
    check(s,"showRemoteShortcutActionPicker","this::showRemoteShortcutOptions");
    check(s,"showRemoteShortcutGesturePicker","() -> showRemoteShortcutActionPicker(slot)");
    check(s,"showRemoteShortcutAppPicker","() -> showRemoteShortcutActionPicker(pendingShortcutSlot)");
    check(s,"showHomeDownLaunchOptions","()->returnToSettingsCategory(SettingsMenuRoutes.APPS)");
    check(s,"showAppLayoutOptions","() -> returnToSettingsCategory(SettingsMenuRoutes.APPS)");
    check(s,"showAppIconSizeOptions","()->showSettingsGroup(SettingsMenuRoutes.ICONS)");
    System.out.println("Settings shortcut/layout BACK routes: 6 passed");
  }
}
