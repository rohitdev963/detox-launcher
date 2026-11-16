package com.ansub.tvhome;

import android.graphics.Canvas;


import android.graphics.LinearGradient;
import android.graphics.Paint;


import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.view.Choreographer;
import android.media.AudioManager;
import android.os.Build;

import java.io.IOException;

/** Full-screen animated wallpaper scenes, rendered behind the translucent Detox home. */
public final class DetoxLiveWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine() { return new SceneEngine(); }

    private final class SceneEngine extends Engine {
        private final Choreographer choreographer = Choreographer.getInstance();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);


        // Shader construction is relatively expensive and the wallpaper renderer runs every
        // display frame. Keep size-dependent shaders and recreate them only after a surface resize.
        private int shaderWidth = -1;
        private int shaderHeight = -1;
        private LinearGradient backgroundGradient;
        private RadialGradient ambientLight;

        private final Choreographer.FrameCallback frame = new Choreographer.FrameCallback() {
            @Override public void doFrame(long frameTimeNanos) {
                if (!visible) return;
                render();
                choreographer.postFrameCallbackDelayed(this, player != null ? 500L : 33L);
            }
        };
        private boolean visible;
        private long started = android.os.SystemClock.uptimeMillis();
        private MediaPlayer player;
        private boolean playerPrepared;
        private String mediaKey;
        private String mediaOptionsKey;
        private String failedMediaKey;
        private long failedUntil;



        SceneEngine() { setTouchEventsEnabled(false); WallpaperCatalogPreferences.repair(getSharedPreferences("config",0)); }

        @Override public void onVisibilityChanged(boolean shown) {
            visible = shown;
            choreographer.removeFrameCallback(frame);
            if (shown) choreographer.postFrameCallback(frame);
            else { stopPlayer();  }
        }

        @Override public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (visible) render();
        }

        @Override public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            choreographer.removeFrameCallback(frame);
            stopPlayer();
            
            super.onSurfaceDestroyed(holder);
        }

        @Override public void onDestroy() {
            visible = false;
            choreographer.removeFrameCallback(frame);
            stopPlayer();
            
            super.onDestroy();
        }

        private void render() {
            String scene = getSharedPreferences("config", 0).getString("liveWallpaperScene", "ambient");
            if ("video".equals(scene)) {
                ensurePlayer(scene);
                if (player != null) return;
                scene = "ambient"; // keep a graceful animated background if a TV cannot decode the video.
            }
            stopPlayer();
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = Build.VERSION.SDK_INT >= 26 ? holder.lockHardwareCanvas() : holder.lockCanvas();
                if (canvas == null) return;
                float w = canvas.getWidth(), h = canvas.getHeight();
                ensureSceneShaders((int) w, (int) h);
                float seconds = (android.os.SystemClock.uptimeMillis() - started) / 1000f;
                paint.setShader(backgroundGradient);
                canvas.drawRect(0, 0, w, h, paint);
                paint.setShader(null);
                float x = w * (.52f + .12f * (float)Math.sin(seconds * .055f));
                canvas.save(); canvas.translate(x, h * .2f);
                paint.setShader(ambientLight); paint.setAlpha(35);
                canvas.drawCircle(0f, 0f, Math.max(w, h) * .5f, paint);
                paint.setShader(null); paint.setAlpha(255); canvas.restore();
            } catch (RuntimeException ignored) {
            } finally {
                if (canvas != null) try { holder.unlockCanvasAndPost(canvas); } catch (RuntimeException ignored) {}
            }
        }

        private void ensureSceneShaders(int width, int height) {
            if (width <= 0 || height <= 0 || (width == shaderWidth && height == shaderHeight)) return;
            shaderWidth = width;
            shaderHeight = height;
            backgroundGradient = new LinearGradient(0, 0, 0, height,
                    new int[] {0xFF07152B, 0xFF101427, 0xFF03060D}, null, Shader.TileMode.CLAMP);
            float radius = Math.max(width, height) * 0.50f;
            ambientLight = new RadialGradient(0, 0, radius,
                    new int[] {0x644DA7FF, 0x354C48D9, 0x00333388}, null, Shader.TileMode.CLAMP);

        }

        private String currentMediaKey(String scene) {
            android.content.SharedPreferences prefs = getSharedPreferences("config", 0);
            String asset = prefs.getString("liveWallpaperVideoAsset", "");
            if (BuiltInLiveVideoCatalog.containsAsset(asset)) return "asset:" + asset;
            String path = prefs.getString("liveWallpaperVideoPath", "");
            java.io.File localFile = path.isEmpty() ? null : new java.io.File(path);
            if (localFile != null && localFile.isFile() && localFile.length() > 0)
                return "file:" + localFile.getAbsolutePath();
            return "uri:" + prefs.getString("liveWallpaperVideoUri", "");
        }

        private void ensurePlayer(String scene) {
            String key = currentMediaKey(scene);
            if (key.equals(failedMediaKey) && android.os.SystemClock.uptimeMillis() < failedUntil) return;
            String options = currentOptionsKey();
            if (key.equals(mediaKey) && player != null) {
                if (playerPrepared && !options.equals(mediaOptionsKey)) {
                    try { applyPlayerOptions(player, options); }
                    catch (RuntimeException error) { failPlayer(key, error); }
                }
                return;
            }
            stopPlayer();
            mediaKey = key;
            mediaOptionsKey = options;
            try {
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setVolume(0f, 0f);
                player.setSurface(getSurfaceHolder().getSurface());
                player.setLooping(true);
                if ("video".equals(scene)) {
                    android.content.SharedPreferences prefs = getSharedPreferences("config", 0);
                    String asset = prefs.getString("liveWallpaperVideoAsset", "");
                    String path = prefs.getString("liveWallpaperVideoPath", "");
                    java.io.File localFile = path.isEmpty() ? null : new java.io.File(path);
                    if (BuiltInLiveVideoCatalog.containsAsset(asset)) {
                        try (android.content.res.AssetFileDescriptor descriptor = getAssets().openFd(asset)) {
                            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                        }
                    } else if (localFile != null && localFile.isFile() && localFile.length() > 0) {
                        player.setDataSource(localFile.getAbsolutePath());
                    } else {
                        String uri = prefs.getString("liveWallpaperVideoUri", "");
                        if (uri.length() == 0) throw new IOException("Video not selected");
                        player.setDataSource(DetoxLiveWallpaperService.this, Uri.parse(uri));
                    }
                }
                player.setOnPreparedListener(mp -> {
                    if (mp != player) return;
                    playerPrepared = true;
                    mediaOptionsKey = null;
                    try {
                        applyPlayerOptions(mp, currentOptionsKey());
                        VideoDisplayPacing.prepared(mp,getSurfaceHolder().getSurface(),true);
                        if (visible) mp.start();
                    } catch (RuntimeException error) { failPlayer(key, error); }
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    if (mp != player) return true;
                    failedMediaKey = key;
                    failedUntil = android.os.SystemClock.uptimeMillis() + 5000L;
                    stopPlayer();
                    return true;
                });
                player.prepareAsync();
            } catch (Exception e) {
                failedMediaKey = key;
                failedUntil = android.os.SystemClock.uptimeMillis() + 5000L;
                stopPlayer();
            }
        }

        private String currentOptionsKey() {
            android.content.SharedPreferences prefs = getSharedPreferences("config", 0);
            return prefs.getInt("liveWallpaperSpeed", 100) + ":" + prefs.getBoolean("liveWallpaperCrop", false)
                    + ":" + prefs.getBoolean("liveWallpaperMuted", true);
        }

        private void failPlayer(String key, RuntimeException error) {
            DetoxDiagnostics.recordFailure("DetoxWallpaper", "Live video", "Could not apply playback settings", error);
            failedMediaKey = key; failedUntil = android.os.SystemClock.uptimeMillis() + 5000L; stopPlayer();
        }

        private void applyPlayerOptions(MediaPlayer mp, String options) {
            android.content.SharedPreferences prefs = getSharedPreferences("config", 0);
            boolean crop = prefs.getBoolean("liveWallpaperCrop", false);
            boolean muted = prefs.getBoolean("liveWallpaperMuted", true);
            int speed = Math.max(25, Math.min(200, prefs.getInt("liveWallpaperSpeed", 100)));
            mp.setVideoScalingMode(crop ? MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    : MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            if (Build.VERSION.SDK_INT >= 23) {
                try { mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed / 100f)); } catch (Exception ignored) {}
            }
            mp.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
            mediaOptionsKey = options;
        }

        private void stopPlayer() {
            playerPrepared = false;
            if (player == null) { mediaKey = null; return; }
            player.setOnPreparedListener(null); player.setOnErrorListener(null);
            try { player.setSurface(null); } catch (Exception ignored) {}
            try { player.reset(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
            mediaKey = null;
            mediaOptionsKey = null;
        }
    }
}
