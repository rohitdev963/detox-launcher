package com.ansub.tvhome;
/** Rate limit using processing time, so queued hardware repeats cannot race through a menu. */
final class MenuNavigationPolicy {
 private long nextMove;private int lastDirection;
 boolean move(int direction,boolean initial,long now,long downAt){
  if(initial||direction!=lastDirection){if(direction==lastDirection&&now<nextMove-100)return false;lastDirection=direction;nextMove=now+180;return true;}
  if(now-downAt<450||now<nextMove)return false;nextMove=now+180;return true;
 }
 void release(){lastDirection=0;nextMove=0;}
}
