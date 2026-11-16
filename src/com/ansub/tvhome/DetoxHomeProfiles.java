package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Named snapshots of launcher preferences only. Never copies or deletes user media. */
final class DetoxHomeProfiles {
    private static final String CONFIG = "config";
    private static final String STORE = "profileStore";
    private static final String ACTIVE = "activeProfile";
    private static final String FORMAT = "detox.home.profiles";
    private static final int VERSION = 1;
    private static final int MAX_DOCUMENT_BYTES = 2 * 1024 * 1024;

    private DetoxHomeProfiles() { }

    static String[] names(Context context) throws JSONException {
        JSONObject store = readStore(prefs(context));
        String[] names = new String[store.length()];
        Iterator<String> keys = store.keys();
        for (int i = 0; i < names.length; i++) names[i] = keys.next();
        java.util.Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    static void save(Context context, String rawName) throws JSONException {
        String name = HomeProfilePolicy.validateName(rawName);
        SharedPreferences prefs = prefs(context);
        JSONObject store = readStore(prefs);
        if (!store.has(name) && store.length() >= HomeProfilePolicy.MAX_PROFILES)
            throw new IllegalStateException("Maximum of 8 profiles reached");
        store.put(name, LauncherProfileCodec.encode(prefs.getAll(), true));
        if (!prefs.edit().putString(STORE, store.toString()).putString(ACTIVE, name).commit())
            throw new IllegalStateException("Could not save profile");
    }

    static void activate(Context context, String rawName) throws JSONException {
        String name = HomeProfilePolicy.validateName(rawName);
        SharedPreferences prefs = prefs(context);
        JSONObject store = readStore(prefs);
        JSONObject snapshot = store.optJSONObject(name);
        if (snapshot == null) throw new IllegalArgumentException("Profile does not exist");
        Map<String, Object> next = LauncherProfileCodec.decode(snapshot, prefs.getAll(), true);

        // Save current visual setup before switching, so Back/switching never
        // silently discards the state the user was just viewing.
        String active = prefs.getString(ACTIVE, "");
        if (!active.isEmpty() && !active.equals(name) && store.has(active))
            store.put(active, LauncherProfileCodec.encode(prefs.getAll(), true));

        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet())
            if (HomeProfilePolicy.isProfileSetting(key)) editor.remove(key);
        putValues(editor, next);
        editor.putString(STORE, store.toString()).putString(ACTIVE, name);
        if (!editor.commit()) throw new IllegalStateException("Could not activate profile");
    }

    static void delete(Context context, String rawName) throws JSONException {
        String name = HomeProfilePolicy.validateName(rawName);
        SharedPreferences prefs = prefs(context);
        JSONObject store = readStore(prefs);
        if (!store.has(name)) return;
        store.remove(name);
        SharedPreferences.Editor editor = prefs.edit().putString(STORE, store.toString());
        if (name.equals(prefs.getString(ACTIVE, ""))) editor.remove(ACTIVE);
        if (!editor.commit()) throw new IllegalStateException("Could not delete profile");
    }

    static String active(Context context) {
        return prefs(context).getString(ACTIVE, "");
    }

    static void exportTo(Context context, Uri uri) throws Exception {
        JSONObject document = new JSONObject();
        JSONObject profiles = readStore(prefs(context));
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("Could not open export destination");
            out.write(document.put("format", FORMAT).put("version", VERSION)
                    .put("profiles", profiles).toString(2).getBytes("UTF-8"));
            out.flush();
        }
    }

    static int importFrom(Context context, Uri uri) throws Exception {
        JSONObject document;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Could not open profile file");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (bytes.size() + n > MAX_DOCUMENT_BYTES) throw new IOException("Profile file is too large");
                bytes.write(buffer, 0, n);
            }
            document = new JSONObject(new String(bytes.toByteArray(), "UTF-8"));
        }
        if (!FORMAT.equals(document.optString("format")) || document.optInt("version", -1) != VERSION)
            throw new IOException("Unsupported profile file");
        JSONObject incoming = document.optJSONObject("profiles");
        if (incoming == null || incoming.length() > HomeProfilePolicy.MAX_PROFILES)
            throw new IOException("Invalid profile collection");

        // Validate every name and preference before modifying the existing store.
        JSONObject validated = new JSONObject();
        Iterator<String> names = incoming.keys();
        while (names.hasNext()) {
            String name = HomeProfilePolicy.validateName(names.next());
            JSONObject snapshot = incoming.optJSONObject(name);
            if (snapshot == null) throw new IOException("Invalid profile snapshot");
            Map<String, Object> values = LauncherProfileCodec.decode(snapshot, new HashMap<>(), true);
            validated.put(name, LauncherProfileCodec.encode(values, true));
        }
        SharedPreferences prefs = prefs(context);
        JSONObject merged = readStore(prefs);
        Iterator<String> keys = validated.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            merged.put(name, validated.get(name));
        }
        if (merged.length() > HomeProfilePolicy.MAX_PROFILES) throw new IOException("Import would exceed 8 profiles");
        if (!prefs.edit().putString(STORE, merged.toString()).commit()) throw new IOException("Could not save imported profiles");
        return validated.length();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(CONFIG, Context.MODE_PRIVATE);
    }

    private static JSONObject readStore(SharedPreferences prefs) throws JSONException {
        JSONObject store = new JSONObject(prefs.getString(STORE, "{}"));
        if (store.length() > HomeProfilePolicy.MAX_PROFILES) throw new IllegalArgumentException("Invalid profile store");
        Iterator<String> keys = store.keys();
        while (keys.hasNext()) {
            String name = HomeProfilePolicy.validateName(keys.next());
            if (store.optJSONObject(name) == null) throw new IllegalArgumentException("Invalid profile entry");
        }
        return store;
    }

    private static void putValues(SharedPreferences.Editor editor, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!HomeProfilePolicy.isProfileSetting(entry.getKey())) continue;
            Object value = entry.getValue(); String key = entry.getKey();
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
            else if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Set) {
                @SuppressWarnings("unchecked") Set<String> strings = (Set<String>) value;
                editor.putStringSet(key, new HashSet<>(strings));
            } else throw new IllegalArgumentException("Unsupported profile setting");
        }
    }
}
