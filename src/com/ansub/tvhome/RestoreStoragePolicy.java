package com.ansub.tvhome;

/** Conservative preflight for copying staged restore media into app storage. */
final class RestoreStoragePolicy {
    private RestoreStoragePolicy() {}

    static boolean hasRoom(long bytesToCopy, long usableBytes) {
        if (bytesToCopy <= 0 || usableBytes < 0) return false;
        // Leave 8 MiB for preference writes, filesystem metadata and other app work.
        final long reserve = 8L * 1024L * 1024L;
        return usableBytes >= reserve && bytesToCopy <= usableBytes - reserve;
    }
}
