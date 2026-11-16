package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Executor;

/** Owns wallpaper image decoding, palette sampling, and the static wallpaper catalog. */
final class WallpaperAssets {
    interface PhotoTools {
        int orientation(String path);
        int orientation(Uri uri);
        Bitmap orient(Bitmap source, int orientation);
        Bitmap fit(Bitmap source);
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final int[][] palettes;
    private final int photoIndex;
    private final PhotoTools photoTools;
    private int cachedAccent;
    private String backgroundKey;
    private Drawable.ConstantState backgroundState;
    private final Object backgroundLock = new Object();
    private final Object thumbnailLock = new Object();
    private final Object previewLock = new Object();
    private final Drawable.ConstantState[] thumbnailStates;
    private String previewKey;
    private Drawable.ConstantState previewState;

    WallpaperAssets(Context context, String prefsName, int photoIndex,
            int[][] palettes, PhotoTools photoTools) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(prefsName, 0);
        this.photoIndex = photoIndex;
        this.palettes = palettes;
        this.photoTools = photoTools;
        this.thumbnailStates = new Drawable.ConstantState[Math.max(0, photoIndex)];
    }

    void invalidateAccent() { cachedAccent = 0; }

    int selected() {
        String saved = prefs.getString("wallpaper", null);
        if (saved == null) return 0;
        return BundledWallpaperCatalog.selected(saved);
    }

    Drawable background(int index) {
        synchronized (backgroundLock) {
            String path=prefs.getString("photoFile","");
            String key=index+"|"+context.getResources().getDisplayMetrics().widthPixels+"|"+context.getResources().getDisplayMetrics().heightPixels;
            if(index==photoIndex)key+="|"+path+"|"+(path.isEmpty()?0:new File(path).lastModified())+"|"+prefs.getString("photoUri","");
            if(key.equals(backgroundKey)&&backgroundState!=null)return backgroundState.newDrawable(context.getResources());
            Drawable result=decodeBackground(index);backgroundKey=key;backgroundState=result.getConstantState();return result;
        }
    }

    private Drawable decodeBackground(int index) {
        return decodeBackground(index, 1920);
    }

    Drawable previewBackground(int index) {
        synchronized (previewLock) {
            String path=prefs.getString("photoFile", "");
            String key=index+"|preview|"+context.getResources().getDisplayMetrics().widthPixels+"|"+context.getResources().getDisplayMetrics().heightPixels;
            if(index==photoIndex)key+="|"+path+"|"+(path.isEmpty()?0:new File(path).lastModified())+"|"+prefs.getString("photoUri", "");
            if(key.equals(previewKey)&&previewState!=null)return previewState.newDrawable(context.getResources());
            Drawable result=decodeBackground(index, 640);
            previewKey=key;previewState=result.getConstantState();
            return result;
        }
    }

