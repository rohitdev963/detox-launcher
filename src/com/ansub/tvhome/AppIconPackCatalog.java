package com.ansub.tvhome;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import org.xmlpull.v1.XmlPullParser;
import java.util.*;

/** One installed-pack discovery path used by Home and Gaming. */
final class AppIconPackCatalog {
 static final class Pack {final String name,packageName;Pack(String name,String pkg){this.name=name;packageName=pkg;}}
 static List<Pack> discover(Context context){
  PackageManager pm=context.getPackageManager();Map<String,Pack> byPackage=new LinkedHashMap<>();
  for(String action:new String[]{"org.adw.launcher.THEMES","com.novalauncher.THEME","com.anddoes.launcher.THEME"})for(ResolveInfo info:pm.queryIntentActivities(new Intent(action),0)){
   if(info.activityInfo==null)continue;String pkg=info.activityInfo.packageName;if(byPackage.containsKey(pkg)||!mapped(pm,pkg))continue;
   String name=pkg;try{name=pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
   byPackage.put(pkg,new Pack(name,pkg));
  }
  List<Pack> sorted=new ArrayList<>(byPackage.values());Collections.sort(sorted,(a,b)->{int order=a.name.compareToIgnoreCase(b.name);return order!=0?order:a.packageName.compareTo(b.packageName);});
  List<Pack> result=new ArrayList<>();result.add(new Pack("System icons",""));
  for(Pack pack:sorted){boolean duplicate=false;for(Pack other:sorted)if(pack!=other&&pack.name.equalsIgnoreCase(other.name)){duplicate=true;break;}
   result.add(new Pack(duplicate?pack.name+" · "+pack.packageName:pack.name,pack.packageName));}
  return result;
 }
 static int selected(List<Pack> packs,String pkg){for(int i=0;i<packs.size();i++)if(packs.get(i).packageName.equals(pkg))return i;return 0;}
 private static boolean mapped(PackageManager pm,String pkg){
  try{Resources res=pm.getResourcesForApplication(pkg);int id=res.getIdentifier("appfilter","xml",pkg);if(id==0)return false;
   XmlResourceParser parser=res.getXml(id);try{while(parser.next()!=XmlPullParser.END_DOCUMENT)if(parser.getEventType()==XmlPullParser.START_TAG&&"item".equals(parser.getName())){
    String component=parser.getAttributeValue(null,"component"),drawable=parser.getAttributeValue(null,"drawable");if(component!=null&&!component.trim().isEmpty()&&drawable!=null&&!drawable.trim().isEmpty())return true;
   }}finally{parser.close();}
  }catch(Exception ignored){}return false;
 }
}
