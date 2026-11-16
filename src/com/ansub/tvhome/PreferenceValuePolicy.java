package com.ansub.tvhome;

/** Reject a restored value that would crash a typed SharedPreferences getter. */
final class PreferenceValuePolicy {
 private PreferenceValuePolicy(){}
 static Object compatible(Object previous,Object value){
  if(previous==null)return value;
  if(previous instanceof Integer){
   if(!(value instanceof Number))throw mismatch();double n=((Number)value).doubleValue();
   if(n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw mismatch();return (int)n;
  }
  if(previous instanceof Long){
   if(!(value instanceof Number))throw mismatch();if(value instanceof Long||value instanceof Integer)return ((Number)value).longValue();
   double n=((Number)value).doubleValue();if(Double.isNaN(n)||Double.isInfinite(n)||n!=Math.rint(n)||n>=0x1p63||n< -0x1p63)throw mismatch();return (long)n;
  }
  if(previous instanceof Float){
   if(!(value instanceof Number))throw mismatch();float n=((Number)value).floatValue();if(Float.isNaN(n)||Float.isInfinite(n))throw mismatch();return n;
  }
  if(previous instanceof String&&value instanceof String||previous instanceof Boolean&&value instanceof Boolean
    ||previous instanceof java.util.Set&&value instanceof java.util.Set)return value;
  throw mismatch();
 }
 private static IllegalArgumentException mismatch(){return new IllegalArgumentException("Restored preference has an incompatible type or range");}
}
