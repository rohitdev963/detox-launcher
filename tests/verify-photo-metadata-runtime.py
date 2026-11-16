"""Run actual photo metadata compatibility code with Android API-level stand-ins."""
from pathlib import Path
import argparse, subprocess

p=argparse.ArgumentParser()
p.add_argument('--java-home',type=Path,required=True)
args=p.parse_args()
root=Path(__file__).resolve().parents[1]
out=root/'build/deep-code-review/photo-runtime'
stubs,classes=out/'stubs',out/'classes'
classes.mkdir(parents=True,exist_ok=True)
sources={
'android/os/Build.java':'''package android.os;public class Build {public static class VERSION {public static int SDK_INT=21;}}''',
'android/net/Uri.java':'''package android.net;public class Uri {}''',
'android/content/ContentResolver.java':'''package android.content;import java.io.*;public class ContentResolver {public byte[] data={6};public boolean fail;public InputStream openInputStream(android.net.Uri u)throws IOException {if(fail)throw new IOException("unreadable");return data==null?null:new ByteArrayInputStream(data);}}''',
'android/content/Context.java':'''package android.content;public class Context {public final ContentResolver resolver=new ContentResolver();public final java.io.File cache;public Context(java.io.File cache){this.cache=cache;}public ContentResolver getContentResolver(){return resolver;}public java.io.File getCacheDir(){return cache;}}''',
'android/media/ExifInterface.java':'''package android.media;import java.io.*;public class ExifInterface {public static final int ORIENTATION_NORMAL=1,ORIENTATION_FLIP_HORIZONTAL=2,ORIENTATION_ROTATE_180=3,ORIENTATION_FLIP_VERTICAL=4,ORIENTATION_TRANSPOSE=5,ORIENTATION_ROTATE_90=6,ORIENTATION_TRANSVERSE=7,ORIENTATION_ROTATE_270=8;public static final String TAG_ORIENTATION="Orientation";public static int streamReads,fileReads;int value;public ExifInterface(InputStream in)throws IOException {if(android.os.Build.VERSION.SDK_INT<24)throw new NoSuchMethodError("API 24 constructor");streamReads++;value=in.read();}public ExifInterface(String path)throws IOException {fileReads++;try(InputStream in=new FileInputStream(path)){value=in.read();}}public int getAttributeInt(String s,int fallback){return value<0?fallback:value;}}''',
'android/graphics/Matrix.java':'''package android.graphics;public class Matrix {public void setScale(float x,float y){}public void setRotate(float n){}public void postScale(float x,float y){}public boolean isIdentity(){return true;}}''',
'android/graphics/Bitmap.java':'''package android.graphics;public class Bitmap {public enum Config {RGB_565}public int getWidth(){return 10;}public int getHeight(){return 10;}public void recycle(){}public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap();}public static Bitmap createBitmap(Bitmap b,int a,int d,int w,int h,Matrix m,boolean f){return new Bitmap();}}''',
'android/graphics/Canvas.java':'''package android.graphics;public class Canvas {public Canvas(Bitmap b){}public void drawBitmap(Bitmap b,Object s,RectF r,Paint p){}public void drawRect(int a,int b,int c,int d,Paint p){}}''',
'android/graphics/Paint.java':'''package android.graphics;public class Paint {public static final int ANTI_ALIAS_FLAG=1,FILTER_BITMAP_FLAG=2,DITHER_FLAG=4;public Paint(int n){}public void setColor(int n){}}''',
'android/graphics/RectF.java':'''package android.graphics;public class RectF {public RectF(float a,float b,float c,float d){}}''',
'android/graphics/Color.java':'''package android.graphics;public class Color {public static final int WHITE=-1;}''',
'com/ansub/tvhome/DetoxDiagnostics.java':'''package com.ansub.tvhome;final class DetoxDiagnostics {static void recordFailure(String a,String b,String c,Throwable e){}}''',
'com/ansub/tvhome/PhotoMetadataRuntime.java':'''package com.ansub.tvhome;import java.io.*;import android.media.ExifInterface;public class PhotoMetadataRuntime {static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}static void clean(File cache){check(cache.list().length==0,"metadata scratch must be removed");}public static void main(String[] args)throws Exception {File cache=new File(args[0]);cache.mkdirs();android.content.Context context=new android.content.Context(cache);android.net.Uri uri=new android.net.Uri();for(int api:new int[]{21,22,23,24,30,34}){android.os.Build.VERSION.SDK_INT=api;check(PhotoImageTools.orientation(context,uri)==6,"EXIF rotation retained at API "+api);clean(cache);}check(ExifInterface.fileReads==3&&ExifInterface.streamReads==3,"only compatible constructors called");context.resolver.data=null;check(PhotoImageTools.orientation(context,uri)==1,"null stream degrades safely");context.resolver.fail=true;check(PhotoImageTools.orientation(context,uri)==1,"unreadable metadata degrades safely");context.resolver.fail=false;context.resolver.data=new byte[24*1024*1024+1];android.os.Build.VERSION.SDK_INT=21;check(PhotoImageTools.orientation(context,uri)==1,"oversized source bounded");clean(cache);File scratch=new File(cache,"copy.tmp");try {PhotoImageTools.copyMetadataSource(new ByteArrayInputStream(new byte[]{1,2,3}),scratch);check(scratch.length()==3,"bounded copy preserves source bytes");}finally{scratch.delete();}clean(cache);System.out.println("PASS: "+checks+" photo metadata compatibility/runtime assertions (Android stand-ins; not real EXIF decoder validation)");}}
'''
}
files=[]
for name,body in sources.items():
    path=stubs/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(body,encoding='utf-8')
    files.append(str(path))
files.append(str(root/'src/com/ansub/tvhome/PhotoImageTools.java'))
subprocess.run([str(args.java_home/'bin/javac.exe'),'-encoding','UTF-8','-d',str(classes),*files],check=True)
subprocess.run([str(args.java_home/'bin/java.exe'),'-cp',str(classes),'com.ansub.tvhome.PhotoMetadataRuntime',str(out/'cache')],check=True)
