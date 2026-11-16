package com.ansub.tvhome;

/** Pure navigation decisions, independent of Android views and saved preferences. */
final class SettingsNavigationPolicy {
    private SettingsNavigationPolicy() {}

    static boolean canRestoreRootFocus(int request, int current, int category) {
        return request == current && category == -1;
    }

    static boolean canRestoreCategoryFocus(int request, int current, int requestedCategory, int category) {
        return request == current && requestedCategory == category && category >= 0 && category < SettingsMenuRoutes.categoryCount();
    }

    /** BACK is handled on its first key-up only, preventing duplicate dialog exits. */
    static final class BackGate {
        private boolean exiting;
        boolean accept(int keyCode, int action, int repeatCount) {
            // Android KeyEvent constants: KEYCODE_BACK=4, ACTION_UP=1.
            if (keyCode != 4 || action != 1 || repeatCount != 0 || exiting) return false;
            exiting = true;
            return true;
        }
        void reset() { exiting = false; }
    }
}
