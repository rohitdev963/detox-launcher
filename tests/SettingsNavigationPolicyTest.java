package com.ansub.tvhome;
public final class SettingsNavigationPolicyTest {
  public static void main(String[] args) {
    if (!SettingsNavigationPolicy.canRestoreRootFocus(2,2,-1)) throw new AssertionError();
    if (SettingsNavigationPolicy.canRestoreRootFocus(2,3,-1)) throw new AssertionError();
    if (SettingsNavigationPolicy.canRestoreRootFocus(2,2,0)) throw new AssertionError();
    if (!SettingsNavigationPolicy.canRestoreCategoryFocus(4,4,2,2)) throw new AssertionError();
    if (SettingsNavigationPolicy.canRestoreCategoryFocus(4,5,2,2)) throw new AssertionError();
    if (SettingsNavigationPolicy.canRestoreCategoryFocus(4,4,2,1)) throw new AssertionError();
    if (!SettingsNavigationPolicy.canRestoreCategoryFocus(4,4,6,6)) throw new AssertionError("seventh category");
    if (SettingsNavigationPolicy.canRestoreCategoryFocus(4,4,7,7)) throw new AssertionError("invalid category");
    SettingsNavigationPolicy.BackGate gate = new SettingsNavigationPolicy.BackGate();
    if (gate.accept(4,0,0) || !gate.accept(4,1,0) || gate.accept(4,1,0)) throw new AssertionError();
    gate.reset();
    if (!gate.accept(4,1,0)) throw new AssertionError();
    System.out.println("Navigation policy tests passed");
  }
}
