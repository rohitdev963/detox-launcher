package com.ansub.tvhome;
public final class RestoreRecoveryPolicyTest {
    static int passed;
    static void check(boolean ok) { if (!ok) throw new AssertionError("case " + (passed + 1)); passed++; }
    public static void main(String[] args) {
        String a="selected_wallpaper.jpg", b="video-wallpaper-transfer.mp4";
        check(RestoreRecoveryPolicy.isRecoveryCopy(a,a+".restore-123-456.previous"));
        check(RestoreRecoveryPolicy.isRecoveryCopy(b,b+".restore-1-2.previous"));
        check(RestoreRecoveryPolicy.isRecoveryCopy(a,a+".restore-123--456.previous"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,a+".restore-1-2.tmp"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,a+".failed-restore-1"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,"../"+a+".restore-1-2.previous"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,b+".restore-1-2.previous"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy("other.jpg","other.jpg.restore-1-2.previous"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,a+".restore-x-2.previous"));
        check(!RestoreRecoveryPolicy.isRecoveryCopy(a,null));
        check(RestoreRecoveryPolicy.shouldRecover(false,1));
        check(!RestoreRecoveryPolicy.shouldRecover(true,1));
        check(!RestoreRecoveryPolicy.shouldRecover(false,0));
        check(!RestoreRecoveryPolicy.shouldRecover(false,2));
        System.out.println("Restore recovery policy: " + passed + "/" + passed + " passed");
    }
}
