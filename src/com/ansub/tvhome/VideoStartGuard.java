package com.ansub.tvhome;

/** Generation-scoped first-frame readiness. Old decoder callbacks cannot complete a new choice. */
final class VideoStartGuard {
 private long generation;private boolean pending,rendered;
 long begin(){pending=true;rendered=false;return ++generation;}
 long token(){return generation;}
 boolean pending(long token){return token==generation&&pending;}
 boolean ready(long token){if(!pending(token))return false;pending=false;rendered=true;return true;}
 boolean timeout(long token){if(!pending(token))return false;pending=false;return true;}
 boolean rendered(){return rendered;}
 void cancel(){generation++;pending=false;rendered=false;}
}
