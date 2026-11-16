package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Shared EXIF orientation and 16:9 photo preparation used by wallpaper and icon imports. */
final class PhotoImageTools {
    private PhotoImageTools() {}

    static int orientation(String path) {
        try { return new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL); }
        catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Photo orientation", "could not read image metadata", error);
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    static int orientation(Context context, Uri uri) {
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) return ExifInterface.ORIENTATION_NORMAL;
            if (Build.VERSION.SDK_INT >= 24) return new ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            // The stream constructor is API 24. Older TVs need the filename constructor.
            // Unique app-cache scratch files avoid collisions between simultaneous imports.
            File scratch = File.createTempFile("detox-exif-", ".tmp", context.getCacheDir());
            try {
                copyMetadataSource(input, scratch);
                return orientation(scratch.getAbsolutePath());
            } finally { scratch.delete(); }
        } catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Photo orientation", "could not read selected image metadata", error);
            return ExifInterface.ORIENTATION_NORMAL;
        }
    }

    static void copyMetadataSource(InputStream input, File scratch) throws IOException {
        try (FileOutputStream output = new FileOutputStream(scratch)) {
            byte[] buffer = new byte[8192]; int count; long total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > 24L * 1024L * 1024L) throw new IOException("Photo metadata source is too large");
                output.write(buffer, 0, count);
            }
        }
    }

    static Bitmap orient(Bitmap source, int orientation) {
        Matrix rotation = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: rotation.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_180: rotation.setRotate(180); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                rotation.setRotate(180); rotation.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                rotation.setRotate(90); rotation.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_90: rotation.setRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                rotation.setRotate(-90); rotation.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_ROTATE_270: rotation.setRotate(-90); break;
        }
        if (rotation.isIdentity()) return source;
        Bitmap oriented = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), rotation, true);
        if (oriented != source) source.recycle();
        return oriented;
    }

    /** Makes a 16:9 wallpaper without stretching or cropping away any part of the photo. */
    static Bitmap fitWallpaper(Bitmap source) {
        Bitmap output = Bitmap.createBitmap(1920, 1080, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        float cover = Math.max(1920f / source.getWidth(), 1080f / source.getHeight());
        float coverW = source.getWidth() * cover, coverH = source.getHeight() * cover;
        RectF coverRect = new RectF((1920 - coverW) / 2f, (1080 - coverH) / 2f,
                (1920 + coverW) / 2f, (1080 + coverH) / 2f);
        canvas.drawBitmap(source, null, coverRect, paint);
        paint.setColor(0x66000000);
        canvas.drawRect(0, 0, 1920, 1080, paint);
        paint.setColor(android.graphics.Color.WHITE);
        float contain = Math.min(1920f / source.getWidth(), 1080f / source.getHeight());
        float fitW = source.getWidth() * contain, fitH = source.getHeight() * contain;
        RectF fitRect = new RectF((1920 - fitW) / 2f, (1080 - fitH) / 2f,
                (1920 + fitW) / 2f, (1080 + fitH) / 2f);
        canvas.drawBitmap(source, null, fitRect, paint);
        source.recycle();
        return output;
    }
}
