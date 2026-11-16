package com.ansub.tvhome;
import java.util.*;import org.json.*;
/** Keeps SharedPreferences types through JSON serialization, including integral floats/longs. */
final class LauncherProfileCodec {
 static final String TYPES="__detoxPreferenceTypes";
 static JSONObject encode(Map<String,?> prefs)throws JSONException{return encode(prefs,true);}
 static JSONObject encode(Map<String,?> prefs,boolean profile)throws JSONException{
  JSONObject result=new JSONObject(),types=new JSONObject();
  for(Map.Entry<String,?> e:prefs.entrySet()){
   String key=e.getKey();if(profile&&!HomeProfilePolicy.isProfileSetting(key)||key.equals(TYPES)||key.equals("homeCompanionMode"))continue;Object v=e.getValue();
   if(v instanceof Float){float f=(Float)v;if(Float.isNaN(f)||Float.isInfinite(f))throw new IllegalArgumentException("Invalid profile number");types.put(key,"float");result.put(key,v);}
   else if(v instanceof Long){types.put(key,"long");result.put(key,v);}
   else if(v instanceof Set){JSONArray a=new JSONArray();for(Object item:(Set<?>)v){if(!(item instanceof String))throw new IllegalArgumentException("Invalid string set");a.put(item);}types.put(key,"set");result.put(key,a);}
   else if(v instanceof String||v instanceof Boolean||v instanceof Integer)result.put(key,v);
  }return result.put(TYPES,types);
 }
 static Map<String,Object> decode(JSONObject snapshot,Map<String,?> current)throws JSONException{return decode(snapshot,current,true);}
 static Map<String,Object> decode(JSONObject snapshot,Map<String,?> current,boolean profile)throws JSONException{
  Map<String,Object> result=new HashMap<>();JSONObject types=snapshot.optJSONObject(TYPES);if(snapshot.has(TYPES)&&types==null)throw new IllegalArgumentException("Invalid type metadata");
  Iterator<String> keys=snapshot.keys();while(keys.hasNext()){
   String key=keys.next();if(key.equals(TYPES)||key.equals("homeCompanionMode")||profile&&!HomeProfilePolicy.isProfileSetting(key))continue;Object value=snapshot.get(key);String type=types==null?"":types.optString(key,"");if(!type.isEmpty()&&!type.equals("float")&&!type.equals("long")&&!type.equals("set"))throw new IllegalArgumentException("Unknown profile type");
   if(type.equals("set")){if(!(value instanceof JSONArray))throw new IllegalArgumentException("Invalid profile set");Set<String> set=new HashSet<>();JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++){Object item=a.get(i);if(!(item instanceof String))throw new IllegalArgumentException("Invalid profile set item");set.add((String)item);}result.put(key,set);continue;}
   if(value instanceof Number){Number n=(Number)value;Object old=current.get(key);boolean asFloat=type.equals("float")||types==null&&(old instanceof Float||key.equals("liveWallpaperCropScale"));boolean asLong=type.equals("long")||types==null&&(old instanceof Long||key.equals("sleepTimerDeadline")||key.equals("pendingExternalAppTime"));
    if(asFloat){float f=n.floatValue();if(Float.isNaN(f)||Float.isInfinite(f))throw new IllegalArgumentException("Invalid profile number");result.put(key,f);}else if(asLong)result.put(key,exactLong(n));else if(value instanceof Integer||value instanceof Long)result.put(key,value);else{float f=n.floatValue();if(Float.isNaN(f)||Float.isInfinite(f))throw new IllegalArgumentException("Invalid profile number");result.put(key,f);}
   }else if(!type.isEmpty())throw new IllegalArgumentException("Profile type mismatch");else if(value instanceof String||value instanceof Boolean)result.put(key,value);else throw new IllegalArgumentException("Unsupported profile value");
  }
  for(Map.Entry<String,Object> entry:result.entrySet())entry.setValue(PreferenceValuePolicy.compatible(current.get(entry.getKey()),entry.getValue()));
  return result;
 }
 private static long exactLong(Number value){if(value instanceof Long||value instanceof Integer)return value.longValue();double n=value.doubleValue();if(Double.isNaN(n)||Double.isInfinite(n)||n!=Math.rint(n)||n>=0x1p63||n< -0x1p63)throw new IllegalArgumentException("Invalid long preference");return (long)n;}
 private LauncherProfileCodec(){}
}
