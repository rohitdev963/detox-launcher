package com.ansub.tvhome;
import java.util.Collections;
public final class PreferenceValuePolicyTest {
 public static void main(String[] args){
  Object i=PreferenceValuePolicy.compatible(100,100L);if(!(i instanceof Integer)||!i.equals(100))throw new AssertionError("integer getter safety");
  reject(100,"large");reject(true,"true");reject("digital",42);reject(1,1.5f);reject(1,Long.MAX_VALUE);reject(1,Double.NaN);reject(1f,Double.POSITIVE_INFINITY);
  long exact=9007199254740993L;if(!PreferenceValuePolicy.compatible(1L,exact).equals(exact))throw new AssertionError("long precision");
  if(!(PreferenceValuePolicy.compatible(.5f,1) instanceof Float))throw new AssertionError("float type");
  if(!(PreferenceValuePolicy.compatible(0L,1) instanceof Long))throw new AssertionError("long type");
  if(!PreferenceValuePolicy.compatible(Collections.singleton("a"),Collections.singleton("b")).equals(Collections.singleton("b")))throw new AssertionError();
  if(!PreferenceValuePolicy.compatible(null,"future option").equals("future option"))throw new AssertionError("forward compatible keys");
  System.out.println("Restored preference types: mismatches/ranges rejected, numeric getter types and long precision preserved");
 }
 private static void reject(Object a,Object b){try{PreferenceValuePolicy.compatible(a,b);throw new AssertionError("invalid preference accepted");}catch(IllegalArgumentException expected){}}
}