    Drawable previewPhoto(String path) {
        if (path == null || path.isEmpty()) return decodeBackground(photoIndex, 640);
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 640);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap image = BitmapFactory.decodeFile(path, options);
            if (image == null) return decodeBackground(photoIndex, 640);
            image = photoTools.orient(image, photoTools.orientation(path));
            return drawable(image);
        } catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Wallpaper preview", "could not decode slideshow photo", error);
            return decodeBackground(photoIndex, 640);
        }
    }

    private Drawable decodeBackground(int index, int maxDimension) {
        if(index >= 0 && index < photoIndex){Bitmap image=bundled(index,maxDimension);if(image!=null)return drawable(maxDimension<1920?image:photoTools.fit(image));}
        if (index == photoIndex) {
            String path = prefs.getString("photoFile", null);
            if (path != null) {
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(path, bounds);
                    int sample = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = sample;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                    if (bitmap != null) {
                        bitmap = photoTools.orient(bitmap, photoTools.orientation(path));
                        if (maxDimension >= 1920) bitmap = photoTools.fit(bitmap);
                        return drawable(bitmap);
                    }
                } catch (Exception error) {
                    DetoxDiagnostics.recordFailure("DetoxWallpaper", "Wallpaper image", "could not decode saved photo", error);
                }
            }
            String uriValue = prefs.getString("photoUri", null);
            if (uriValue != null) {
                Uri uri = Uri.parse(uriValue);
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(input, null, bounds);
                    int sample = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension);
                    try (InputStream image = context.getContentResolver().openInputStream(uri)) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = sample;
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        Bitmap bitmap = BitmapFactory.decodeStream(image, null, options);
                        if (bitmap != null) {
                            bitmap = photoTools.orient(bitmap, photoTools.orientation(uri));
                            if (maxDimension >= 1920) bitmap = photoTools.fit(bitmap);
                            return drawable(bitmap);
                        }
                    }
                } catch (Exception error) {
                    DetoxDiagnostics.recordFailure("DetoxWallpaper", "Wallpaper image", "could not decode selected photo", error);
                }
            }
        }
        int safeIndex = Math.max(0, Math.min(palettes.length - 1, index));
        int[] colors = palettes[safeIndex];
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {colors[0], colors[1], colors[2]});
        GradientDrawable top = radial(colors[3], 0.08f, -0.12f, px(1250));
        GradientDrawable bottom = radial(colors[4], 1.0f, 1.1f, px(1000));
        for (GradientDrawable layer : new GradientDrawable[] {base, top, bottom}) layer.setDither(true);
        return new LayerDrawable(new Drawable[] {base, top, bottom});
    }

    int accentColor() {
        if (cachedAccent != 0) return cachedAccent;
        int index = selected();
        if (index != photoIndex) {
            // Bundled palettes are authored with each wallpaper; use them directly so a
            // wallpaper change never opens and decodes another JPEG on the UI thread.
            float[] hsv = new float[3];
            Color.colorToHSV(palettes[index][3], hsv);
            hsv[1] = Math.max(0.62f, hsv[1]);
            hsv[2] = 0.94f;
            cachedAccent = Color.HSVToColor(hsv);
            return cachedAccent;
        }
        String path = prefs.getString("photoFile", null);
        if (path != null) cachedAccent = sampleAccent(new File(path));
        if (cachedAccent == 0) {
            String uri = prefs.getString("photoUri", null);
            if (uri != null) cachedAccent = sampleAccent(Uri.parse(uri));
        }
        if (cachedAccent == 0) cachedAccent = 0xFF63D7FF;
        return cachedAccent;
    }

    private Bitmap bundled(int index,int limit){
        String path=BundledWallpaperCatalog.assetAt(index);
        try(InputStream input=context.getAssets().open(path)){
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeStream(input,null,bounds);
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sampleSize(bounds.outWidth,bounds.outHeight,limit);options.inPreferredConfig=Bitmap.Config.RGB_565;
            try(InputStream image=context.getAssets().open(path)){return orientBundledWallpaper(index,BitmapFactory.decodeStream(image,null,options));}
        }catch(Exception error){DetoxDiagnostics.recordFailure("DetoxWallpaper","Bundled wallpaper","Could not load "+path,error);return null;}
    }
    private Bitmap orientBundledWallpaper(int index,Bitmap image){
        if(!BundledWallpaperCatalog.landscapeRotation(index)||image==null||image.getWidth()>=image.getHeight())return image;
        Matrix rotation=new Matrix();rotation.setRotate(270f);
        Bitmap landscape=Bitmap.createBitmap(image,0,0,image.getWidth(),image.getHeight(),rotation,true);
        if(landscape!=image)image.recycle();
        return landscape;
    }
    Drawable thumbnail(int index){
        if (index < 0 || index >= thumbnailStates.length)
            return new android.graphics.drawable.ColorDrawable(0xFF142030);
        synchronized (thumbnailLock) {
            Drawable.ConstantState state=thumbnailStates[index];
            if(state==null){Bitmap image=bundled(index,96);if(image==null){Drawable fallback=new android.graphics.drawable.ColorDrawable(0xFF142030);thumbnailStates[index]=fallback.getConstantState();return fallback;}state=drawable(image).getConstantState();thumbnailStates[index]=state;}
            return state.newDrawable(context.getResources());
        }
    }

    /** Decode wallpaper-list thumbnails away from the UI thread, then return a cached drawable. */
    void thumbnailAsync(int index, Executor worker, Handler mainHandler, ThumbnailCallback callback) {
        if (index < 0 || index >= thumbnailStates.length || worker == null
                || mainHandler == null || callback == null) return;
        try {
            worker.execute(() -> {
                thumbnail(index);
                mainHandler.post(() -> callback.ready(thumbnail(index)));
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    interface ThumbnailCallback { void ready(Drawable drawable); }

    private Drawable drawable(Bitmap bitmap) {
        return new AspectWallpaperDrawable(bitmap);
    }

    private int sampleAccent(File file) {
        if (!file.isFile()) return 0;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 64);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return sampleAccent(BitmapFactory.decodeFile(file.getAbsolutePath(), options));
        } catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Wallpaper color", "could not sample saved photo", error);
            return 0;
        }
    }

    private int sampleAccent(Uri uri) {
        try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, 64);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            try (InputStream imageStream = context.getContentResolver().openInputStream(uri)) {
                return sampleAccent(BitmapFactory.decodeStream(imageStream, null, options));
            }
        } catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Wallpaper color", "could not sample selected photo", error);
            return 0;
        }
    }

    private int sampleAccent(Bitmap bitmap) {
        if (bitmap == null) return 0;
        try {
            int[] bins = new int[24];
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int stepX = Math.max(1, width / 48), stepY = Math.max(1, height / 48);
            float[] hsv = new float[3];
            for (int y = stepY / 2; y < height; y += stepY) {
                for (int x = stepX / 2; x < width; x += stepX) {
                    Color.colorToHSV(bitmap.getPixel(x, y), hsv);
                    if (hsv[1] < 0.20f || hsv[2] < 0.18f) continue;
                    int bin = Math.min(23, (int) (hsv[0] / 15f));
                    bins[bin] += Math.max(1, Math.round(hsv[1] * hsv[2] * 100f));
                }
            }
            int best = 0;
            for (int i = 1; i < bins.length; i++) if (bins[i] > bins[best]) best = i;
            if (bins[best] == 0) return 0;
            return Color.HSVToColor(new float[] {best * 15f + 7.5f, 0.72f, 0.94f});
        } finally { bitmap.recycle(); }
    }

    private int sampleSize(int width, int height, int maxDimension) {
        int sample = 1;
        while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2;
        return sample;
    }

    private int px(float value) {
        float scale = context.getResources().getDisplayMetrics().widthPixels / 1920f;
        return Math.round(value * scale);
    }

    private static GradientDrawable radial(int color, float cx, float cy, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        drawable.setColors(new int[] {color, color & 0x00FFFFFF});
        drawable.setGradientCenter(cx, cy);
        drawable.setGradientRadius(radius);
        return drawable;
    }
}



