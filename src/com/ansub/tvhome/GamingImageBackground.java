package com.ansub.tvhome;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.util.concurrent.*;

/** Bounded shared image cache and off-main-thread decode; no animation/timer or copied media. */
final class GamingImageBackground extends ImageView {
 interface Failure { void failed(String source); default void ready() { } }
 private static final LruCache<String,Bitmap> CACHE=new LruCache<String,Bitmap>(12*1024*1024){
  @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
 };
 private final GamingBackgroundCatalog.Choice choice;private final Failure failure;
 private final boolean fill;private final float zoom,x,y;
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"DetoxGamingWallpaper");t.setDaemon(true);return t;});
 private Bitmap image;private Future<?> decode;private int generation;private Runnable delivery;
 GamingImageBackground(Context context,GamingBackgroundCatalog.Choice choice,boolean fill,float zoom,float x,float y,Failure failure){
  super(context);this.choice=choice;this.failure=failure;this.fill=fill;this.zoom=zoom;this.x=x;this.y=y;
  setScaleType(ScaleType.MATRIX);setBackgroundColor(Color.BLACK);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();load();}
 private void load(){
  final int request=++generation;final Context application=getContext().getApplicationContext();
  try{decode=worker.submit(()->{
   Bitmap loaded=null;String key=(choice.followsHome?"home:":"shared:")+choice.source;
   if(choice.source.startsWith("file:")){File file=new File(choice.source.substring(5));key+="|"+file.lastModified()+"|"+file.length();}
   try{
    loaded=CACHE.get(key);
    if(loaded==null){loaded=read(application,choice);if(loaded!=null)CACHE.put(key,loaded);}
   }catch(Exception|OutOfMemoryError error){DetoxDiagnostics.recordFailure("DetoxGaming","Wallpaper image","Could not decode "+choice.source,error);}
   final Bitmap ready=loaded;
   if(Thread.currentThread().isInterrupted())return;
   delivery=()->{if(request!=generation||!isAttachedToWindow())return;
    if(ready==null){if(failure!=null)failure.failed(choice.source);return;}
    image=ready;setImageBitmap(image);fit();if(failure!=null)failure.ready();};post(delivery);
  });}catch(RejectedExecutionException ignored){}
 }
 private static InputStream open(Context context,String source)throws IOException{
  if(source.startsWith("asset:"))return context.getAssets().open(source.substring(6));
  if(source.startsWith("file:"))return new FileInputStream(source.substring(5));
  InputStream stream=context.getContentResolver().openInputStream(Uri.parse(source));
  if(stream==null)throw new IOException("No readable image stream");return stream;
 }
 private static Bitmap read(Context context,GamingBackgroundCatalog.Choice choice)throws IOException{
  BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
  try(InputStream input=open(context,choice.source)){BitmapFactory.decodeStream(input,null,bounds);}
  if(bounds.outWidth<=0||bounds.outHeight<=0)return null;
  BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;options.inPreferredConfig=Bitmap.Config.RGB_565;
  while(bounds.outWidth/options.inSampleSize>1920||bounds.outHeight/options.inSampleSize>1920)options.inSampleSize*=2;
  Bitmap result;try(InputStream input=open(context,choice.source)){result=BitmapFactory.decodeStream(input,null,options);}
  if(result==null)return null;
  if(choice.source.startsWith("asset:")){
   for(int i=0;i<BundledWallpaperCatalog.size();i++)if(choice.source.equals("asset:"+BundledWallpaperCatalog.assetAt(i))&&BundledWallpaperCatalog.landscapeRotation(i)&&result.getHeight()>result.getWidth()){
    Matrix rotation=new Matrix();rotation.setRotate(270);Bitmap landscape=Bitmap.createBitmap(result,0,0,result.getWidth(),result.getHeight(),rotation,true);if(landscape!=result)result.recycle();result=landscape;break;
   }
  }else result=PhotoImageTools.orient(result,choice.source.startsWith("file:")?PhotoImageTools.orientation(choice.source.substring(5)):PhotoImageTools.orientation(context,Uri.parse(choice.source)));
  // Match Home's existing photo preparation for imported/portrait imagery.
  if(choice.followsHome)result=PhotoImageTools.fitWallpaper(result);
  return result;
 }
 private void fit(){if(image==null)return;float[] values=GamingBackgroundGeometry.matrix(image.getWidth(),image.getHeight(),getWidth(),getHeight(),fill,zoom,x,y);Matrix matrix=new Matrix();matrix.setScale(values[0],values[0]);matrix.postTranslate(values[1],values[2]);setImageMatrix(matrix);}
 @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);fit();}
 @Override protected void onDetachedFromWindow(){generation++;if(decode!=null)decode.cancel(true);if(delivery!=null)removeCallbacks(delivery);worker.shutdownNow();setImageDrawable(null);image=null;super.onDetachedFromWindow();}
}
