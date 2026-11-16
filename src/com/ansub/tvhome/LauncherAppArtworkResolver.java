package com.ansub.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Resolves the same per-app custom artwork and icon-pack settings on every Detox surface. */
final class LauncherAppArtworkResolver {
    private final Context context;
    private final Map<String, String> packMappings = new HashMap<>();
    private String loadedPack = "";
    private float packScale = 1f;

    LauncherAppArtworkResolver(Context context) {
        this.context = context.getApplicationContext();
    }

    synchronized Drawable resolve(String packageName) {
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        Drawable icon = customIcon(prefs, packageName);
        if (icon != null) return icon;

        String selectedPack = prefs.getString("iconPackPackage", "");
        if (!selectedPack.isEmpty()) {
            icon = packIcon(selectedPack, packageName);
            if (icon != null) return icon;
        }

        PackageManager pm = context.getPackageManager();
        try {
            icon = pm.getApplicationIcon(packageName);
            if (icon != null && !selectedPack.isEmpty())
                icon = new PackIconDrawable(icon, false, packScale);
        } catch (Exception ignored) {}

        if (icon == null) {
            Intent launch = AppGridCatalog.launchIntent(pm, packageName);
            if (launch != null) try { icon = pm.getActivityBanner(launch); }
            catch (Exception ignored) {}
        }
        return icon;
    }

    synchronized void invalidate() {
        loadedPack = null;
        packScale = 1f;
        packMappings.clear();
    }

    private Drawable customIcon(SharedPreferences prefs, String packageName) {
        String file = prefs.getString("customAppIconFile:" + packageName, null);
        if (file != null) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file, bounds);
                Bitmap bitmap = BitmapFactory.decodeFile(file, boundedOptions(bounds));
                if (bitmap != null) return new BitmapDrawable(context.getResources(), bitmap);
            } catch (Exception ignored) {}
        }

        String savedUri = prefs.getString("customAppIcon:" + packageName, null);
        if (savedUri == null) return null;
        try {
            Uri uri = Uri.parse(savedUri);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(stream, null, bounds);
            }
            BitmapFactory.Options options = boundedOptions(bounds);
            Bitmap bitmap;
            try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(stream, null, options);
            }
            return bitmap == null ? null : new BitmapDrawable(context.getResources(), bitmap);
        } catch (Exception ignored) { return null; }
    }

    private BitmapFactory.Options boundedOptions(BitmapFactory.Options bounds) {
        int sample = 1;
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return options;
    }

    private Drawable packIcon(String pack, String appPackage) {
        loadPackMappings(pack);
        String name = packMappings.get(appPackage);
        if (name == null) return null;
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(pack);
            int id = resources.getIdentifier(name, "drawable", pack);
            if (id == 0) id = resources.getIdentifier(name, "mipmap", pack);
            return id == 0 ? null : new PackIconDrawable(resources.getDrawable(id, null), true, 1f);
        } catch (Exception ignored) { return null; }
    }

    private void loadPackMappings(String packageName) {
        if (packageName.equals(loadedPack)) return;
        loadedPack = packageName;
        packScale = 1f;
        packMappings.clear();
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(packageName);
            int id = resources.getIdentifier("appfilter", "xml", packageName);
            if (id == 0) return;
            XmlResourceParser parser = resources.getXml(id);
            try {
                while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() != org.xmlpull.v1.XmlPullParser.START_TAG) continue;
                    if ("scale".equals(parser.getName())) {
                        try { packScale = IconSizingPolicy.factor(Float.parseFloat(parser.getAttributeValue(null, "factor"))); }
                        catch (Exception ignored) { packScale = 1f; }
                    } else if ("item".equals(parser.getName())) {
                        String app = packageFromComponent(parser.getAttributeValue(null, "component"));
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (app != null && drawable != null) packMappings.put(app, drawable);
                    }
                }
            } finally { parser.close(); }
        } catch (Exception ignored) { packMappings.clear(); }
    }

    private String packageFromComponent(String component) {
        if (component == null) return null;
        int brace = component.indexOf('{');
        if (brace >= 0) component = component.substring(brace + 1);
        int endBrace = component.indexOf('}');
        if (endBrace >= 0) component = component.substring(0, endBrace);
        int slash = component.indexOf('/');
        String value = slash < 0 ? component.trim() : component.substring(0, slash).trim();
        return value.isEmpty() ? null : value;
    }
}
