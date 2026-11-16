package com.ansub.tvhome;
public final class RestoreStoragePolicyTest {
    public static void main(String[] args) {
        long reserve = 8L * 1024 * 1024;
        check(!RestoreStoragePolicy.hasRoom(0, reserve + 1));
        check(!RestoreStoragePolicy.hasRoom(-1, reserve + 1));
        check(!RestoreStoragePolicy.hasRoom(1, -1));
        check(!RestoreStoragePolicy.hasRoom(1, reserve));
        check(RestoreStoragePolicy.hasRoom(1, reserve + 1));
        check(RestoreStoragePolicy.hasRoom(100, reserve + 100));
        check(!RestoreStoragePolicy.hasRoom(101, reserve + 100));
        check(!RestoreStoragePolicy.hasRoom(Long.MAX_VALUE, Long.MAX_VALUE));
        System.out.println("Restore storage preflight: 8 checks passed");
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
}
