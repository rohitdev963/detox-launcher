package com.ansub.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.tv.TvContract;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Optional compact home cards. Provider and media-session lookups stay off the UI thread. */
final class HomeMediaCards {
    private HomeMediaCards() { }

    static void refresh(Activity activity, FrameLayout host) {
        View old = host.findViewWithTag("home-media-cards");
        if (old != null) host.removeView(old);
        if (!activity.getSharedPreferences("config", 0).getBoolean("homeMediaCards", false)) return;

        LinearLayout row = new LinearLayout(activity);
        row.setTag("home-media-cards");
        row.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = 16;
        host.addView(row, params);

        Thread lookup = new Thread(() -> {
            String watchTitle = null;
            Intent watchAction = null;
            String mediaTitle = null;
            Bitmap artwork = null;
            boolean mediaActive = false;
            try (Cursor cursor = activity.getContentResolver().query(TvContract.WatchNextPrograms.CONTENT_URI,
                    new String[] {"title", "intent_uri"}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    watchTitle = cursor.getString(0);
                    String uri = cursor.getString(1);
                    if (uri != null) {
                        Intent candidate = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                        candidate.setComponent(null);
                        candidate.setSelector(null);
                        if (Intent.ACTION_VIEW.equals(candidate.getAction())) watchAction = candidate;
                    }
                }
            } catch (Exception ignored) { }

            try {
                MediaSessionManager manager = (MediaSessionManager)
                        activity.getSystemService(Context.MEDIA_SESSION_SERVICE);
                List<MediaController> sessions = manager == null ? null
                        : manager.getActiveSessions(new ComponentName(activity, DetoxMediaAccess.class));
                if (sessions != null && !sessions.isEmpty()) {
                    mediaActive = true;
                    MediaMetadata metadata = sessions.get(0).getMetadata();
                    if (metadata != null) {
                        mediaTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
                        Bitmap source = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                        if (source == null) source = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                        artwork = compactArtwork(source);
                    }
                }
            } catch (SecurityException ignored) { }
            catch (RuntimeException ignored) { }

            final String resolvedWatchTitle = watchTitle;
            final Intent resolvedWatchAction = watchAction;
            final String resolvedMediaTitle = mediaTitle;
            final Bitmap resolvedArtwork = artwork;
            final boolean resolvedMediaActive = mediaActive;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed() || !row.isAttachedToWindow()) {
                    if (resolvedArtwork != null && !resolvedArtwork.isRecycled()) resolvedArtwork.recycle();
                    return;
                }
                if (resolvedWatchTitle != null && resolvedWatchAction != null) {
                    card(activity, row, "Continue watching\n" + resolvedWatchTitle, null, () -> {
                        try { activity.startActivity(resolvedWatchAction); }
                        catch (Exception error) { TvIntegrations.watch(activity); }
                    });
                }
                if (resolvedMediaActive) {
                    String title = resolvedMediaTitle == null ? "Active media" : resolvedMediaTitle;
                    card(activity, row, "Now playing\n" + title, resolvedArtwork,
                            () -> TvIntegrations.media(activity));
                }
                if (row.getChildCount() == 0) {
                    card(activity, row, "Media cards\nOpen media access & available titles", null,
                            () -> TvIntegrations.show(activity, () -> { }));
                }
            });
        }, "detox-home-media");
        lookup.setDaemon(true);
        lookup.start();
    }

    /** Make a private, bounded-size copy so a provider's full-resolution art is not kept in Home. */
    private static Bitmap compactArtwork(Bitmap source) {
        if (source == null || source.isRecycled()) return null;
        try {
            int max = Math.max(source.getWidth(), source.getHeight());
            if (max <= 0) return null;
            float scale = Math.min(1f, 256f / max);
            int width = Math.max(1, Math.round(source.getWidth() * scale));
            int height = Math.max(1, Math.round(source.getHeight() * scale));
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(source, new Rect(0, 0, source.getWidth(), source.getHeight()),
                    new Rect(0, 0, width, height), paint);
            return result;
        } catch (RuntimeException ignored) { return null; }
    }

    private static void card(Activity activity, LinearLayout row, String label, Bitmap artwork,
                             Runnable action) {
        LinearLayout card = new LinearLayout(activity);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(20, 12, 20, 12);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCB122030);
        background.setCornerRadius(18);
        background.setStroke(1, 0x608DA8BD);
        card.setBackground(background);
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(label);
        card.setOnClickListener(view -> action.run());
        card.setOnFocusChangeListener((view, focused) -> {
            background.setColor(focused ? 0xED365572 : 0xCB122030);
            background.setStroke(focused ? 3 : 1, focused ? 0xFFFFFFFF : 0x608DA8BD);
        });
        if (artwork != null && !artwork.isRecycled()) {
            ImageView image = new ImageView(activity);
            image.setImageBitmap(artwork);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(image, new LinearLayout.LayoutParams(64, 64));
        }
        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(16);
        text.setMaxLines(2);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setPadding(12, 0, 0, 0);
        card.addView(text, new LinearLayout.LayoutParams(270, -2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(8, 0, 8, 0);
        row.addView(card, params);
    }
}
