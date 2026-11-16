package com.ansub.tvhome;

import java.util.HashSet;
import java.util.Set;

public final class BuiltInLiveVideoCatalogTest {
    public static void main(String[] args) {
        check(BuiltInLiveVideoCatalog.size() == 1, "original public live wallpaper is catalogued");
        check(BuiltInLiveVideoCatalog.NAMES.length == BuiltInLiveVideoCatalog.size(), "names match asset count");
        Set<String> names = new HashSet<>();
        Set<String> assets = new HashSet<>();
        for (int i = 0; i < BuiltInLiveVideoCatalog.size(); i++) {
            String name = BuiltInLiveVideoCatalog.nameAt(i);
            String asset = BuiltInLiveVideoCatalog.assetAt(i);
            check(name != null && !name.trim().isEmpty() && !name.endsWith(".mp4"), "friendly display name exists");
            check(names.add(name), "display names are unique");
            check(asset.startsWith("public/videos/") && asset.endsWith(".mp4"), "asset path is in public live-video catalog");
            check(assets.add(asset), "asset paths are unique");
            check(BuiltInLiveVideoCatalog.containsAsset(asset), "catalog recognizes its own asset");
            check(BuiltInLiveVideoCatalog.indexOfAsset(asset) == i, "catalog reverse lookup matches selection");
            check(name.equals(BuiltInLiveVideoCatalog.nameForAsset(asset)), "asset resolves to its friendly name");
        }
        check(BuiltInLiveVideoCatalog.indexOfAsset("wallpapers/live-videos/unknown.mp4") == -1,
                "unknown bundled media cannot be selected");
        check(BuiltInLiveVideoCatalog.nameForAsset(null).isEmpty(), "null asset has no display name");
        try {
            BuiltInLiveVideoCatalog.assetAt(BuiltInLiveVideoCatalog.size());
            throw new AssertionError("catalog accepted an out-of-range selection");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Public live video catalog: original named asset and selection lookups passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
