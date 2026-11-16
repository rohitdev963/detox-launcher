package com.ansub.tvhome;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Small, temporary, PIN-paired HTTP companion for the Detox home screen. */
final class PhoneCompanionServer implements Runnable {
    interface Listener {
        String appsJson();
        boolean onRemoteKey(String key);
        void onPaired();
        void onSessionStopped();
        boolean onLaunch(String packageName);
        boolean onTextReceived(String text);
        void onWallpaperReceived(File image);
        void onVideoReceived(File video);
        default String experienceRequest(String operation,JSONObject body){return "{\"error\":\"Dashboard unavailable\"}";}
    }

    private static final int PORT = 8765;
    private static final int MAX_JSON = 16 * 1024;
    private static final int MAX_PHOTO = 24 * 1024 * 1024;
    private static final long MAX_VIDEO = 512L * 1024L * 1024L;
    private static final int MAX_VIDEO_CHUNK = 2 * 1024 * 1024;
    private static final long IDLE_LIMIT_MS = 10 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Context context;
    private final Listener listener;
    private final String pin;
    private final String address;
    private final File uploadFile;
    private final File uploadVideoFile;
    private volatile ServerSocket server;
    private volatile Socket activeClient;
    private volatile boolean running;
    private volatile String sessionToken;
    private volatile long lastRequestAt;
    private int badPinAttempts;
    private volatile String resumableVideoId;
    private volatile File resumableVideoFile;
    private volatile long resumableVideoSize;
    private volatile String resumableVideoExtension;

    PhoneCompanionServer(Context context, Listener listener) throws IOException {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.address = localWifiAddress(context);
        this.pin = String.format(Locale.US, "%06d", RANDOM.nextInt(1_000_000));
        String stagingPrefix=CompanionSessionPolicy.newUploadPrefix();
        this.uploadFile = new File(context.getCacheDir(), stagingPrefix+"-wallpaper.upload");
        this.uploadVideoFile = new File(context.getCacheDir(), stagingPrefix+"-video.upload");
    }

    String url() { return "http://" + address + ":" + PORT; }
    String pairingUrl() { return url() + "/?pin=" + pin; }
    String pin() { return pin; }
    boolean isRunning() { return running; }
    boolean isPaired() { return sessionToken != null; }

