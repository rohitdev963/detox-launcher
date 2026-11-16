"""Execute the actual MenuTypography against small host Android stand-ins.

Proves state/scale/late-row behavior; does not claim Android rendering or device performance.
Generated stand-ins and class files stay in the existing project's build directory.
"""
from pathlib import Path
import argparse,subprocess

parser=argparse.ArgumentParser()
parser.add_argument('--java-home',type=Path,required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
out=root/'build/change-review/menu-runtime'
stubs=out/'stubs';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
sources={
'android/content/SharedPreferences.java':'''package android.content; import java.util.*; public class SharedPreferences {public final Map<String,Object> values=new HashMap<>(); public int getInt(String k,int d){Object v=values.get(k);return v instanceof Integer?(Integer)v:d;}public String getString(String k,String d){Object v=values.get(k);return v instanceof String?(String)v:d;}}''',
'android/content/res/AssetManager.java':'''package android.content.res; public class AssetManager {}''',
'android/util/DisplayMetrics.java':'''package android.util; public class DisplayMetrics {public float density=1,scaledDensity=1;}''',
'android/util/TypedValue.java':'''package android.util; public class TypedValue {public static final int COMPLEX_UNIT_PX=0;}''',
'android/content/res/Resources.java':'''package android.content.res; public class Resources {private final android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();public android.util.DisplayMetrics getDisplayMetrics(){return metrics;}}''',
'android/content/Context.java':'''package android.content;public class Context {private final SharedPreferences prefs=new SharedPreferences();private final android.content.res.Resources resources=new android.content.res.Resources();public SharedPreferences getSharedPreferences(String n,int m){return prefs;}public Context getApplicationContext(){return this;}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}public android.content.res.Resources getResources(){return resources;}}''',
'android/content/ContextWrapper.java':'''package android.content;public class ContextWrapper extends Context {private final Context base;public ContextWrapper(Context base){this.base=base;}public Context getBaseContext(){return base;}public SharedPreferences getSharedPreferences(String n,int m){return base.getSharedPreferences(n,m);}}''',
'android/app/Activity.java':'''package android.app;public class Activity extends android.content.Context {public boolean isDestroyed(){return false;}}''',
'android/graphics/Shader.java':'''package android.graphics;public class Shader {public enum TileMode{CLAMP}}''',
'android/graphics/LinearGradient.java':'''package android.graphics;public class LinearGradient extends Shader {public static int created;public LinearGradient(float a,float b,float c,float d,int[] colors,float[] stops,TileMode mode){created++;}}''',
'android/graphics/Typeface.java':'''package android.graphics;public class Typeface {public static final int NORMAL=0,BOLD=1;public static int loaded;public final String family;public final int weight;Typeface(String f,int w){family=f;weight=w;}public static Typeface create(String f,int w){return new Typeface(f,w);}public static Typeface create(Typeface f,int w){return new Typeface(f.family,w);}public static Typeface createFromAsset(android.content.res.AssetManager a,String path){loaded++;return new Typeface(path,0);}}''',
'android/graphics/Paint.java':'''package android.graphics;public class Paint {private Shader shader;public void setFakeBoldText(boolean b){}public void setShader(Shader s){shader=s;}public Shader getShader(){return shader;}}''',
'android/view/View.java':'''package android.view;import java.util.*;public class View {public interface OnLayoutChangeListener{void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob);}private final android.content.Context context;private int width=200;public final List<OnLayoutChangeListener> layouts=new ArrayList<>();public View(android.content.Context c){context=c;}public android.content.Context getContext(){return context;}public android.content.res.Resources getResources(){return context.getResources();}public int getWidth(){return width;}public int getPaddingLeft(){return 0;}public int getPaddingRight(){return 0;}public void addOnLayoutChangeListener(OnLayoutChangeListener l){layouts.add(l);}public void resize(int w){int old=width;width=w;for(OnLayoutChangeListener l:layouts)l.onLayoutChange(this,0,0,w,40,0,0,old,40);}}''',
'android/view/ViewGroup.java':'''package android.view;import java.util.*;public class ViewGroup extends View {public interface OnHierarchyChangeListener{void onChildViewAdded(View parent,View child);void onChildViewRemoved(View parent,View child);}private final List<View> children=new ArrayList<>();private OnHierarchyChangeListener hierarchy;public ViewGroup(android.content.Context c){super(c);}public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}public void setOnHierarchyChangeListener(OnHierarchyChangeListener h){hierarchy=h;}public void addView(View v){children.add(v);if(hierarchy!=null)hierarchy.onChildViewAdded(this,v);}}''',
'android/widget/AbsListView.java':'''package android.widget;public class AbsListView extends android.view.ViewGroup {public AbsListView(android.content.Context c){super(c);}}''',
'android/widget/TextView.java':'''package android.widget;public class TextView extends android.view.View {private float size=18;private final android.graphics.Paint paint=new android.graphics.Paint();public int color;public android.graphics.Typeface font;public TextView(android.content.Context c){super(c);}public float getTextSize(){return size;}public void setTextSize(int unit,float s){size=s;}public void setTypeface(android.graphics.Typeface f){font=f;}public android.graphics.Paint getPaint(){return paint;}public void setIncludeFontPadding(boolean b){}public void setLetterSpacing(float n){}public void setTextColor(int c){color=c;}public void setShadowLayer(float a,float b,float c,int d){}}''',
'android/widget/EditText.java':'''package android.widget;public class EditText extends TextView {public int hint;public EditText(android.content.Context c){super(c);}public void setHintTextColor(int c){hint=c;}}''',
'com/ansub/tvhome/MainActivity.java':'''package com.ansub.tvhome;public class MainActivity extends android.app.Activity {public Integer previewColor;public String previewFont;public String menuFontKey(){return previewFont!=null?previewFont:getSharedPreferences("config",0).getString("font","nunito");}public int menuFontColor(){return previewColor!=null?previewColor:getSharedPreferences("config",0).getInt("fontColor",-1);}public int menuWallpaperAccent(){return 0xFFAAEE77;}}''',
'com/ansub/tvhome/MenuTypographyRuntime.java':r'''package com.ansub.tvhome;
import android.widget.*;import android.view.*;import android.graphics.*;
public class MenuTypographyRuntime {
 static int failures,checks;static void check(boolean ok,String why){checks++;if(!ok){failures++;System.out.println("FAIL: "+why);}}
 static void near(float a,float b,String why){check(Math.abs(a-b)<.001f,why+" (actual "+a+", expected "+b+")");}
 static void origin(TextView view,float base)throws Exception {try{MenuTypography.class.getDeclaredMethod("recordScaledText",TextView.class,float.class).invoke(null,view,base);}catch(NoSuchMethodException old){/* Reproduce the pre-fix implementation. */}}
 public static void main(String[] args)throws Exception {
  MainActivity host=new MainActivity();host.getSharedPreferences("config",0).values.put("fontColor",0xFFAAE4C1);host.getSharedPreferences("config",0).values.put("fontScalePercent",120);MenuTypography.bind(host);
  ViewGroup dialog=new ViewGroup(host);EditText search=new EditText(host);search.setTextSize(0,36);origin(search,30);dialog.addView(search);MenuTypography.tree(dialog);near(search.getTextSize(),36,"already-scaled custom dialog text must not be scaled twice");
  AbsListView list=new AbsListView(host);MenuTypography.tree(list);TextView late=new TextView(host);list.addView(late);check(late.color==0xFFAAE4C1,"late adapter row inherits selected color");check(late.font!=null&&late.font.family.equals("fonts/Nunito.ttf"),"late adapter row inherits bundled family");near(late.getTextSize(),21.6f,"native late row gets font scale once");
  TextView body=new TextView(host),title=new TextView(host);MenuTypography.set(body,false,18);MenuTypography.set(title,true,24);ViewGroup group=new ViewGroup(host);group.addView(title);group.addView(body);
  for(int i=0;i<100;i++)MenuTypography.tree(group,true);near(body.getTextSize(),21.6f,"repeated focus/tree refresh does not compound size");check(title.font.weight==Typeface.BOLD&&body.font.weight==Typeface.NORMAL,"heading role survives recursive styling");check(title.layouts.size()==1&&body.layouts.size()==1,"no accumulated layout callbacks");
  host.previewColor=1;MenuTypography.refresh(host);check(body.getPaint().getShader()!=null,"rainbow preview uses shader");int gradients=LinearGradient.created;for(int i=0;i<20;i++)MenuTypography.refresh(body);check(LinearGradient.created==gradients,"same-width refresh reuses gradient");body.resize(300);check(LinearGradient.created==gradients+1,"width change recreates one gradient");
  host.previewColor=0xFFFFD28A;MenuTypography.refresh(host);check(body.color==0xFFFFD28A&&body.getPaint().getShader()==null,"solid preview removes stale gradient");host.previewColor=null;MenuTypography.refresh(host);check(body.color==0xFFAAE4C1,"Cancel restores saved color");
  host.previewFont="orbitron";MenuTypography.refresh(host);check(body.font.family.equals("fonts/Orbitron.ttf"),"family preview applied");host.previewFont=null;MenuTypography.refresh(host);check(body.font.family.equals("fonts/Nunito.ttf"),"family Cancel restored");int loads=Typeface.loaded;for(int i=0;i<100;i++)MenuTypography.refresh(host);check(Typeface.loaded==loads,"no repeated font asset loads");
  host.getSharedPreferences("config",0).values.put("fontScalePercent",140);MenuTypography.refresh(host);near(body.getTextSize(),25.2f,"saved scale applies from original base");near(search.getTextSize(),42,"custom input retains unscaled base on setting change");
  android.content.ContextWrapper wrapped=new android.content.ContextWrapper(host);TextView wrappedRow=new TextView(wrapped);MenuTypography.set(wrappedRow,false,18);check(wrappedRow.color==0xFFAAE4C1,"dialog theme wrapper resolves actual host settings");
  System.out.println("Runtime typography: "+checks+" assertions, "+failures+" failures (host stand-ins; no Android pixel/device claim)");if(failures>0)throw new AssertionError("runtime regression");
 }
}'''
}
for name,content in sources.items():
    target=stubs/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(content,encoding='utf-8')
production=root/'src/com/ansub/tvhome'
command=[str(args.java_home/'bin/javac.exe'),'-encoding','UTF-8','-d',str(classes),'-sourcepath',str(stubs)]+[str(stubs/name) for name in sources]+[str(production/'MenuTypography.java'),str(production/'MenuTextPolicy.java')]
subprocess.run(command,check=True)
subprocess.run([str(args.java_home/'bin/java.exe'),'-cp',str(classes),'com.ansub.tvhome.MenuTypographyRuntime'],check=True)
