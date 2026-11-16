package com.ansub.tvhome;

import java.util.Random;

public final class GamingMusicCatalogTest {
    public static void main(String[] args) {
        check(GamingMusicCatalog.NAMES.length == 0&&!GamingMusicCatalog.available(), "public edition has no bundled music");
        try { GamingMusicCatalog.asset(0); throw new AssertionError("empty public catalog accepted a track"); }
        catch (IllegalArgumentException expected) { }

        GamingMusicQueue queue = new GamingMusicQueue(1, new Random(1));
        check(!queue.needsCrossfadePolling(), "native single-track looping needs no crossfade timer");
        check(new GamingMusicQueue(2,new Random(1)).needsCrossfadePolling(), "multiple tracks retain crossfade scheduling");
        check(queue.next(-1, false) == 0, "single track is selected without shuffle");
        check(queue.next(0, true) == 0 && queue.next(0, true) == 0, "single track remains repeatable with shuffle enabled");
        System.out.println("Public music catalog: no bundled tracks; existing queue behavior remains valid");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
