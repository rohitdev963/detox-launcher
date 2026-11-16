package com.ansub.tvhome;

/** Pure configuration for the Gaming video readability overlay. */
final class GamingReadabilityPolicy {
 static final String[] LABELS={"Off","Subtle","Balanced","Strong"};
 static int clamp(int level){return Math.max(0,Math.min(LABELS.length-1,level));}
 static int[] gradient(int level){
  switch(clamp(level)){
   case 0:return new int[]{0x00080B12,0x00080B12,0x00080B12};
   case 1:return new int[]{0xB8080B12,0x70080B12,0x08080B12};
   case 2:return new int[]{0xDC080B12,0x9A080B12,0x18080B12};
   default:return new int[]{0xF0080B12,0xC0080B12,0x50080B12};
  }
 }
 private GamingReadabilityPolicy(){}
}
