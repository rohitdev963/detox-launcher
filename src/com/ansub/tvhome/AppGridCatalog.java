package com.ansub.tvhome;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Resolves the ordered, launchable app keys used to populate the home grid. */
final class AppGridCatalog {
    private AppGridCatalog() {}

    static List<String> load(PackageManager pm, SharedPreferences prefs,
            String preferenceKey, String[] defaults, Collection<String> excluded,
            String ownPackage, int autoFillLimit) {
        List<String> apps = new ArrayList<>();
        String saved = prefs.getString(preferenceKey, null);
        String[] wanted = saved != null ? saved.split("[,\\s]+") : defaults;
        for (String packageName : wanted) {
            if (packageName.isEmpty() || apps.contains(packageName)) continue;
            if (packageName.startsWith("folder:")) {
                if (prefs.contains("folderApps:" + packageName)) apps.add(packageName);
            } else if (launchIntent(pm, packageName) != null) {
                apps.add(packageName);
            }
        }
        if (saved == null) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                if (apps.size() >= autoFillLimit) break;
                if (info.activityInfo == null || info.activityInfo.applicationInfo == null) continue;
                String packageName = info.activityInfo.packageName;
                int flags = info.activityInfo.applicationInfo.flags;
                boolean stockSystem = (flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
                if (stockSystem || apps.contains(packageName) || excluded.contains(packageName)
                        || ownPackage.equals(packageName)) continue;
                apps.add(packageName);
            }
        }
        return apps;
    }

    private static final java.util.Map<String, Intent> launchCache = new java.util.HashMap<>();
    private static long generation;
    static synchronized void invalidate() { launchCache.clear(); generation++; }
    static Intent launchIntent(PackageManager pm, String packageName) {
        long version;
        synchronized (AppGridCatalog.class) {
            if (launchCache.containsKey(packageName)) {
                Intent cached = launchCache.get(packageName);
                return cached == null ? null : new Intent(cached);
            }
            version = generation;
        }
        // Binder calls can stall during boot. Never hold the cache lock while resolving.
        Intent resolved = resolveLaunchIntent(pm, packageName);
        synchronized (AppGridCatalog.class) {
            if (version == generation)
                launchCache.put(packageName, resolved == null ? null : new Intent(resolved));
        }
        return resolved;
    }
    private static Intent resolveLaunchIntent(PackageManager pm, String packageName) {
        Intent intent = pm.getLeanbackLaunchIntentForPackage(packageName);
        if (intent != null) return intent;
        intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) return intent;
        // Some TV apps register a Leanback entry point without DEFAULT, so resolve it directly.
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(packageName);
            List<ResolveInfo> matches = pm.queryIntentActivities(query, 0);
            if (!matches.isEmpty() && matches.get(0).activityInfo != null)
                return new Intent(query).setClassName(packageName, matches.get(0).activityInfo.name);
        }
        return null;
    }
}

