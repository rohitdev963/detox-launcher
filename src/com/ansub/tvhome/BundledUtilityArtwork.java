package com.ansub.tvhome;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Thirteen bounded local bitmaps, cached once; independent drawable/tint state per control. */
final class BundledUtilityArtwork {
 private static final Map<String,Bitmap> CACHE=new HashMap<>();
 private static final class Artwork extends BitmapDrawable {
  final boolean preserveColors;
  Artwork(Context context,Bitmap bitmap,boolean preserveColors){
   super(context.getResources(),bitmap);this.preserveColors=preserveColors;
   setFilterBitmap(true);setDither(true);setAntiAlias(true);
  }
 }
 static Drawable home(Context context,String control){return load(context,UtilityArtworkCatalog.home(control));}
 static boolean bundled(Drawable drawable){return drawable instanceof Artwork;}
 static boolean preserveColors(Drawable drawable){return drawable instanceof Artwork&&((Artwork)drawable).preserveColors;}
 static CharSequence railLabel(Context context,String control,String fallback,int side){
  Drawable icon=load(context,UtilityArtworkCatalog.rail(control));if(icon==null)return fallback;
  icon.setBounds(0,0,side,side);SpannableString label=new SpannableString("\uFFFC");
  label.setSpan(new ImageSpan(icon,ImageSpan.ALIGN_BOTTOM),0,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return label;
 }
 private static synchronized Drawable load(Context context,String stem){
  String asset=UtilityArtworkCatalog.asset(stem);if(asset==null)return null;
  Bitmap bitmap=CACHE.get(stem);
  if(bitmap==null){
   try(InputStream input=context.getAssets().open(asset)){
    BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
    bitmap=BitmapFactory.decodeStream(input,null,options);
    if(bitmap==null||bitmap.getWidth()!=256||bitmap.getHeight()!=256){if(bitmap!=null)bitmap.recycle();return null;}
    // TV utilities shrink a 256 px original to 30–80 physical pixels. GPU mipmaps
    // filter that large reduction instead of sampling only four source pixels.
    bitmap.setHasMipMap(true);
    CACHE.put(stem,bitmap);
   }catch(Exception ignored){return null;}
  }
  return new Artwork(context,bitmap,UtilityArtworkCatalog.preserveColors(stem));
 }
 private BundledUtilityArtwork(){}
}
