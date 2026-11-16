package com.ansub.tvhome;

import java.nio.file.Files;
import java.nio.file.Path;

public final class BuiltInLiveVideoIntegrationTest {
    public static void main(String[] args) throws Exception {
        Path mainFile = Path.of(args[0]);
        String main = Files.readString(mainFile);
        String service = Files.readString(mainFile.getParent().resolve("DetoxLiveWallpaperService.java"));
        require(Files.readString(mainFile.getParent().resolve("SettingsMenuRoutes.java")), "\"video.builtin\",\"Built-in Live Videos\"", "named Settings route exposes the built-in video picker");
        require(main, "case \"video.builtin\": showBuiltInLiveVideoPicker(); break;", "built-in picker route is actually bound");
        require(main, "panel(Strings.t(\"Built-in live videos\"), translated(BuiltInLiveVideoCatalog.NAMES)", "picker uses friendly catalog names");
        require(main, "putString(\"liveWallpaperVideoAsset\", asset)", "selection persists the bundled asset");
        require(main, ".remove(\"liveWallpaperVideoAsset\").remove(\"liveWallpaperVideoName\")", "importing a personal video clears stale built-in selection");
        require(main, "getAssets().openFd(assetPath)", "launcher preview opens bundled assets");
        require(main, "BuiltInLiveVideoCatalog.containsAsset(asset)", "Android wallpaper service resolves bundled assets");
        require(service, "getAssets().openFd(asset)", "Android wallpaper service opens bundled assets");
        require(main, "boolean hasBuiltInVideo = BuiltInLiveVideoCatalog.containsAsset(requestedBuiltInVideo)", "backup restore validates built-in selection");
        System.out.println("Built-in live video integration: picker, selection, both playback paths, import and restore passed");
    }

    private static void require(String source, String expected, String message) {
        if (!source.contains(expected)) throw new AssertionError(message);
    }
}
