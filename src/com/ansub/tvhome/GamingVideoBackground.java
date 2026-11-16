package com.ansub.tvhome;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.HashSet;
import java.util.Set;

/** Muted video plane composited independently of the animated Gaming controls. */
final class GamingVideoBackground extends SurfaceView implements SurfaceHolder.Callback {
    interface Listener {
        default void onReady() { }
        void onFallback(String failedSource, String replacementSource);
        void onUnavailable(String lastFailedSource);
    }

    private String source;
    private final Listener listener;
    private MediaPlayer player;
    private boolean active, prepared, failed;
    private final VideoStartGuard startup = new VideoStartGuard();
    private Runnable startTimeout;
    private boolean fill=true;private float zoom=1f;
    private final Set<String> rejectedSources = new HashSet<>();

    GamingVideoBackground(Context context, String source, Listener listener) {
        super(context);
        this.source = source;
        this.listener = listener;
        getHolder().addCallback(this);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
    }

    void active(boolean value) {
        active = value;
        if (!active) cancelStartTimeout();
        if (player == null && getHolder().getSurface().isValid() && !failed && active) open();
        if (player != null && prepared) try {
            if (active) { player.start(); armStartTimeout(player, startup.token()); }
            else player.pause();
        } catch (IllegalStateException error) { fail(error); }
        if (player != null && active) armStartTimeout(player, startup.token());
    }
    void framing(boolean fill,float zoom){this.fill=fill;this.zoom=Math.max(1,Math.min(2.5f,zoom));layoutVideo();}
    private void layoutVideo(){
        if(player==null||!prepared)return;
        try{player.setVideoScalingMode(fill?MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING:MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);}catch(RuntimeException ignored){}
        setScaleX(zoom);setScaleY(zoom);
    }

    private void open() {
        Surface surface = getHolder().getSurface();
        if (player != null || !surface.isValid() || failed) return;
        // Leave refresh selection to Android TV. The compositor presents decoded frames at the
        // display's current cadence instead of asking it to switch to a fixed 60 Hz mode.
        MediaPlayer next = new MediaPlayer();
        player = next;
        final long token = startup.begin();
        try {
        next.setDisplay(getHolder());
        next.setVolume(0f, 0f);
        next.setLooping(true);
        next.setOnPreparedListener(p -> {
            if (p != player) return;
            try {
                prepared = true;
                layoutVideo();
                VideoDisplayPacing.prepared(p,getHolder().getSurface(),true);
                android.util.Log.i("DetoxGaming", "Video prepared: " + source + " " + p.getVideoWidth() + "x" + p.getVideoHeight());
                if (active) p.start();
            } catch (RuntimeException error) { fail(error); }
        });
        next.setOnErrorListener((p, what, extra) -> {
            if (p == player) fail(new IllegalStateException("Decoder error " + what + "/" + extra));
            return true;
        });
        next.setOnInfoListener((p,what,extra)->{
            if(p==player&&what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START&&startup.ready(token)){
                cancelStartTimeout();
                if(listener!=null)listener.onReady();
            }
            return false;
        });
            if (source.startsWith("asset:")) {
                try (AssetFileDescriptor file = getContext().getAssets().openFd(source.substring(6))) {
                    next.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                }
            } else next.setDataSource(getContext(), Uri.parse(source));
            next.prepareAsync();
            armStartTimeout(next, token);
        } catch (Exception error) { fail(error); }
    }

    private void cancelStartTimeout() {
        if (startTimeout != null) removeCallbacks(startTimeout);
        startTimeout = null;
    }

    private void armStartTimeout(MediaPlayer owner, long token) {
        if (startTimeout != null || !active || !startup.pending(token)) return;
        startTimeout = new Runnable() { @Override public void run() {
            // A cancelled/queued callback from an old surface must not cancel a new deadline.
            if (startTimeout != this || owner != player || token != startup.token()) return;
            startTimeout = null;
            if (active && startup.timeout(token))
                fail(new IllegalStateException("No video frame rendered within 12 seconds"));
        }};
        postDelayed(startTimeout, 12000L);
    }

    private void fail(Exception error) {
        String failedSource = source;
        rejectedSources.add(failedSource);
        failed = true;
        DetoxDiagnostics.recordFailure("DetoxGaming", "Video background", "playback failed: " + failedSource, error);
        close();
        // Try each bundled clip at most once; never loop back to a failed default.
        for (int i = 0; i < GamingThemeCatalog.NAMES.length; i++) {
            String candidate = GamingThemeCatalog.source(i);
            if (!candidate.isEmpty() && !rejectedSources.contains(candidate)) {
                source = candidate;
                failed = false;
                if (listener != null) listener.onFallback(failedSource, candidate);
                post(() -> { if (active && !failed) open(); });
                return;
            }
        }
        if (listener != null) listener.onUnavailable(source);
    }

    void close() {
        cancelStartTimeout();
        startup.cancel();
        prepared = false;
        MediaPlayer old = player; player = null;
        if (old != null) { old.setOnPreparedListener(null); old.setOnErrorListener(null); old.setOnInfoListener(null); old.release(); }
    }
    @Override protected void onDetachedFromWindow() { active = false; close(); super.onDetachedFromWindow(); }
    @Override public void surfaceCreated(SurfaceHolder holder) { if (active) open(); }
    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        layoutVideo();
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) { close(); }
}
