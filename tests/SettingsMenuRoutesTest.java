package com.ansub.tvhome;
import java.util.HashSet;
public final class SettingsMenuRoutesTest {
    public static void main(String[] args) {
        int[] counts = {6,2,7,3,2,7,1};
        HashSet<String> unique = new HashSet<>();
        for (int category=0;category<counts.length;category++) {
            if (SettingsMenuRoutes.count(category)!=counts[category]) throw new AssertionError("count");
            for (int index=0;index<counts[category];index++) {
                String action=SettingsMenuRoutes.actionAt(category,index);
                if (action==null || !unique.add(action)) throw new AssertionError("duplicate or missing route");
            }
            if (SettingsMenuRoutes.actionAt(category,counts[category])!=null) throw new AssertionError("out of bounds");
        }
        if (SettingsMenuRoutes.actionAt(-1,0)!=null || SettingsMenuRoutes.actionAt(7,0)!=null || SettingsMenuRoutes.actionAt(0,-1)!=null) throw new AssertionError("invalid category/index");
        if (!SettingsMenuRoutes.CLOCK_GROUP.equals(SettingsMenuRoutes.actionAt(4,0))) throw new AssertionError("clock group mapping");
        if (!SettingsMenuRoutes.PERFORMANCE.equals(SettingsMenuRoutes.actionAt(5,0))) throw new AssertionError("performance mapping");
        if (!SettingsMenuRoutes.BACKUP.equals(SettingsMenuRoutes.actionAt(5,1))) throw new AssertionError("backup mapping");
        if (!"phone.pair".equals(SettingsMenuRoutes.actionAt(6,0))) throw new AssertionError("phone pairing mapping");
        System.out.println("Settings menu routes: seven root categories and 28 unique root routes passed");
    }
}
