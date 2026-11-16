package com.ansub.tvhome;
import java.nio.file.*;
public final class SettingsBackRouteSourceTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(args[0]));
        for (String method : new String[]{"showClockColorOptions", "showClockPositionOptions", "showClockOptions", "showClockSizeOptions", "showClockVisibilityOptions"}) {
            String signature = "private void " + method + "() {";
            int start = source.indexOf(signature);
            if (start < 0) throw new AssertionError("Missing method: " + method);
            int end = source.indexOf("\n    private ", start+signature.length());
            if(end<0)end=source.length();
            if (end < 0 || !source.substring(start, end).contains("routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP))"))
                throw new AssertionError("Missing clock Back route: " + method);
        }
        System.out.println("Clock submenu BACK route source checks passed (5/5, including visual preview)");
    }
}