    void start() throws IOException {
        if (running) return;
        ServerSocket socket = new ServerSocket();
        try {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName(address), PORT), 4);
            socket.setSoTimeout(30_000);
        } catch (IOException error) {
            try { socket.close(); } catch (IOException ignored) { }
            throw error;
        }
        server = socket;
        running = true;
        lastRequestAt = android.os.SystemClock.elapsedRealtime();
        if (uploadFile.exists()) uploadFile.delete();
        if (uploadVideoFile.exists()) uploadVideoFile.delete();
        // Only after acquiring this server's port: no other live pairing listener owns these remnants.
        File[] staleUploads=context.getCacheDir().listFiles((dir,name)->CompanionSessionPolicy.stagedUpload(name)
                &&!name.equals(uploadFile.getName())&&!name.equals(uploadVideoFile.getName()));
        if(staleUploads!=null)for(File stale:staleUploads)if(stale.isFile())stale.delete();
        File[] staleVideoParts = context.getCacheDir().listFiles((dir, name) ->
                name.startsWith("phone-video-") && name.endsWith(".part"));
        if (staleVideoParts != null) for (File part : staleVideoParts) part.delete();
        Thread thread = new Thread(this, "DetoxPhoneCompanion");
        thread.setDaemon(true);
        thread.start();
    }

    void stop() {
        running = false;
        sessionToken = null;
        ServerSocket socket = server;
        server = null;
        if (socket != null) try { socket.close(); } catch (IOException ignored) {}
        Socket client = activeClient;
        activeClient = null;
        if (client != null) try { client.close(); } catch (IOException ignored) {}
        File partialVideo = resumableVideoFile;
        resumableVideoId = null;
        resumableVideoFile = null;
        resumableVideoSize = 0L;
        resumableVideoExtension = null;
        if (partialVideo != null) partialVideo.delete();
        uploadFile.delete();
        uploadVideoFile.delete();
    }

    @Override public void run() {
        try {
        while (running) {
            ServerSocket active = server;
            if (active == null) break;
            Socket accepted;
            try {
                accepted = active.accept();
            } catch (SocketTimeoutException timeout) {
                long idleLimit = resumableVideoFile != null && resumableVideoFile.exists()
                        ? 30L * 60L * 1000L : IDLE_LIMIT_MS;
                if (android.os.SystemClock.elapsedRealtime() - lastRequestAt >= idleLimit) stop();
                continue;
            } catch (IOException serverError) {
                if (running) {
                    DetoxDiagnostics.recordFailure("DetoxCompanion", "Phone companion", "listener stopped unexpectedly", serverError);
                    stop();
                }
                break;
            }
            try (Socket client = accepted) {
                activeClient = client;
                if (!running) continue;
                client.setSoTimeout(60_000);
                client.setTcpNoDelay(true);
                try {
                    serve(client);
                } catch (IOException clientError) {
                    // A phone browser closing one request must not tear down the whole companion.
                    Log.d("DetoxCompanion", "Phone companion request ended early: " + clientError.getClass().getSimpleName());
                }
            } catch (IOException clientCloseError) {
                // Isolate individual client disconnects; keep accepting the next request.
                Log.d("DetoxCompanion", "Phone companion client disconnected");
            } catch (RuntimeException malformedRequest) {
                // Malformed local requests must not bring down the launcher or server loop.
                Log.w("DetoxCompanion", "Rejected malformed phone companion request", malformedRequest);
            } finally {
                if (activeClient == accepted) activeClient = null;
            }
        }
        } finally {
            stop();
            listener.onSessionStopped();
        }
    }
    private void serve(Socket client) throws IOException {
        BufferedInputStream input = new BufferedInputStream(client.getInputStream());
        BufferedOutputStream output = new BufferedOutputStream(client.getOutputStream());
        String requestLine = readLine(input, 4096);
        if (requestLine == null) return;
        String[] request = requestLine.split(" ", 3);
        if (request.length != 3 || !CompanionRequestPolicy.validRequestLine(request[0], request[1], request[2])) {
            respond(output, 400, "Bad request", "text/plain", "Bad request"); return;
        }
        String method = request[0];
        String path = request[1].split("\\?", 2)[0];
        Map<String, String> headers = new HashMap<>();
        int headerBytes = 0;
        String line;
        boolean headersComplete = false;
        while ((line = readLine(input, 8192)) != null) {
            if (line.isEmpty()) { headersComplete = true; break; }
            headerBytes += line.length();
            if (headerBytes > 24 * 1024) { respond(output, 431, "Headers too large", "text/plain", "Headers too large"); return; }
            int colon = line.indexOf(':');
            if (colon <= 0) { respond(output, 400, "Bad header", "text/plain", "Malformed header"); return; }
            String name = line.substring(0, colon).toLowerCase(Locale.US);
            if (!CompanionRequestPolicy.validHeaderName(name) || headers.containsKey(name)
                    || !CompanionRequestPolicy.validHeaderValue(line.substring(colon + 1))) {
                respond(output, 400, "Bad header", "text/plain", "Duplicate or invalid header"); return;
            }
            headers.put(name, line.substring(colon + 1).trim());
        }
        if (!headersComplete) { respond(output, 400, "Bad request", "text/plain", "Incomplete headers"); return; }
        if (!validRequestAuthority(headers)) {
            respond(output, 421, "Misdirected request", "text/plain", "This companion address is not valid for this request.");
            return;
        }
        if (!CompanionRequestPolicy.validBodyFraming(method, headers.containsKey("content-length"),
                headers.containsKey("transfer-encoding"))) {
            respond(output, 400, "Invalid body framing", "text/plain", "Unsupported or missing request length"); return;
        }
        int length = parseLength(headers.get("content-length"));
        if (length < 0) { respond(output, 400, "Invalid content length", "text/plain", "Invalid request"); return; }

        if ("GET".equals(method) && "/".equals(path)) {
            byte[] page = readAsset();
            respondBytes(output, 200, "OK", "text/html; charset=utf-8", page);
            return;
        }
        if ("POST".equals(method) && "/pair".equals(path)) {
            if (length > 256) { respond(output, 413, "Too large", "application/json", "{\"error\":\"request too large\"}"); return; }
            JSONObject body = readJson(input, length);
            String supplied = body.optString("pin", "");
            if (badPinAttempts >= 5) { respond(output, 429, "Locked", "application/json", "{\"error\":\"too many attempts; close and reopen the TV panel\"}"); return; }
            if (!constantTimeEquals(pin, supplied) || sessionToken != null) {
                badPinAttempts++;
                respond(output, 401, "Unauthorized", "application/json", "{\"error\":\"pairing code is incorrect or already used\"}");
                return;
            }
            byte[] tokenBytes = new byte[24];
            RANDOM.nextBytes(tokenBytes);
            sessionToken = Base64.encodeToString(tokenBytes, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
            lastRequestAt = android.os.SystemClock.elapsedRealtime();
            JSONObject paired = new JSONObject();
            try { paired.put("token", sessionToken); } catch (Exception ignored) {}
            listener.onPaired();
            respond(output, 200, "OK", "application/json", paired.toString());
            return;
        }
        if (!authorized(headers)) { respond(output, 401, "Unauthorized", "application/json", "{\"error\":\"pair with the code shown on the TV\"}"); return; }
        lastRequestAt = android.os.SystemClock.elapsedRealtime();

        if ("GET".equals(method) && "/api/apps".equals(path)) {
            respond(output, 200, "OK", "application/json; charset=utf-8", listener.appsJson());
        } else if (("GET".equals(method)&&"/api/experience".equals(path))
                || ("POST".equals(method)&&("/api/assistant".equals(path)||"/api/experience".equals(path)))) {
            if(length>MAX_JSON){respond(output,413,"Too large","application/json","{\"error\":\"Request too large\"}");return;}
            JSONObject body="POST".equals(method)?readJson(input,length):new JSONObject();
            String operation="GET".equals(method)?"state":"/api/assistant".equals(path)?"ask":"action";
            String reply=listener.experienceRequest(operation,body);
            boolean failed=reply.contains("\"error\":");
            respond(output,failed?409:200,failed?"Unavailable":"OK","application/json; charset=utf-8",reply);
        } else if ("POST".equals(method) && "/api/key".equals(path)) {
            if (length > MAX_JSON) { respond(output, 413, "Too large", "application/json", "{\"error\":\"request too large\"}"); return; }
            String key = readJson(input, length).optString("key", "");
            if (!("up".equals(key) || "down".equals(key) || "left".equals(key) || "right".equals(key)
                    || "ok".equals(key) || "back".equals(key) || "volume-up".equals(key)
                    || "volume-down".equals(key) || "home".equals(key) || "settings".equals(key)
                    || "play-pause".equals(key) || "mute".equals(key) || "menu".equals(key)
                    || "launcher-settings".equals(key))) {
                respond(output, 400, "Bad key", "application/json", "{\"error\":\"unsupported key\"}"); return;
            }
            if (!listener.onRemoteKey(key)) {
                respond(output, 409, "Unavailable", "application/json", "{\"error\":\"TV launcher is not ready to receive remote input\"}");
                return;
            }
            respond(output, 200, "OK", "application/json", "{\"ok\":true}");
        } else if ("POST".equals(method) && "/api/launch".equals(path)) {
            if (length > MAX_JSON) { respond(output, 413, "Too large", "application/json", "{\"error\":\"request too large\"}"); return; }
            String pkg = readJson(input, length).optString("package", "");
            if (pkg.length() > 180 || !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
                respond(output, 400, "Bad app", "application/json", "{\"error\":\"invalid app\"}"); return;
            }
            if (!listener.onLaunch(pkg)) {
                respond(output, 404, "App unavailable", "application/json", "{\"error\":\"That app is no longer available on the TV. Refresh the app list.\"}"); return;
            }
            respond(output, 200, "OK", "application/json", "{\"ok\":true}");
        } else if ("POST".equals(method) && "/api/text".equals(path)) {
            if (length > MAX_JSON) { respond(output, 413, "Too large", "application/json", "{\"error\":\"text is too long\"}"); return; }
            String text = readJson(input, length).optString("text", "");
            if (text.trim().isEmpty() || text.length() > 2000) {
                respond(output, 400, "Invalid text", "application/json", "{\"error\":\"Enter up to 2,000 characters of text.\"}"); return;
            }
            if (!listener.onTextReceived(text)) {
                respond(output, 409, "Unavailable", "application/json", "{\"error\":\"TV clipboard is unavailable.\"}"); return;
            }
            respond(output, 200, "OK", "application/json", "{\"ok\":true,\"message\":\"Text copied to the TV clipboard.\"}");
        } else if ("GET".equals(method) && "/api/ping".equals(path)) {
            respond(output, 200, "OK", "application/json", "{\"ok\":true,\"scope\":\"detox\",\"launcherReady\":"+CompanionInput.ready()+"}");
        } else if ("POST".equals(method) && "/api/video/start".equals(path)) {
            if (length > MAX_JSON) { respond(output, 413, "Too large", "application/json", "{\"error\":\"request too large\"}"); return; }
            JSONObject body = readJson(input, length);
            long size = body.optLong("size", 0L);
            String filename = body.optString("name", "video.mp4");
            String extension = videoExtension(filename, body.optString("type", "video/mp4"));
            long available = new android.os.StatFs(context.getCacheDir().getAbsolutePath()).getAvailableBytes();
            if (extension == null || size <= 0 || size > MAX_VIDEO || 2L * size + 128L * 1024L * 1024L > available) {
                respond(output, 413, "Video too large or unsupported", "application/json", "{\"error\":\"Choose a supported video up to 512 MB and leave enough free TV storage for import.\"}"); return;
            }
            if (resumableVideoFile != null && resumableVideoFile.exists()) resumableVideoFile.delete();
            byte[] idBytes = new byte[18]; RANDOM.nextBytes(idBytes);
            resumableVideoId = Base64.encodeToString(idBytes, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
            resumableVideoFile = new File(context.getCacheDir(), "phone-video-" + resumableVideoId + ".part");
            resumableVideoSize = size;
            resumableVideoExtension = extension;
            try {
                if (!resumableVideoFile.createNewFile()) throw new IOException("could not create temporary upload");
                JSONObject response = new JSONObject();
                response.put("id", resumableVideoId);
                response.put("offset", 0);
                respond(output, 200, "OK", "application/json", response.toString());
            } catch (Exception error) {
                if (resumableVideoFile.exists()) resumableVideoFile.delete();
                resumableVideoId = null; resumableVideoFile = null; resumableVideoSize = 0; resumableVideoExtension = null;
                respond(output, 500, "Server error", "application/json", "{\"error\":\"Could not start video transfer.\"}");
            }
        } else if ("GET".equals(method) && "/api/video/status".equals(path)) {
            if (!matchesVideoUpload(headers.get("x-upload-id")) || !resumableVideoFile.isFile()) {
                respond(output, 404, "Upload not found", "application/json", "{\"error\":\"Video transfer expired. Start it again.\"}"); return;
            }
            try {
                JSONObject response = new JSONObject();
                response.put("id", resumableVideoId);
                response.put("offset", resumableVideoFile.length());
                response.put("size", resumableVideoSize);
                respond(output, 200, "OK", "application/json", response.toString());
            } catch (Exception error) { respond(output, 500, "Server error", "application/json", "{\"error\":\"Could not read transfer state.\"}"); }
        } else if ("POST".equals(method) && "/api/video/chunk".equals(path)) {
            if (!matchesVideoUpload(headers.get("x-upload-id")) || !resumableVideoFile.isFile()) {
                respond(output, 404, "Upload not found", "application/json", "{\"error\":\"Video transfer expired. Start it again.\"}"); return;
            }
            long offset = parseLong(headers.get("x-upload-offset"), -1L);
            long current = resumableVideoFile.length();
            if (offset != current) {
                respond(output, 409, "Offset mismatch", "application/json", "{\"error\":\"Transfer position changed. Resume from the reported position.\",\"offset\":" + current + "}"); return;
            }
            if (length <= 0 || length > MAX_VIDEO_CHUNK || current + length > resumableVideoSize) {
                respond(output, 413, "Invalid video chunk", "application/json", "{\"error\":\"Invalid video chunk size.\"}"); return;
            }
            try (FileOutputStream file = new FileOutputStream(resumableVideoFile, true)) {
                byte[] buffer = new byte[32 * 1024];
                int remaining = length;
                while (remaining > 0) {
                    int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (count < 0) throw new IOException("incomplete video chunk");
                    file.write(buffer, 0, count);
                    remaining -= count;
                }
                file.getFD().sync();
            } catch (IOException error) {
                long actual = resumableVideoFile.exists() ? resumableVideoFile.length() : 0L;
                respond(output, 400, "Incomplete chunk", "application/json", "{\"error\":\"Chunk was interrupted. Resume from the reported position.\",\"offset\":" + actual + "}"); return;
            }
            respond(output, 200, "OK", "application/json", "{\"ok\":true,\"offset\":" + resumableVideoFile.length() + "}");
        } else if ("POST".equals(method) && "/api/video/finish".equals(path)) {
            if (!matchesVideoUpload(headers.get("x-upload-id")) || !resumableVideoFile.isFile()) {
                respond(output, 404, "Upload not found", "application/json", "{\"error\":\"Video transfer expired. Start it again.\"}"); return;
            }
            if (resumableVideoFile.length() != resumableVideoSize) {
                respond(output, 409, "Video incomplete", "application/json", "{\"error\":\"Video is incomplete. Resume uploading before applying it.\"}"); return;
            }
            File received = new File(context.getCacheDir(), "phone-video-" + System.currentTimeMillis() + "." + resumableVideoExtension);
            if (!resumableVideoFile.renameTo(received)) {
                respond(output, 500, "Storage error", "application/json", "{\"error\":\"Could not finalize the uploaded video.\"}"); return;
            }
            resumableVideoId = null; resumableVideoFile = null; resumableVideoSize = 0; resumableVideoExtension = null;
            respond(output, 202, "Accepted", "application/json", "{\"ok\":true,\"message\":\"Video received; crop and apply it on the TV.\"}");
            listener.onVideoReceived(received);
        } else if ("POST".equals(method) && "/api/stop".equals(path)) {
            respond(output, 200, "OK", "application/json", "{\"ok\":true,\"message\":\"session stopped\"}");
            stop();
        } else if ("POST".equals(method) && "/api/wallpaper".equals(path)) {
            String mime = headers.get("content-type");
            if (!("image/jpeg".equalsIgnoreCase(mime) || "image/png".equalsIgnoreCase(mime)
                    || "image/webp".equalsIgnoreCase(mime)) || length <= 0 || length > MAX_PHOTO) {
                respond(output, 415, "Unsupported image", "application/json", "{\"error\":\"send a JPEG, PNG, or WebP image up to 24 MB\"}"); return;
            }
            if (uploadFile.exists()) uploadFile.delete();
            try (FileOutputStream file = new FileOutputStream(uploadFile)) {
                byte[] buffer = new byte[16 * 1024];
                int remaining = length;
                while (remaining > 0) {
                    int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (count < 0) throw new IOException("incomplete upload");
                    file.write(buffer, 0, count);
                    remaining -= count;
                }
                file.flush();
            } catch (IOException error) {
                uploadFile.delete();
                respond(output, 400, "Incomplete upload", "application/json", "{\"error\":\"upload did not finish\"}");
                return;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(uploadFile.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || (long) bounds.outWidth * bounds.outHeight > 100_000_000L) {
                uploadFile.delete();
                respond(output, 415, "Invalid image", "application/json", "{\"error\":\"file is not a supported photo\"}");
                return;
            }
            File received = new File(context.getCacheDir(), "phone-wallpaper-" + System.nanoTime() + ".image");
            if (!uploadFile.renameTo(received)) {
                uploadFile.delete();
                respond(output, 500, "Storage error", "application/json", "{\"error\":\"Could not save uploaded photo on the TV.\"}");
                return;
            }
            respond(output, 202, "Accepted", "application/json", "{\"ok\":true,\"message\":\"Photo received; finish cropping on the TV.\"}");
            listener.onWallpaperReceived(received);
        } else if ("POST".equals(method) && "/api/video".equals(path)) {
            String mime = headers.get("content-type");
            String extension = videoExtension(headers.get("x-file-name"), mime);
            long available = new android.os.StatFs(context.getCacheDir().getAbsolutePath()).getAvailableBytes();
            if (extension == null || length <= 0 || length > MAX_VIDEO || 2L * length + 128L * 1024L * 1024L > available) {
                respond(output, 413, "Video too large or unsupported", "application/json", "{\"error\":\"Send a supported video up to 512 MB with enough free TV storage for the upload and imported copy.\"}"); return;
            }
            if (uploadVideoFile.exists()) uploadVideoFile.delete();
            try (FileOutputStream file = new FileOutputStream(uploadVideoFile)) {
                byte[] buffer = new byte[64 * 1024];
                int remaining = length;
                while (remaining > 0) {
                    int count = input.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (count < 0) throw new IOException("incomplete upload");
                    file.write(buffer, 0, count);
                    remaining -= count;
                }
                file.getFD().sync();
            } catch (IOException error) {
                uploadVideoFile.delete();
                respond(output, 400, "Incomplete upload", "application/json", "{\"error\":\"Video upload did not finish. Try again.\"}"); return;
            }
            File received = new File(context.getCacheDir(), "phone-video-" + System.nanoTime() + "." + extension);
            if (!uploadVideoFile.renameTo(received)) {
                uploadVideoFile.delete();
                respond(output, 500, "Storage error", "application/json", "{\"error\":\"Could not save uploaded video on the TV.\"}"); return;
            }
            respond(output, 202, "Accepted", "application/json", "{\"ok\":true,\"message\":\"Video received; crop and apply it on the TV.\"}");
            listener.onVideoReceived(received);
        } else {
            respond(output, 404, "Not found", "application/json", "{\"error\":\"not found\"}");
        }
    }

    private boolean authorized(Map<String, String> headers) {
        String auth = headers.get("authorization");
        String token = sessionToken;
        return token != null && auth != null && auth.startsWith("Bearer ")
                && constantTimeEquals(token, auth.substring(7));
    }

    /** Rejects DNS-rebinding and cross-origin browser requests while allowing non-browser clients. */
    private boolean validRequestAuthority(Map<String, String> headers) {
        String expectedHost = address + ":" + PORT;
        String host = headers.get("host");
        if (host == null || !expectedHost.equalsIgnoreCase(host)) return false;
        String origin = headers.get("origin");
        return origin == null || origin.isEmpty()
                || ("http://" + expectedHost).equalsIgnoreCase(origin);
    }

    private boolean matchesVideoUpload(String id) {
        return resumableVideoId != null && id != null && constantTimeEquals(resumableVideoId, id);
    }

    private static long parseLong(String value, long fallback) {
        try { return value == null ? fallback : Long.parseLong(value); }
        catch (NumberFormatException error) { return fallback; }
    }

    private static String videoExtension(String filename, String mime) {
        String ext = null;
        if (filename != null) {
            String safe = filename.replace('\\', '/');
            int slash = safe.lastIndexOf('/');
            if (slash >= 0) safe = safe.substring(slash + 1);
            int dot = safe.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < safe.length()) ext = safe.substring(dot + 1).toLowerCase(Locale.US);
        }
        if (ext == null || ext.isEmpty()) {
            if (mime != null && mime.toLowerCase(Locale.US).startsWith("video/")) {
                String subtype = mime.substring(mime.indexOf('/') + 1).toLowerCase(Locale.US).split(";", 2)[0].trim();
                if ("quicktime".equals(subtype)) ext = "mov";
                else if ("x-matroska".equals(subtype)) ext = "mkv";
                else if ("3gpp".equals(subtype)) ext = "3gp";
                else if ("mp4".equals(subtype) || "webm".equals(subtype) || "mpeg".equals(subtype)) ext = subtype;
            }
        }
        return java.util.Arrays.asList("mp4", "m4v", "mov", "mkv", "webm", "avi", "3gp", "ts", "mpg", "mpeg").contains(ext) ? ext : null;
    }
    private byte[] readAsset() throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(context.getAssets().open("phone_companion.html"));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toByteArray();
        }
    }

    private static JSONObject readJson(BufferedInputStream input, int length) throws IOException {
        if (length == 0) return new JSONObject();
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(body, offset, length - offset);
            if (count < 0) throw new IOException("incomplete body");
            offset += count;
        }
        try { return new JSONObject(new String(body, StandardCharsets.UTF_8)); }
        catch (Exception error) { return new JSONObject(); }
    }

    private static int parseLength(String value) {
        if (value == null) return 0;
        try { int n = Integer.parseInt(value); return n < 0 ? -1 : n; }
        catch (NumberFormatException error) { return -1; }
    }

    private static String readLine(BufferedInputStream input, int limit) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = input.read()) != -1) {
            if (value == '\n') break;
            if (value != '\r') line.write(value);
            if (line.size() > limit) throw new IOException("line too long");
        }
        if (value == -1 && line.size() == 0) return null;
        return new String(line.toByteArray(), StandardCharsets.ISO_8859_1);
    }

    private static void respond(BufferedOutputStream out, int status, String reason, String type, String body) throws IOException {
        respondBytes(out, status, reason, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respondBytes(BufferedOutputStream out, int status, String reason, String type, byte[] body) throws IOException {
        String header = "HTTP/1.1 " + status + " " + reason + "\r\nContent-Type: " + type
                + "\r\nContent-Length: " + body.length + "\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String localWifiAddress(Context context) throws IOException {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network active = connectivity == null ? null : connectivity.getActiveNetwork();
                String activeAddress = firstLocalAddress(active == null ? null : connectivity.getLinkProperties(active));
                if (activeAddress != null) return activeAddress;
            } catch (Exception ignored) {}
        }
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                int ip = wifi.getConnectionInfo().getIpAddress();
                if (ip != 0) return String.format(Locale.US, "%d.%d.%d.%d", ip & 255, (ip >> 8) & 255,
                        (ip >> 16) & 255, (ip >> 24) & 255);
            }
        } catch (Exception ignored) {}
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface nic : Collections.list(interfaces)) {
            if (!nic.isUp() || nic.isLoopback()) continue;
            for (InetAddress candidate : Collections.list(nic.getInetAddresses()))
                if (candidate instanceof Inet4Address && candidate.isSiteLocalAddress() && !candidate.isLoopbackAddress())
                    return candidate.getHostAddress();
        }
        throw new IOException("Connect the TV to your home network, then try again.");
    }

    private static String firstLocalAddress(LinkProperties properties) {
        if (properties == null) return null;
        for (LinkAddress link : properties.getLinkAddresses()) {
            InetAddress address = link.getAddress();
            if (address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress())
                return address.getHostAddress();
        }
        return null;
    }
}
