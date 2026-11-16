package com.ansub.tvhome;

/** Bundled live-video wallpapers shipped with Detox. Keep labels and asset paths aligned. */
final class BuiltInLiveVideoCatalog {
    static final String[] NAMES = {"Aurora Drift"};

    private static final String[] ASSETS = {"public/videos/aurora-drift.mp4"};

    static int size() { return ASSETS.length; }

    static String assetAt(int index) {
        if (index < 0 || index >= ASSETS.length) throw new IllegalArgumentException("Invalid live video wallpaper");
        return ASSETS[index];
    }

    static String nameAt(int index) {
        if (index < 0 || index >= NAMES.length) throw new IllegalArgumentException("Invalid live video wallpaper");
        return NAMES[index];
    }

    static int indexOfAsset(String asset) {
        if (asset == null) return -1;
        for (int i = 0; i < ASSETS.length; i++) if (ASSETS[i].equals(asset)) return i;
        return -1;
    }

    static boolean containsAsset(String asset) { return indexOfAsset(asset) >= 0; }

    static String nameForAsset(String asset) {
        int index = indexOfAsset(asset);
        return index < 0 ? "" : NAMES[index];
    }

    private BuiltInLiveVideoCatalog() { }
}
