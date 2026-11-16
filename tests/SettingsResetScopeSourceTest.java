import java.nio.file.*;
public final class SettingsResetScopeSourceTest {
 public static void main(String[] args)throws Exception{
  String source=Files.readString(Path.of(args[0]));int a=source.indexOf("private void showUtilityResetConfirmation("),b=source.indexOf("\n    private ",a+15);
  if(a<0||b<0)throw new AssertionError("Reset handler missing");String body=source.substring(a,b);
  if(body.contains("remove(\"themePreset\")")||body.contains(".clear()"))throw new AssertionError("Utilities reset clears unrelated global settings");
  if(!body.contains("SettingsPreferencePolicy.utilityResetKeys"))throw new AssertionError("Reset ownership policy bypassed");
  System.out.println("Utilities reset source regression: global theme retained, exact policy used, no clear-all");
 }
}
