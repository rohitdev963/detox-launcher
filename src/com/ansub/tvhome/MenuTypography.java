package com.ansub.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared menu-only text styling. No preference writes, timers or retained Activity references. */
final class MenuTypography {
 private static final Map<String,Typeface> FONTS=new HashMap<>();
 private static final WeakHashMap<TextView,Style> TEXT=new WeakHashMap<>();
 private static final WeakHashMap<TextView,Float> SCALED_BASES=new WeakHashMap<>();
 private static final WeakHashMap<android.widget.AbsListView,Boolean> LAZY_LISTS=new WeakHashMap<>();
 private static WeakReference<MainActivity> home=new WeakReference<>(null);
 private static int wallpaperAccent=0xFF63D7FF;
 private static final class Style {
  float basePx;boolean heading;int shaderWidth=-1;boolean rainbow;
  Style(float size,boolean heading){basePx=size;this.heading=heading;}
 }
 static void bind(MainActivity activity){home=new WeakReference<>(activity);}
 static int rememberWallpaperAccent(int color){wallpaperAccent=color;return color;}
 private static Activity owner(Context context){
  for(int i=0;i<12;i++){
   if(context instanceof Activity)return (Activity)context;
   if(!(context instanceof ContextWrapper))break;
   Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;
  }
  return null;
 }
 private static SharedPreferences prefs(Context context){return context.getSharedPreferences("config",0);}
 private static float scale(Context context){return MenuTextPolicy.scale(prefs(context).getInt("fontScalePercent",120));}
 static synchronized Typeface typeface(Context context,String key,int weight){
  String asset=MenuTextPolicy.fontAsset(key);
  if(asset==null)return Typeface.create("sans-serif",weight);
  String id=key+":"+weight;Typeface font=FONTS.get(id);if(font!=null)return font;
  try{
   Typeface base=FONTS.get(key);
   if(base==null){base=Typeface.createFromAsset(context.getApplicationContext().getAssets(),asset);FONTS.put(key,base);}
   font=Typeface.create(base,weight);FONTS.put(id,font);return font;
  }catch(RuntimeException missing){return Typeface.create("sans-serif",weight);}
 }
 private static String fontKey(Context context){Activity host=owner(context);return host instanceof MainActivity?((MainActivity)host).menuFontKey():prefs(context).getString("font","nunito");}
 private static int requestedColor(Context context){Activity host=owner(context);return host instanceof MainActivity?((MainActivity)host).menuFontColor():prefs(context).getInt("fontColor",0xFFFFFFFF);}
 private static int accent(Context context){
  Activity host=owner(context);MainActivity source=host instanceof MainActivity?(MainActivity)host:home.get();
  return source!=null&&!source.isDestroyed()?source.menuWallpaperAccent():wallpaperAccent;
 }
 static boolean managed(TextView view){return TEXT.containsKey(view);}
 /** Record Main's factory sizing without opting Home/utility labels into menu styling. */
 static void recordScaledText(TextView view,float basePx){SCALED_BASES.put(view,basePx);}
 /** Explicit unscaled pixel size; repeated application cannot compound font scaling. */
 static void set(TextView view,boolean heading,float basePx){
  Style style=TEXT.get(view);
  if(style==null){
   style=new Style(basePx,heading);TEXT.put(view,style);
   // The callback uses its argument, not a captured View/Activity. Recompute only on width changes.
   view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol)paint((TextView)v);});
  }else{style.basePx=basePx;style.heading=heading;}
  refresh(view);
 }
 /** Main's existing text factory has already applied fontScalePercent. */
 static void adoptScaled(TextView view,boolean heading){
  Style style=TEXT.get(view);if(style==null)set(view,heading,view.getTextSize()/scale(view.getContext()));
  else{style.heading=heading;refresh(view);}
 }
 static void adopt(TextView view){
  if(!managed(view)){Float base=SCALED_BASES.get(view);set(view,false,base==null?view.getTextSize():base);}
  else refresh(view);
 }
 static void refresh(TextView view){
  Style style=TEXT.get(view);if(style==null)return;
  view.setTextSize(TypedValue.COMPLEX_UNIT_PX,style.basePx*scale(view.getContext()));
  view.setTypeface(typeface(view.getContext(),fontKey(view.getContext()),style.heading?Typeface.BOLD:Typeface.NORMAL));
  view.getPaint().setFakeBoldText(false);view.setIncludeFontPadding(false);
  view.setLetterSpacing(style.heading?.018f:0f);paint(view);
 }
 private static void paint(TextView view){
  Style style=TEXT.get(view);if(style==null)return;
  int request=requestedColor(view.getContext()),ink=MenuTextPolicy.ink(request,request==MenuTextPolicy.WALLPAPER?accent(view.getContext()):wallpaperAccent);
  view.setTextColor(ink);
  if(view instanceof android.widget.EditText)((android.widget.EditText)view).setHintTextColor((ink&0x00FFFFFF)|0x99000000);
  float density=view.getResources().getDisplayMetrics().density;
  view.setShadowLayer(2*density,0,density,MenuTextPolicy.shadow(ink));
  int width=Math.max(1,view.getWidth()-view.getPaddingLeft()-view.getPaddingRight());
  if(request==MenuTextPolicy.RAINBOW){
   if(!style.rainbow||style.shaderWidth!=width||view.getPaint().getShader()==null){view.getPaint().setShader(new LinearGradient(0,0,width,0,MenuTextPolicy.RAINBOW_COLORS,null,Shader.TileMode.CLAMP));style.shaderWidth=width;}
   style.rainbow=true;
  }else{view.getPaint().setShader(null);style.rainbow=false;style.shaderWidth=-1;}
 }
 static void tree(View root){tree(root,false);}
 static void tree(View root,boolean scaledFactory){
  if(root instanceof TextView){TextView text=(TextView)root;if(managed(text))refresh(text);else if(scaledFactory)adoptScaled(text,false);else adopt(text);}
  if(root instanceof android.widget.AbsListView){
   android.widget.AbsListView list=(android.widget.AbsListView)root;
   if(!LAZY_LISTS.containsKey(list)){
    LAZY_LISTS.put(list,true);
    list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){
     @Override public void onChildViewAdded(View parent,View child){tree(child);}
     @Override public void onChildViewRemoved(View parent,View child){}
    });
   }
  }
  if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)tree(group.getChildAt(i),scaledFactory);}
 }
 static void refresh(Activity host){
  for(TextView view:new ArrayList<>(TEXT.keySet()))if(view!=null&&owner(view.getContext())==host)refresh(view);
 }
 private MenuTypography(){}
}
