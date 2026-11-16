package com.ansub.tvhome;
/** A preview is a transaction: browsing never produces a preference write. */
final class ClockPreviewState {
 final String saved; private String candidate; private boolean closed;
 ClockPreviewState(String saved){this.saved=ClockFacePolicy.normalise(saved);candidate=this.saved;}
 String candidate(){return candidate;}
 void select(String id){if(!closed)candidate=ClockFacePolicy.normalise(id);}
 String save(){closed=true;return candidate;}
 String cancel(){closed=true;candidate=saved;return saved;}
}
