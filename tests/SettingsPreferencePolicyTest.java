package com.ansub.tvhome;
import java.util.*;
public final class SettingsPreferencePolicyTest {
 public static void main(String[] args){
  Set<String> existing=new HashSet<>(Arrays.asList("utilityTextColor","utilitiesPlacement","utilityIconScalePercent","utilityIconOpacity","utilityAppearanceStyle","utilityAppearanceMotion","utilityHidden_network","utilityHidden_newControl","themePreset","fontColor","clockColor","wallpaper","quickControlOrder","homeDownLaunchPackage","profileStore"));
  Set<String> reset=SettingsPreferencePolicy.utilityResetKeys(existing);
  if(reset.size()!=8)throw new AssertionError("utility reset boundaries "+reset);
  for(String shared:new String[]{"themePreset","fontColor","clockColor","wallpaper","quickControlOrder","homeDownLaunchPackage","profileStore"})if(reset.contains(shared))throw new AssertionError("unrelated preference reset "+shared);
  existing.removeAll(reset);if(!SettingsPreferencePolicy.utilityResetKeys(existing).isEmpty())throw new AssertionError("reset not idempotent");
  if(SettingsPreferencePolicy.clockAccent(0,0xFF123456,-99)!=0xFF123456)throw new AssertionError("match accent");
  if(SettingsPreferencePolicy.clockAccent(0xFFAABBCC,0xFF123456,-99)!=0xFFAABBCC)throw new AssertionError("independent clock color overwritten");
  if(SettingsPreferencePolicy.clockAccent(-99,0,-99)!=0xFFFFFFFF)throw new AssertionError("rainbow baseline");
  System.out.println("Settings preference ownership: isolated utility reset, shared colors, independent clock color and repeat safety passed");
 }
}
