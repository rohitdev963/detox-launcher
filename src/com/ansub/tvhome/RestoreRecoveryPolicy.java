package com.ansub.tvhome;

/** Conservative recovery of media displaced by an interrupted restore. */
final class RestoreRecoveryPolicy {
    private RestoreRecoveryPolicy() {}

    static boolean isRecoveryCopy(String destinationName, String candidateName) {
        if (!"selected_wallpaper.jpg".equals(destinationName)
                && !"video-wallpaper-transfer.mp4".equals(destinationName)) return false;
        if (candidateName == null) return false;
        return candidateName.matches(java.util.regex.Pattern.quote(destinationName)
                + "\\.restore-[0-9]+--?[0-9]+\\.previous");
    }

    static boolean shouldRecover(boolean destinationExists, int matchingCopies) {
        return !destinationExists && matchingCopies == 1;
    }
}
