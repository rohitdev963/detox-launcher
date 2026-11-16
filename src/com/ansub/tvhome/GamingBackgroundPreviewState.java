package com.ansub.tvhome;
/** No SharedPreferences writes while previewing; callers persist only Apply's result. */
final class GamingBackgroundPreviewState {
 final String saved,candidate;private boolean closed;
 GamingBackgroundPreviewState(String saved,String candidate){this.saved=saved;this.candidate=candidate;}
 String apply(){if(closed)throw new IllegalStateException("Preview already closed");closed=true;return candidate;}
 String cancel(){closed=true;return saved;}
}
