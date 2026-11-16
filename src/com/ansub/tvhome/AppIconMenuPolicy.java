package com.ansub.tvhome;

/** Keeps app-tile long-press menu ordering and selection mapping out of MainActivity. */
final class AppIconMenuPolicy {
    enum FolderAction { OPEN, EDIT, REMOVE }
    enum AppAction { MOVE, REPLACE, CHANGE_ICON, RESET_ICON, CLOSE, REMOVE, UNINSTALL, INFO }

    private static final String[] FOLDER_LABELS = {"Open folder", "Edit folder", "Remove folder"};
    private static final String[] APP_LABELS = {"Move", "Replace app", "Change icon"};

    private AppIconMenuPolicy() {}

    static String[] folderLabels() { return FOLDER_LABELS.clone(); }

    static FolderAction folderAction(int index) {
        return index >= 0 && index < FolderAction.values().length ? FolderAction.values()[index] : null;
    }

    static String[] appLabels(boolean hasCustomIcon, boolean userInstalled) {
        int extra = (hasCustomIcon ? 1 : 0) + 3;
        String[] labels = new String[APP_LABELS.length + extra];
        System.arraycopy(APP_LABELS, 0, labels, 0, APP_LABELS.length);
        int index = APP_LABELS.length;
        if (hasCustomIcon) labels[index++] = "Reset icon";
        labels[index++] = "Close app";
        labels[index++] = "Remove from home";
        labels[index] = userInstalled ? "Uninstall app" : "App info";
        return labels;
    }

    static AppAction appAction(int index, boolean hasCustomIcon, boolean userInstalled) {
        if (index < 0) return null;
        if (index < APP_LABELS.length) return AppAction.values()[index];
        int actionIndex = index - APP_LABELS.length;
        if (hasCustomIcon) {
            if (actionIndex == 0) return AppAction.RESET_ICON;
            actionIndex--;
        }
        if (actionIndex == 0) return AppAction.CLOSE;
        if (actionIndex == 1) return AppAction.REMOVE;
        if (actionIndex == 2) return userInstalled ? AppAction.UNINSTALL : AppAction.INFO;
        return null;
    }
}
