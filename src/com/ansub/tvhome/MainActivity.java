package com.ansub.tvhome;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Iterator;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    // Top-row apps, in this order of preference:
    //  1. a list saved at install time:  am start -n com.ansub.tvhome/.MainActivity --es apps "pkg1,pkg2"
    //     (--es apps auto  goes back to 2; add --es stamp <unique> so it's applied only once, see saveConfig)
    //  2. apps.txt (built in as Apps.LIST), topped up with other TV apps installed on this TV.
    // Missing apps are skipped. Topping up stops at six apps, but a chosen list can be any length:
    // apps wrap into rows of six, like Apple TV, and the page scrolls down once they don't all fit.
    // On the TV, hold OK on an app (or press Menu) to move or remove it, or add another; that saves list 1.
    // A network tile always sits at the top right, level with the clock. Settings shortcuts can join
    // it there, off until picked the same way:
    //   --es quick "bluetooth,sound,picture"
    // (--es quick none  takes them away again; "wifi" and "ethernet" are the network tile now).
    private static final String PREFS = "config";
    private static final List<String> NOT_APPS = Arrays.asList("com.android.vending", "com.android.tv.settings",
            "com.android.tv", "com.google.android.tvlauncher", "com.google.android.apps.tv.launcherx",
            "com.google.android.leanbacklauncher");
    private static final String PLAY_STORE = "com.android.vending";
    private static final String SETTINGS = "settings";
    private static final String TV_SETTINGS = "tv-settings";
    private static final String SCREEN_SLEEP = "screen-sleep";
    private static final String TV_RESTART = "tv-restart";
    static final String ACTION_OPEN_DETOX_SETTINGS = "com.ansub.tvhome.OPEN_DETOX_SETTINGS";
    private static final String ADD = "add";
    private static final String FOLDER_PREFIX = "folder:";
    private static final int PER_ROW = 6, MAX_INPUTS = 4;
    // A compact All apps control gives more horizontal width back to the app tiles.

    // Settings shortcuts: name, label, icon. Tile keys are QUICK + name.
    private static final String QUICK = "quick:";
    private static final String[] QUICK_NAMES = {"bluetooth", "sound", "picture"};
    private static final String[] QUICK_LABELS = {"Bluetooth", "Sound", "Picture"};
    private static final int[][] QUICK_ICONS = {LineIcon.BLUETOOTH, LineIcon.SOUND, LineIcon.PICTURE};
    private static final String NETWORK = "network";
    private static final String QUICK_PANEL = "quick-panel";
    private static final String HOME_DOWN_LAUNCH_PACKAGE = "homeDownLaunchPackage";
    private static final int OFFLINE = 0, ETHERNET = 4; // network states; 1 to 3 are Wi-Fi bars
    // TCL keeps its own Picture and Sound panels in a service; Android's Sound page is missing there.
    private static final String TCL_SHOW_WINDOW = "com.tcl.settings.SHOW_WINDOW";

    // Layout is designed at 1920x1080 and scaled to the TV's real UI size by px().
    private static final int MARGIN = 96, GAP = 32, RADIUS = 28;
    private static final float MENU_SCALE = 0.90f;
    private static final int ROW1_TOP = 604, ROW_STEP = 200;
    private static final int GRID_MIN_TOP = 400; // more rows push the grid up, but not into the clock
    private static final int ICON_TOP = 12, ICON_BUTTON = 72, ICON_SIZE = 40, ICON_GAP = 12; // 80% top action icons
    private static final int RAIL_BUTTON = 80, RAIL_ICON_SIZE = 56, RAIL_STEP = 108;
    private static final int UTILITY_BUTTON = 56; // Shared footprint for rail and clock utilities
    private static final int UTILITY_APP_GAP = 16; // Tighter gutter between the utility rail and app grid

    private static final String[] SETTINGS_ITEMS = SettingsMenuRoutes.categoryNames();
    private static final String[] QUICK_CONTROL_KEYS = {"network", "bluetooth", "picture", "sound", "launcher", "tv"};
    private static final String[] QUICK_CONTROL_LABELS = {"Wi-Fi & network", "Bluetooth", "Picture", "Sound", "Launcher settings", "TV settings"};
    private static final String[] MOTION_NAMES = WallpaperEffects.NAMES;
    private static final String[] MOTION_KEYS = WallpaperEffects.KEYS;
    private static final String[] THEME_NAMES = {"Ocean glow", "Sunset ember", "Forest mint", "Lavender sky", "Monochrome", "Wallpaper match", "Multicolor"};
    private static final String[] FONT_NAMES = {"Nunito", "Orbitron", "Playfair Display", "Pacifico", "Poppins"};
    private static final String[] FONT_KEYS = {"nunito", "orbitron", "playfair", "pacifico", "poppins"};
    private static final String[] COLOR_NAMES = {"White", "Warm gold", "Sky blue", "Coral", "Mint", "Lavender", "Rose", "Ice blue", "Amber", "Lime", "Cyan", "Violet", "Soft peach", "Silver", "Black", "Charcoal", "Dark brown", "Navy", "Forest green"};
    private static final int[] FONT_COLORS = {0xFFFFFFFF, 0xFFFFD28A, 0xFF9ADCF2, 0xFFFFA58D, 0xFFAAE4C1,
            0xFFD0B5FF, 0xFFFF9DBB, 0xFFB9E6FF, 0xFFFFB35C, 0xFFC8F27A, 0xFF71E6E2, 0xFF9B8CFF, 0xFFFFC6A5, 0xFFD7DEE8,
            0xFF000000, 0xFF30343B, 0xFF4A3025, 0xFF23354D, 0xFF214433};
    private static final int RAINBOW_COLOR = 0x00000001;
    private static final int WALLPAPER_MATCH_COLOR = 0x00000002;
    private static final int[] RAINBOW_COLORS = {0xFFFF4D6D, 0xFFFFB84D, 0xFFFFF052, 0xFF58D68D,
            0xFF40C4FF, 0xFF7B61FF, 0xFFE056FD, 0xFFFF4D6D};
    private static final int[] THEME_COLORS = {0xFF63D7FF, 0xFFFF9A72, 0xFF89E0AA, 0xFFBEA4FF, 0xFFE0E5EB, 0xFF63D7FF, 0xFF63D7FF};
    private static final int WALLPAPER_THEME_INDEX = 5, MULTICOLOR_THEME_INDEX = 6;
    private static final int[] MULTICOLOR_SWATCH = {0xFF20C8F2, 0xFFFFD23F, 0xFF40E08B, 0xFFC49BFF, 0xFFFF8B72};
    private static final String[] CLOCK_NAMES=ClockFacePolicy.NAMES;
    private static final String[] CLOCK_KEYS=ClockFacePolicy.KEYS;
    private static final String[] CLOCK_POSITION_NAMES = {"Top left", "Top center", "Top right", "Middle left", "Middle right", "Bottom left", "Bottom center", "Bottom right"};
    private static final String[] CLOCK_POSITION_KEYS = {"top-left", "top-center", "top-right", "middle-left", "middle-right", "bottom-left", "bottom-center", "bottom-right"};
    private static final int PICK_WALLPAPER = 7301;
    private static final int PICK_SLIDESHOW_PHOTOS = 7304;
    private static final int PICK_VIDEO_WALLPAPER = 7305;
    private static final int PICK_APP_ICON = 7306;
    private static final int REQUEST_VOICE_SEARCH = 7307;
    private static final int CREATE_BACKUP = 7302, RESTORE_BACKUP = 7303;
    private static final int IMPORT_HOME_PROFILES = 7308, EXPORT_HOME_PROFILES = 7309;
    private Bitmap pendingWallpaper;
    private Bitmap pendingAppIconBitmap;
    private String pendingCustomIconPackage;
    private volatile int wallpaperPhotoImportGeneration;
    private volatile int appIconImportGeneration;
    private String pendingShortcutPackage;
    private String pendingShortcutKind;
    private String pendingShortcutGesture = "tap";
    private int pendingShortcutSlot;
    private int activeRemoteShortcutKey = KeyEvent.KEYCODE_UNKNOWN;
    private int activeRemoteShortcutSlot = -1;
    private final Handler shortcutHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRemoteShortcutHold;private RemoteShortcutState shortcutState;private Dialog shortcutOwner;
    private boolean capturingRemoteShortcut;
    private int consumedShortcutCaptureKey = KeyEvent.KEYCODE_UNKNOWN;
    private Dialog remoteShortcutCaptureDialog;
    private LauncherAppArtworkResolver appArtworkResolver;
    private Dialog cropDialog;
    private Dialog appIconCropDialog;
    // Wallpapers are saved by name; stable asset paths survive picker reordering.
    private static final String[] WALLPAPER_NAMES = BundledWallpaperCatalog.NAMES;
    private static final int PHOTO = WALLPAPER_NAMES.length - 1;
    private static final int[][] WALLPAPERS = BundledWallpaperCatalog.PALETTES;
    private float scale = 1f;
    private float textScale = 1.2f;

    private static Typeface regular, semibold;
    private Integer previewFontColorOverride, previewClockColorOverride;
    private String previewFontKey, previewClockStyleOverride, previewMotionStyle;
    private Boolean previewVideoCropOverride;
    private int nextPanelHomeDiagram=-1;
    private final java.util.Map<String,String> settingsGroupSelections=new java.util.HashMap<>();

    private FrameLayout root; // the page, inside a vertical scroller
    private ScrollView homeScroller;
    private View clockPanel;
    private DetoxPerformanceProbe performanceProbe;
    private HomeReadabilityView homeReadability;
    private View focusBeforeAmbient;private HomeEntrance homeEntrance;private boolean welcomePending,ambientPreviewPending;private ClockUtilities clockUtilities;private AmbientInfoView ambientInfo;private android.view.ViewTreeObserver.OnGlobalFocusChangeListener navigationFocusListener;
    private HomeUiPolicy.Layout homeLayout;
    private TextView favouritesHeading, moreAppsHeading, homeConnectionStatus;
    private int tileW, tileBaseW, tileH, gridTop, appColumns = PER_ROW, tileRowStepPx, gridLeftInsetPx; // app grid geometry
    private List<String> utilityFocusKeys = Collections.emptyList();
    private final List<View> tiles = new ArrayList<>();
    private final TvMotion uiMotion=new TvMotion();
    private final Map<View, Runnable> focusAnimations = new IdentityHashMap<>();
    private final Map<View, Runnable> focusGlowAnimations = new IdentityHashMap<>();
    private TextClock dateClock, timeClock;
    private TextView homeGreeting;
    private FrameLayout timeClockStack;
    private NativeClockFaceView nativeClockFace;
    private WallpaperMotionView wallpaperMotion;
    private WallpaperParallaxDrawable wallpaperParallax;
    private ValueAnimator wallpaperParallaxAnimator;
    private TextureView videoWallpaperView;
    private Surface videoWallpaperSurface;
    private MediaPlayer videoWallpaperPlayer;
    private final VideoStartGuard videoStartGuard = new VideoStartGuard();
    private Runnable videoWallpaperStartTimeout;
    private long videoWallpaperFrameAt;
    private String pendingVideoWallpaperName;
    private String activeVideoWallpaperUri;
    private int videoWallpaperWidth, videoWallpaperHeight;
    private float pendingVideoCropScale = 1f;
    private int videoCropGeneration;
    private Dialog videoWallpaperCropDialog;
    private WallpaperAssets wallpaperAssets;
    private final Handler wallpaperDecodeHandler = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService wallpaperDecodeWorker = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> new Thread(task, "DetoxWallpaperDecode"));
    private final java.util.concurrent.ExecutorService photoImportWorker = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> new Thread(task, "DetoxPhotoImport"));
    private final java.util.concurrent.ExecutorService videoPreviewWorker = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> new Thread(task, "DetoxVideoPreview"));
    private final java.util.concurrent.ExecutorService videoImportWorker = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> new Thread(task, "DetoxVideoImport"));
    private volatile int wallpaperDecodeGeneration;
    private boolean wallpaperWindowInitialized, wallpaperWindowShowingVideo;
    private boolean wallpaperPickerPreviewing;
    private Runnable pendingWallpaperPreview;
    private int idleWallpaperDecodeGeneration;
    private volatile int videoImportToken;
    private boolean activityResumed, videoWallpaperPrepared;
    private FrameLayout screenHost;
    private static final int REMOTE_SHORTCUT_SLOTS=RemoteShortcutState.SLOTS;
    // Each settings dialog remembers the menu level to return to on the TV remote's Back key.
    private Runnable nextDialogBackAction;
    private boolean suppressSettingsReturn;
    private int settingsRootSelection;
    private boolean settingsRootColumnActive = true;
    private Dialog settingsRootDialog;private final UtilityUiPolicy.Selection utilityMenuSelection=new UtilityUiPolicy.Selection();private ScrollView settingsCategoryScroll;private View settingsDivider;
    private final java.util.WeakHashMap<TextView,Boolean> stableMenuText=new java.util.WeakHashMap<>();
    private LinearLayout settingsRootItems, settingsCategoryItems;
    private TextView[] settingsRootRows;
    private TextView[] settingsCategoryRows;
    private int settingsPanelCategory = -1;
    // Invalidates queued focus requests when a settings column is rebuilt or closed.
    private int settingsFocusGeneration = 0;
    private int settingsPanelX, settingsPanelY, settingsPanelWidth, settingsPanelHeight;
    private Rect pendingPanelOrigin;
    private TextView settingsUtilityLabel;
    private final int[] settingsCategorySelections = new int[SettingsMenuRoutes.categoryCount()];
    private PhoneCompanionServer phoneCompanionServer;
    private Dialog phoneCompanionDialog;
    private File pendingCompanionVideo;
    private FrameLayout idleWallpaperOverlay;
    private ImageView idleWallpaperImage;
    private WallpaperMotionView idleWallpaperMotion;
    private TextView idleWallpaperHint;
    private Runnable idleHintGlowFrame;
    private LinearGradient idleHintGlowShader;
    private final android.graphics.Matrix idleHintGlowMatrix = new android.graphics.Matrix();
    private int idleHintGlowSpan = -1;
    private int consumedIdleWakeKey = KeyEvent.KEYCODE_UNKNOWN;
    private boolean consumingIdleWakeTouch;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    private boolean pendingCompanionWallpaper;
    private final Runnable fullscreenWallpaperIdle = () -> enterFullscreenWallpaperIdle();
    private final Handler slideshowHandler = new Handler(Looper.getMainLooper());
    private final WallpaperSlideshowPolicy.Timing slideshowTiming=new WallpaperSlideshowPolicy.Timing();
    private final Runnable slideshowTick = new Runnable() {
        @Override public void run() {
            if (!activityResumed || slideshowPreviewBlocked() || hasActiveVideoWallpaper()) {slideshowTiming.reset();return;}
            SharedPreferences prefs = getSharedPreferences(PREFS, 0);
            List<String> photos = getWallpaperSlideshowPhotos();
            if (!prefs.getBoolean("wallpaperSlideshowPhotosEnabled", false) || photos.isEmpty()) return;
            // A single photo needs no periodic wallpaper reload; resume when the list changes.
            if (photos.size() == 1) return;
            int current = Math.max(0, Math.min(photos.size()-1, prefs.getInt("wallpaperSlideshowPhotoIndex", 0)));
            int next = prefs.getBoolean("wallpaperSlideshowShuffle", false) && photos.size() > 1
                    ? nextRandomIndex(current, photos.size()) : (current + 1) % photos.size();
            prefs.edit().putInt("wallpaperSlideshowPhotoIndex", next)
                    .putString("photoFile", photos.get(next))
                    .putString("wallpaper", WALLPAPER_NAMES[PHOTO]).apply();
            slideshowTiming.reset();
            showWallpaper(PHOTO);
            scheduleWallpaperSlideshow();
        }
    };
    private LinearLayout clockLayout;
    private boolean dirty = true;
    private final java.util.concurrent.ExecutorService appArtworkWorker = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> new Thread(task,"DetoxAppArtwork"));
    private final Map<String,AppTileData> appArtworkCache = new HashMap<>();
    private final Map<String,String> appArtworkInFlight = new HashMap<>();
    private int appArtworkGeneration;
    private static final class AppTileData {
        final String signature,label;final Drawable icon;
        AppTileData(String signature,String label,Drawable icon){this.signature=signature;this.label=label;this.icon=icon;}
    }
    private java.util.Locale shownLocale; // the TV's language (and region) when the page was built
    private String focusedKey;
    private final List<String> rowApps = new ArrayList<>(); // the top row as shown, in order
    private String movingKey;   // the app being moved with left/right, if any
    private TextView moveHint;  // shown above the apps while moving
    private boolean longPressed; // OK was held: swallow the key-up so the app doesn't open
    // The tile OK went down on. An app that closes on OK's key-down (Netflix's "Exit") hands the matching
    // key-up to the tile still focused here, so a key-up only opens the app it went down on (#30).
    private String okDownKey;

    // Apps installed, removed or updated. This often lands once Home is already back in front (an app
    // uninstalled from the menu is gone only after Android's prompt has closed), so catch up then.
    private boolean packageReceiverRegistered;
    private boolean networkCallbackRegistered;
    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            AppGridCatalog.invalidate();
            appArtworkGeneration++;appArtworkCache.clear();
            if(appArtworkResolver!=null)appArtworkResolver.invalidate();
            dirty = true;
            refreshApps();
        }
    };

    // The network tile follows the connection live: Wi-Fi bars, Ethernet or Offline. Only its icon and
    // label change, so nothing is rebuilt.
    private ImageView netIcon;
    private int netState = -1;
    private final ConnectivityManager.NetworkCallback networkChanged = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        @Override public void onLost(Network n) { runOnUiThread(MainActivity.this::refreshNetwork); }
        // Also fires when the Wi-Fi signal gets stronger or weaker.
        @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities c) {
            runOnUiThread(MainActivity.this::refreshNetwork);
        }
    };

    private long startupStarted;
    private boolean firstGridPending = true;
    private String lastHomeIconSettings = "";
    private boolean firstGridLoading;
    private boolean firstHomeDrawn;
    private Runnable firstGridApply;
    private final java.util.List<Runnable> pendingStartupMigrations = new java.util.ArrayList<>();
    private void scheduleStartupMigration(View host, Runnable migration, long delayMs) {
        pendingStartupMigrations.add(migration);
        if (delayMs > 0) host.postDelayed(migration, delayMs);
        else host.post(migration);
    }
    private List<String> initialGridApps;
    @Override
    protected void onCreate(Bundle state) {
        startupStarted = android.os.SystemClock.elapsedRealtime();
        super.onCreate(state);
        if (DetoxApplication.blocked(this)) {
            DetoxDialogStyle.builder(this).setTitle("Detox recovery").setMessage("Detox crashed repeatedly. Open Android Home settings or reset the crash guard to try again.").setPositiveButton("Home settings",(d,w)->TvIntegrations.recovery(this)).setNegativeButton("Try again",(d,w)->{getSharedPreferences("recovery",0).edit().clear().apply();recreate();}).setCancelable(false).show();
            return;
        }
        // Run off the UI thread; only remove stale restore scratch files.
        // Original-media .previous recovery copies are deliberately preserved.
        new Thread(() -> {
            recoverInterruptedRestoreMedia();
            cleanupStaleRestoreScratch();
        }, "detox-restore-cleanup").start();
        wallpaperAssets = createWallpaperAssets();
        appArtworkResolver = new LauncherAppArtworkResolver(this);
        welcomePending=state==null && SystemClock.elapsedRealtime()>180000L;seedShareableDefaults();
        // Legacy cleanup runs after the first Home frame.
        SharedPreferences placementPrefs = getSharedPreferences(PREFS, 0);
        if (!placementPrefs.getBoolean("utilitiesAboveAppsForInstallV1", false)) {
            SharedPreferences.Editor placement=placementPrefs.edit();
            if (!placementPrefs.contains("utilitiesPlacement")) placement.putInt("utilitiesPlacement", 3);
            placement.putBoolean("utilitiesAboveAppsForInstallV1", true).apply();
        }
        WallpaperCatalogPreferences.repair(getSharedPreferences(PREFS,0));
        migrateBundledPhotoWallpaper();
        ensureVideoWallpaperFill();
        saveConfig(getIntent());
        loadFonts();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        textScale = Math.max(0.8f, Math.min(1.4f,
                getSharedPreferences(PREFS, 0).getInt("fontScalePercent", 120) / 100f));
        configureWallpaperWindow();
        // configureWallpaperWindow applies video immediately and queues still-image decoding off-thread.

        ScrollView scroller = new ScrollView(this);
        homeScroller = scroller;
        scroller.setFillViewport(true);
        scroller.setSmoothScrollingEnabled(true);
        scroller.setOnGenericMotionListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_SCROLL&&event.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL)<0&&!scroller.canScrollVertically(1)){openGamingScreen();return true;}return false;});
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroller.setClipChildren(false);
        // A restrained edge-to-edge scrim keeps the wallpaper vivid while protecting text contrast.
        scroller.setBackgroundColor(Color.TRANSPARENT);
        root = new FrameLayout(this);
        root.setClipChildren(false);
        videoWallpaperView = new TextureView(this);
        videoWallpaperView.setOpaque(true);
        videoWallpaperView.setVisibility(View.GONE);
        videoWallpaperView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                videoWallpaperSurface = new Surface(surface);
                applyVideoWallpaperTransform();
                startVideoWallpaper();
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                applyVideoWallpaperTransform();
            }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                stopVideoWallpaper();
                if (videoWallpaperSurface != null) {
                    videoWallpaperSurface.release();
                    videoWallpaperSurface = null;
                }
                return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {if(videoWallpaperPlayer!=null&&videoWallpaperPrepared)videoWallpaperFrameAt=SystemClock.uptimeMillis();}
        });
        root.addView(videoWallpaperView, new FrameLayout.LayoutParams(-1, videoWallpaperHeight()));
        wallpaperMotion = new WallpaperMotionView(this);
        SharedPreferences motionPrefs = getSharedPreferences(PREFS, 0);
        String savedMotionStyle = motionPrefs.getString("wallpaperMotionStyle", "ambient");
        String currentMotionStyle = normalizeMotionStyle(savedMotionStyle);
        if (!savedMotionStyle.equals(currentMotionStyle))
            motionPrefs.edit().putString("wallpaperMotionStyle", currentMotionStyle).apply();
        wallpaperMotion.setMotionStyle(currentMotionStyle);
        wallpaperMotion.setMotionEnabled(motionEffectsEnabled());
        wallpaperMotion.setMotionTuning(effectiveMotionPreset());
        wallpaperMotion.setPerformanceMode(getSharedPreferences(PREFS, 0).getInt("wallpaperPerformance", 1));
        root.addView(wallpaperMotion, new FrameLayout.LayoutParams(-1,
                getResources().getDisplayMetrics().heightPixels));
        // A quiet edge gradient preserves wallpaper color while keeping clock and icon artwork
        // readable on both bright photos and moving video backgrounds.
        homeReadability = new HomeReadabilityView(this);
        applyHomeReadability();
        root.addView(homeReadability, new FrameLayout.LayoutParams(-1,
                getResources().getDisplayMetrics().heightPixels));
        clockPanel = new View(this);
        // Keep the clock placement container without painting the empty rounded panel behind it.
        clockPanel.setBackgroundColor(Color.TRANSPARENT);
        clockPanel.setElevation(0f);
        root.addView(clockPanel, at(px(MARGIN - 20), px(72), px(620), px(260)));
        // Wallpaper layers stay below every launcher control.
        scroller.addView(root, new FrameLayout.LayoutParams(-1, -2));

        clockLayout = new LinearLayout(this);
        clockLayout.setOrientation(LinearLayout.VERTICAL);
        shownLocale = java.util.Locale.getDefault();
        String day = Strings.datePattern();
        dateClock = clock(day, day, px(30), 0xA8FFFFFF, regular);
        timeClock = clock("h:mm", "H:mm", px(176), Color.WHITE, semibold);
        timeClock.setLetterSpacing(-0.028f);
        timeClock.setFontFeatureSettings("tnum");
        // Animate only actual minute changes, never every frame; retain TextClock's own scheduler.
        timeClock.addTextChangedListener(new android.text.TextWatcher() {
            private String previous = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String next = s == null ? "" : s.toString();
                boolean changed = !previous.isEmpty() && !previous.equals(next);
                previous = next;
                if (!changed || homeThemeIndex() == 0 || launcherAnimationMode() == 2
                        || !activityResumed || timeClock.getVisibility() != View.VISIBLE) return;
                timeClock.animate().cancel();
                timeClock.setTranslationY(-px(5));
                timeClock.setAlpha(0.78f);
                timeClock.animate().translationY(0f).alpha(1f)
                        .setDuration(motionDuration(320L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        timeClock.setIncludeFontPadding(false);
        timeClockStack = new FrameLayout(this);
        timeClockStack.setClipChildren(false);
        timeClockStack.setClipToPadding(false);
        timeClockStack.addView(timeClock, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        // Shared focus container keeps date, selected face and clock utilities aligned.
        clockLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        clockLayout.setClipChildren(false);
        clockLayout.setClipToPadding(false);
        clockLayout.addView(dateClock, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.topMargin = px(-20);
        clockLayout.addView(timeClockStack, tp);
        homeGreeting = text(px(25), 0xDFFFFFFF, semibold);
        homeGreeting.setGravity(Gravity.CENTER);
        homeGreeting.setIncludeFontPadding(false);
        homeGreeting.setLetterSpacing(0.012f);
        homeGreeting.setText(currentGreeting());
        homeGreeting.setContentDescription(homeGreeting.getText());
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(-2, -2);
        greetingParams.topMargin = px(2);
        clockLayout.addView(homeGreeting, greetingParams);
        root.addView(clockLayout, at(px(MARGIN), px(92), -2, -2));
        nativeClockFace = new NativeClockFaceView(this);
        nativeClockFace.setVisibility(View.GONE);
        clockLayout.addView(nativeClockFace, 2, new LinearLayout.LayoutParams(px(360), px(360)));

        homeConnectionStatus = text(px(22), 0xCFD6E6F2, regular);
        homeConnectionStatus.setGravity(Gravity.CENTER);
        homeConnectionStatus.setSingleLine(true);
        homeConnectionStatus.setIncludeFontPadding(false);
        homeConnectionStatus.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.min(fontPx(px(22)),px(26)));
        homeConnectionStatus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        homeConnectionStatus.setVisibility(View.GONE);
        applyClockStyle();
        applyClockPosition();

        moveHint = text(px(24), 0xA8FFFFFF, regular);
        moveHint.setText(Strings.t("Use the arrows to move it, then press OK"));
        moveHint.setVisibility(View.GONE);
        root.addView(moveHint, at(px(MARGIN), px(ROW1_TOP - 64), -2, -2));

        FrameLayout screen = new FrameLayout(this);
        screen.addView(scroller, new FrameLayout.LayoutParams(-1, -1));
        screenHost = screen;
        // Bottom quick dock removed; app rail and existing controls provide navigation.
        clockLayout.setFocusable(true);clockLayout.setClickable(true);clockLayout.setId(View.generateViewId());clockLayout.setContentDescription("Clock. Press OK for alarms, timers, stopwatch and world clocks");clockLayout.setOnClickListener(v->showClockUtilities());clockLayout.setOnFocusChangeListener((v,focused)->animateFocus(v,focused,1.015f));
        navigationFocusListener=(oldFocus,newFocus)->{updateWallpaperParallax(newFocus);};screen.getViewTreeObserver().addOnGlobalFocusChangeListener(navigationFocusListener);
        idleWallpaperOverlay = new FrameLayout(this);
        idleWallpaperOverlay.setVisibility(View.GONE);
        idleWallpaperOverlay.setFocusable(true);
        idleWallpaperOverlay.setFocusableInTouchMode(true);
        idleWallpaperOverlay.setClickable(true);
        idleWallpaperImage = new ImageView(this);
        idleWallpaperImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        idleWallpaperOverlay.addView(idleWallpaperImage, new FrameLayout.LayoutParams(-1, -1));
        idleWallpaperMotion = new WallpaperMotionView(this);
        idleWallpaperMotion.setPerformanceMode(getSharedPreferences(PREFS, 0).getInt("wallpaperPerformance", 1));
        idleWallpaperOverlay.addView(idleWallpaperMotion, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout idleHomePrompt = new LinearLayout(this);
        idleHomePrompt.setOrientation(LinearLayout.HORIZONTAL);
        idleHomePrompt.setGravity(Gravity.CENTER);
        idleHomePrompt.setBackgroundColor(Color.TRANSPARENT);
        idleWallpaperHint = text(px(24), 0xEFFFFFFF, Typeface.create("sans-serif-medium", Typeface.NORMAL));
        idleWallpaperHint.setText(Strings.t("Press any key to return Home"));
        idleWallpaperHint.setGravity(Gravity.CENTER);
        idleWallpaperHint.setPadding(0, 0, 0, 0);
        idleWallpaperHint.setBackgroundColor(Color.TRANSPARENT);
        idleHomePrompt.addView(idleWallpaperHint, new LinearLayout.LayoutParams(-2, -2));
        FrameLayout.LayoutParams hintParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hintParams.bottomMargin = px(24);
        idleWallpaperOverlay.addView(idleHomePrompt, hintParams);
        screen.addView(idleWallpaperOverlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(screen);
        screen.getViewTreeObserver().addOnDrawListener(new android.view.ViewTreeObserver.OnDrawListener() {
            private boolean recorded;
            @Override public void onDraw() {
                if (recorded) return; recorded = true; firstHomeDrawn = true;
                android.util.Log.i("DetoxStartup", "First home draw in " + (android.os.SystemClock.elapsedRealtime()-startupStarted) + " ms");
                screen.post(() -> { if (screen.getViewTreeObserver().isAlive()) screen.getViewTreeObserver().removeOnDrawListener(this); Runnable apply=firstGridApply;firstGridApply=null;if(apply!=null)apply.run(); });
            }
        });
        android.util.Log.i("DetoxStartup", "Home view created in " + (android.os.SystemClock.elapsedRealtime()-startupStarted) + " ms");
        scheduleStartupMigration(screen, ()->{SharedPreferences c=getSharedPreferences(PREFS,0);if(c.contains("wallpaperZones"))c.edit().remove("wallpaperZones").apply();if(!c.getBoolean("cinemaRemovalComplete",false)){getSharedPreferences("detox_experience_v1",0).edit().clear().apply();new File(getFilesDir(),"cinematic-foreground.png").delete();c.edit().putBoolean("cinemaRemovalComplete",true).apply();}},1800L);
        scheduleStartupMigration(screen, this::clearRemovedArcadeData,1500L);
        scheduleStartupMigration(screen, ()->{SharedPreferences p=getSharedPreferences(PREFS,0);if(!p.getBoolean("fishRemovalComplete",false)){SharedPreferences.Editor e=p.edit();for(String key:p.getAll().keySet())if(key.toLowerCase(java.util.Locale.ROOT).contains("aquarium")||key.startsWith("tankprofile."))e.remove(key);e.putBoolean("fishRemovalComplete",true).apply();getSharedPreferences("detox_aquarium_profile_preview",0).edit().clear().apply();}}, 0L);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(packagesChanged, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(packagesChanged, f);
        packageReceiverRegistered = true;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) try {
            if (android.os.Build.VERSION.SDK_INT >= 24) cm.registerDefaultNetworkCallback(networkChanged);
            else cm.registerNetworkCallback(new NetworkRequest.Builder().build(), networkChanged);
            networkCallbackRegistered = true;
        } catch (Exception e) { android.util.Log.w("DetoxLifecycle", "Network callback registration failed", e); }
    }

    private void clearRemovedArcadeData() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (prefs.getBoolean("removedArcadeDataV1", false)) return;
        if (!removeRetiredGamePreferences("flight_arcade")
                || !removeRetiredGamePreferences("night-flight")) {
            android.util.Log.w("DetoxMigration", "Retired game cleanup will retry after a preference write failure");
            return;
        }
        SharedPreferences.Editor shortcuts = prefs.edit();
        for (int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++) {
            if ("arcade".equals(prefs.getString(remoteShortcutActionKey(slot), "")))
                shortcuts.remove(remoteShortcutKeyKey(slot)).remove(remoteShortcutActionKey(slot))
                        .remove(remoteShortcutPackageKey(slot)).remove("remoteShortcutGesture"+slot);
        }
        if ("arcade".equals(prefs.getString("remoteShortcutKind", "")))
            shortcuts.remove("remoteShortcutKeyCode").remove("remoteShortcutKind").remove("remoteShortcutPackage");
        if (!shortcuts.putBoolean("removedArcadeDataV1", true).commit())
            android.util.Log.w("DetoxMigration", "Retired game cleanup marker could not be saved; cleanup will retry");
    }

    private boolean removeRetiredGamePreferences(String storeName) {
        SharedPreferences game = getSharedPreferences(storeName, 0);
        SharedPreferences.Editor editor = game.edit();
        boolean changed = false;
        for (String key : game.getAll().keySet()) {
            if (LegacyArcadeCleanupPolicy.isRetiredPreference(storeName, key)) {
                editor.remove(key);
                changed = true;
            }
        }
        return !changed || editor.commit();
    }

    private void migrateBundledPhotoWallpaper() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        boolean hasUserPhoto = prefs.contains("photoFile") || prefs.contains("photoUri")
                || prefs.getBoolean("wallpaperSlideshowPhotosEnabled", false);
        if ("Photo".equals(prefs.getString("wallpaper", "")) && !hasUserPhoto)
            prefs.edit().putString("wallpaper", WALLPAPER_NAMES[0]).apply();
    }
    private void ensureVideoWallpaperFill() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (!prefs.getBoolean("videoWallpaperFillMigration184", false)) {
            SharedPreferences.Editor fill=prefs.edit();
            if (!prefs.contains("liveWallpaperCrop")) fill.putBoolean("liveWallpaperCrop", true);
            fill.putBoolean("videoWallpaperFillMigration184", true).apply();
        }
    }
    /** Give a fresh install a complete, ready-to-use look without transferring private app data. */
    private void seedShareableDefaults() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (!prefs.getAll().isEmpty()) return;
        prefs.edit().putString("wallpaper", WALLPAPER_NAMES[0])
                .putString("font", "nunito")
                .putInt("fontScalePercent", 120)
                .putInt("fontColor", 0xFF89E0AA)
                .putInt("clockColor", 0xFF89E0AA)
                .putString("clockStyle", "digital")
                .putString("clockPosition", "top-right")
                .putBoolean("clockEdgeRightV3", true)
                .putBoolean("wallpaperMotion", true)
                .putString("wallpaperMotionStyle", "ambient")
                .apply();
    }

    @Override
    protected void onDestroy() {
        if (DetoxApplication.blocked(this)) { super.onDestroy(); return; }
        SettingsWorkbench.clearFramePreview(this);
        appArtworkWorker.shutdownNow();firstGridApply=null;
        wallpaperDecodeGeneration++;
        idleWallpaperDecodeGeneration++;
        wallpaperPhotoImportGeneration++;
        appIconImportGeneration++;
        videoCropGeneration++;
        if (pendingWallpaperPreview != null) wallpaperDecodeHandler.removeCallbacks(pendingWallpaperPreview);
        wallpaperDecodeWorker.shutdownNow();
        photoImportWorker.shutdownNow();
        videoPreviewWorker.shutdownNow();
        videoImportWorker.shutdownNow();
        if (screenHost != null) for (Runnable migration : pendingStartupMigrations) screenHost.removeCallbacks(migration);
        pendingStartupMigrations.clear();
        shortcutHandler.removeCallbacksAndMessages(null);
        idleHandler.removeCallbacksAndMessages(null);
        slideshowHandler.removeCallbacksAndMessages(null);
        sleepTimerHandler.removeCallbacksAndMessages(null);
        if(shortcutState!=null)shortcutState.cancel();if(homeEntrance!=null)homeEntrance.finish();if(clockUtilities!=null)clockUtilities.close();if(ambientInfo!=null)ambientInfo.active(false,null);if(navigationFocusListener!=null&&screenHost!=null&&screenHost.getViewTreeObserver().isAlive())screenHost.getViewTreeObserver().removeOnGlobalFocusChangeListener(navigationFocusListener);
        uiMotion.cancelAll();
        CompanionInput.pause(this);
        stopHomeFocusAnimations();
        videoImportToken++;
        if (sleepTimerRunnable != null) sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        dismissCropDialog();
        dismissAppIconCrop();
        if (videoWallpaperCropDialog != null && videoWallpaperCropDialog.isShowing()) videoWallpaperCropDialog.dismiss();
        discardPendingWallpaper();
        discardPendingAppIcon();
        cleanupCompanionVideo(null);
        stopPhoneCompanion();
        if (packageReceiverRegistered) {
            try { unregisterReceiver(packagesChanged); }
            catch (IllegalArgumentException e) { android.util.Log.w("DetoxLifecycle", "Package receiver already unregistered", e); }
            finally { packageReceiverRegistered = false; }
        }
        stopVideoWallpaper();
        if (videoWallpaperSurface != null) {
            videoWallpaperSurface.release();
            videoWallpaperSurface = null;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallbackRegistered) {
            try { cm.unregisterNetworkCallback(networkChanged); }
            catch (IllegalArgumentException e) { android.util.Log.w("DetoxLifecycle", "Network callback already unregistered", e); }
            finally { networkCallbackRegistered = false; }
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (saveConfig(intent)) {
            if (intent.hasExtra("wallpaper")) showWallpaper(wallpaper());
            buildTiles();
        }
        openSettingsShortcutIfRequested(intent);
    }

    private void openSettingsShortcutIfRequested(Intent intent) {
        if (intent == null || !ACTION_OPEN_DETOX_SETTINGS.equals(intent.getAction()) || root == null) return;
        welcomePending=false;if(homeEntrance!=null)homeEntrance.finish();intent.setAction(null);
        root.post(() -> {
            if (!isFinishing()) {
                resetIdleTimers();
                showSettingsRoot();
            }
        });
    }
    // Saves an app list passed with --es apps "..." and shortcuts passed with --es quick "...".
    // Returns true if it changed.
    // When Android kills the home screen in the background, it can start it again with the same old
    // intent; the stamp install.sh sends stops that from undoing changes made on the TV since.
    private boolean saveConfig(Intent intent) {
        String apps = intent == null ? null : intent.getStringExtra("apps");
        String quick = intent == null ? null : intent.getStringExtra("quick");
        String wallpaper = intent == null ? null : intent.getStringExtra("wallpaper");
        if (apps == null && quick == null && wallpaper == null) return false;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String stamp = intent.getStringExtra("stamp");
        if (stamp != null && stamp.equals(prefs.getString("stamp", null))) return false;
        SharedPreferences.Editor e = prefs.edit();
        if (stamp != null) e.putString("stamp", stamp);
        if (apps != null) {
            apps = apps.trim();
            if (apps.isEmpty() || apps.equalsIgnoreCase("auto")) e.remove("apps");
            else e.putString("apps", apps);
        }
        if (quick != null) {
            quick = quick.trim();
            if (quick.isEmpty() || quick.equalsIgnoreCase("none")) e.remove("quick");
            else e.putString("quick", quick);
        }
        if ("photo".equalsIgnoreCase(wallpaper)) {
            e.remove("photoUri").remove("photoFile").putString("wallpaper", WALLPAPER_NAMES[PHOTO]);
        }
        e.apply();
        dirty = true;
        return true;
    }

    private List<String> appList(PackageManager pm) {
        return AppGridCatalog.load(pm, getSharedPreferences(PREFS, 0), "apps", Apps.LIST,
                NOT_APPS, getPackageName(), PER_ROW);
    }

    static List<String> homeAppPlacementOrder(Context context) {
        return AppGridCatalog.load(context.getPackageManager(), context.getSharedPreferences(PREFS, 0),
                "apps", Apps.LIST, NOT_APPS, context.getPackageName(), PER_ROW);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppGridCatalog.invalidate();
        if (DetoxApplication.blocked(this)) return;
        CompanionInput.resume(this);
        if (performanceProbe == null) performanceProbe = new DetoxPerformanceProbe(this, "home");
        performanceProbe.start();
        activityResumed = true;
        refreshNativeClock();
        if(screenHost!=null)screenHost.postDelayed(()->{if(activityResumed&&!isFinishing())refreshHomeMediaCards();},800L);
        if (homeGreeting != null) {
            homeGreeting.setText(currentGreeting());
            homeGreeting.setContentDescription(homeGreeting.getText());
        }
        configureWallpaperWindow();
        if (wallpaperMotion != null) wallpaperMotion.setActive(!isFullscreenWallpaperIdle());
        if (idleWallpaperMotion != null) idleWallpaperMotion.setActive(isFullscreenWallpaperIdle());

        resetIdleTimers();
        scheduleWallpaperSlideshow();
        checkSleepTimerOnResume();
        Drawable wallpaper = getWindow().getDecorView().getBackground();
        if (wallpaper != null) wallpaper.setVisible(true, false);
        // Shared icon editors in Gaming must also refresh Home on return.
        if (!lastHomeIconSettings.equals(AppIconSettingsPolicy.signature(getSharedPreferences(PREFS, 0).getAll()))) dirty = true;
        if (dirty && firstGridPending) {
            // Package-manager calls can stall during boot. Resolve targets off the UI thread.
            if (!firstGridLoading) {
                firstGridLoading = true;
                new Thread(() -> {
                    List<String> found;
                    long started=SystemClock.elapsedRealtime();
                    try { found=appList(getPackageManager()); }
                    catch (RuntimeException error) { found=Collections.emptyList();DetoxDiagnostics.recordFailure("DetoxStartup","App catalog","preload failed",error); }
                    final List<String> apps=found;
                    android.util.Log.i("DetoxStartup","App targets resolved in "+(SystemClock.elapsedRealtime()-started)+" ms");
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed())return;
                        Runnable apply=()->{
                            firstGridLoading=false;firstGridPending=false;initialGridApps=apps;
                            if(!activityResumed)return;
                            dirty=false;buildTiles();startHomeWelcome();
                            android.util.Log.i("DetoxStartup","Home controls ready in "+(SystemClock.elapsedRealtime()-startupStarted)+" ms");
                        };
                        if(firstHomeDrawn)apply.run();else firstGridApply=apply;
                    });
                }, "DetoxAppCatalog").start();
            }
        } else if (dirty) {
            dirty = false; buildTiles();
        }
        openSettingsShortcutIfRequested(getIntent());
        refreshNetwork();
        View f = getCurrentFocus();
        if (f == null && !tiles.isEmpty()) tiles.get(0).requestFocus();
        animateReturnedApp();if(screenHost!=null)screenHost.post(this::startHomeWelcome);
    }

    @Override
    protected void onPause() {
        if (performanceProbe != null) performanceProbe.stop();
        if (DetoxApplication.blocked(this)) { super.onPause(); return; }
        if(homeEntrance!=null)homeEntrance.finish();if(clockUtilities!=null)clockUtilities.close();if(ambientInfo!=null)ambientInfo.active(false,null);
        if(shortcutState!=null)shortcutState.cancel();uiMotion.cancelAll();
        if(wallpaperParallaxAnimator!=null){wallpaperParallaxAnimator.cancel();wallpaperParallaxAnimator=null;}
        CompanionInput.pause(this);consumedIdleWakeKey=KeyEvent.KEYCODE_UNKNOWN;consumingIdleWakeTouch=false;
        // Persist only real app focus; never replace it with Settings or utility shortcuts.
        if (focusedKey != null && rowApps.contains(focusedKey) && !isFolderKey(focusedKey))
            getSharedPreferences(PREFS, 0).edit().putString("homeLastFocusedApp", focusedKey).apply();
        activityResumed = false;ambientPreviewPending=false;
        refreshNativeClock();
        stopHomeFocusAnimations();
        cancelVideoWallpaperStartTimeout();
        pauseVideoWallpaper();
        if (wallpaperMotion != null) wallpaperMotion.setActive(false);
        if (idleWallpaperMotion != null) idleWallpaperMotion.setActive(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        idleHandler.removeCallbacks(fullscreenWallpaperIdle);
        slideshowHandler.removeCallbacks(slideshowTick);
        slideshowTiming.reset();
        Drawable wallpaper = getWindow().getDecorView().getBackground();
        if (wallpaper != null) wallpaper.setVisible(false, false);
        super.onPause();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(homeEntrance!=null&&homeEntrance.running()&&event.getAction()==KeyEvent.ACTION_DOWN){homeEntrance.finish();consumedIdleWakeKey=event.getKeyCode();return true;}
        if (consumedIdleWakeKey != KeyEvent.KEYCODE_UNKNOWN) {
            if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == consumedIdleWakeKey)
                consumedIdleWakeKey = KeyEvent.KEYCODE_UNKNOWN;
            return true;
        }
        if (isFullscreenWallpaperIdle()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int key=event.getKeyCode();
                consumedIdleWakeKey = key;
                exitFullscreenWallpaperIdle(true);
            }
            return true;
        }
        if(shortcutState!=null&&event.getAction()==KeyEvent.ACTION_DOWN)shortcutState.otherKey(event.getKeyCode());
        if(event.getKeyCode()==consumedShortcutCaptureKey&&consumedShortcutCaptureKey!=KeyEvent.KEYCODE_UNKNOWN){if(event.getAction()==KeyEvent.ACTION_UP)consumedShortcutCaptureKey=KeyEvent.KEYCODE_UNKNOWN;return true;}
        if(capturingRemoteShortcut&&captureRemoteShortcutKey(event))return true;
        if(handleConfiguredRemoteShortcut(event))return true;
        // A dedicated Settings key on many Android TV remotes is KEYCODE_SETTINGS.
        // Route it to Detox's in-launcher menu instead of leaving it unhandled.
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                resetIdleTimers();
                showSettingsRoot();
            }
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_INFO) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                resetIdleTimers();
                showQuickControls();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) resetIdleTimers();
        return super.dispatchKeyEvent(event);
    }

    private boolean captureRemoteShortcutKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == consumedShortcutCaptureKey) {
            consumedShortcutCaptureKey = KeyEvent.KEYCODE_UNKNOWN;
            return true;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() != 0) return true;
        int code = event.getKeyCode();
        if (!isRemoteShortcutKeyAllowed(code)) {
            Toast.makeText(this, Strings.t("That button is reserved by Android. Try another remote button."), Toast.LENGTH_SHORT).show();
            return true;
        }
        SharedPreferences shortcutPrefs=getSharedPreferences(PREFS,0);
        SharedPreferences.Editor edit=shortcutPrefs.edit();
        for(int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++)if(slot!=pendingShortcutSlot&&shortcutPrefs.getInt(remoteShortcutKeyKey(slot),slot==0?shortcutPrefs.getInt("remoteShortcutKeyCode",KeyEvent.KEYCODE_UNKNOWN):KeyEvent.KEYCODE_UNKNOWN)==code){
            edit.remove(remoteShortcutKeyKey(slot)).remove(remoteShortcutActionKey(slot)).remove(remoteShortcutPackageKey(slot)).remove("remoteShortcutGesture"+slot);
            if(slot==0)edit.remove("remoteShortcutKeyCode").remove("remoteShortcutKind").remove("remoteShortcutPackage");
        }
        edit.putInt(remoteShortcutKeyKey(pendingShortcutSlot), code)
                .putString(remoteShortcutActionKey(pendingShortcutSlot), pendingShortcutKind == null ? "app" : pendingShortcutKind);
        edit.putString("remoteShortcutGesture" + pendingShortcutSlot,
                pendingShortcutGesture == null ? "tap" : pendingShortcutGesture);
        if (pendingShortcutPackage != null) edit.putString(remoteShortcutPackageKey(pendingShortcutSlot), pendingShortcutPackage);
        else edit.remove(remoteShortcutPackageKey(pendingShortcutSlot));
        edit.apply();
        consumedShortcutCaptureKey = code;
        capturingRemoteShortcut = false;
        if (remoteShortcutCaptureDialog != null && remoteShortcutCaptureDialog.isShowing())
            remoteShortcutCaptureDialog.dismiss();
        remoteShortcutCaptureDialog = null;
        pendingShortcutPackage = null;
        pendingShortcutKind = null;
        showRemoteShortcutOptions();
        Toast.makeText(this, Strings.t("Remote shortcut saved"), Toast.LENGTH_SHORT).show();
        return true;
    }

    private String remoteShortcutKeyKey(int slot) { return "remoteShortcutKeyCode" + slot; }
    private String remoteShortcutActionKey(int slot) { return "remoteShortcutAction" + slot; }
    private String remoteShortcutPackageKey(int slot) { return "remoteShortcutPackage" + slot; }

    boolean routeDialogShortcut(Dialog owner,KeyEvent event){
        int code=event.getKeyCode();
        if(code==KeyEvent.KEYCODE_SETTINGS||code==KeyEvent.KEYCODE_INFO){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                nextDialogBackAction=null;suppressSettingsReturn=true;
                try{if(owner!=null&&owner.isShowing())owner.cancel();}finally{suppressSettingsReturn=false;}
                resetIdleTimers();if(code==KeyEvent.KEYCODE_SETTINGS)showSettingsRoot();else showQuickControls();
            }
            return true;
        }
if(code==consumedShortcutCaptureKey&&code!=KeyEvent.KEYCODE_UNKNOWN){if(event.getAction()==KeyEvent.ACTION_UP)consumedShortcutCaptureKey=KeyEvent.KEYCODE_UNKNOWN;return true;}if(!isRemoteShortcutKeyAllowed(code))return false;SharedPreferences prefs=getSharedPreferences(PREFS,0);for(int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++)if(prefs.getInt(remoteShortcutKeyKey(slot),slot==0?prefs.getInt("remoteShortcutKeyCode",KeyEvent.KEYCODE_UNKNOWN):KeyEvent.KEYCODE_UNKNOWN)==code){shortcutOwner=owner;return handleConfiguredRemoteShortcut(event);}return false;}
    private void fireRemoteShortcut(int slot){Dialog owner=shortcutOwner;shortcutOwner=null;if(owner!=null&&owner.isShowing()){nextDialogBackAction=null;suppressSettingsReturn=true;try{owner.dismiss();}finally{suppressSettingsReturn=false;}}SharedPreferences prefs=getSharedPreferences(PREFS,0);String action=prefs.getString(remoteShortcutActionKey(slot),slot==0?prefs.getString("remoteShortcutKind","app"):"app");String target=prefs.getString(remoteShortcutPackageKey(slot),slot==0?prefs.getString("remoteShortcutPackage",""):"");runRemoteShortcutAction(action,target);}
    private RemoteShortcutState shortcutState(){if(shortcutState==null)shortcutState=new RemoteShortcutState(new RemoteShortcutState.Host(){public void fire(int slot){fireRemoteShortcut(slot);}public void later(Runnable task,long delay){shortcutHandler.postDelayed(task,delay);}public void remove(Runnable task){shortcutHandler.removeCallbacks(task);}});return shortcutState;}
    private boolean handleConfiguredRemoteShortcut(KeyEvent event){SharedPreferences prefs=getSharedPreferences(PREFS,0);if(!isRemoteShortcutKeyAllowed(event.getKeyCode()))return false;for(int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++){int key=prefs.getInt(remoteShortcutKeyKey(slot),slot==0?prefs.getInt("remoteShortcutKeyCode",KeyEvent.KEYCODE_UNKNOWN):KeyEvent.KEYCODE_UNKNOWN);if(key==KeyEvent.KEYCODE_UNKNOWN||key!=event.getKeyCode())continue;if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)resetIdleTimers();return shortcutState().event(key,event.getAction(),event.getRepeatCount(),event.isCanceled(),slot,"hold".equals(prefs.getString("remoteShortcutGesture"+slot,"tap")));}return false;}

    private void runRemoteShortcutAction(String action, String packageName) {
        if ("voice".equals(action)) launchVoiceSearch();
        else if ("settings".equals(action)) showSettingsRoot();
        else if ("wallpaper".equals(action)) showWallpapers();
        else if ("gaming".equals(action)) openGamingScreen();
        else if ("all-apps".equals(action)) showAllInstalledApps();
        else if ("app".equals(action)) {
            Intent launch = launchIntent(getPackageManager(), packageName);
            if (launch != null) launchApp(launch, tile(packageName));else Toast.makeText(this,"This shortcut app is unavailable. Choose another app.",Toast.LENGTH_LONG).show();
        }
    }

    private boolean isRemoteShortcutKeyAllowed(int keyCode) {
        return keyCode != KeyEvent.KEYCODE_UNKNOWN && keyCode != KeyEvent.KEYCODE_BACK
                && keyCode != KeyEvent.KEYCODE_HOME && keyCode != KeyEvent.KEYCODE_SETTINGS
                && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_POWER
                && keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
                && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
                && keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                && keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT
                && keyCode != KeyEvent.KEYCODE_DPAD_CENTER
                && keyCode != KeyEvent.KEYCODE_ENTER && keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if(homeEntrance!=null&&homeEntrance.running()&&event.getAction()==android.view.MotionEvent.ACTION_DOWN){homeEntrance.finish();consumingIdleWakeTouch=true;return true;}
        if (consumingIdleWakeTouch) {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP
                    || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) consumingIdleWakeTouch = false;
            return true;
        }
        if (isFullscreenWallpaperIdle()) {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                consumingIdleWakeTouch = true;
                exitFullscreenWallpaperIdle(true);
            }
            return true;
        }
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) resetIdleTimers();
        return super.dispatchTouchEvent(event);
    }


    private void startHomeWelcome(){if(!welcomePending||!activityResumed||isFinishing())return;if(!hasWindowFocus())return;welcomePending=false;if(!getSharedPreferences(PREFS,0).getBoolean("cinematicWelcome",true)||launcherAnimationMode()==2)return;homeEntrance=new HomeEntrance();for(int i=0;i<root.getChildCount();i++){View child=root.getChildAt(i);if(child==wallpaperMotion||child==videoWallpaperView||child==homeReadability)continue;homeEntrance.add(child,child==clockLayout?120:340);}homeEntrance.start(null);}
    private void showClockUtilities(){if(clockUtilities==null)clockUtilities=new ClockUtilities(this,new ClockUtilities.Host(){public void enter(View view){animateDialogIn(view);}public void dismiss(Dialog dialog){dismissDialogAnimated(dialog,null);}public void focus(View view,boolean focused){animateFocus(view,focused,1.025f);}public boolean shortcut(Dialog dialog,KeyEvent event){return routeDialogShortcut(dialog,event);}public void ambientSettings(){showIdleWallpaperSettings();}});clockUtilities.show(consumeNextDialogBackAction());}
    private void showIdleWallpaperSettings(){showSettingsGroup("idle.settings");}
    private void showIdleValueOptions(String title,String key,int[] values,int fallback){
        SharedPreferences prefs=getSharedPreferences(PREFS,0);String[] labels=new String[values.length];int selected=0;
        for(int i=0;i<values.length;i++){labels[i]=values[i]+"%";if(values[i]==prefs.getInt(key,fallback))selected=i;}
        routeNextDialogBack(this::showIdleWallpaperSettings);
        panel(Strings.t(title),translated(labels),null,0,selected,null,choice->{prefs.edit().putInt(key,values[choice]).apply();showIdleWallpaperSettings();});
    }
    private void tryAmbientPreview(){if(ambientPreviewPending&&activityResumed&&hasWindowFocus()){ambientPreviewPending=false;enterFullscreenWallpaperIdle(true);}}
    private void prepareIdleScene(){
        if(ambientInfo==null){ambientInfo=new AmbientInfoView(this);idleWallpaperOverlay.addView(ambientInfo,new FrameLayout.LayoutParams(-1,-1));}

        ambientInfo.bringToFront();ambientInfo.active(true,null);
        idleWallpaperHint.setText(Strings.t("Press any key to return Home"));
        idleWallpaperHint.setVisibility(getSharedPreferences(PREFS,0).getBoolean("idleShowHint",true)?View.VISIBLE:View.GONE);
        ((View)idleWallpaperHint.getParent()).bringToFront();
    }

    private void resetIdleTimers() {
        idleHandler.removeCallbacks(fullscreenWallpaperIdle);
        exitFullscreenWallpaperIdle(false);
        if (root != null) { root.setTranslationX(0f); root.setTranslationY(0f); }
        if (idleWallpaperOverlay != null) { idleWallpaperOverlay.setTranslationX(0f); idleWallpaperOverlay.setTranslationY(0f); }
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (activityResumed && prefs.getBoolean("idleWallpaperEnabled", true))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (prefs.getBoolean("idleWallpaperEnabled", true) && hasWindowFocus() && !isFinishing())
            idleHandler.postDelayed(fullscreenWallpaperIdle, idleWallpaperDelayMs(prefs));
    }

    private long idleWallpaperDelayMs(SharedPreferences prefs) {
        int minutes = Math.max(1, Math.min(30, prefs.getInt("idleWallpaperMinutes", 10)));
        return minutes * 60_000L;
    }

    private boolean isFullscreenWallpaperIdle() {
        return idleWallpaperOverlay != null && idleWallpaperOverlay.getVisibility() == View.VISIBLE;
    }

    private void enterFullscreenWallpaperIdle(){enterFullscreenWallpaperIdle(false);}
    private void enterFullscreenWallpaperIdle(boolean preview) {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (idleWallpaperOverlay == null || (!preview&&!prefs.getBoolean("idleWallpaperEnabled", true))
                || !hasWindowFocus() || isFinishing()) return;
        if(homeEntrance!=null)homeEntrance.finish();if(!isFullscreenWallpaperIdle())focusBeforeAmbient=getCurrentFocus();getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);prepareIdleScene();
        if (hasActiveVideoWallpaper()) {
            idleWallpaperImage.setVisibility(View.GONE);
            moveVideoWallpaperToIdle(true);
        } else {
            idleWallpaperImage.setImageDrawable(new ColorDrawable(Color.BLACK));
            loadIdleWallpaper(wallpaper());
            idleWallpaperImage.setVisibility(View.VISIBLE);
        }
        wallpaperMotion.setActive(false);
        idleWallpaperMotion.setMotionStyle(idleWallpaperMotionStyle(prefs));
        idleWallpaperMotion.setMotionEnabled(idleWallpaperMotionEnabled(prefs));
        idleWallpaperMotion.setMotionTuning(effectiveMotionPreset());
        idleWallpaperMotion.setActive(true);
        // Keep Home under the idle overlay until the crossfade finishes; hiding it before the
        // overlay becomes opaque flashes the window's black base color on slower TV hardware.
        homeScroller.setVisibility(View.VISIBLE);
        String transition = prefs.getString("idleWallpaperTransition", "fade-zoom");
        boolean zoom = "zoom".equals(transition) || "fade-zoom".equals(transition);
        float idleZoom = launcherAnimationMode() == 1 ? 1.012f : 1.035f;
        idleWallpaperOverlay.setAlpha(0f);
        idleWallpaperOverlay.setScaleX(zoom ? idleZoom : 1f);
        idleWallpaperOverlay.setScaleY(zoom ? idleZoom : 1f);
        idleWallpaperOverlay.setVisibility(View.VISIBLE);
        idleWallpaperOverlay.bringToFront();
        idleWallpaperOverlay.requestFocus();
        long duration = "none".equals(transition) ? 0L : motionDuration(320L);
        if (duration == 0L) {
            idleWallpaperOverlay.setAlpha(1f);
            idleWallpaperOverlay.setScaleX(1f);
            idleWallpaperOverlay.setScaleY(1f);
            homeScroller.setVisibility(View.INVISIBLE);
        } else {
            uiMotion.surface(idleWallpaperOverlay,1,1,duration,()->{if(isFullscreenWallpaperIdle())homeScroller.setVisibility(View.INVISIBLE);},null);
        }
        idleWallpaperHint.setAlpha(launcherAnimationMode()==2?1:0);uiMotion.surface(idleWallpaperHint,1,1,motionDuration(320),null,null);
        startIdleHintGlow();
    }

    private void startIdleHintGlow(){if(idleWallpaperHint==null||idleWallpaperHint.getVisibility()!=View.VISIBLE)return;if(idleHintGlowFrame!=null)idleWallpaperHint.removeCallbacks(idleHintGlowFrame);idleWallpaperHint.getPaint().setShader(null);idleWallpaperHint.setShadowLayer(0,0,0,Color.TRANSPARENT);idleHintGlowFrame=()->uiMotion.surface(idleWallpaperHint,0,1,motionDuration(260),null,null);idleWallpaperHint.postDelayed(idleHintGlowFrame,8000);}

    private void stopIdleHintGlow() {
        if (idleWallpaperHint == null) return;
        if (idleHintGlowFrame != null) idleWallpaperHint.removeCallbacks(idleHintGlowFrame);
        idleHintGlowFrame = null;
        idleWallpaperHint.getPaint().setShader(null);
        idleWallpaperHint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        idleWallpaperHint.setTextColor(withFontColor(0xEFFFFFFF));
        idleWallpaperHint.invalidate();
    }
    private boolean hasActiveVideoWallpaper() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String path = prefs.getString("liveWallpaperVideoPath", "");
        String uri = prefs.getString("liveWallpaperVideoUri", "");
        String asset = prefs.getString("liveWallpaperVideoAsset", "");
        return prefs.getBoolean("liveWallpaperEnabled", false)
                && ((!path.isEmpty() && new File(path).isFile() && new File(path).length() > 0)
                    || !uri.isEmpty() || BuiltInLiveVideoCatalog.containsAsset(asset));
    }

    // Reuse the real TextureView for idle playback so the video keeps moving and the same
    // aspect-ratio/crop transform is applied in both home and idle layouts.
    private void moveVideoWallpaperToIdle(boolean idle) {
        if (videoWallpaperView == null) {
            if (idleWallpaperImage != null) idleWallpaperImage.setVisibility(View.VISIBLE);
            return;
        }
        boolean activeVideo = hasActiveVideoWallpaper();
        ViewGroup destination = idle ? idleWallpaperOverlay : root;
        ViewGroup current = (ViewGroup) videoWallpaperView.getParent();
        if (current != destination) {
            if (current != null) current.removeView(videoWallpaperView);
            int height = idle ? -1 : videoWallpaperHeight();
            destination.addView(videoWallpaperView, 0, new FrameLayout.LayoutParams(-1, height));
        }
        videoWallpaperView.setVisibility(activeVideo ? View.VISIBLE : View.GONE);
        if (idleWallpaperImage != null) idleWallpaperImage.setVisibility(idle && activeVideo ? View.GONE : View.VISIBLE);
        if (activeVideo) videoWallpaperView.post(() -> {
            applyVideoWallpaperTransform();
            startVideoWallpaper();
        });
    }

    private void exitFullscreenWallpaperIdle(boolean animate) {
        if (idleWallpaperOverlay == null || idleWallpaperOverlay.getVisibility() != View.VISIBLE) return;
        idleWallpaperDecodeGeneration++;
        idleWallpaperOverlay.animate().cancel();
        idleWallpaperHint.animate().cancel();
        stopIdleHintGlow();if(ambientInfo!=null)ambientInfo.active(false,null);
        Runnable finish = () -> {
            homeScroller.setVisibility(View.VISIBLE);
            homeScroller.setAlpha(1f);
            moveVideoWallpaperToIdle(false);
            idleWallpaperOverlay.setVisibility(View.GONE);
            idleWallpaperOverlay.setAlpha(1f);
            idleWallpaperOverlay.setScaleX(1f);
            idleWallpaperOverlay.setScaleY(1f);
            idleWallpaperHint.setAlpha(1f);
            idleWallpaperMotion.setActive(false);
            wallpaperMotion.setActive(activityResumed);
            View focus = getCurrentFocus();
            if(focusBeforeAmbient!=null&&focusBeforeAmbient.isShown())focusBeforeAmbient.requestFocus();else if (focus == null && !tiles.isEmpty()) tiles.get(0).requestFocus();focusBeforeAmbient=null;
        };
        if (!animate) {uiMotion.surface(idleWallpaperOverlay,0,1,0,null,null);finish.run(); return; }
        homeScroller.setVisibility(View.VISIBLE);
        homeScroller.setAlpha(0f);
        uiMotion.surface(homeScroller,1,1,motionDuration(260),null,null);
        uiMotion.surface(idleWallpaperHint,0,1,motionDuration(100),null,null);
        uiMotion.surface(idleWallpaperOverlay,0,1.012f,motionDuration(260),finish,finish);

    }

    // Back from a menu or a window on top (like the uninstall prompt): catch up on apps that changed.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        refreshNativeClock();
        if (DetoxApplication.blocked(this)) return;
        if (hasFocus) {
            if (wallpaperMotion != null) wallpaperMotion.setActive(activityResumed && !isFullscreenWallpaperIdle());
            if (idleWallpaperMotion != null) idleWallpaperMotion.setActive(activityResumed && isFullscreenWallpaperIdle());
            if(screenHost!=null){screenHost.post(this::startHomeWelcome);screenHost.post(this::tryAmbientPreview);}resetIdleTimers();
            refreshApps();
            scheduleWallpaperSlideshow();
        } else {
            if(shortcutState!=null)shortcutState.cancel();scheduleWallpaperSlideshow();
            idleHandler.removeCallbacks(fullscreenWallpaperIdle);
            if (idleWallpaperMotion != null) idleWallpaperMotion.setActive(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            okDownKey = null; // an OK still held finishes in the window that took over
        }
    }

    // Rebuilds the tiles if apps changed, but only while Home is in front with no menu open and no
    // app being moved; otherwise the next onResume, focus change or end of the move does it.
    private void refreshApps() {
        if (!dirty || firstGridPending || !hasWindowFocus() || movingKey != null) return;
        dirty = false;
        buildTiles();
    }

    // The TV's language changed: start over so the labels and the date follow it.
    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (!java.util.Locale.getDefault().equals(shownLocale)) recreate();
    }

    @Override
    public void onBackPressed() { /* home screen: nothing to go back to */ }

    private WallpaperAssets createWallpaperAssets() {
        return new WallpaperAssets(this, PREFS, PHOTO, WALLPAPERS,
                new WallpaperAssets.PhotoTools() {
                    @Override public int orientation(String path) { return readPhotoOrientation(path); }
                    @Override public int orientation(Uri uri) { return readPhotoOrientation(uri); }
                    @Override public Bitmap orient(Bitmap source, int orientation) {
                        return orientPhotoBitmap(source, orientation);
                    }
                    @Override public Bitmap fit(Bitmap source) { return fitWallpaperBitmap(source); }
                });
    }

    private int readPhotoOrientation(String path) {
        return PhotoImageTools.orientation(path);
    }

    private int readPhotoOrientation(Uri uri) {
        return PhotoImageTools.orientation(this, uri);
    }

    private Bitmap orientPhotoBitmap(Bitmap source, int orientation) {
        return PhotoImageTools.orient(source, orientation);
    }

    // Makes a 16:9 wallpaper without stretching or cropping away any part of the user's photo.
    private Bitmap fitWallpaperBitmap(Bitmap source) {
        return PhotoImageTools.fitWallpaper(source);
    }

    private int wallpaperAccentColor() { return MenuTypography.rememberWallpaperAccent(wallpaperAssets.accentColor()); }

    // The user's photo is the default; the saved name remains compatible with the wallpaper menu.
    private int wallpaper() {
        return wallpaperAssets.selected();
    }

    private void showWallpaper(int i) {
        // A direct wallpaper choice supersedes any photo/slideshow import still being decoded.
        wallpaperPhotoImportGeneration++;
        videoCropGeneration++;
        videoImportToken++;
        cleanupCompanionVideo(null);
        cancelPendingWallpaperPreview();
        wallpaperPickerPreviewing = false;
        updateVideoAwareMotion();
        wallpaperAssets.invalidateAccent();
        final int request = ++wallpaperDecodeGeneration;
        if (hasActiveVideoWallpaper()) {
            // A video wallpaper owns the full background layer. Keep the static wallpaper from
            // peeking through at the edges while the TextureView is being laid out or resized.
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            wallpaperParallax=null;
            SharedPreferences prefs = getSharedPreferences(PREFS, 0);
            if (root != null && (prefs.getInt("fontColor", Color.WHITE) == WALLPAPER_MATCH_COLOR
                    || prefs.getInt("clockColor", 0) == WALLPAPER_MATCH_COLOR)) {
                applyFontPreview(root);
                applyClockStyle();
            }
            return;
        }
        try {
            wallpaperDecodeWorker.execute(() -> {
                if (request != wallpaperDecodeGeneration) return;
                Drawable decoded = wallpaperAssets.background(i);
                wallpaperDecodeHandler.post(() -> {
                    if (request != wallpaperDecodeGeneration || isFinishing() || isDestroyed()
                            || getWindow() == null) return;
                    applyWallpaperDrawable(decoded, motionDuration(420L));
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    private void previewWallpaper(int index) {
        if (hasActiveVideoWallpaper()) return;
        final int request = ++wallpaperDecodeGeneration;
        cancelPendingWallpaperPreview();
        wallpaperPickerPreviewing = true;
        updateVideoAwareMotion();
        Runnable task = () -> {
            pendingWallpaperPreview = null;
            try {
                wallpaperDecodeWorker.execute(() -> {
                    if (request != wallpaperDecodeGeneration) return;
                    Drawable decoded = wallpaperAssets.previewBackground(index);
                    wallpaperDecodeHandler.post(() -> {
                        if (request != wallpaperDecodeGeneration || isFinishing() || isDestroyed()
                                || getWindow() == null || hasActiveVideoWallpaper()) return;
                        applyWallpaperDrawable(decoded, motionDuration(140L));
                    });
                });
            } catch (java.util.concurrent.RejectedExecutionException ignored) { }
        };
        pendingWallpaperPreview = task;
        wallpaperDecodeHandler.postDelayed(task, 70L);
    }

    private void cancelPendingWallpaperPreview() {
        if (pendingWallpaperPreview != null) {
            wallpaperDecodeHandler.removeCallbacks(pendingWallpaperPreview);
            pendingWallpaperPreview = null;
        }
    }

    private void applyWallpaperDrawable(Drawable decoded, long transitionDuration) {
        Drawable old = getWindow().getDecorView().getBackground();
        Drawable next = new FramedWallpaper(decoded, getSharedPreferences(PREFS, 0));
        if(getSharedPreferences(PREFS,0).getBoolean("wallpaperParallaxEnabled",true)){
            wallpaperParallax=new WallpaperParallaxDrawable(next);next=wallpaperParallax;
        }else wallpaperParallax=null;
        if (old == null || transitionDuration == 0L) {
            getWindow().setBackgroundDrawable(next);
        } else {
            TransitionDrawable fade = new TransitionDrawable(new Drawable[] {old, next});
            fade.setCrossFadeEnabled(true);
            getWindow().setBackgroundDrawable(fade);
            fade.startTransition((int)transitionDuration);
        }
        if (root != null && (getSharedPreferences(PREFS, 0).getInt("fontColor", Color.WHITE) == WALLPAPER_MATCH_COLOR
                || getSharedPreferences(PREFS, 0).getInt("clockColor", 0) == WALLPAPER_MATCH_COLOR)) {
            applyFontPreview(root);
            applyClockStyle();
        }
        updateWallpaperParallax(getCurrentFocus());
    }

    private void updateWallpaperParallax(View focused){
        WallpaperParallaxDrawable drawable=wallpaperParallax;
        if(drawable==null)return;
        float targetX=0f,targetY=0f;
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        if(prefs.getBoolean("wallpaperParallaxEnabled",true)
                &&root!=null&&focused!=null&&root.getWidth()>0&&isWithinHomeRoot(focused)){
            int[] rootAt=new int[2],focusAt=new int[2];root.getLocationOnScreen(rootAt);focused.getLocationOnScreen(focusAt);
            float nx=Math.max(-1f,Math.min(1f,((focusAt[0]+focused.getWidth()*.5f-rootAt[0])/root.getWidth()-.5f)*2f));
            float ny=Math.max(-1f,Math.min(1f,((focusAt[1]+focused.getHeight()*.5f-rootAt[1])/Math.max(1,root.getHeight())-.5f)*2f));
            targetX=-nx*px(18);targetY=-ny*px(10);
        }
        if(wallpaperParallaxAnimator!=null)wallpaperParallaxAnimator.cancel();
        final float endX=targetX,endY=targetY;
        float fromX=drawable.offsetX(),fromY=drawable.offsetY();
        long duration=motionDuration(380L);
        if(duration==0||(!activityResumed&&fromX==endX&&fromY==endY)){drawable.offset(endX,endY);return;}
        wallpaperParallaxAnimator=ValueAnimator.ofFloat(0f,1f);wallpaperParallaxAnimator.setDuration(duration);
        wallpaperParallaxAnimator.addUpdateListener(anim->{float t=(Float)anim.getAnimatedValue();drawable.offset(fromX+(endX-fromX)*t,fromY+(endY-fromY)*t);});
        wallpaperParallaxAnimator.start();
    }
    private boolean isWithinHomeRoot(View view){
        View current=view;
        while(current!=null){if(current==root)return true;android.view.ViewParent parent=current.getParent();current=parent instanceof View?(View)parent:null;}
        return false;
    }

    private boolean liveWallpaperEnabled() {
        return getSharedPreferences(PREFS, 0).getBoolean("liveWallpaperEnabled", false);
    }

    private int launcherAnimationMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (prefs.contains("launcherMotionPreset")) {
            switch (Math.max(0, Math.min(3, prefs.getInt("launcherMotionPreset", 0)))) {
                case 2: return 1; // Calm
                case 3: return 2; // Reduced motion
                default: return 0;
            }
        }
        return Math.max(0, Math.min(2, prefs.getInt("launcherAnimationMode", 0)));
    }

    private int launcherMotionPreset() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (prefs.contains("launcherMotionPreset"))
            return Math.max(0, Math.min(3, prefs.getInt("launcherMotionPreset", 0)));
        int legacy = Math.max(0, Math.min(2, prefs.getInt("launcherAnimationMode", 0)));
        return legacy == 1 ? 2 : legacy == 2 ? 3 : 0;
    }

    private long motionDuration(long normalDuration) {
        int mode = launcherAnimationMode();
        if (mode == 2) return 0L;
        int preset = GamingPerformancePolicy.normalize(getSharedPreferences(PREFS, 0).getInt("detoxPerformancePreset", GamingPerformancePolicy.DEFAULT));
        float speed = mode == 1 ? 0.78f : launcherMotionPreset() == 1 ? 0.92f : 1f;
        // Smooth reduces animation work; Cinematic lengthens transitions while
        // respecting the separate Reduced Motion setting above.
        if (preset == 0) speed *= 0.78f;
        else if (preset == 2) speed *= 1.12f;
        return Math.max(1L, Math.round(normalDuration * speed));
    }

    private boolean motionEffectsEnabled() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        return prefs.getBoolean("wallpaperMotion", true) && launcherAnimationMode() != 2
                                && !"off".equals(normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient")));
    }

    private String idleWallpaperMotionStyle(SharedPreferences prefs) {
        String saved = prefs.getString("idleWallpaperMotionStyle", "same");
        return "same".equals(saved)
                ? normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient"))
                : normalizeMotionStyle(saved);
    }

    private boolean idleWallpaperMotionEnabled(SharedPreferences prefs) {
        return prefs.getBoolean("wallpaperMotion", true) && launcherAnimationMode() != 2
                                && !"off".equals(idleWallpaperMotionStyle(prefs));
    }

    private String currentGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return Strings.t("Good morning");
        if (hour < 17) return Strings.t("Good afternoon");
        if (hour < 21) return Strings.t("Good evening");
        return Strings.t("Good night");
    }

    private int effectiveMotionPreset() {
        if (launcherAnimationMode() == 1) return 0;
        return getSharedPreferences(PREFS, 0).getInt("motionPreset", 1);
    }

    private void applyMotionPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String style = normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient"));
        if (wallpaperMotion != null) {
            wallpaperMotion.setPerformanceMode(prefs.getInt("wallpaperPerformance", 1));
            wallpaperMotion.setMotionStyle(style);
            wallpaperMotion.setMotionEnabled(motionEffectsEnabled());
            wallpaperMotion.setMotionTuning(effectiveMotionPreset());
        }
        if (idleWallpaperMotion != null) {
            idleWallpaperMotion.setPerformanceMode(prefs.getInt("wallpaperPerformance", 1));
            idleWallpaperMotion.setMotionStyle(idleWallpaperMotionStyle(prefs));
            idleWallpaperMotion.setMotionEnabled(idleWallpaperMotionEnabled(prefs));
            idleWallpaperMotion.setMotionTuning(effectiveMotionPreset());
        }
    }

    private void configureWallpaperWindow() {
        if (getWindow() == null) return;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        // Keep Android TV's black navigation strip from covering the bottom edge of the launcher.
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(decor.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        boolean hasVideo = hasActiveVideoWallpaper();
        if (hasVideo) {
            cancelPendingWallpaperPreview();
            wallpaperPickerPreviewing = false;
            updateVideoAwareMotion();
            wallpaperDecodeGeneration++;
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        } else if (!wallpaperWindowInitialized || wallpaperWindowShowingVideo) {
            // Decode still images off the UI thread, including the first Home frame.
            showWallpaper(wallpaper());
        }
        wallpaperWindowInitialized = true;
        wallpaperWindowShowingVideo = hasVideo;
        if (videoWallpaperView != null) {
            videoWallpaperView.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
            if (hasVideo) startVideoWallpaper();
            else stopVideoWallpaper();
        }
    }

    private void startVideoWallpaper() {
        if (!activityResumed || videoWallpaperView == null || videoWallpaperSurface == null
                || !liveWallpaperEnabled()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String assetPath = prefs.getString("liveWallpaperVideoAsset", "");
        boolean useBuiltInAsset = BuiltInLiveVideoCatalog.containsAsset(assetPath);
        String path = prefs.getString("liveWallpaperVideoPath", "");
        File localVideo = path.isEmpty() ? null : new File(path);
        boolean useLocalVideo = localVideo != null && localVideo.isFile() && localVideo.length() > 0;
        String sourceKey = useBuiltInAsset ? "asset:" + assetPath
                : useLocalVideo ? localVideo.getAbsolutePath() : prefs.getString("liveWallpaperVideoUri", "");
        if (sourceKey.isEmpty()) return;
        if (videoWallpaperPlayer != null && sourceKey.equals(activeVideoWallpaperUri)) {
            try {
                if (videoWallpaperPrepared && !videoWallpaperPlayer.isPlaying()) videoWallpaperPlayer.start();
                applyVideoWallpaperAudioAndSpeed();
                updateVideoAwareMotion();
                if(videoStartGuard.pending(videoStartGuard.token()))armVideoWallpaperStartTimeout(videoWallpaperPlayer,videoStartGuard.token());
                else if(videoStartGuard.rendered()){videoWallpaperFrameAt=SystemClock.uptimeMillis();armVideoWallpaperProgressTimeout(videoWallpaperPlayer,videoStartGuard.token());}
                return;
            } catch (Exception ignored) { stopVideoWallpaper(); }
        }
        if (videoWallpaperPlayer != null) stopVideoWallpaper();
        MediaPlayer player = new MediaPlayer();
        videoWallpaperPlayer = player;
        activeVideoWallpaperUri = sourceKey;
        final long startToken=videoStartGuard.begin();
        armVideoWallpaperStartTimeout(player,startToken);
        try {
            player.setSurface(videoWallpaperSurface);
            VideoDisplayPacing.followDisplay(videoWallpaperSurface);
            player.setLooping(true);
            player.setVolume(0f, 0f);
            player.setOnVideoSizeChangedListener((mp, width, height) -> {
                if(videoWallpaperPlayer!=mp)return;
                videoWallpaperWidth = width;
                videoWallpaperHeight = height;
                applyVideoWallpaperTransform();
            });
            player.setOnPreparedListener(mp -> {
                if (videoWallpaperPlayer != mp) return;
                videoWallpaperPrepared = true;
                VideoDisplayPacing.prepared(mp,videoWallpaperSurface,false);
                applyVideoWallpaperAudioAndSpeed();
                applyVideoWallpaperTransform();
                if (activityResumed) {
                    try { mp.start(); updateVideoAwareMotion(); } catch (Exception e) { videoWallpaperError(mp); }
                }
            });
            player.setOnInfoListener((mp,what,extra)->{
                if(mp==videoWallpaperPlayer&&what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START&&videoStartGuard.ready(startToken)){
                    videoWallpaperFrameAt=SystemClock.uptimeMillis();armVideoWallpaperProgressTimeout(mp,startToken);
                    android.util.Log.i("DetoxVideo","First frame rendered: "+activeVideoWallpaperUri);
                    if(pendingVideoWallpaperName!=null){Toast.makeText(this,Strings.t("Live wallpaper applied: ")+pendingVideoWallpaperName,Toast.LENGTH_SHORT).show();pendingVideoWallpaperName=null;}
                }return false;
            });
            player.setOnErrorListener((mp, what, extra) -> {
                videoWallpaperError(mp,"Decoder error "+what+"/"+extra,null);
                return true;
            });
            if (useBuiltInAsset) {
                try (android.content.res.AssetFileDescriptor descriptor = getAssets().openFd(assetPath)) {
                    player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                }
            } else if (useLocalVideo) player.setDataSource(localVideo.getAbsolutePath());
            else player.setDataSource(this, Uri.parse(sourceKey));
            player.prepareAsync();
        } catch (Exception e) {
            videoWallpaperError(player,"Could not open selected video",e);
        }
    }

    private void cancelVideoWallpaperStartTimeout(){if(videoWallpaperStartTimeout!=null){wallpaperDecodeHandler.removeCallbacks(videoWallpaperStartTimeout);videoWallpaperStartTimeout=null;}}
    private void armVideoWallpaperStartTimeout(MediaPlayer player,long token){
        cancelVideoWallpaperStartTimeout();
        videoWallpaperStartTimeout=()->{videoWallpaperStartTimeout=null;if(activityResumed&&player==videoWallpaperPlayer&&videoStartGuard.timeout(token))videoWallpaperError(player,"No video frame rendered within 12 seconds",null);};
        wallpaperDecodeHandler.postDelayed(videoWallpaperStartTimeout,12000L);
    }
    private void armVideoWallpaperProgressTimeout(MediaPlayer player,long token){
        cancelVideoWallpaperStartTimeout();
        videoWallpaperStartTimeout=()->{
            videoWallpaperStartTimeout=null;
            if(!activityResumed||player!=videoWallpaperPlayer||videoStartGuard.token()!=token||!videoStartGuard.rendered())return;
            if(videoWallpaperView!=null&&videoWallpaperView.isShown()&&videoWallpaperView.getWindowVisibility()==View.VISIBLE&&SystemClock.uptimeMillis()-videoWallpaperFrameAt>8000L){videoWallpaperError(player,"Video stopped producing frames for 8 seconds",null);return;}
            armVideoWallpaperProgressTimeout(player,token);
        };
        wallpaperDecodeHandler.postDelayed(videoWallpaperStartTimeout,4000L);
    }
    private void videoWallpaperError(MediaPlayer player){videoWallpaperError(player,"Video playback failed",null);}
    private void videoWallpaperError(MediaPlayer player,String reason,Throwable cause) {
        if (videoWallpaperPlayer != player) return;
        DetoxDiagnostics.recordFailure("DetoxVideo","Home video wallpaper",reason+": "+activeVideoWallpaperUri,cause);
        stopVideoWallpaper();
        getSharedPreferences(PREFS, 0).edit().putBoolean("liveWallpaperEnabled", false).apply();
        if (videoWallpaperView != null) videoWallpaperView.setVisibility(View.GONE);
        configureWallpaperWindow();
        if (isFullscreenWallpaperIdle() && idleWallpaperImage != null) {
            loadIdleWallpaper(wallpaper());
            idleWallpaperImage.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, Strings.t("The video could not start. Your image wallpaper has been restored. Try another video." ), Toast.LENGTH_LONG).show();
    }

    private void applyVideoWallpaperAudioAndSpeed() {
        MediaPlayer player = videoWallpaperPlayer;
        if (player == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        float volume = prefs.getBoolean("liveWallpaperMuted", true) ? 0f : 1f;
        try { player.setVolume(volume, volume); } catch (Exception ignored) {}
        if (android.os.Build.VERSION.SDK_INT >= 23) try {
            float speed = Math.max(25,Math.min(200,prefs.getInt("liveWallpaperSpeed", 100))) / 100f;
            player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
        } catch (Exception ignored) {}
    }

    private int videoWallpaperHeight() {
        android.util.DisplayMetrics real = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(real);
        return Math.max(real.heightPixels, getResources().getDisplayMetrics().heightPixels);
    }

    private void applyVideoWallpaperTransform() {
        if (videoWallpaperView == null || videoWallpaperWidth <= 0 || videoWallpaperHeight <= 0) return;
        int viewWidth = videoWallpaperView.getWidth(), viewHeight = videoWallpaperView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        boolean fill = previewVideoCropOverride!=null?previewVideoCropOverride:prefs.getBoolean("liveWallpaperCrop", true);
        float viewAspect = viewWidth / (float) viewHeight;
        float videoAspect = videoWallpaperWidth / (float) videoWallpaperHeight;
        float scaleX = 1f, scaleY = 1f, offsetX = 0f, offsetY = 0f;
        if (fill) {
            if (videoAspect > viewAspect) {
                scaleX = videoAspect / viewAspect;
                offsetX = (viewWidth - viewWidth * scaleX) / 2f;
            } else {
                scaleY = viewAspect / videoAspect;
                offsetY = (viewHeight - viewHeight * scaleY) / 2f;
            }
        } else {
            if (videoAspect > viewAspect) {
                scaleY = viewAspect / videoAspect;
                offsetY = (viewHeight - viewHeight * scaleY) / 2f;
            } else {
                scaleX = videoAspect / viewAspect;
                offsetX = (viewWidth - viewWidth * scaleX) / 2f;
            }
        }
        float cropScale = Math.max(1f, Math.min(2.5f, prefs.getFloat("liveWallpaperCropScale", 1f)));
        scaleX *= cropScale;
        scaleY *= cropScale;
        offsetX = (viewWidth - viewWidth * scaleX) / 2f;
        offsetY = (viewHeight - viewHeight * scaleY) / 2f;
        Matrix transform = new Matrix();
        transform.setScale(scaleX, scaleY);
        transform.postTranslate(offsetX, offsetY);
        videoWallpaperView.setTransform(transform);
    }

    private void loadIdleWallpaper(int index) {
        final int request = ++idleWallpaperDecodeGeneration;
        try {
            wallpaperDecodeWorker.execute(() -> {
                Drawable decoded = wallpaperAssets.background(index);
                wallpaperDecodeHandler.post(() -> {
                    if (request != idleWallpaperDecodeGeneration || isFinishing() || isDestroyed()
                            || idleWallpaperImage == null || hasActiveVideoWallpaper()) return;
                    idleWallpaperImage.setImageDrawable(new FramedWallpaper(decoded, getSharedPreferences(PREFS, 0)));
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    private void pauseVideoWallpaper() {
        if (videoWallpaperPlayer == null) { updateVideoAwareMotion(); return; }
        try { if (videoWallpaperPrepared && videoWallpaperPlayer.isPlaying()) videoWallpaperPlayer.pause(); }
        catch (Exception ignored) { stopVideoWallpaper(); }
        updateVideoAwareMotion();
    }

    private void updateVideoAwareMotion() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        boolean videoPlaying = prefs.getBoolean("videoAwareMotion", true)
                && liveWallpaperEnabled() && videoWallpaperPlayer != null && videoWallpaperPrepared;
        if (videoPlaying) {
            try { videoPlaying = videoWallpaperPlayer.isPlaying(); }
            catch (Exception ignored) { videoPlaying = false; }
        }
        boolean playing = wallpaperPickerPreviewing || videoPlaying;
        if (wallpaperMotion != null) wallpaperMotion.setVideoAwareLoad(playing);
        if (idleWallpaperMotion != null) idleWallpaperMotion.setVideoAwareLoad(playing);
    }

    private void stopVideoWallpaper() {
        cancelVideoWallpaperStartTimeout();videoStartGuard.cancel();pendingVideoWallpaperName=null;videoWallpaperFrameAt=0;
        MediaPlayer player = videoWallpaperPlayer;
        videoWallpaperPlayer = null;
        activeVideoWallpaperUri = null;
        videoWallpaperPrepared = false;
        videoWallpaperWidth = videoWallpaperHeight = 0;
        if (player == null) { updateVideoAwareMotion(); return; }
        try { player.setOnPreparedListener(null); player.setOnVideoSizeChangedListener(null); player.setOnErrorListener(null); player.setOnInfoListener(null); } catch (Exception ignored) {}
        // Release already stops the decoder; avoid redundant synchronous stop/reset transitions.
        try { player.release(); } catch (Exception ignored) {}
        updateVideoAwareMotion();
    }

    private void showVideoWallpaperPicker() {
        consumeNextDialogBackAction();
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("video/*");
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(pick, PICK_VIDEO_WALLPAPER); }
        catch (Exception e) { routeNextDialogBack(this::showVideoWallpaperMenu);showPhoneCompanion(); }
    }

    private void showVideoWallpaperCrop(Uri source) {
        wallpaperPhotoImportGeneration++;
        videoImportToken++;
        final int request = ++videoCropGeneration;
        final float savedScale = getSharedPreferences(PREFS, 0).getFloat("liveWallpaperCropScale", 1f);
        Toast.makeText(this, Strings.t("Preparing video crop preview…"), Toast.LENGTH_SHORT).show();
        try {
            videoPreviewWorker.execute(() -> {
                if (request != videoCropGeneration || isFinishing() || isDestroyed()) return;
                Bitmap frame = null;
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                try {
                    retriever.setDataSource(getApplicationContext(), source);
                    if (android.os.Build.VERSION.SDK_INT >= 27)
                        frame = retriever.getScaledFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 1600, 900);
                    else frame = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (frame != null && (frame.getWidth() > 1600 || frame.getHeight() > 900)) {
                        float factor = Math.min(1600f / frame.getWidth(), 900f / frame.getHeight());
                        Bitmap scaled = Bitmap.createScaledBitmap(frame,
                                Math.max(1, Math.round(frame.getWidth() * factor)),
                                Math.max(1, Math.round(frame.getHeight() * factor)), true);
                        if (scaled != frame) frame.recycle();
                        frame = scaled;
                    }
                } catch (Exception ignored) {
                } finally {
                    try { retriever.release(); } catch (Exception ignored) {}
                }
                final Bitmap preview = frame;
                wallpaperDecodeHandler.post(() -> {
                    if (request != videoCropGeneration || isFinishing() || isDestroyed()) {
                        if (preview != null && !preview.isRecycled()) preview.recycle();
                        return;
                    }
                    if (preview == null) {
                        detoxAlertBuilder().setTitle(Strings.t("Video preview unavailable"))
                                .setMessage(Strings.t("The TV could not create a crop preview for this video. Apply it without custom cropping?"))
                                .setNegativeButton(Strings.t("Cancel"), (d, which) -> { cancelCompanionVideoImport(source); showLiveWallpaperOptions(); })
                                .setPositiveButton(Strings.t("Apply without crop"), (d, which) -> importVideoWallpaper(source, 1f))
                                .setOnCancelListener(d -> { cancelCompanionVideoImport(source); showLiveWallpaperOptions(); })
                                .show();
                        return;
                    }
                    showVideoWallpaperCropDialog(source, preview, savedScale);
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            if (!isFinishing() && !isDestroyed())
                Toast.makeText(this, Strings.t("Video preview is unavailable right now"), Toast.LENGTH_LONG).show();
        }
    }

    private void showVideoWallpaperCropDialog(Uri source, Bitmap preview, float initialScale) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        videoWallpaperCropDialog = d;
        d.setOnDismissListener(ignored -> {
            if (videoWallpaperCropDialog == d) videoWallpaperCropDialog = null;
            if (!preview.isRecycled()) preview.recycle();
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(menuPx(32), menuPx(28), menuPx(32), menuPx(28));

        TextView title = text(menuPx(34), Color.WHITE, semibold);
        title.setText(Strings.t("Crop live wallpaper"));
        title.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(title, new LinearLayout.LayoutParams(-1, menuPx(50)));
        TextView zoomLabel = text(menuPx(22), 0xCCFFFFFF, regular);
        final float[] zoom = {Math.max(1f, Math.min(2.5f, initialScale))};
        zoomLabel.setText(Math.round(zoom[0] * 100f) + "%");
        box.addView(zoomLabel, new LinearLayout.LayoutParams(-1, menuPx(38)));

        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setClipChildren(true);
        previewFrame.setClipToPadding(true);
        previewFrame.setBackgroundColor(0xFF080B11);
        ImageView image = new ImageView(this);
        image.setImageBitmap(preview);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setScaleX(zoom[0]);
        image.setScaleY(zoom[0]);
        previewFrame.addView(image, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        previewParams.topMargin = menuPx(10);
        previewParams.bottomMargin = menuPx(20);
        box.addView(previewFrame, previewParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        TextView zoomOut = videoCropButton(Strings.t("Zoom out"));
        TextView zoomIn = videoCropButton(Strings.t("Zoom in"));
        TextView apply = videoCropButton(Strings.t("Apply"));
        TextView cancel = videoCropButton(Strings.t("Cancel"));
        TextView[] controls = {zoomOut, zoomIn, apply, cancel};
        for (TextView control : controls) {
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, menuPx(66), 1f);
            buttonParams.leftMargin = menuPx(6);
            buttonParams.rightMargin = menuPx(6);
            actions.addView(control, buttonParams);
        }
        box.addView(actions, new LinearLayout.LayoutParams(-1, menuPx(66)));
        Runnable updateZoom = () -> {
            image.setScaleX(zoom[0]);
            image.setScaleY(zoom[0]);
            zoomLabel.setText(Math.round(zoom[0] * 100f) + "%");
        };
        zoomOut.setOnClickListener(v -> { zoom[0] = Math.max(1f, zoom[0] - 0.10f); updateZoom.run(); });
        zoomIn.setOnClickListener(v -> { zoom[0] = Math.min(2.5f, zoom[0] + 0.10f); updateZoom.run(); });
        apply.setOnClickListener(v -> {
            pendingVideoCropScale = zoom[0];
            dismissDialogAnimated(d, () -> importVideoWallpaper(source, pendingVideoCropScale));
        });
        cancel.setOnClickListener(v -> dismissDialogAnimated(d, () -> { cancelCompanionVideoImport(source); showLiveWallpaperOptions(); }));
        setAnimatedBack(d, () -> { cancelCompanionVideoImport(source); showLiveWallpaperOptions(); });

        box.setBackground(glassSurface(new int[] {0x1A2A3A52, 0x1A182436}, 0x20FFFFFF,
                Math.max(1, Math.round(RADIUS * MENU_SCALE))));
        box.setElevation(menuPx(18));
        d.setContentView(box);
        Window w = d.getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setDimAmount(0.55f);
        if (android.os.Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false) ? menuPx(28) : 0);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int width = Math.min(menuPx(1480), screenWidth - menuPx(80));
        int height = Math.min(menuPx(850), screenHeight - menuPx(64));
        w.setLayout(width, height);
        w.setGravity(Gravity.CENTER);
        d.show(); CompanionInput.dialog(d);
        animateDialogIn(w.getDecorView());
        zoomOut.requestFocus();
    }

    private TextView videoCropButton(String label) {
        TextView button = text(menuPx(24), 0xEFFFFFFF, semibold);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(menuPx(18));
        bg.setColor(0x663B526F);
        bg.setStroke(Math.max(1, menuPx(2)), 0x447CE4FF);
        button.setBackground(bg);
        button.setOnFocusChangeListener((v, focused) -> {
            bg.setColor(focused ? 0xAA236D96 : 0x663B526F);
            bg.setStroke(Math.max(1, menuPx(2)), focused ? 0xFF8FE8FF : 0x447CE4FF);
            button.setTextColor(focused ? Color.WHITE : withFontColor(0xEFFFFFFF));
            button.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f)
                    .setDuration(motionDuration(150L)).start();
        });
        return button;
    }
    private String videoWallpaperExtension(Uri source) {
        String name = null;
        try (android.database.Cursor cursor = getContentResolver().query(source,
                new String[] {android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst())
                name = cursor.getString(0);
        } catch (Exception ignored) { }
        String extension = null;
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) extension = name.substring(dot + 1);
        }
        if ((extension == null || extension.isEmpty()) && "file".equalsIgnoreCase(source.getScheme())) {
            String path = source.getLastPathSegment();
            if (path != null) {
                int dot = path.lastIndexOf('.');
                if (dot >= 0 && dot + 1 < path.length()) extension = path.substring(dot + 1);
            }
        }
        if (extension == null || extension.isEmpty()) {
            String mime = getContentResolver().getType(source);
            if (mime != null) extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        }
        if (extension == null) extension = "mp4";
        extension = extension.toLowerCase(java.util.Locale.ROOT);
        return java.util.Arrays.asList("mp4", "m4v", "mov", "mkv", "webm", "avi", "3gp", "ts", "mpg", "mpeg").contains(extension)
                ? extension : "mp4";
    }
    /** Import the picker result before playback so TV document providers cannot revoke or
     * expose a non-seekable stream while the wallpaper is looping. File I/O stays off the UI thread. */
    private void importVideoWallpaper(Uri source, float cropScale) {
        final int token = ++videoImportToken;
        Toast.makeText(this, Strings.t("Importing video wallpaper…"), Toast.LENGTH_SHORT).show();
        try {
            videoImportWorker.execute(() -> {
                if (token != videoImportToken) {
                    wallpaperDecodeHandler.post(() -> cleanupCompanionVideo(source));
                    return;
                }
                String importId = System.currentTimeMillis() + "-" + token;
                File staged = new File(getFilesDir(), "video-wallpaper-" + importId + ".importing");
                File imported = new File(getFilesDir(), "video-wallpaper-" + importId + "." + videoWallpaperExtension(source));
                String error = null;
                try (InputStream input = openVideoInput(source);
                     FileOutputStream output = new FileOutputStream(staged)) {
                    if (input == null) throw new java.io.IOException("The selected video could not be opened");
                    byte[] buffer = new byte[256 * 1024];
                    long total = 0;
                    long availableBytes = new android.os.StatFs(getFilesDir().getAbsolutePath()).getAvailableBytes();
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted() || token != videoImportToken)
                            throw new java.io.InterruptedIOException("Video import cancelled");
                        total += read;
                        if (total + 128L * 1024L * 1024L > availableBytes)
                            throw new java.io.IOException("Not enough free storage to import this video");
                        if (total > 8L * 1024L * 1024L * 1024L)
                            throw new java.io.IOException("Video is larger than 8 GB");
                        output.write(buffer, 0, read);
                    }
                    if (Thread.currentThread().isInterrupted() || token != videoImportToken)
                        throw new java.io.InterruptedIOException("Video import cancelled");
                    output.getFD().sync();
                    if (total < 1024) throw new java.io.IOException("Selected video is empty or incomplete");
                    if (!staged.renameTo(imported)) throw new java.io.IOException("Could not save the video on this TV");
                } catch (Exception e) {
                    error = e.getMessage();
                    staged.delete();
                    imported.delete();
                }
                final String importError = error;
                wallpaperDecodeHandler.post(() -> {
                    if (token != videoImportToken || isFinishing() || isDestroyed()) {
                        imported.delete();
                        cleanupCompanionVideo(source);
                        return;
                    }
                    if (importError != null) {
                        if (pendingCompanionVideo != null) updatePhoneTransferStatus("Video received but import failed");
                        Toast.makeText(this, Strings.t("Could not import this video. Try a supported video under 8 GB with enough free storage."),
                                Toast.LENGTH_LONG).show();
                        cleanupCompanionVideo(source);
                        return;
                    }
                    SharedPreferences prefs = getSharedPreferences(PREFS, 0);
                    String previousPath = prefs.getString("liveWallpaperVideoPath", "");
                    try {
                        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(source, flags);
                    } catch (Exception ignored) { }
                    stopVideoWallpaper();
                    prefs.edit().putString("liveWallpaperVideoUri", source.toString())
                            .putString("liveWallpaperVideoPath", imported.getAbsolutePath())
                            .remove("liveWallpaperVideoAsset").remove("liveWallpaperVideoName")
                            .putString("liveWallpaperScene", "video").putBoolean("liveWallpaperEnabled", true)
                            .putBoolean("liveWallpaperCrop", true).putFloat("liveWallpaperCropScale", Math.max(1f, Math.min(2.5f, cropScale))).apply();
                    configureWallpaperWindow();
                    Toast.makeText(this, Strings.t("Video wallpaper imported and applied"), Toast.LENGTH_SHORT).show();
                    if (pendingCompanionVideo != null) updatePhoneTransferStatus("Video live wallpaper applied");
                    cleanupCompanionVideo(source);
                    if (!previousPath.isEmpty() && !previousPath.equals(imported.getAbsolutePath())) {
                        File old = new File(previousPath);
                        if (old.getParentFile() != null && old.getParentFile().equals(getFilesDir())
                                && old.getName().startsWith("video-wallpaper-")) old.delete();
                    }
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            cleanupCompanionVideo(source);
            if (!isFinishing() && !isDestroyed())
                Toast.makeText(this, Strings.t("Video import is unavailable right now"), Toast.LENGTH_LONG).show();
        }
    }

    private void showLiveWallpaperOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String videoPath = prefs.getString("liveWallpaperVideoPath", "");
        String builtInAsset = prefs.getString("liveWallpaperVideoAsset", "");
        boolean hasBuiltInVideo = BuiltInLiveVideoCatalog.containsAsset(builtInAsset);
        boolean hasImportedVideo = (!videoPath.isEmpty() && new File(videoPath).isFile())
                || !prefs.getString("liveWallpaperVideoUri", "").isEmpty();
        boolean hasVideo = hasBuiltInVideo || hasImportedVideo;
        String style = normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient"));
        boolean effectsOn = motionEffectsEnabled() && !"off".equals(style);
        List<SettingsChoice> rows=new ArrayList<>();
        if(hasVideo){
            rows.add(new SettingsChoice("enabled","Live wallpaper · "+(prefs.getBoolean("liveWallpaperEnabled",false)?"On":"Off")));
            rows.add(new SettingsChoice("effects","Wallpaper effects…"));
            rows.add(new SettingsChoice("mute","Mute video · "+(prefs.getBoolean("liveWallpaperMuted",true)?"On":"Off")));
            rows.add(new SettingsChoice("framing","Framing · "+(prefs.getBoolean("liveWallpaperCrop",true)?"Fill screen":"Fit entire video")));
            if(hasImportedVideo)rows.add(new SettingsChoice("crop","Crop video · "+Math.round(prefs.getFloat("liveWallpaperCropScale",1f)*100f)+"%"));
            rows.add(new SettingsChoice("speed","Playback speed · "+liveSpeedName(prefs.getInt("liveWallpaperSpeed",100))));
        }else rows.add(new SettingsChoice("import","Choose a video wallpaper"));
        routeNextDialogBack(this::showVideoWallpaperMenu);
        String activeName = hasBuiltInVideo ? BuiltInLiveVideoCatalog.nameForAsset(builtInAsset) : "";
        String optionsTitle = activeName.isEmpty() ? Strings.t("Live wallpaper options")
                : Strings.t("Live wallpaper options") + " · " + activeName;
        panel(optionsTitle, settingsChoiceLabels(rows), null, 0, -1, null, which -> {
            String action=rows.get(which).id;
            if ("import".equals(action)) {
                showVideoWallpaperPicker();
                return;
            }
            if ("enabled".equals(action)) {
                wallpaperPhotoImportGeneration++;
                videoCropGeneration++;
                videoImportToken++;
                prefs.edit().putBoolean("liveWallpaperEnabled",
                        !prefs.getBoolean("liveWallpaperEnabled", false)).apply();
                configureWallpaperWindow();
                scheduleWallpaperSlideshow();
            } else if ("effects".equals(action)) {
                showSettingsGroup(SettingsMenuRoutes.EFFECTS);
                return;
            } else if ("mute".equals(action)) {
                boolean muted = prefs.getBoolean("liveWallpaperMuted", true);
                prefs.edit().putBoolean("liveWallpaperMuted", !muted).apply();
                applyVideoWallpaperAudioAndSpeed();
            } else if ("framing".equals(action)) {
                String[] framing = {"Fill screen · crop edges", "Fit entire video · show all"};
                int selected = prefs.getBoolean("liveWallpaperCrop", true) ? 0 : 1;
                routeNextDialogBack(this::showLiveWallpaperOptions);
                panel(Strings.t("Video framing"), translated(framing), null, 0, selected, choice -> {
                    previewVideoCropOverride=choice==0;
                    applyVideoWallpaperTransform();
                }, choice -> {
                    prefs.edit().putBoolean("liveWallpaperCrop", choice == 0).apply();
                    previewVideoCropOverride=null;
                    applyVideoWallpaperTransform();
                    showLiveWallpaperOptions();
                });
                return;
            } else if ("crop".equals(action)) {
                String uri = prefs.getString("liveWallpaperVideoUri", "");
                Uri source = uri.isEmpty() ? Uri.fromFile(new File(videoPath)) : Uri.parse(uri);
                showVideoWallpaperCrop(source);
                return;
            } else if ("speed".equals(action)) {
                String[] speeds = {"0.75×", "Normal", "1.25×"};
                int[] values = {75, 100, 125};
                int saved = prefs.getInt("liveWallpaperSpeed", 100);
                int selected = saved <= 75 ? 0 : saved >= 125 ? 2 : 1;
                routeNextDialogBack(this::showLiveWallpaperOptions);
                panel(Strings.t("Playback speed"), translated(speeds), null, 0, selected, null, choice -> {
                    prefs.edit().putInt("liveWallpaperSpeed", values[choice]).apply();
                    applyVideoWallpaperAudioAndSpeed();
                    showLiveWallpaperOptions();
                });
                return;
            }
            showLiveWallpaperOptions();
        });
    }
    private String liveSpeedName(int value) { return value <= 75 ? "0.75×" : value >= 125 ? "1.25×" : "Normal"; }

    private void scheduleWallpaperSlideshow() {
        slideshowHandler.removeCallbacks(slideshowTick);
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        int seconds=slideshowSeconds();
        if(WallpaperSlideshowPolicy.shouldSchedule(activityResumed,slideshowPreviewBlocked(),
                prefs.getBoolean("wallpaperSlideshowPhotosEnabled",false),
                hasActiveVideoWallpaper(),seconds,getWallpaperSlideshowPhotos().size()))
            slideshowHandler.postDelayed(slideshowTick,slideshowTiming.delay(SystemClock.uptimeMillis(),seconds));
        else slideshowTiming.reset();
    }

    private boolean slideshowPreviewBlocked(){
        return wallpaperPickerPreviewing||(cropDialog!=null&&cropDialog.isShowing())||isFullscreenWallpaperIdle();
    }

    private int slideshowSeconds() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int seconds = prefs.getInt("wallpaperSlideshowSeconds", -1);
        return seconds >= 0 ? seconds : prefs.getInt("wallpaperSlideshowMinutes", 0) * 60;
    }

    private String slideshowDurationLabel(int seconds) {
        if (seconds == 0) return "Off";
        if (seconds < 60) return "Every " + seconds + " seconds";
        int minutes = seconds / 60;
        return minutes % 60 == 0 ? (minutes / 60) + " hour" + (minutes == 60 ? "" : "s")
                : "Every " + minutes + " minutes";
    }

    private List<String> translated(String[] names) {
        List<String> out = new ArrayList<>();
        for (String name : names) out.add(Strings.t(name));
        return out;
    }

    private List<String> translated(List<String> names) {
        List<String> out = new ArrayList<>();
        for (String name : names) out.add(Strings.t(name));
        return out;
    }

    private void showFontOptions() {
        String current = getSharedPreferences(PREFS, 0).getString("font", "nunito");
        int selected = Math.max(0, Arrays.asList(FONT_KEYS).indexOf(current));
        routeNextDialogBack(this::showFontSettings);
        panel(Strings.t("Font"), translated(FONT_NAMES), null, 0, selected, which -> {
            previewFontKey = FONT_KEYS[which];
            loadFonts();
            applyFontPreview(root);
            applyClockStyle();
        }, which -> {
            getSharedPreferences(PREFS, 0).edit().remove("lookPreset").putString("font", FONT_KEYS[which]).apply();
            previewFontKey = null;
            loadFonts();
            applyFontPreview(root);
            applyClockStyle();
            showFontSettings();
        });
    }

    private void showFontSettings(){showSettingsGroup(SettingsMenuRoutes.FONTS);}

    private String currentThemePreset() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String saved = prefs.getString("themePreset", THEME_NAMES[0]);
        if ("Rainbow".equals(saved)) {
            saved = THEME_NAMES[MULTICOLOR_THEME_INDEX];
            prefs.edit().putString("themePreset", saved).apply();
        }
        return Arrays.asList(THEME_NAMES).contains(saved) ? saved : THEME_NAMES[0];
    }

    private void showFontColorOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int saved = prefs.getInt("fontColor", Color.WHITE);
        int selected = saved == WALLPAPER_MATCH_COLOR ? 0 : 1;
        for (int i = 0; i < FONT_COLORS.length; i++) if (FONT_COLORS[i] == saved) selected = i + 1;
        List<String> choices = new ArrayList<>();
        choices.add("Wallpaper match");
        choices.addAll(translated(COLOR_NAMES));
        routeNextDialogBack(this::showFontSettings);
        panel(Strings.t("Font color"), choices, which -> {
            GradientDrawable swatch = new GradientDrawable();
            swatch.setShape(GradientDrawable.OVAL);
            swatch.setColor(which == 0 ? wallpaperAccentColor() : FONT_COLORS[which - 1]);
            swatch.setStroke(Math.max(1, px(2)), 0x80FFFFFF);
            return swatch;
        }, px(28), selected, which -> {
            previewFontColorOverride = which == 0 ? wallpaperAccentColor() : FONT_COLORS[which - 1];
            applyFontPreview(root);
            MenuTypography.refresh(this);
        }, which -> {
            prefs.edit().remove("lookPreset").putInt("fontColor", which == 0 ? WALLPAPER_MATCH_COLOR : FONT_COLORS[which - 1]).apply();
            previewFontColorOverride = null;
            applyFontPreview(root);
            applyClockStyle();
            buildTiles(); // Recreate utility/app labels with the newly saved font color.
            showFontSettings();
        });
    }
    private void showTextSizeOptions() {
        int[] values = {90, 100, 110, 120, 130, 140};
        String[] labels = {"90%", "100%", "110%", "120%", "130%", "140%"};
        int current = getSharedPreferences(PREFS, 0).getInt("fontScalePercent", 120);
        int selected = 3;
        for (int i = 0; i < values.length; i++) if (values[i] == current) selected = i;
        routeNextDialogBack(this::showFontSettings);
        panel(Strings.t("Text size"), translated(labels), null, 0, selected, null, which -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, 0);
            float oldScale = textScale;
            textScale = Math.max(0.8f, Math.min(1.4f, values[which] / 100f));
            prefs.edit().putInt("fontScalePercent", values[which]).apply();
            if (root != null && oldScale > 0f) applyFontScale(root, textScale / oldScale);
            applyClockStyle();
            MenuTypography.refresh(this);
            showFontSettings();
        });
    }

    private void showClockColorOptions() {
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        List<String> choices = new ArrayList<>();
        choices.add("Match utilities colour");
        choices.add("Wallpaper match");
        choices.addAll(translated(COLOR_NAMES));
        choices.add("Rainbow");
        int saved = getSharedPreferences(PREFS, 0).getInt("clockColor", 0);
        int selected = 0;
        if (saved == WALLPAPER_MATCH_COLOR) selected = 1;
        else for (int i = 0; i < FONT_COLORS.length; i++) if (FONT_COLORS[i] == saved) selected = i + 2;
        if (saved == RAINBOW_COLOR) selected = choices.size() - 1;
        final int rainbowIndex = choices.size() - 1;
        panel(Strings.t("Clock color"), choices, which -> {
            GradientDrawable swatch = new GradientDrawable();
            swatch.setShape(GradientDrawable.OVAL);
            if (which == 0) swatch.setColor(uiAccentColor());
            else if (which == 1) swatch.setColor(wallpaperAccentColor());
            else if (which == rainbowIndex) swatch.setColors(RAINBOW_COLORS);
            else swatch.setColor(FONT_COLORS[which - 2]);
            swatch.setStroke(Math.max(1, px(2)), 0x80FFFFFF);
            return swatch;
        }, px(28), selected, which -> {
            previewClockColorOverride = which == 0 ? 0
                    : which == 1 ? wallpaperAccentColor()
                    : which == rainbowIndex ? RAINBOW_COLOR : FONT_COLORS[which - 2];
            applyClockStyle();
        }, which -> {
            int color = which == 0 ? 0
                    : which == 1 ? WALLPAPER_MATCH_COLOR
                    : which == rainbowIndex ? RAINBOW_COLOR : FONT_COLORS[which - 2];
            SharedPreferences.Editor editor = getSharedPreferences(PREFS, 0).edit().remove("lookPreset");
            // Clock color is independent from the Utilities colour theme.
            editor.putInt("clockColor", color).apply();
            previewClockColorOverride = null;
            applyClockStyle();
            Toast.makeText(this, Strings.t("Clock color updated"), Toast.LENGTH_SHORT).show();
            showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        });
    }

    private void showThemeOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int selected = Math.max(0, Arrays.asList(THEME_NAMES).indexOf(currentThemePreset()));
        routeNextDialogBack(this::showThemesAndStyle);
        panel(Strings.t("Color Theme"), translated(THEME_NAMES), which -> {
            GradientDrawable swatch = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    which == MULTICOLOR_THEME_INDEX ? MULTICOLOR_SWATCH
                            : new int[] {which == WALLPAPER_THEME_INDEX ? wallpaperAccentColor() : THEME_COLORS[which],
                                         which == WALLPAPER_THEME_INDEX ? wallpaperAccentColor() : THEME_COLORS[which]});
            swatch.setShape(GradientDrawable.OVAL);
            swatch.setStroke(Math.max(1, px(2)), 0x80FFFFFF);
            return swatch;
        }, px(28), selected, null, which -> {
            prefs.edit().remove("lookPreset").putString("themePreset", THEME_NAMES[which]).apply();
            wallpaperAssets.invalidateAccent();
            applyClockStyle();
            buildTiles();
            showThemesAndStyle();
        });
    }

    private String normalizeMotionStyle(String style) {
        return WallpaperEffects.normalize(style);
    }
    private String motionDisplayName(String style) { return WallpaperEffects.displayName(style); }

    private void showBackupRestoreOptions() {
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.BACKUP));
        List<SettingsChoice> rows=Arrays.asList(new SettingsChoice("save","Save settings + wallpaper/video"),new SettingsChoice("restore","Restore transfer backup"));
        panel(Strings.t("Backup & restore"),settingsChoiceLabels(rows),
                null, 0, 0, null, which -> {
            if ("save".equals(rows.get(which).id)) {
                try {
                    Uri uri = createTransferBackupUri();
                    if (uri == null) throw new java.io.IOException("Could not create Downloads backup");
                    writeLauncherBackup(uri);
                } catch (Exception e) {
                    Toast.makeText(this, Strings.t("Could not save transfer backup"), Toast.LENGTH_LONG).show();
                }
                return;
            }
            Uri existing = findTransferBackupUri();
            if (existing != null) {
                confirmRestoreTransferBackup(existing);
                return;
            }
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("application/zip");
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try { startActivityForResult(pick, RESTORE_BACKUP); }
            catch (Exception e) {
                Toast.makeText(this, Strings.t("Choose the backup ZIP from Downloads"), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showHomeProfiles() {
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.BACKUP));
        try {
            List<SettingsChoice> rows=new ArrayList<>();String active=DetoxHomeProfiles.active(this);
            String[] names=DetoxHomeProfiles.names(this);
            rows.add(new SettingsChoice("save","Save current setup as profile…"));
            for(String name:names)rows.add(new SettingsChoice("activate",(name.equals(active)?"✓ Active · ":"Activate · ")+name,name));
            for(String name:names)rows.add(new SettingsChoice("delete","Delete · "+name,name));
            rows.add(new SettingsChoice("export","Export profiles…"));rows.add(new SettingsChoice("import","Import profiles…"));
            detoxAlertBuilder().setTitle("Home profiles").setItems(settingsChoiceLabels(rows).toArray(new String[0]),(dialog,index)->{
                SettingsChoice row=rows.get(index);
                switch(row.id){
                    case "save":promptSaveHomeProfile();break;
                    case "activate":try{DetoxHomeProfiles.activate(this,row.value);recreate();}catch(Exception error){showProfileError("Could not activate profile",error);}break;
                    case "delete":
                        routeNextDialogBack(this::showHomeProfiles);
                        detoxAlertBuilder().setTitle("Delete profile?").setMessage("Delete “"+row.value+"”? This does not delete wallpaper or video files.")
                            .setNegativeButton("Cancel",(d,w)->showHomeProfiles()).setPositiveButton("Delete",(d,w)->{
                                try{DetoxHomeProfiles.delete(this,row.value);showHomeProfiles();}catch(Exception error){showProfileError("Could not delete profile",error);}
                            }).show();break;
                    case "export":
                        Intent create=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");create.putExtra(Intent.EXTRA_TITLE,"detox-home-profiles.json");
                        try{startActivityForResult(create,EXPORT_HOME_PROFILES);}catch(Exception error){showProfileError("Could not open export picker",error);}break;
                    case "import":
                        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        try{startActivityForResult(pick,IMPORT_HOME_PROFILES);}catch(Exception error){showProfileError("Could not open import picker",error);}break;
                }
            }).setNegativeButton("Close",(d,w)->showSettingsGroup(SettingsMenuRoutes.BACKUP)).show();
        }catch(Exception error){showProfileError("Could not read home profiles",error);}
    }

    private void promptSaveHomeProfile() {
        routeNextDialogBack(this::showHomeProfiles);
        EditText name = new EditText(this);
        name.setSingleLine(true); name.setHint("Profile name");
        name.setText(DetoxHomeProfiles.active(this).isEmpty() ? "Home" : DetoxHomeProfiles.active(this));
        detoxAlertBuilder().setTitle("Save home profile")
                .setMessage("Saves launcher appearance and settings. It does not copy or remove your media files.")
                .setView(name)
                .setNegativeButton("Cancel", (d, w) -> showHomeProfiles())
                .setPositiveButton("Save", (d, w) -> {
                    final String checked;
                    try { checked = HomeProfilePolicy.validateName(name.getText().toString()); }
                    catch (Exception error) { showProfileError("Invalid profile name", error); return; }
                    boolean exists = false;
                    try { exists = Arrays.asList(DetoxHomeProfiles.names(this)).contains(checked); }
                    catch (Exception error) { showProfileError("Could not read profiles", error); return; }
                    if (exists) {
                        routeNextDialogBack(this::promptSaveHomeProfile);
                        detoxAlertBuilder().setTitle("Replace profile?").setMessage("Replace the saved settings in “" + checked + "”?")
                                .setNegativeButton("Cancel", (x, y) -> showHomeProfiles())
                                .setPositiveButton("Replace", (x, y) -> saveHomeProfile(checked)).show();
                    } else saveHomeProfile(checked);
                }).show();
    }

    private void saveHomeProfile(String name) {
        try { DetoxHomeProfiles.save(this, name); Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show(); showHomeProfiles(); }
        catch (Exception error) { showProfileError("Could not save profile", error); }
    }

    private void showPerformanceSampling() {
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.PERFORMANCE));
        boolean enabled = getSharedPreferences("config", MODE_PRIVATE).getBoolean("detoxPerformanceProbe", false);
        String[] actions = {enabled ? "Turn frame sampling off" : "Turn frame sampling on", "Refresh report"};
        detoxAlertBuilder().setTitle("Frame sampling")
                .setMessage(DetoxPerformanceProbe.report(this) + "\n\nSampling records Detox UI frame intervals while Home or Gaming is visible. It does not measure decoder or system-wide FPS.")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        getSharedPreferences("config", MODE_PRIVATE).edit().putBoolean("detoxPerformanceProbe", !enabled).apply();
                        if (performanceProbe != null) { performanceProbe.stop(); if (!enabled && activityResumed) performanceProbe.start(); }
                    }
                    showPerformanceSampling();
                }).setNegativeButton("Done", null).show();
    }

    private void showPerformancePreset() {
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.PERFORMANCE));
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int selected = GamingPerformancePolicy.normalize(prefs.getInt("detoxPerformancePreset", GamingPerformancePolicy.DEFAULT));
        detoxAlertBuilder().setTitle("Performance preset")
                .setMessage("Applies across Home and Gaming. It changes Detox overlay effects and transition pacing; it does not change source video frame rates.")
                .setSingleChoiceItems(GamingPerformancePolicy.NAMES, selected, (dialog, which) -> {
                    prefs.edit().putInt("detoxPerformancePreset", GamingPerformancePolicy.normalize(which)).apply();
                    dialog.dismiss();
                    if (wallpaperMotion != null) wallpaperMotion.invalidate();
                    Toast.makeText(this, GamingPerformancePolicy.name(which) + " preset selected", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Close", null).show();
    }

    private void showProfileError(String title, Exception error) {
        detoxAlertBuilder().setTitle(title).setMessage(error.getMessage() == null ? "The operation failed." : error.getMessage())
                .setPositiveButton("OK", null).show();
    }

    private void confirmRestoreTransferBackup(Uri backup) {
        detoxAlertBuilder()
                .setTitle(Strings.t("Restore launcher backup?"))
                .setMessage(Strings.t("This replaces the current Detox layout and settings with the saved backup. App installations are not changed."))
                .setNegativeButton(Strings.t("Cancel"), (dialog, which) -> dialog.dismiss())
                .setPositiveButton(Strings.t("Restore"), (dialog, which) -> readLauncherBackup(backup))
                .show();
    }

    private Uri createTransferBackupUri() throws Exception {
        if (android.os.Build.VERSION.SDK_INT < 29) return Uri.fromFile(transferBackupFile());
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "detox-backup.zip");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);
        return getContentResolver().insert(collection, values);
    }

    private Uri findTransferBackupUri() {
        if (android.os.Build.VERSION.SDK_INT < 29) return null;
        String[] columns = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        try (android.database.Cursor c = getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                columns, selection, new String[] {"detox-backup.zip"}, MediaStore.MediaColumns.DATE_MODIFIED + " DESC")) {
            if (c != null && c.moveToFirst())
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getString(0));
        } catch (Exception ignored) {}
        return null;
    }

    private void pruneTransferBackups(Uri keep) {
        if (android.os.Build.VERSION.SDK_INT < 29) return;
        String[] columns = {MediaStore.MediaColumns._ID};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        List<Uri> stale = new ArrayList<>();
        try (android.database.Cursor c = getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                columns, selection, new String[] {"detox-backup.zip"}, null)) {
            if (c != null) while (c.moveToNext()) {
                Uri candidate = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getString(0));
                if (!candidate.equals(keep)) stale.add(candidate);
            }
        } catch (Exception ignored) { return; }
        for (Uri old : stale) { try { getContentResolver().delete(old, null, null); } catch (Exception ignored) {} }
    }

    private File transferBackupFile() {
        File downloads = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
        if (downloads == null) downloads = new File(getFilesDir(), "Download");
        if (!downloads.exists()) downloads.mkdirs();
        return new File(downloads, "detox-backup.zip");
    }

    private void writeLauncherBackup(Uri uri) {
        new Thread(() -> {
            boolean saved = false;
            try {
                java.io.OutputStream raw = "file".equals(uri.getScheme())
                        ? new FileOutputStream(new File(uri.getPath())) : getContentResolver().openOutputStream(uri);
                if (raw == null) throw new java.io.IOException("Could not open backup destination");
                try (java.io.OutputStream out = raw) { writeLauncherBackup(out); }
                saved = true;
            } catch (Exception ignored) {}
            if (saved) pruneTransferBackups(uri);
            else if ("content".equals(uri.getScheme())) { try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {} }
            final boolean success = saved;
            runOnUiThread(() -> Toast.makeText(this,
                    Strings.t(success ? "Launcher backup saved" : "Could not save transfer backup"), Toast.LENGTH_LONG).show());
        }, "detox-backup-write").start();
    }

    private void writeLauncherBackup(java.io.OutputStream raw) throws Exception {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        org.json.JSONObject document = LauncherBackupCodec.create(prefs.getAll(), getPackageName(),System.currentTimeMillis());
        try (ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.putNextEntry(new ZipEntry("backup.json"));
            zip.write(document.toString(2).getBytes("UTF-8"));
            zip.closeEntry();
            String photoFile = prefs.getString("photoFile", null);
            String photoUri = prefs.getString("photoUri", null);
            InputStream image = null;
            if (photoFile != null && new File(photoFile).isFile()) image = new FileInputStream(photoFile);
            else if (photoUri != null && !photoUri.isEmpty()) image = getContentResolver().openInputStream(Uri.parse(photoUri));
            // Never report a complete backup if the selected personal wallpaper is missing.
            if (image == null && ((photoFile != null && !photoFile.isEmpty())
                    || (photoUri != null && !photoUri.isEmpty())))
                throw new java.io.IOException("Selected wallpaper is inaccessible; backup cancelled");
            if (image != null) {
                zip.putNextEntry(new ZipEntry("wallpaper.jpg"));
                try { copyBackupBytes(image, zip, 128L * 1024L * 1024L); }
                finally { try { image.close(); } catch (Exception ignored) {} }
                zip.closeEntry();
            }
            String videoPath = prefs.getString("liveWallpaperVideoPath", "");
            File video = videoPath.isEmpty() ? null : new File(videoPath);
            boolean portableVideo = video != null && video.isFile()
                    && video.getCanonicalPath().startsWith(getFilesDir().getCanonicalPath() + File.separator)
                    && video.getName().startsWith("video-wallpaper-");
            // External video references cannot be embedded by this exporter. Fail rather
            // than silently create a backup which loses the user's video wallpaper.
            if (!videoPath.isEmpty() && !portableVideo)
                throw new java.io.IOException("Video wallpaper cannot be embedded; backup cancelled");
            if (portableVideo) {
                zip.putNextEntry(new ZipEntry("video-wallpaper.bin"));
                try (InputStream in = new FileInputStream(video)) { copyBackupBytes(in, zip, 1024L * 1024L * 1024L); }
                zip.closeEntry();
            }
        }
    }

    private long copyBackupBytes(InputStream in, java.io.OutputStream out, long maxBytes) throws Exception {
        byte[] buffer = new byte[16384];
        long total = 0;
        int count;
        while ((count = in.read(buffer)) != -1) {
            total += count;
            if (total > maxBytes) throw new java.io.IOException("Backup item is too large");
            out.write(buffer, 0, count);
        }
        return total;
    }

    private void readLauncherBackup(Uri uri) {
        new Thread(() -> {
            try {
                java.io.InputStream in = "file".equals(uri.getScheme())
                        ? new FileInputStream(new File(uri.getPath())) : getContentResolver().openInputStream(uri);
                if (in == null) throw new java.io.IOException("Could not open backup");
                try (java.io.InputStream input = in) { readLauncherBackup(input); }
            } catch (Exception ignored) {
                runOnUiThread(() -> Toast.makeText(this, Strings.t("Could not restore this backup"), Toast.LENGTH_LONG).show());
            }
        }, "detox-backup-read").start();
    }

    /** Only restore an unambiguous displaced original when its destination is absent. */
    private void recoverInterruptedRestoreMedia() {
        File directory = getFilesDir();
        File[] entries = directory.listFiles();
        if (entries == null) return;
        for (String name : new String[]{"selected_wallpaper.jpg", "video-wallpaper-transfer.mp4"}) {
            File destination = new File(directory, name);
            File candidate = null;
            int count = 0;
            for (File entry : entries) {
                if (entry.isFile() && RestoreRecoveryPolicy.isRecoveryCopy(name, entry.getName())) {
                    candidate = entry;
                    count++;
                }
            }
            if (!RestoreRecoveryPolicy.shouldRecover(destination.exists(), count)) {
                if (!destination.exists() && count > 1)
                    android.util.Log.w("DetoxBackup", "Multiple recovery copies; manual review needed for " + name);
                continue;
            }
            if (candidate == null || !candidate.renameTo(destination))
                android.util.Log.e("DetoxBackup", "Could not recover original media: " + name);
            else android.util.Log.i("DetoxBackup", "Recovered interrupted restore media: " + name);
        }
    }

    private void cleanupStaleRestoreScratch() {
        long now = System.currentTimeMillis();
        cleanupStaleRestoreDirectory(getCacheDir(), true, now);
        cleanupStaleRestoreDirectory(getFilesDir(), false, now);
    }

    private void cleanupStaleRestoreDirectory(File directory, boolean cacheDirectory, long now) {
        File[] candidates = directory.listFiles();
        if (candidates == null) return;
        for (File file : candidates) {
            if (!file.isFile()) continue;
            if (RestoreTempCleanupPolicy.shouldDelete(file.getName(), cacheDirectory,
                    file.lastModified(), now) && !file.delete()) {
                android.util.Log.w("DetoxBackup", "Could not remove stale restore scratch: " + file.getName());
            }
        }
    }

    private void readLauncherBackup(java.io.InputStream input) {
        File stagedWallpaper = new File(getCacheDir(), "restore-wallpaper-" + SystemClock.uptimeMillis() + ".tmp");
        File stagedVideo = new File(getCacheDir(), "restore-video-" + SystemClock.uptimeMillis() + ".tmp");
        List<RestoreFileSwap> fileSwaps = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        Map<String, ?> previousSettings = new HashMap<>(prefs.getAll());
        boolean settingsCommitAttempted = false;
        boolean hasWallpaper = false, hasVideo = false;
        try {
            org.json.JSONObject document = null;
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                BackupEntryPolicy entryPolicy = new BackupEntryPolicy();
                while ((entry = zip.getNextEntry()) != null) {
                    entryPolicy.accept(entry.getName(), entry.isDirectory());
                    if ("settings.json".equals(entry.getName()) || "backup.json".equals(entry.getName())) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        copyBackupBytes(zip, bytes, 2L * 1024L * 1024L);
                        document = new org.json.JSONObject(new String(bytes.toByteArray(), "UTF-8"));
                    } else if ("wallpaper.jpg".equals(entry.getName())) {
                        try (FileOutputStream out = new FileOutputStream(stagedWallpaper)) {
                            copyBackupBytes(zip, out, 128L * 1024L * 1024L);
                        }
                        hasWallpaper = true;
                    } else if ("video-wallpaper.bin".equals(entry.getName())) {
                        try (FileOutputStream out = new FileOutputStream(stagedVideo)) {
                            copyBackupBytes(zip, out, 1024L * 1024L * 1024L);
                        }
                        hasVideo = true;
                    }
                    zip.closeEntry();
                }
            }
            if (document == null) throw new java.io.IOException("Backup has no settings");
            LauncherBackupCodec.version(document,getPackageName()); // Validate backup version and app identity.
            org.json.JSONObject settings = document.optJSONObject("settings");
            if (settings == null) settings = document; // Accept pre-manifest backups from older releases.
            String requestedBuiltInVideo = settings.optString("liveWallpaperVideoAsset", "");
            boolean hasBuiltInVideo = BuiltInLiveVideoCatalog.containsAsset(requestedBuiltInVideo);

            File restoredVideo = new File(getFilesDir(), "video-wallpaper-transfer.mp4");
            if (hasWallpaper) fileSwaps.add(prepareRestoreFileSwap(stagedWallpaper,
                    new File(getFilesDir(), "selected_wallpaper.jpg")));
            if (hasVideo) fileSwaps.add(prepareRestoreFileSwap(stagedVideo, restoredVideo));

            SharedPreferences.Editor editor = prefs.edit().clear();
            Map<String,Object> decodedSettings=LauncherProfileCodec.decode(settings,prefs.getAll(),false);
            for(Map.Entry<String,Object> item:decodedSettings.entrySet()){
                String key=item.getKey();if(key.startsWith("_")||key.equals("photoFile")||key.equals("photoUri")
                        ||key.equals("liveWallpaperVideoPath")||key.equals("liveWallpaperVideoUri")
                        ||key.equals("liveWallpaperVideoAsset")||key.equals("liveWallpaperVideoName"))continue;
                putLauncherPreference(editor,key,item.getValue());
            }
            if (hasWallpaper) editor.putString("photoFile", new File(getFilesDir(), "selected_wallpaper.jpg").getAbsolutePath())
                    .remove("photoUri");
            else if (WALLPAPER_NAMES[PHOTO].equals(settings.optString("wallpaper")))
                editor.remove("photoFile").remove("photoUri").remove("wallpaper");
            if (hasVideo) editor.putString("liveWallpaperVideoPath", restoredVideo.getAbsolutePath())
                    .putString("liveWallpaperVideoUri", Uri.fromFile(restoredVideo).toString())
                    .remove("liveWallpaperVideoAsset").remove("liveWallpaperVideoName");
            else if (hasBuiltInVideo) editor.remove("liveWallpaperVideoPath").remove("liveWallpaperVideoUri")
                    .putString("liveWallpaperVideoAsset", requestedBuiltInVideo)
                    .putString("liveWallpaperVideoName", BuiltInLiveVideoCatalog.nameForAsset(requestedBuiltInVideo))
                    .putBoolean("liveWallpaperEnabled", settings.optBoolean("liveWallpaperEnabled", false));
            else editor.remove("liveWallpaperVideoPath").remove("liveWallpaperVideoUri")
                    .remove("liveWallpaperVideoAsset").remove("liveWallpaperVideoName")
                    .putBoolean("liveWallpaperEnabled", false);
            for (RestoreFileSwap swap : fileSwaps) swap.install();
            settingsCommitAttempted = true;
            if (!editor.commit()) throw new java.io.IOException("Could not apply backup settings");
            for (RestoreFileSwap swap : fileSwaps) swap.finish();
            runOnUiThread(() -> {
                Toast.makeText(this, Strings.t("Launcher backup restored"), Toast.LENGTH_LONG).show();
                recreate();
            });
        } catch (Exception e) {
            // Restore media before preferences so a failed rollback does not leave
            // preferences pointing at an incompletely restored media file.
            for (int i = fileSwaps.size() - 1; i >= 0; i--) fileSwaps.get(i).rollback();
            if (settingsCommitAttempted && !restoreLauncherPreferences(prefs, previousSettings))
                android.util.Log.e("DetoxBackup", "Unable to persist previous settings after failed restore");
            runOnUiThread(() -> Toast.makeText(this, Strings.t("Could not restore this backup"), Toast.LENGTH_LONG).show());
        } finally {
            for (RestoreFileSwap swap : fileSwaps) swap.cleanup();
            if (stagedWallpaper.exists()) stagedWallpaper.delete();
            if (stagedVideo.exists()) stagedVideo.delete();
        }
    }

    private boolean restoreLauncherPreferences(SharedPreferences prefs, Map<String, ?> values) {
        SharedPreferences.Editor editor = prefs.edit().clear();
        for (Map.Entry<String, ?> entry : values.entrySet())putLauncherPreference(editor,entry.getKey(),entry.getValue());
        // A synchronous commit lets us detect a failed rollback write rather than
        // reporting that the old settings were safely persisted when they were not.
        return editor.commit();
    }

    private void putLauncherPreference(SharedPreferences.Editor editor,String key,Object value){
            if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
            else if (value instanceof Set) {
                Set<String> strings = new HashSet<>();
                boolean valid = true;
                for (Object item : (Set<?>) value) {
                    if (!(item instanceof String)) { valid = false; break; }
                    strings.add((String) item);
                }
                if (valid) editor.putStringSet(key, strings);
            }
    }

    private RestoreFileSwap prepareRestoreFileSwap(File staged, File destination) throws Exception {
        if (!staged.isFile() || staged.length() <= 0)
            throw new java.io.IOException("Backup item is empty");
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
            throw new java.io.IOException("Could not prepare restore directory");
        String suffix = ".restore-" + SystemClock.uptimeMillis() + "-" + System.nanoTime();
        File replacement = new File(parent, destination.getName() + suffix + ".tmp");
        File previous = new File(parent, destination.getName() + suffix + ".previous");
        if (!RestoreStoragePolicy.hasRoom(staged.length(), parent.getUsableSpace()))
            throw new java.io.IOException("Not enough free storage to safely restore media");
        try (InputStream in = new FileInputStream(staged); FileOutputStream out = new FileOutputStream(replacement)) {
            copyBackupBytes(in, out, 1024L * 1024L * 1024L);
            out.getFD().sync();
        } catch (Exception error) {
            replacement.delete();
            throw error;
        }
        return new RestoreFileSwap(destination, replacement, previous);
    }

    /** A failed multi-file restore must leave the old wallpaper/video files intact. */
    private final class RestoreFileSwap {
        final File destination, replacement, previous;
        boolean previousSaved, replacementInstalled;

        RestoreFileSwap(File destination, File replacement, File previous) {
            this.destination = destination;
            this.replacement = replacement;
            this.previous = previous;
        }

        void install() throws java.io.IOException {
            if (destination.exists()) {
                if (!destination.renameTo(previous))
                    throw new java.io.IOException("Could not preserve existing wallpaper data");
                previousSaved = true;
            }
            if (!replacement.renameTo(destination)) {
                if (previousSaved) {
                    if (previous.renameTo(destination)) previousSaved = false;
                }
                throw new java.io.IOException("Could not install backup media");
            }
            replacementInstalled = true;
        }

        void rollback() {
            if (previousSaved && previous.exists()) {
                // Never discard the only surviving copy of the original file.
                // Move the newly installed media aside before restoring the old one.
                File failedReplacement = new File(destination.getParentFile(),
                        destination.getName() + ".failed-restore-" + System.nanoTime());
                boolean movedNew = !destination.exists() || destination.renameTo(failedReplacement);
                if (movedNew && previous.renameTo(destination)) {
                    previousSaved = false;
                    if (failedReplacement.exists()) failedReplacement.delete();
                } else {
                    // Retain .previous on disk if the filesystem refuses restoration.
                    if (failedReplacement.exists() && !destination.exists())
                        failedReplacement.renameTo(destination);
                    android.util.Log.e("DetoxBackup", "Restore rollback retained original media at " + previous);
                }
            } else if (replacementInstalled && destination.exists() && !destination.delete()) {
                android.util.Log.e("DetoxBackup", "Could not remove newly installed restore media: " + destination);
            }
            replacementInstalled = false;
        }

        void finish() {
            if (previousSaved) previous.delete();
            previousSaved = false;
        }

        void cleanup() {
            if (replacement.exists()) replacement.delete();
            if (previousSaved && previous.exists() && !destination.exists()) previous.renameTo(destination);
        }
    }

    private void showAppSearch() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        TreeMap<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            CharSequence label = ri.loadLabel(pm);
            byLabel.put(label == null ? pkg : label.toString(), pkg);
        }
        List<String> labels = new ArrayList<>(byLabel.keySet());
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(28), px(24), px(28), px(24));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPx(px(30)));
        MenuTypography.recordScaledText(input,px(30));
        input.setHint(Strings.t("Search apps"));
        input.setHintTextColor(0x99FFFFFF);
        box.addView(input, new LinearLayout.LayoutParams(-1, px(72)));
        ListView list = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(labels));
        list.setAdapter(adapter);
        box.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        AlertDialog dialog = detoxAlertBuilder().setTitle(Strings.t("Search apps")).setView(box).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            String label = adapter.getItem(position);
            String pkg = byLabel.get(label);
            dialog.dismiss();
            if (pkg != null) open(pkg);
        });
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.clear();
                for (String label : labels) if (label.toLowerCase(java.util.Locale.ROOT)
                        .contains(s.toString().toLowerCase(java.util.Locale.ROOT))) adapter.add(label);
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });
        dialog.show(); CompanionInput.dialog(dialog);
        dialog.getWindow().setLayout(px(820), px(760));
        input.requestFocus();
    }

    private void showWallpaperSlideshowOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int saved = slideshowSeconds();
        List<String> photos=getWallpaperSlideshowPhotos();
        boolean enabled=prefs.getBoolean("wallpaperSlideshowPhotosEnabled",false)&&!photos.isEmpty();
        String slideshowState=!enabled?"Off":saved<=0?"Paused":"On";
        List<SettingsChoice> rows=Arrays.asList(
            new SettingsChoice("enabled","Slideshow · "+slideshowState),
            new SettingsChoice("import","Choose up to 10 photos · "+photos.size()+"/10"),
            new SettingsChoice("interval","Interval · "+slideshowDurationLabel(saved)),
            new SettingsChoice("shuffle","Shuffle · "+(prefs.getBoolean("wallpaperSlideshowShuffle",false)?"On":"Off")),
            new SettingsChoice("clear","Clear selected photos"),new SettingsChoice("frame","Adjust current photo framing"),new SettingsChoice("preview","Preview next wallpaper"));
        routeNextDialogBack(()->returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER));
        panel(Strings.t("Wallpaper slideshow"), settingsChoiceLabels(rows), null, 0, 0, null, which -> {
            String action=rows.get(which).id;
            if ("enabled".equals(action)) {
                if(photos.isEmpty()){openWallpaperSlideshowPhotoPicker();return;}
                boolean turnOn=!enabled||saved<=0;
                SharedPreferences.Editor edit=prefs.edit().putBoolean("wallpaperSlideshowPhotosEnabled",turnOn);
                if(turnOn){
                    int index=WallpaperSlideshowPolicy.currentIndex(prefs.getInt("wallpaperSlideshowPhotoIndex",0),photos.size());
                    edit.putInt("wallpaperSlideshowSeconds",WallpaperSlideshowPolicy.activationSeconds(saved))
                            .remove("wallpaperSlideshowMinutes")
                            .putInt("wallpaperSlideshowPhotoIndex",index)
                            .putString("photoFile",photos.get(index)).putString("wallpaper",WALLPAPER_NAMES[PHOTO])
                            .putBoolean("liveWallpaperEnabled",false);
                }
                edit.apply();
                if(turnOn){configureWallpaperWindow();showWallpaper(PHOTO);}
                scheduleWallpaperSlideshow();showWallpaperSlideshowOptions();
            } else if ("import".equals(action)) openWallpaperSlideshowPhotoPicker();
            else if ("interval".equals(action)) {
                String[] labels = {"Off", "Every 30 seconds", "Every minute", "Every 2 minutes", "Every 5 minutes",
                        "Every 10 minutes", "Every 15 minutes", "Every 30 minutes", "Every hour"};
                int[] seconds = {0, 30, 60, 120, 300, 600, 900, 1800, 3600};
                int selected = 0;
                for (int i = 0; i < seconds.length; i++) if (seconds[i] == saved) selected = i;
                routeNextDialogBack(this::showWallpaperSlideshowOptions);
                panel(Strings.t("Slideshow interval"), translated(labels), null, 0, selected, null, choice -> {
                    prefs.edit().putInt("wallpaperSlideshowSeconds", seconds[choice])
                            .remove("wallpaperSlideshowMinutes").apply();
                    scheduleWallpaperSlideshow();
                    showWallpaperSlideshowOptions();
                });
            } else if ("shuffle".equals(action)) {
                boolean shuffle = prefs.getBoolean("wallpaperSlideshowShuffle", false);
                routeNextDialogBack(this::showWallpaperSlideshowOptions);
                panel(Strings.t("Shuffle wallpapers"), translated(new String[] {"Off", "On"}), null, 0,
                        shuffle ? 1 : 0, null, choice -> {
                    prefs.edit().putBoolean("wallpaperSlideshowShuffle", choice == 1).apply();
                    showWallpaperSlideshowOptions();
                });
            } else if ("clear".equals(action)) {
                for (String path : getWallpaperSlideshowPhotos()) {
                    File f = new File(path);
                    if (f.getName().startsWith("wallpaper_slideshow_")) f.delete();
                }
                prefs.edit().remove("wallpaperSlideshowPhotos").remove("wallpaperSlideshowPhotoIndex")
                        .putBoolean("wallpaperSlideshowPhotosEnabled", false).apply();
                scheduleWallpaperSlideshow();
                showWallpaperSlideshowOptions();
            } else if("frame".equals(action))showSlideshowPhotoFraming();
            else previewNextSlideshowWallpaper();
        });
    }

    private void showSlideshowPhotoFraming(){
        List<String> photos=getWallpaperSlideshowPhotos();
        if(photos.isEmpty()){openWallpaperSlideshowPhotoPicker();return;}
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        int index=Math.max(0,Math.min(photos.size()-1,prefs.getInt("wallpaperSlideshowPhotoIndex",0)));
        File target=new File(photos.get(index));
        final int request=++wallpaperPhotoImportGeneration;
        Toast.makeText(this,Strings.t("Preparing slideshow photo…"),Toast.LENGTH_SHORT).show();
        try {
            photoImportWorker.execute(()->{
                Bitmap source=null;
                try { source=decodePhotoForCrop(Uri.fromFile(target),1920,1080); }
                catch(Exception error){DetoxDiagnostics.recordFailure("DetoxWallpaper","Slideshow framing","could not decode slideshow photo",error);}
                final Bitmap result=source;
                wallpaperDecodeHandler.post(()->{
                    if(request!=wallpaperPhotoImportGeneration||isFinishing()||isDestroyed()){
                        if(result!=null&&!result.isRecycled())result.recycle();
                        return;
                    }
                    if(result==null){Toast.makeText(this,Strings.t("Could not open slideshow photo"),Toast.LENGTH_SHORT).show();return;}
                    showSlideshowPhotoFraming(target,index,result);
                });
            });
        } catch(java.util.concurrent.RejectedExecutionException closed) {
            Toast.makeText(this,Strings.t("Photo preview is unavailable right now"),Toast.LENGTH_SHORT).show();
        }
    }
    private void showSlideshowPhotoFraming(File target,int index,Bitmap source){
        cropDialog=new Dialog(this,android.R.style.Theme_Material_NoActionBar);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setGravity(Gravity.CENTER_HORIZONTAL);page.setPadding(px(48),px(24),px(48),px(24));page.setBackgroundColor(0xFF101218);
        TextView title=text(px(38),Color.WHITE,semibold);title.setText(Strings.t("Frame slideshow photo"));title.setGravity(Gravity.CENTER);page.addView(title,new LinearLayout.LayoutParams(-1,px(64)));
        CropPreview preview=new CropPreview(this,source);preview.setId(View.generateViewId());preview.setFocusable(true);preview.setFocusableInTouchMode(true);preview.setBackgroundColor(0xFF050608);
        LinearLayout.LayoutParams previewLp=new LinearLayout.LayoutParams(-1,0,1f);previewLp.setMargins(px(18),px(16),px(18),px(10));page.addView(preview,previewLp);
        TextView help=text(px(22),0xFFE2E6EF,regular);help.setText(Strings.t("Use arrows to pan. Zoom in, frame the subject, then save."));help.setGravity(Gravity.CENTER);page.addView(help,new LinearLayout.LayoutParams(-1,px(54)));
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);String[] labels={"Zoom out","Zoom in","Save framing","Cancel"};TextView[] buttons=new TextView[labels.length];
        for(int i=0;i<labels.length;i++){final int action=i;TextView button=text(px(24),Color.WHITE,semibold);button.setText(Strings.t(labels[i]));button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setId(View.generateViewId());button.setPadding(px(24),0,px(24),0);GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(px(18));bg.setColor(0xFF282D38);button.setBackground(bg);button.setOnFocusChangeListener((v,focused)->bg.setColor(focused?0xFF43536D:0xFF282D38));button.setOnClickListener(v->{if(action==0)preview.zoom(.85f);else if(action==1)preview.zoom(1.18f);else if(action==2)saveSlideshowPhotoFraming(preview,target,index);else animateDialogOut(cropDialog,this::showWallpaperSlideshowOptions,true);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,px(64));lp.setMargins(px(8),px(4),px(8),px(4));controls.addView(button,lp);buttons[i]=button;}
        preview.setControlsTarget(buttons[0]);preview.setNextFocusDownId(buttons[0].getId());buttons[0].setNextFocusUpId(preview.getId());page.addView(controls,new LinearLayout.LayoutParams(-1,px(82)));cropDialog.setContentView(page);
        cropDialog.setOnCancelListener(d->{if(!source.isRecycled())source.recycle();});cropDialog.setOnDismissListener(d->{if(!source.isRecycled())source.recycle();});
        setAnimatedBack(cropDialog,this::showWallpaperSlideshowOptions);
        cropDialog.show();Window window=cropDialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setLayout(-1,-1);}preview.requestFocus();
    }
    private void saveSlideshowPhotoFraming(CropPreview preview,File original,int index){
        File framed=new File(original.getParentFile(),"wallpaper_slideshow_framed_"+System.currentTimeMillis()+".jpg");
        try{
            Bitmap output=preview.createCrop(1920,1080);
            try(FileOutputStream stream=new FileOutputStream(framed)){if(!output.compress(Bitmap.CompressFormat.JPEG,94,stream))throw new java.io.IOException("Could not save frame");stream.flush();}
            finally{output.recycle();}
            List<String> photos=new ArrayList<>(getWallpaperSlideshowPhotos());if(index>=photos.size())throw new java.io.IOException("Photo list changed");
            photos.set(index,framed.getAbsolutePath());StringBuilder saved=new StringBuilder();for(String path:photos){if(saved.length()>0)saved.append('\n');saved.append(path);}
            SharedPreferences prefs=getSharedPreferences(PREFS,0);boolean current=prefs.getInt("wallpaperSlideshowPhotoIndex",0)==index&&wallpaper()==PHOTO;
            SharedPreferences.Editor edit=prefs.edit().putString("wallpaperSlideshowPhotos",saved.toString());if(current)edit.putString("photoFile",framed.getAbsolutePath());edit.apply();
            if(original.exists())original.delete();if(current)showWallpaper(PHOTO);scheduleWallpaperSlideshow();dismissDialogAnimated(cropDialog,()->showWallpaperSlideshowOptions());
            Toast.makeText(this,Strings.t("Photo framing saved"),Toast.LENGTH_SHORT).show();
        }catch(Exception error){if(framed.exists())framed.delete();Toast.makeText(this,Strings.t("Could not save photo framing"),Toast.LENGTH_LONG).show();}
    }

    private List<String> getWallpaperSlideshowPhotos() {
        String saved = getSharedPreferences(PREFS, 0).getString("wallpaperSlideshowPhotos", "");
        List<String> photos = new ArrayList<>();
        if (saved.isEmpty()) return photos;
        for (String path : saved.split("\\n")) {
            if (photos.size() == 10) break;
            if (!path.isEmpty() && new File(path).isFile()) photos.add(path);
        }
        return photos;
    }

    private void openWallpaperSlideshowPhotoPicker() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            Toast.makeText(this, "Select up to 10 photos. Use multi-select, then choose Open.", Toast.LENGTH_LONG).show();
            startActivityForResult(pick, PICK_SLIDESHOW_PHOTOS);
        }
        catch (Exception e) { showPhoneCompanion(); }
    }

    private void importWallpaperSlideshowPhotos(Intent data) {
        List<Uri> uris = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount() && uris.size() < 10; i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null && !uris.contains(uri)) uris.add(uri);
            }
        } else if (data.getData() != null) uris.add(data.getData());
        if (uris.isEmpty()) {
            Toast.makeText(this, "No photos were selected", Toast.LENGTH_LONG).show();
            return;
        }
        int access = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        for (Uri uri : uris) {
            try { if (access != 0) getContentResolver().takePersistableUriPermission(uri, access); }
            catch (Exception ignored) { /* The import copies each item immediately while its picker grant is valid. */ }
        }
        final int request = ++wallpaperPhotoImportGeneration;
        final List<Uri> selected = new ArrayList<>(uris);
        Toast.makeText(this, "Importing " + selected.size() + " slideshow photos…", Toast.LENGTH_SHORT).show();
        try {
            photoImportWorker.execute(() -> {
                File dir = new File(getFilesDir(), "wallpaper_slideshow_" + System.currentTimeMillis());
                List<String> paths = new ArrayList<>();
                try {
                    if (!dir.mkdirs()) throw new java.io.IOException("Could not create slideshow folder");
                    for (int i = 0; i < selected.size(); i++) {
                        if (request != wallpaperPhotoImportGeneration) { deleteWallpaperImportDirectory(dir); return; }
                        File output = new File(dir, "wallpaper_slideshow_" + i + ".jpg");
                        copyWallpaperPhoto(selected.get(i), output);
                        paths.add(output.getAbsolutePath());
                    }
                    final List<String> importedPaths = new ArrayList<>(paths);
                    wallpaperDecodeHandler.post(() -> {
                        if (request != wallpaperPhotoImportGeneration || isFinishing() || isDestroyed()) {
                            deleteWallpaperImportDirectory(dir);
                            return;
                        }
                        StringBuilder stored = new StringBuilder();
                        for (String path : importedPaths) {
                            if (stored.length() > 0) stored.append('\n');
                            stored.append(path);
                        }
                        int seconds = slideshowSeconds();
                        int duration = seconds <= 0 ? 300 : seconds;
                        getSharedPreferences(PREFS, 0).edit().putString("wallpaperSlideshowPhotos", stored.toString())
                                .putInt("wallpaperSlideshowPhotoIndex", 0).putBoolean("wallpaperSlideshowPhotosEnabled", true)
                                .putString("photoFile", importedPaths.get(0)).putString("wallpaper", WALLPAPER_NAMES[PHOTO])
                                .putInt("wallpaperSlideshowSeconds", duration).remove("wallpaperSlideshowMinutes")
                                .putBoolean("liveWallpaperEnabled", false).apply();
                        configureWallpaperWindow();
                        showWallpaper(PHOTO);
                        scheduleWallpaperSlideshow();
                        Toast.makeText(this, importedPaths.size() + " photos added. Slideshow changes "
                                + slideshowDurationLabel(duration) + ".", Toast.LENGTH_LONG).show();
                        showWallpaperSlideshowOptions();
                    });
                } catch (Exception error) {
                    deleteWallpaperImportDirectory(dir);
                    wallpaperDecodeHandler.post(() -> {
                        if (request == wallpaperPhotoImportGeneration && !isFinishing() && !isDestroyed())
                            Toast.makeText(this, "Could not load the selected photos", Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            Toast.makeText(this, "Photo import is unavailable right now", Toast.LENGTH_LONG).show();
        }
    }

    private static void deleteWallpaperImportDirectory(File dir) {
        File[] children = dir == null ? null : dir.listFiles();
        if (children != null) for (File child : children) child.delete();
        if (dir != null) dir.delete();
    }

    private void copyWallpaperPhoto(Uri uri, File target) throws Exception {
        int orientation = readPhotoOrientation(uri);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new java.io.IOException("Photo could not be opened");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new java.io.IOException("Unsupported photo");
        int sample = 1;
        while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1920) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new java.io.IOException("Photo could not be opened");
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new java.io.IOException("Unsupported photo");
        Bitmap oriented = bitmap;
        Bitmap output = bitmap;
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
            oriented = orientPhotoBitmap(bitmap, orientation);
            output = fitWallpaperBitmap(oriented);
            if (!output.compress(Bitmap.CompressFormat.JPEG, 92, out)) throw new java.io.IOException("Photo save failed");
            out.flush();
        } finally {
            if (output != bitmap && output != oriented && !output.isRecycled()) output.recycle();
            if (oriented != bitmap && !oriented.isRecycled()) oriented.recycle();
            if (!bitmap.isRecycled()) bitmap.recycle();
        }
    }

    private int nextRandomIndex(int current, int count) {
        if (count < 2) return current;
        int next = new Random().nextInt(count - 1);
        return next >= current ? next + 1 : next;
    }

    private int nextRandomWallpaper(int current) {
        if (WALLPAPER_NAMES.length < 2) return current;
        int next = new Random().nextInt(WALLPAPER_NAMES.length - 1);
        return next >= current ? next + 1 : next;
    }

    private void previewNextSlideshowWallpaper() {
        List<String> photos=getWallpaperSlideshowPhotos();
        if(!photos.isEmpty()){
            SharedPreferences prefs=getSharedPreferences(PREFS,0);
            int current=Math.max(0,Math.min(photos.size()-1,prefs.getInt("wallpaperSlideshowPhotoIndex",0)));
            int next=prefs.getBoolean("wallpaperSlideshowShuffle",false)&&photos.size()>1
                    ?nextRandomIndex(current,photos.size()):(current+1)%photos.size();
            final int request=++wallpaperDecodeGeneration;
            final String nextPhoto=photos.get(next);
            try {
                wallpaperDecodeWorker.execute(()->{
                    if(request!=wallpaperDecodeGeneration)return;
                    Drawable previewDrawable=wallpaperAssets.previewPhoto(nextPhoto);
                    wallpaperDecodeHandler.post(()->{
                        if(request!=wallpaperDecodeGeneration||isFinishing()||isDestroyed())return;
                        showNextSlideshowPhotoPreview(prefs,next,nextPhoto,previewDrawable);
                    });
                });
            } catch (java.util.concurrent.RejectedExecutionException ignored) { }
            return;
        }
        int current = wallpaper();
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int next = prefs.getBoolean("wallpaperSlideshowShuffle", false)
                ? nextRandomWallpaper(current) : (current + 1) % WALLPAPER_NAMES.length;
        showWallpaper(next);
        routeNextDialogBack(this::showWallpaperSlideshowOptions);
        detoxAlertBuilder().setTitle(Strings.t("Next wallpaper"))
                .setMessage(Strings.t(WALLPAPER_NAMES[next]))
                .setPositiveButton(Strings.t("Use now"), (d, w) -> {
                    prefs.edit().putString("wallpaper", WALLPAPER_NAMES[next]).apply();
                    showWallpaper(next);
                })
                .setNegativeButton(Strings.t("Keep current"), (d, w) -> showWallpaper(current))
                .setOnCancelListener(d -> showWallpaper(current)).show();
    }

    private void showNextSlideshowPhotoPreview(SharedPreferences prefs,int next,String nextPhoto,Drawable previewDrawable){
        ImageView preview=new ImageView(this);preview.setImageDrawable(new FramedWallpaper(previewDrawable,prefs));preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setContentDescription(Strings.t("Next slideshow photo"));
        LinearLayout frame=new LinearLayout(this);frame.setGravity(Gravity.CENTER);frame.setPadding(px(12),px(8),px(12),px(8));
        frame.addView(preview,new LinearLayout.LayoutParams(px(960),px(540)));
        routeNextDialogBack(this::showWallpaperSlideshowOptions);
        detoxAlertBuilder().setTitle(Strings.t("Next slideshow photo")).setView(frame)
                .setPositiveButton(Strings.t("Use now"),(d,w)->{
                    prefs.edit().putString("photoFile",nextPhoto).putInt("wallpaperSlideshowPhotoIndex",next)
                            .putBoolean("wallpaperSlideshowPhotosEnabled",true).putString("wallpaper",WALLPAPER_NAMES[PHOTO])
                            .putBoolean("liveWallpaperEnabled",false).apply();
                    configureWallpaperWindow();showWallpaper(PHOTO);scheduleWallpaperSlideshow();showWallpaperSlideshowOptions();
                }).setNegativeButton(Strings.t("Keep current"),null).show();
    }

    private void showClockOptions() {
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        int color=prefs.getInt("clockColor",0);
        if(color==WALLPAPER_MATCH_COLOR)color=wallpaperAccentColor();
        color=SettingsPreferencePolicy.clockAccent(color,uiAccentColor(),RAINBOW_COLOR);
        ClockStylePreview.show(this,detoxAlertBuilder(),prefs.getString("clockStyle","digital"),color,
                fontTypeface(prefs.getString("font","nunito"),Typeface.NORMAL),launcherAnimationMode(),
                prefs.getInt("detoxPerformancePreset",GamingPerformancePolicy.DEFAULT),style->{
            prefs.edit().putString("clockStyle",style).apply();
            previewClockStyleOverride=null;applyClockStyle();buildTiles();
            showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        },()->showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
    }

    private void showClockDateOptions() {
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        panel("Date Display",Arrays.asList("Show date","Hide date"),null,0,prefs.getBoolean("clockDateVisible",true)?0:1,null,i->{
            prefs.edit().putBoolean("clockDateVisible",i==0).apply();applyClockStyle();showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        });
    }

    private void showClockDisplayOptions() {
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        String face=effectiveClockStyle(prefs);
        ArrayList<SettingsChoice> choices=new ArrayList<>();
        boolean modern=ClockFacePolicy.premium(face),digital="digital".equals(face);
        if(modern||digital){
            choices.add(new SettingsChoice("seconds","Seconds · "+(prefs.getBoolean("clockSecondsVisible",false)?"On":"Off")));
            if(!ClockFacePolicy.analog(face))choices.add(new SettingsChoice("format","Time format · "+prefs.getString("clockTimeFormat","system")));
        }
        if(ClockFacePolicy.accent(face))choices.add(new SettingsChoice("accent","Accent color"));
        if(ClockFacePolicy.glow(face))choices.add(new SettingsChoice("glow","Glow intensity"));
        if(ClockFacePolicy.animated(face)&&(!ClockFacePolicy.analog(face)||prefs.getBoolean("clockSecondsVisible",false)))choices.add(new SettingsChoice("motion","Animation intensity"));
        if(ClockFacePolicy.background(face))choices.add(new SettingsChoice("background","Background opacity"));
        // Existing Orbit/Words/World faces keep their own established mechanics, without inert controls.
        if(choices.isEmpty()){showClockOptions();return;}
        String[] labels=new String[choices.size()];for(int i=0;i<labels.length;i++)labels[i]=choices.get(i).label;
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        panel("Clock Display & Effects",Arrays.asList(labels),null,0,0,null,i->{
            String id=choices.get(i).id;
            if("seconds".equals(id)){prefs.edit().putBoolean("clockSecondsVisible",!prefs.getBoolean("clockSecondsVisible",false)).apply();applyClockStyle();showClockDisplayOptions();return;}
            routeNextDialogBack(this::showClockDisplayOptions);
            if("format".equals(id)){
                String[] values={"system","12","24"};
                panel("Time format",Arrays.asList("Follow system","12-hour · AM/PM","24-hour"),null,0,Math.max(0,Arrays.asList(values).indexOf(prefs.getString("clockTimeFormat","system"))),null,n->{prefs.edit().putString("clockTimeFormat",values[n]).apply();applyClockStyle();showClockDisplayOptions();});
            }else if("accent".equals(id)){
                int[] values={0,0xFF88E6EC,0xFFB6A5EE,0xFFF3B6D4,0xFFF3D19B,0xFFF4F5FA};int selected=0;
                for(int n=0;n<values.length;n++)if(values[n]==prefs.getInt("clockAccentColor",0))selected=n;
                panel("Clock accent",Arrays.asList("Match clock color","Ice cyan","Soft violet","Rose","Warm amber","Pearl"),null,0,selected,null,n->{prefs.edit().putInt("clockAccentColor",values[n]).apply();applyClockStyle();showClockDisplayOptions();});
            }else if("motion".equals(id)&&ClockFacePolicy.analog(face)){
                panel("Second hand movement",Arrays.asList("Stepped","Smooth sweep"),null,0,prefs.getInt("clockAnimationPercent",55)>0?1:0,null,n->{prefs.edit().putInt("clockAnimationPercent",n==0?0:55).apply();applyClockStyle();showClockDisplayOptions();});
            }else{
                String key="glow".equals(id)?"clockGlowPercent":"motion".equals(id)?"clockAnimationPercent":"clockBackgroundPercent";
                int def="glow".equals(id)?45:"motion".equals(id)?55:85;
                int[] values={0,25,45,55,75,85,100};int selected=0;for(int n=0;n<values.length;n++)if(values[n]==prefs.getInt(key,def))selected=n;
                panel("motion".equals(id)?"Animation intensity · respects Reduced Motion":"glow".equals(id)?"Glow intensity":"Background opacity",Arrays.asList("0%","25%","45%","55%","75%","85%","100%"),null,0,selected,null,n->{prefs.edit().putInt(key,values[n]).apply();applyClockStyle();showClockDisplayOptions();});
            }
        });
    }

    private void refreshNativeClock() {
        if (nativeClockFace == null) return;
        nativeClockFace.motion(launcherAnimationMode(), getSharedPreferences(PREFS, 0)
                .getInt("detoxPerformancePreset", GamingPerformancePolicy.DEFAULT));
        nativeClockFace.setActive(activityResumed && hasWindowFocus());
    }

    private void applyClockVisibility() {
        int percent = Math.max(0, Math.min(100,
                getSharedPreferences(PREFS, 0).getInt("clockVisibilityPercent", 60)));
        float alpha = percent / 100f;
        if(clockLayout!=null)clockLayout.setVisibility(percent==0?View.INVISIBLE:View.VISIBLE);
        if (dateClock != null) dateClock.setAlpha(alpha);
        if (timeClock != null) timeClock.setAlpha(alpha);
        if (nativeClockFace != null) { nativeClockFace.setAlpha(alpha); refreshNativeClock(); }
    }

    private void showClockSizeOptions() {
        int[] values = {70, 80, 90, 100, 110, 120, 130};
        String[] labels = {"70%", "80%", "90%", "100%", "110%", "120%", "130%"};
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int current = Math.max(70, Math.min(130, prefs.getInt("clockSizePercent", 100)));
        int selected = 3;
        for (int i = 0; i < values.length; i++) if (values[i] == current) selected = i;
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        panel(Strings.t("Clock size"), translated(labels), null, 0, selected, null, which -> {
            prefs.edit().putInt("clockSizePercent", values[which]).apply();
            applyClockStyle();
            buildTiles();
            showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        });
    }    private void showClockVisibilityOptions() {
        int[] values = {0, 25, 50, 60, 75, 90, 100};
        String[] labels = {"0% · Hidden", "25%", "50%", "60%", "75%", "90%", "100%"};
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int current = Math.max(0, Math.min(100, prefs.getInt("clockVisibilityPercent", 60)));
        int selected = 3;
        for (int i = 0; i < values.length; i++) if (values[i] == current) selected = i;
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        panel(Strings.t("Clock visibility"), translated(labels), null, 0, selected, null, which -> {
            prefs.edit().putInt("clockVisibilityPercent", values[which]).apply();
            applyClockVisibility();
            showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        });
    }
    private void showClockPositionOptions() {
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP));
        String saved = getSharedPreferences(PREFS, 0).getString("clockPosition", "top-left");
        int selected = Math.max(0, Arrays.asList(CLOCK_POSITION_KEYS).indexOf(saved));
        panel(Strings.t("Clock position"), translated(CLOCK_POSITION_NAMES), null, 0, selected, null, which -> {
            getSharedPreferences(PREFS, 0).edit().putString("clockPosition", CLOCK_POSITION_KEYS[which])
                    .putBoolean("clockEdgeRightV3", true).apply();
            applyClockPosition();
            showSettingsGroup(SettingsMenuRoutes.CLOCK_GROUP);
        });
    }

    private float clockSizeScale() {
        int percent = getSharedPreferences(PREFS, 0).getInt("clockSizePercent", 100);
        return Math.max(0.70f, Math.min(1.30f, percent / 100f)) * (compactHomeHeader() ? .72f : 1f);
    }

    private String effectiveClockStyle(SharedPreferences prefs) {
        String style = previewClockStyleOverride != null ? previewClockStyleOverride
                : prefs.getString("clockStyle", CLOCK_KEYS[0]);
        // Migrate retired faces without touching size, position, colours or utilities.
        style=ClockFacePolicy.normalise(style);
        if (previewClockStyleOverride == null && !style.equals(prefs.getString("clockStyle", CLOCK_KEYS[0])))
            prefs.edit().putString("clockStyle", style).apply();
        return style;
    }

    private void applyClockPosition() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (!prefs.getBoolean("clockEdgeRightV3", false)) {
            prefs.edit().putString("clockPosition", "top-right").putBoolean("clockEdgeRightV3", true).apply();
        }
        String savedPosition = prefs.getString("clockPosition", "top-left");
        // Clock position is user-controlled; utility rail placement must not silently override it.
        String position = Arrays.asList(CLOCK_POSITION_KEYS).contains(savedPosition) ? savedPosition : "top-left";
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        boolean center = position.endsWith("center");
        boolean right = position.endsWith("right");
        boolean middle = position.startsWith("middle");
        boolean bottom = position.startsWith("bottom");
        String clockStyle = effectiveClockStyle(prefs);
        float clockScale = clockSizeScale();
        int digitalWidth = Math.min(px(Math.round(620f * clockScale)), width - px(MARGIN) * 2);
        int digitalHeight = px(Math.round(190f * clockScale));
        int digitalX = center ? Math.max(0, (width - digitalWidth) / 2)
                : right ? Math.max(0, width - digitalWidth - px(24)) : 0;
        int digitalY = bottom ? Math.max(0, height - px(Math.round(300f * clockScale)))
                : middle ? Math.max(0, height / 2 - digitalHeight / 2) : 0;
        FrameLayout.LayoutParams digital = (FrameLayout.LayoutParams) clockLayout.getLayoutParams();
        digital.leftMargin = digitalX;
        digital.topMargin = digitalY;
        clockLayout.setLayoutParams(digital);
        // Center and edge-align from measured clock bounds so all faces stay aligned.
        clockLayout.post(() -> {
            if (root == null || clockLayout.getWidth() <= 0 || clockLayout.getHeight() <= 0) return;
            FrameLayout.LayoutParams actual = (FrameLayout.LayoutParams) clockLayout.getLayoutParams();
            int viewportHeight = getResources().getDisplayMetrics().heightPixels;
            actual.leftMargin = center ? Math.max(0, (root.getWidth() - clockLayout.getWidth()) / 2)
                    : right ? Math.max(0, root.getWidth() - clockLayout.getWidth() - px(24)) : 0;
            actual.topMargin = bottom ? Math.max(0, viewportHeight - clockLayout.getHeight() - px(24))
                    : middle ? Math.max(0, (viewportHeight - clockLayout.getHeight()) / 2) : 0;
            clockLayout.setLayoutParams(actual);
            positionTopActionsUnderClock();
        });
        if (root != null) root.post(() -> root.post(this::positionTopActionsUnderClock));
    }
    private void applyClockStyle() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String style = effectiveClockStyle(prefs);
        if (!Arrays.asList(CLOCK_KEYS).contains(style)) {
            style = CLOCK_KEYS[0];
        }
        if (previewClockStyleOverride == null && !style.equals(prefs.getString("clockStyle", CLOCK_KEYS[0])))
            prefs.edit().putString("clockStyle", style).apply();
        clockLayout.setVisibility(View.VISIBLE);
        dateClock.setVisibility(View.VISIBLE);
        timeClockStack.setVisibility(View.VISIBLE);
        timeClock.setVisibility(View.VISIBLE);

        PremiumClockConfig clockOptions=NativeClockFaceView.readOptions(prefs);
        String format12=clockOptions.seconds?"h:mm:ss a":"h:mm a";
        String format24=clockOptions.seconds?"HH:mm:ss":"HH:mm";
        timeClock.setFormat12Hour("24".equals(clockOptions.format)?format24:format12);
        timeClock.setFormat24Hour("12".equals(clockOptions.format)?format12:format24);
        float clockScale = clockSizeScale();
        float timeSize = fontPx(px(Math.round((clockOptions.seconds?133f:168f) * clockScale)));
        float spacing = -0.028f;
        timeClock.setLetterSpacing(spacing);
        timeClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeSize);
        dateClock.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPx(px(Math.round(30f * clockScale))));
        homeGreeting.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPx(px(Math.round(25f * clockScale))));
        homeGreeting.setTextColor(withFontColor(0xDFFFFFFF));
        homeGreeting.setTypeface(semibold, Typeface.BOLD);
        homeGreeting.getPaint().setFakeBoldText(true);
        homeGreeting.setVisibility("digital".equals(style) ? View.VISIBLE : View.GONE);
        applyFontPreview(homeGreeting);
        timeClock.setBackground(null);
        timeClock.setPadding(0, 0, 0, 0);
        timeClock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        timeClock.getPaint().setFakeBoldText(false);
        dateClock.setTypeface(regular, Typeface.BOLD);
        dateClock.getPaint().setFakeBoldText(true);
        dateClock.setGravity(Gravity.CENTER);
        timeClock.setGravity(Gravity.CENTER);

        int savedClockColor = previewClockColorOverride != null ? previewClockColorOverride
                : prefs.getInt("clockColor", 0);
        if (savedClockColor == WALLPAPER_MATCH_COLOR) savedClockColor = wallpaperAccentColor();
        int premiumClock = homeThemeIndex();
        // A manually chosen clock colour always wins over optional Home theme styling.
        int dateColor = savedClockColor == 0 ? (premiumClock > 0
                ? HomePremiumThemes.CLOCK_SECONDARY[premiumClock] : 0xFFD0DCE5)
                : savedClockColor == RAINBOW_COLOR ? Color.WHITE : savedClockColor;
        int clockAccent = SettingsPreferencePolicy.clockAccent(savedClockColor, uiAccentColor(), RAINBOW_COLOR);
        if (premiumClock > 0 && savedClockColor == 0) {
            timeClock.setTypeface(Typeface.create(HomePremiumThemes.CLOCK_FONTS[premiumClock], Typeface.NORMAL));
            dateClock.setTypeface(Typeface.create(HomePremiumThemes.CLOCK_FONTS[premiumClock], Typeface.NORMAL));
            timeClock.setLetterSpacing(HomePremiumThemes.CLOCK_SPACING[premiumClock]);
        }
        dateClock.setTextColor(dateColor);
        timeClock.setTextColor(clockAccent);
        timeClock.setAlpha(1f);
        timeClock.setShadowLayer(px(8), px(1), px(4), 0xBB000000);
        if (savedClockColor == RAINBOW_COLOR) {
            applyRainbow(timeClock);
            applyRainbow(dateClock);
        } else {
            timeClock.getPaint().setShader(null);
            dateClock.getPaint().setShader(null);
        }
        timeClock.invalidate();
        dateClock.invalidate();
        boolean customFace = !"digital".equals(style);
        timeClockStack.setVisibility(customFace ? View.GONE : View.VISIBLE);
        nativeClockFace.setVisibility(customFace ? View.VISIBLE : View.GONE);
        boolean premiumFace=ClockFacePolicy.premium(style);
        dateClock.setVisibility(premiumFace||!clockOptions.date?View.GONE:View.VISIBLE);
        int faceWidth=px(Math.round((premiumFace&&ClockFacePolicy.wide(style)?480f:360f)*clockScale));
        int faceHeight=px(Math.round((premiumFace&&ClockFacePolicy.wide(style)?280f:360f)*clockScale));
        nativeClockFace.setLayoutParams(new LinearLayout.LayoutParams(faceWidth,faceHeight));
        String faceFont = previewFontKey != null ? previewFontKey : prefs.getString("font", "nunito");
        nativeClockFace.configure(style, clockAccent, fontTypeface(faceFont, Typeface.NORMAL),
                launcherAnimationMode(), prefs.getInt("detoxPerformancePreset", GamingPerformancePolicy.DEFAULT));
        clockLayout.setContentDescription("Clock. Press OK for alarms, timers, stopwatch and world clocks");
        applyClockPosition();
        applyClockVisibility();
    }
    // Browse with debounced, reduced-resolution previews; confirmation loads the full image.
    private void showWallpapers() {
        final int size = px(40);
        List<String> choices = new ArrayList<>(Arrays.asList(WALLPAPER_NAMES));
        choices.add("Choose from internal storage…");
        Icon wallpaperIcon = new Icon() {
            @Override public Drawable at(int pos) {
                if (pos >= WALLPAPER_NAMES.length) return new ColorDrawable(Color.TRANSPARENT);
                if (pos < PHOTO) return new ColorDrawable(0xFF142030);
                int[] c = WALLPAPERS[pos];
                GradientDrawable dot = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[] {glow(c[3], c[0]), c[1], glow(c[4], c[0])});
                dot.setShape(GradientDrawable.OVAL);
                dot.setStroke(Math.max(1, px(2)), 0x26FFFFFF);
                dot.setSize(size, size);
                return dot;
            }

            @Override public void loadAsync(int pos, IconLoaded loaded) {
                if (pos < PHOTO) wallpaperAssets.thumbnailAsync(pos, wallpaperDecodeWorker,
                        wallpaperDecodeHandler, loaded::ready);
            }
        };
        panel(Strings.t("Wallpaper"), choices, wallpaperIcon, size, wallpaper(), which -> {
            if (which >= WALLPAPER_NAMES.length) return;
            if (which == wallpaper()) showWallpaper(which);
            else previewWallpaper(which);
        }, which -> {
            if (which >= WALLPAPER_NAMES.length) {
                showWallpaper(wallpaper());
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("image/*");
                pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(pick, PICK_WALLPAPER); } catch (Exception ignored) {}
                return;
            }
            getSharedPreferences(PREFS, 0).edit().remove("photoUri").remove("photoFile")
                    .putBoolean("wallpaperSlideshowPhotosEnabled", false)
                    .putBoolean("liveWallpaperEnabled", false).apply();
            configureWallpaperWindow();
            getSharedPreferences(PREFS, 0).edit().putString("wallpaper", WALLPAPER_NAMES[which]).apply();
            showWallpaper(which);
            scheduleWallpaperSlideshow();
            returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER);
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VOICE_SEARCH) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) searchForSpokenText(results.get(0));
            }
            return;
        }
        if (resultCode != RESULT_OK || data == null) {
            if (requestCode == PICK_APP_ICON) { appIconImportGeneration++; pendingCustomIconPackage = null; discardPendingAppIcon(); }
            if (requestCode == PICK_WALLPAPER) { wallpaperPhotoImportGeneration++; discardPendingWallpaper(); }
            if (requestCode == PICK_SLIDESHOW_PHOTOS) wallpaperPhotoImportGeneration++;
            if (requestCode == PICK_SLIDESHOW_PHOTOS) showWallpaperSlideshowOptions();
            else if (requestCode == PICK_VIDEO_WALLPAPER) showLiveWallpaperOptions();
            else if (requestCode == PICK_WALLPAPER) returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER);
            return;
        }
        if (requestCode == EXPORT_HOME_PROFILES) {
            try { if(data.getData()==null)throw new java.io.IOException("No export destination was selected"); DetoxHomeProfiles.exportTo(this, data.getData()); Toast.makeText(this, "Profiles exported", Toast.LENGTH_SHORT).show(); }
            catch (Exception error) { showProfileError("Could not export profiles", error); }
            return;
        }
        if (requestCode == IMPORT_HOME_PROFILES) {
            try { if(data.getData()==null)throw new java.io.IOException("No profile file was selected"); int count = DetoxHomeProfiles.importFrom(this, data.getData()); Toast.makeText(this, "Imported " + count + " profile(s)", Toast.LENGTH_LONG).show(); showHomeProfiles(); }
            catch (Exception error) { showProfileError("Could not import profiles", error); }
            return;
        }
        if (requestCode == PICK_SLIDESHOW_PHOTOS) { importWallpaperSlideshowPhotos(data); return; }
        if (data.getData() == null) {
            if (requestCode == PICK_APP_ICON) { appIconImportGeneration++; pendingCustomIconPackage = null; discardPendingAppIcon(); }
            if (requestCode == PICK_WALLPAPER) { wallpaperPhotoImportGeneration++; discardPendingWallpaper(); returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER); }
            if (requestCode == PICK_SLIDESHOW_PHOTOS) { wallpaperPhotoImportGeneration++; showWallpaperSlideshowOptions(); }
            return;
        }
        if (requestCode == PICK_APP_ICON) {
            Uri image = data.getData();
            String targetPackage = pendingCustomIconPackage;
            if (targetPackage == null) return;
            queueAppIconDecode(image, targetPackage);
            return;
        }
        if (requestCode == PICK_VIDEO_WALLPAPER) {
            Uri video = data.getData();
            try {
                int read = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(video, read);
            } catch (Exception ignored) {}
            showVideoWallpaperCrop(video);
            return;
        }
        if (requestCode == CREATE_BACKUP) { writeLauncherBackup(data.getData()); return; }
        if (requestCode == RESTORE_BACKUP) { confirmRestoreTransferBackup(data.getData()); return; }
        if (requestCode != PICK_WALLPAPER) return;
        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (flags != 0) getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (Exception ignored) { /* A transient picker grant is enough for this immediate import. */ }
        queueWallpaperDecode(uri);
    }

    private Bitmap decodePhotoForCrop(Uri source, int maxWidth, int maxHeight) throws java.io.IOException {
        int orientation = readPhotoOrientation(source);
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(source)) {
            if (input == null) throw new java.io.IOException("Selected photo could not be opened");
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || (long) bounds.outWidth * bounds.outHeight > 200_000_000L)
            throw new java.io.IOException("Selected photo has unsupported dimensions");
        int sample = 1;
        while ((long) bounds.outWidth > (long) maxWidth * sample
                || (long) bounds.outHeight > (long) maxHeight * sample) sample <<= 1;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap decoded;
        try (InputStream input = getContentResolver().openInputStream(source)) {
            if (input == null) throw new java.io.IOException("Selected photo could not be opened");
            decoded = BitmapFactory.decodeStream(input, null, options);
        }
        if (decoded == null) throw new java.io.IOException("Selected photo could not be decoded");
        try {
            Bitmap oriented = orientPhotoBitmap(decoded, orientation);
            if (oriented == null) throw new java.io.IOException("Selected photo orientation could not be applied");
            if (oriented != decoded && !decoded.isRecycled()) decoded.recycle();
            return oriented;
        } catch (RuntimeException error) {
            if (!decoded.isRecycled()) decoded.recycle();
            throw new java.io.IOException("Selected photo could not be prepared", error);
        }
    }

    private void queueWallpaperDecode(Uri source) {
        final int request = ++wallpaperPhotoImportGeneration;
        discardPendingWallpaper();
        Toast.makeText(this, "Preparing wallpaper preview…", Toast.LENGTH_SHORT).show();
        try {
            photoImportWorker.execute(() -> {
                Bitmap decoded = null;
                try { decoded = decodePhotoForCrop(source, 1920, 1080); }
                catch (Exception error) {
                    DetoxDiagnostics.recordFailure("DetoxWallpaper", "Photo import", "could not decode selected wallpaper", error);
                }
                final Bitmap result = decoded;
                wallpaperDecodeHandler.post(() -> {
                    if (request != wallpaperPhotoImportGeneration || isFinishing() || isDestroyed()) {
                        if (result != null && !result.isRecycled()) result.recycle();
                        return;
                    }
                    if (result == null) {
                        Toast.makeText(this, "Could not open the selected wallpaper", Toast.LENGTH_LONG).show();
                        returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER);
                        return;
                    }
                    pendingWallpaper = result;
                    showWallpaperCrop();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            Toast.makeText(this, "Wallpaper import is unavailable right now", Toast.LENGTH_LONG).show();
        }
    }

    private void queueAppIconDecode(Uri source, String targetPackage) {
        final int request = ++appIconImportGeneration;
        discardPendingAppIcon();
        try {
            photoImportWorker.execute(() -> {
                Bitmap decoded = null;
                try { decoded = decodePhotoForCrop(source, 1024, 1024); }
                catch (Exception error) {
                    DetoxDiagnostics.recordFailure("DetoxLauncher", "App icon import", "could not decode selected icon", error);
                }
                final Bitmap result = decoded;
                wallpaperDecodeHandler.post(() -> {
                    if (request != appIconImportGeneration || isFinishing() || isDestroyed()
                            || !targetPackage.equals(pendingCustomIconPackage)) {
                        if (result != null && !result.isRecycled()) result.recycle();
                        return;
                    }
                    if (result == null) {
                        pendingCustomIconPackage = null;
                        Toast.makeText(this, "Could not open the selected app icon", Toast.LENGTH_LONG).show();
                        return;
                    }
                    pendingAppIconBitmap = result;
                    showAppIconCrop(targetPackage);
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            pendingCustomIconPackage = null;
            Toast.makeText(this, "App icon import is unavailable right now", Toast.LENGTH_LONG).show();
        }
    }

    /** A TV-remote-friendly crop step: arrows pan, zoom buttons change scale, Save applies a 16:9 crop. */
    private void showWallpaperCrop() {
        if (pendingWallpaper == null) return;
        cropDialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(px(48), px(24), px(48), px(24));
        page.setBackgroundColor(0xFF101218);
        TextView title = text(px(38), Color.WHITE, semibold);
        title.setText("Crop wallpaper");
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, px(64)));
        CropPreview preview = new CropPreview(this, pendingWallpaper);
        preview.setId(View.generateViewId());
        preview.setFocusable(true);
        preview.setFocusableInTouchMode(true);
        preview.setBackgroundColor(0xFF050608);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        previewLp.setMargins(px(18), px(16), px(18), px(10));
        page.addView(preview, previewLp);
        TextView help = text(px(22), 0xFFE2E6EF, regular);
        help.setText("Use arrows or drag the image to pan. Press OK for controls.");
        help.setGravity(Gravity.CENTER);
        page.addView(help, new LinearLayout.LayoutParams(-1, px(54)));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        String[] labels = {"Zoom out", "Zoom in", "Save wallpaper", "Cancel"};
        TextView[] buttons = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            TextView button = text(px(24), Color.WHITE, semibold);
            button.setText(labels[i]);
            button.setGravity(Gravity.CENTER);
            button.setFocusable(true);
            button.setId(View.generateViewId());
            button.setPadding(px(24), 0, px(24), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(px(18));
            bg.setColor(0xFF282D38);
            button.setBackground(bg);
            button.setOnFocusChangeListener((v, focused) -> bg.setColor(focused ? 0xFF43536D : 0xFF282D38));
            button.setOnClickListener(v -> {
                if (action == 0) preview.zoom(0.85f);
                else if (action == 1) preview.zoom(1.18f);
                else if (action == 2) saveWallpaperCrop(preview);
                else cancelWallpaperCrop();
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, px(64));
            bp.setMargins(px(8), px(4), px(8), px(4));
            controls.addView(button, bp);
            buttons[i] = button;
        }
        preview.setControlsTarget(buttons[0]);
        preview.setNextFocusDownId(buttons[0].getId());
        buttons[0].setNextFocusUpId(preview.getId());
        page.addView(controls, new LinearLayout.LayoutParams(-1, px(82)));
        cropDialog.setContentView(page);
        Window win = cropDialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setLayout(-1, -1);
        }
        cropDialog.setOnCancelListener(d -> discardPendingWallpaper());
        cropDialog.show();
        if (win != null) win.setLayout(-1, -1);
        preview.requestFocus();
    }

    private void saveWallpaperCrop(CropPreview preview) {
        try {
            Bitmap cropped = preview.createCrop(1920, 1080);
            File file = new File(getFilesDir(), "selected_wallpaper.jpg");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                cropped.compress(Bitmap.CompressFormat.JPEG, 94, out);
                out.flush();
            }
            SharedPreferences.Editor e = getSharedPreferences(PREFS, 0).edit();
            e.remove("photoUri").putString("photoFile", file.getAbsolutePath())
                    .putBoolean("wallpaperSlideshowPhotosEnabled", false)
                    .putBoolean("liveWallpaperEnabled", false)
                    .putString("wallpaper", WALLPAPER_NAMES[PHOTO]).apply();
            configureWallpaperWindow();
            cropped.recycle();
            dismissCropDialog();
            showWallpaper(PHOTO);
            scheduleWallpaperSlideshow();
            if (pendingCompanionWallpaper) {
                pendingCompanionWallpaper = false;
                updatePhoneTransferStatus("Photo wallpaper applied");
            }
            discardPendingWallpaper();
        } catch (Exception ignored) { cancelWallpaperCrop(); }
    }

    private void cancelWallpaperCrop() {
        dismissCropDialog();
        discardPendingWallpaper();
    }

    private void dismissCropDialog() {
        if (cropDialog != null && cropDialog.isShowing()) cropDialog.dismiss();
        cropDialog = null;
    }

    private void discardPendingWallpaper() {
        if (pendingWallpaper != null && !pendingWallpaper.isRecycled()) pendingWallpaper.recycle();
        pendingWallpaper = null;
        if (pendingCompanionWallpaper) {
            pendingCompanionWallpaper = false;
            if (!isFinishing() && !isDestroyed()) updatePhoneTransferStatus("Photo received but not applied");
        }
    }

    private void showAppIconCrop(String packageName) {
        if (pendingAppIconBitmap == null) return;
        pendingCustomIconPackage = packageName;
        appIconCropDialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(px(48), px(24), px(48), px(24));
        page.setBackgroundColor(0xFF101218);
        TextView title = text(px(38), Color.WHITE, semibold);
        title.setText(String.format(Strings.t("Crop icon · %s"), labelOf(packageName)));
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        page.addView(title, new LinearLayout.LayoutParams(-1, px(64)));
        CropPreview preview = new CropPreview(this, pendingAppIconBitmap, 1f);
        preview.setId(View.generateViewId());
        preview.setFocusable(true);
        preview.setFocusableInTouchMode(true);
        preview.setBackgroundColor(0xFF050608);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        previewLp.setMargins(px(18), px(16), px(18), px(10));
        page.addView(preview, previewLp);
        TextView help = text(px(22), 0xFFE2E6EF, regular);
        help.setText("Use arrows or drag to position. Zoom in or out, then save.");
        help.setGravity(Gravity.CENTER);
        page.addView(help, new LinearLayout.LayoutParams(-1, px(54)));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        String[] labels = {"Zoom out", "Zoom in", "Save icon", "Cancel"};
        TextView[] buttons = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            TextView button = text(px(24), Color.WHITE, semibold);
            button.setText(labels[i]);
            button.setGravity(Gravity.CENTER);
            button.setFocusable(true);
            button.setId(View.generateViewId());
            button.setPadding(px(24), 0, px(24), 0);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(px(18));
            bg.setColor(0xFF282D38);
            button.setBackground(bg);
            button.setOnFocusChangeListener((v, focused) -> bg.setColor(focused ? 0xFF43536D : 0xFF282D38));
            button.setOnClickListener(v -> {
                if (action == 0) preview.zoom(0.85f);
                else if (action == 1) preview.zoom(1.18f);
                else if (action == 2) saveAppIconCrop(preview);
                else cancelAppIconCrop();
            });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, px(64));
            bp.setMargins(px(8), px(4), px(8), px(4));
            controls.addView(button, bp);
            buttons[i] = button;
        }
        preview.setControlsTarget(buttons[0]);
        preview.setNextFocusDownId(buttons[0].getId());
        buttons[0].setNextFocusUpId(preview.getId());
        page.addView(controls, new LinearLayout.LayoutParams(-1, px(82)));
        appIconCropDialog.setContentView(page);
        Window win = appIconCropDialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setLayout(-1, -1);
        }
        appIconCropDialog.setOnCancelListener(d -> {
            pendingCustomIconPackage = null;
            discardPendingAppIcon();
        });
        appIconCropDialog.show();
        if (win != null) win.setLayout(-1, -1);
        preview.requestFocus();
    }

    private void saveAppIconCrop(CropPreview preview) {
        Bitmap cropped = null;
        try {
            cropped = preview.createCrop(512, 512);
            File directory = new File(getFilesDir(), "app-icons");
            if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException("Could not create icon folder");
            String safePackage = pendingCustomIconPackage.replaceAll("[^A-Za-z0-9._-]", "_");
            File file = new File(directory, safePackage + ".png");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                if (!cropped.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new java.io.IOException("Could not save icon");
                out.flush();
            }
            String target = pendingCustomIconPackage;
            getSharedPreferences(PREFS, 0).edit()
                    .remove("customAppIcon:" + target)
                    .putString("customAppIconFile:" + target, file.getAbsolutePath()).apply();
            pendingCustomIconPackage = null;
            dismissAppIconCrop();
            discardPendingAppIcon();
            cropped.recycle();
            cropped = null;
            focusedKey = target;
            buildTiles();
        } catch (Exception ignored) {
            if (cropped != null && !cropped.isRecycled()) cropped.recycle();
            cancelAppIconCrop();
        }
    }

    private void cancelAppIconCrop() {
        pendingCustomIconPackage = null;
        dismissAppIconCrop();
        discardPendingAppIcon();
    }

    private void dismissAppIconCrop() {
        if (appIconCropDialog != null && appIconCropDialog.isShowing()) appIconCropDialog.dismiss();
        appIconCropDialog = null;
    }

    private void discardPendingAppIcon() {
        if (pendingAppIconBitmap != null && !pendingAppIconBitmap.isRecycled()) pendingAppIconBitmap.recycle();
        pendingAppIconBitmap = null;
    }

    private final class CropPreview extends View {
        private final Bitmap bitmap;
        private final float cropAspect;
        private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint overlay = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float zoom = 1f, centerX = 0.5f, centerY = 0.5f;
        private float lastTouchX, lastTouchY;
        private View controlsTarget;
        CropPreview(Context context, Bitmap bitmap) { this(context, bitmap, 16f / 9f); }
        CropPreview(Context context, Bitmap bitmap, float cropAspect) {
            super(context);
            this.bitmap = bitmap;
            this.cropAspect = cropAspect;
            setContentDescription(cropAspect == 1f
                    ? "App icon crop preview. Use arrows or drag to move the image; press OK for crop controls."
                    : "Wallpaper crop preview. Use arrows or drag to move the image; press OK for crop controls.");
            grid.setColor(0xBFFFFFFF);
            grid.setStrokeWidth(px(2));
            grid.setStyle(Paint.Style.STROKE);
        }
        void setControlsTarget(View target) { controlsTarget = target; }
        void zoom(float factor) { zoom = Math.max(1f, Math.min(3f, zoom * factor)); invalidate(); }
        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    && controlsTarget != null) {
                controlsTarget.requestFocus();
                return true;
            }
            float step = 0.035f / zoom;
            // Arrow directions move the visible image in the same direction as the key.
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) centerX = Math.min(1f, centerX + step);
            else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) centerX = Math.max(0f, centerX - step);
            else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP
                    || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) centerY = Math.min(1f, centerY + step);
            else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
                    || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) centerY = Math.max(0f, centerY - step);
            else return super.onKeyDown(keyCode, event);
            invalidate();
            return true;
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            int action = event.getActionMasked();
            if (action == android.view.MotionEvent.ACTION_DOWN) {
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_MOVE) {
                Rect source = sourceRect();
                float width = Math.max(1f, Math.min(getWidth(), getHeight() * cropAspect));
                float height = Math.max(1f, width / cropAspect);
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;
                centerX = Math.max(0f, Math.min(1f, centerX - dx * source.width() / width / bitmap.getWidth()));
                centerY = Math.max(0f, Math.min(1f, centerY - dy * source.height() / height / bitmap.getHeight()));
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                invalidate();
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_UP) {
                performClick();
                return true;
            }
            if (action == android.view.MotionEvent.ACTION_CANCEL) return true;
            return true;
        }
        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
        private Rect sourceRect() {
            float aspect = cropAspect;
            float sw = bitmap.getWidth() / zoom, sh = sw / aspect;
            if (sh > bitmap.getHeight() / zoom) { sh = bitmap.getHeight() / zoom; sw = sh * aspect; }
            float cx = centerX * bitmap.getWidth(), cy = centerY * bitmap.getHeight();
            float left = Math.max(0, Math.min(bitmap.getWidth() - sw, cx - sw / 2));
            float top = Math.max(0, Math.min(bitmap.getHeight() - sh, cy - sh / 2));
            return new Rect(Math.round(left), Math.round(top), Math.round(left + sw), Math.round(top + sh));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float aspect = cropAspect;
            float vw = Math.min(getWidth(), getHeight() * aspect), vh = vw / aspect;
            float left = (getWidth() - vw) / 2f, top = (getHeight() - vh) / 2f;
            RectF dst = new RectF(left, top, left + vw, top + vh);
            canvas.drawBitmap(bitmap, sourceRect(), dst, imagePaint);
            overlay.setColor(0xB0000000);
            canvas.drawRect(0, top, left, top + vh, overlay);
            canvas.drawRect(left + vw, top, getWidth(), top + vh, overlay);
            canvas.drawRect(0, 0, getWidth(), top, overlay);
            canvas.drawRect(0, top + vh, getWidth(), getHeight(), overlay);
            canvas.drawRect(dst, grid);
            canvas.drawLine(left + vw / 3f, top, left + vw / 3f, top + vh, grid);
            canvas.drawLine(left + vw * 2f / 3f, top, left + vw * 2f / 3f, top + vh, grid);
            canvas.drawLine(left, top + vh / 3f, left + vw, top + vh / 3f, grid);
            canvas.drawLine(left, top + vh * 2f / 3f, left + vw, top + vh * 2f / 3f, grid);
        }
        Bitmap createCrop(int width, int height) {
            Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            canvas.drawBitmap(bitmap, sourceRect(), new Rect(0, 0, width, height), imagePaint);
            return out;
        }
    }

    // A glow colour made solid over the base, three times as strong, so a small dot shows it.
    private static int glow(int glow, int base) {
        float a = Math.min(1f, 3 * (glow >>> 24) / 255f);
        int r = Math.round(Color.red(base) + (Color.red(glow) - Color.red(base)) * a);
        int g = Math.round(Color.green(base) + (Color.green(glow) - Color.green(base)) * a);
        int b = Math.round(Color.blue(base) + (Color.blue(glow) - Color.blue(base)) * a);
        return Color.rgb(r, g, b);
    }

    /** Shared soft-glass treatment for existing clock and tile surfaces. */
    private GradientDrawable glassSurface(int[] colors, int strokeColor, int radius) {
        GradientDrawable surface = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        surface.setCornerRadius(px(radius));
        surface.setStroke(Math.max(1, px(1)), strokeColor);
        surface.setDither(true);
        return surface;
    }

    private boolean compactHomeHeader() { return getSharedPreferences(PREFS, 0).getBoolean("homeCompactHeader", true); }

    private boolean homeSectionHeadings() { return getSharedPreferences(PREFS, 0).getBoolean("homeSectionHeadings", true); }
    private int topActionButtonSize() { return UTILITY_BUTTON; }

    private int homeTileStyle() { return HomeUiPolicy.bounded(getSharedPreferences(PREFS, 0).getInt("homeTileStyle", 1), 0, 2); }
    private void applyHomeReadability() {
        if (homeReadability != null) homeReadability.tune(100, 18);
    }
    private void stopHomeFocusAnimations() {
        uiMotion.cancelFocus();
        for (Map.Entry<View,Runnable> entry : new ArrayList<>(focusAnimations.entrySet())) entry.getKey().removeCallbacks(entry.getValue());
        for (Map.Entry<View,Runnable> entry : new ArrayList<>(focusGlowAnimations.entrySet())) entry.getKey().removeCallbacks(entry.getValue());
        focusAnimations.clear(); focusGlowAnimations.clear();
        for (View tile : tiles) {
            tile.animate().cancel();tile.setScaleX(1f);tile.setScaleY(1f);tile.setTranslationY(0f);tile.setTranslationZ(0f);
            View artwork = tile.findViewWithTag("tile-artwork");
            if (artwork != null) { artwork.animate().cancel(); if(rowApps.contains(tile.getTag())&&!isFolderKey(String.valueOf(tile.getTag()))){artwork.setScaleX(1f);artwork.setScaleY(1f);artwork.setTranslationX(0f);artwork.setTranslationY(0f);} }
        }
    }

    private void positionHomeHeadings(int count) {
        if(favouritesHeading==null){
            favouritesHeading=text(px(27),0xF2E5F0F7,semibold);moreAppsHeading=text(px(25),0xD5DBE9F3,semibold);
            for(TextView label:new TextView[]{favouritesHeading,moreAppsHeading}){
                label.setSingleLine(true);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                label.setIncludeFontPadding(false);label.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.min(fontPx(px(27)),px(32)));
                root.addView(label,at(px(homeSafeInset()),0,px(560),px(42)));
            }
        }
        favouritesHeading.setText(Strings.t(count==0?"Add your favourites":"Favourites")); moreAppsHeading.setText(Strings.t("More apps"));
        favouritesHeading.setVisibility(homeSectionHeadings()?View.VISIBLE:View.GONE);
        moreAppsHeading.setVisibility(homeSectionHeadings()&&count>appColumns?View.VISIBLE:View.GONE);
        for(TextView label:new TextView[]{favouritesHeading,moreAppsHeading}){
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)label.getLayoutParams();lp.leftMargin=gridLeftInsetPx;
            lp.topMargin=(label==favouritesHeading?gridTop:gridY(appColumns))-px(getSharedPreferences(PREFS,0).getInt("homeAppLabels",0)==2?48:108);label.setLayoutParams(lp);
        }
    }
    private void wireUtilityNavigation() {
        List<View> utilities=new ArrayList<>();
        for(String key:utilityFocusKeys){View v=tile(key);if(v!=null){if(v.getId()==View.NO_ID)v.setId(View.generateViewId());utilities.add(v);}}
        List<View> lastRow=new ArrayList<>();
        for(int i=0;i<rowApps.size();i++){
            View app=tile(rowApps.get(i));if(app==null)continue;
            int next=HomeUiPolicy.downIndex(i,rowApps.size(),appColumns);
            if(next>=0)app.setNextFocusDownId(tile(rowApps.get(next)).getId());
            else{app.setNextFocusDownId(app.getId());lastRow.add(app);}
            if(i%appColumns==0)app.setNextFocusLeftId(app.getId());
            if(i%appColumns==appColumns-1||i==rowApps.size()-1)app.setNextFocusRightId(app.getId());
        }
        int placement=getSharedPreferences(PREFS,0).getInt("utilitiesPlacement",0);
        if(placement==2&&!utilities.isEmpty()){
            sortViewsByCenterX(utilities);wireHorizontalRow(utilities);
            connectFocusRows(lastRow,utilities);
            for(View utility:utilities)utility.setNextFocusDownId(utility.getId());
        }
        if(placement==0||placement==1){
            List<View> edgeApps=new ArrayList<>();
            for(int i=0;i<rowApps.size();i++)if(placement==0?i%appColumns==0:i%appColumns==appColumns-1||i==rowApps.size()-1){
                View app=tile(rowApps.get(i));edgeApps.add(app);View utility=nearestViewByCenterY(utilities,app);
                if(utility!=null){if(placement==0)app.setNextFocusLeftId(utility.getId());else app.setNextFocusRightId(utility.getId());}
            }
            for(int i=0;i<utilities.size();i++){
                View utility=utilities.get(i),app=nearestViewByCenterY(edgeApps,utility);
                if(app!=null){if(placement==0)utility.setNextFocusRightId(app.getId());else utility.setNextFocusLeftId(app.getId());}
                if(i>0)utility.setNextFocusUpId(utilities.get(i-1).getId());
                else utility.setNextFocusUpId(utility.getId());
                if(i+1<utilities.size())utility.setNextFocusDownId(utilities.get(i+1).getId());
                else if(app!=null)utility.setNextFocusDownId(app.getId());
                else utility.setNextFocusDownId(utility.getId());
            }
        }
    }
    private void wireHorizontalRow(List<View> row){
        for(int i=0;i<row.size();i++){
            View view=row.get(i);
            view.setNextFocusLeftId(row.get(Math.max(0,i-1)).getId());
            view.setNextFocusRightId(row.get(Math.min(row.size()-1,i+1)).getId());
        }
    }
    private View nearestViewByCenterY(List<View> candidates,View source){
        int[] origin=new int[2];source.getLocationOnScreen(origin);int centre=origin[1]+source.getHeight()/2;
        View nearest=null;int distance=Integer.MAX_VALUE;
        for(View candidate:candidates){int[] location=new int[2];candidate.getLocationOnScreen(location);int delta=Math.abs(centre-location[1]-candidate.getHeight()/2);if(delta<distance){distance=delta;nearest=candidate;}}
        return nearest;
    }
    private void showHomeInputs() {
        List<TvInputInfo> inputs=hdmiInputs();
        if(inputs.isEmpty()){Toast.makeText(this,Strings.t("No HDMI inputs found"),Toast.LENGTH_SHORT).show();return;}
        List<String> labels=new ArrayList<>();for(TvInputInfo input:inputs)labels.add(inputLabel(input));
        panel(Strings.t("Inputs"),labels,null,0,-1,null,which->openInput(inputs.get(which).getId()));
    }
    private void showHomeSearchOptions() {
        panel(Strings.t("Search"),translated(new String[]{"Find an installed app","Voice search"}),null,0,0,null,
                which->{if(which==0)showAppSearch();else if(which==1)launchVoiceSearch();});
    }

    private String appArtworkSignature(String pkg){
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        String file=prefs.getString("customAppIconFile:"+pkg,"");
        return appArtworkGeneration+"|"+prefs.getString("iconPackPackage","")+"|"+file+"|"
                +(file.isEmpty()?0:new File(file).lastModified())+"|"+prefs.getString("customAppIcon:"+pkg,"");
    }
    private void loadAppArtwork(String pkg,String signature){
        if(signature.equals(appArtworkInFlight.get(pkg))||appArtworkWorker.isShutdown())return;
        appArtworkInFlight.put(pkg,signature);
        appArtworkWorker.execute(()->{
            if(Thread.currentThread().isInterrupted())return;
            long started=SystemClock.elapsedRealtime();String label=pkg;Drawable icon=null;
            PackageManager pm=getPackageManager();ApplicationInfo info=null;
            try{info=pm.getApplicationInfo(pkg,0);label=pm.getApplicationLabel(info).toString();}catch(Exception ignored){}
            try{icon=appArtworkResolver.resolve(pkg);}
            catch(Exception error){DetoxDiagnostics.recordFailure("DetoxStartup","App artwork",pkg,error);}
            AppTileData result=new AppTileData(signature,label,icon==null?new LineIcon(LineIcon.ALL_APPS):icon);
            getSharedPreferences("app_labels",0).edit().putString(pkg,label).apply();
            long duration=SystemClock.elapsedRealtime()-started;
            if(duration>100)android.util.Log.i("DetoxStartup","Artwork "+pkg+" loaded off UI thread in "+duration+" ms");
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                if(signature.equals(appArtworkInFlight.get(pkg)))appArtworkInFlight.remove(pkg);
                if(!signature.equals(appArtworkSignature(pkg)))return;
                appArtworkCache.put(pkg,result);
                View card=tile(pkg);if(card==null)return;
                View artwork=card.findViewWithTag("tile-artwork");
                if(artwork instanceof ImageView)((ImageView)artwork).setImageDrawable(new AppIconDrawable(result.icon));
                card.setContentDescription(result.label);
                View parent=(View)card.getParent();View caption=parent==null?null:parent.findViewWithTag("tile-caption");
                if(caption instanceof TextView)((TextView)caption).setText(result.label);
                if(appArtworkInFlight.isEmpty())android.util.Log.i("DetoxStartup","Visible app artwork ready in "+(SystemClock.elapsedRealtime()-startupStarted)+" ms");
            });
        });
    }
    private void buildTiles() {
        lastHomeIconSettings = AppIconSettingsPolicy.signature(getSharedPreferences(PREFS, 0).getAll());
        if(homeEntrance!=null)homeEntrance.finish();stopHomeFocusAnimations();
        applyHomeReadability();
        // Restore the last app focus after a launcher restart, without changing icon order.
        if (focusedKey == null) {
            String rememberedApp = getSharedPreferences(PREFS, 0).getString("homeLastFocusedApp", "");
            if (!rememberedApp.isEmpty()) focusedKey = rememberedApp;
        }
        // While tiles are swapped out, removing the focused one would hand focus down the row (and
        // overwrite focusedKey), so hold focus off until the new tiles are in.
        String want = focusedKey;
        int wantAt = rowApps.indexOf(want); // where it was, in case the app is gone now
        root.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        for (View t : tiles) {
            View wrap = (View) t.getParent();
            ((ViewGroup) wrap.getParent()).removeView(wrap);
        }
        tiles.clear();
        rowApps.clear();
        movingKey = null;
        moveHint.setVisibility(View.GONE);

        PackageManager pm = getPackageManager();
        int m = px(MARGIN), g = px(GAP);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        List<String> apps = initialGridApps != null ? initialGridApps : appList(pm);
        initialGridApps = null;
        int launchable = 0;
        for (String pkg : apps) if (isFolderKey(pkg) || launchIntent(pm, pkg) != null) launchable++;
        int layoutMode = appLayoutMode();
        int preferredColumns = layoutMode == 1 ? 4 : layoutMode == 2 ? 8 : PER_ROW;
        appColumns = preferredColumns; // Keep one or two favourites from expanding into giant banners.
        int utilitiesPlacement = Math.max(0, Math.min(3, getSharedPreferences(PREFS, 0).getInt("utilitiesPlacement", 0)));
        int safeInset = homeSafeInset();
        int contentRight = safeInset
                + (utilitiesPlacement == 1 ? UTILITY_BUTTON + UTILITY_APP_GAP : 0);
        int baseW = (screenWidth - px(safeInset + contentRight) - g * (appColumns - 1)) / appColumns;
        // A single bottom row sits below the side rail and needs no empty rail-width gutter.
        // Multiple rows retain clearance where apps can be beside the utility buttons.
        int contentLeft = safeInset + (utilitiesPlacement == 0 && launchable > appColumns
                ? UTILITY_BUTTON + UTILITY_APP_GAP : 0);
        gridLeftInsetPx = px(contentLeft);
        int w = baseW;
        int h = w * 9 / 16;
        tileBaseW = baseW;
        tileW = w;
        tileH = h;

        // Apps sit along the bottom, with any extra rows rising above them.
        int rows = Math.max(1, (launchable + appColumns - 1) / appColumns);
        int utilityBottomReserve = utilitiesPlacement == 2 ? px(UTILITY_BUTTON + 60) : 0;
        int minimumTop = px(compactHomeHeader() ? 320 : GRID_MIN_TOP);
        // Reserve a clean row above the apps and below the clock controls when Utilities is placed there.
        if (utilitiesPlacement == 3) {
        String clockStyle = effectiveClockStyle(getSharedPreferences(PREFS, 0));
            int controlsBottom = px(Math.round(("digital".equals(clockStyle) ? 225f : 410f) * clockSizeScale()) + 54 + topActionButtonSize());
            int utilityTop = controlsBottom + menuPx(50) + px(12);
            minimumTop = Math.max(minimumTop, utilityTop + px(UTILITY_BUTTON + (homeSectionHeadings()?120:62)));
        }
        homeLayout = HomeUiPolicy.layout(screenWidth, getResources().getDisplayMetrics().heightPixels,
                scale, layoutMode, launchable, px(safeInset), px(contentLeft + contentRight - 2 * safeInset), minimumTop,
                utilityBottomReserve, homeSectionHeadings());
        tileW = w = homeLayout.tileWidth; tileH = h = homeLayout.tileHeight; tileBaseW = w;
        tileRowStepPx = homeLayout.step; gridTop = homeLayout.top;
        int lastAppTop = homeLayout.y((rows - 1) * appColumns);
        ((FrameLayout.LayoutParams) moveHint.getLayoutParams()).topMargin = gridTop - px(64);
        int contentHeight = homeLayout.contentHeight;
        root.setMinimumHeight(contentHeight);
        if (homeReadability != null) {
            FrameLayout.LayoutParams readabilityParams = (FrameLayout.LayoutParams) homeReadability.getLayoutParams();
            readabilityParams.height = contentHeight; homeReadability.setLayoutParams(readabilityParams);
        }
        positionHomeHeadings(launchable);

        // Cover the root's extra scrollable height with the wallpaper to prevent a black footer.
        if (wallpaperMotion != null) {
            FrameLayout.LayoutParams motionParams = (FrameLayout.LayoutParams) wallpaperMotion.getLayoutParams();
            motionParams.height = contentHeight;
            wallpaperMotion.setLayoutParams(motionParams);
        }
        if (videoWallpaperView != null && videoWallpaperView.getParent() == root) {
            FrameLayout.LayoutParams videoParams = (FrameLayout.LayoutParams) videoWallpaperView.getLayoutParams();
            videoParams.height = contentHeight;
            videoWallpaperView.setLayoutParams(videoParams);
        }

        for (String pkg : apps) {
            if (isFolderKey(pkg)) {
                int i = rowApps.size();
                rowApps.add(pkg);
                addTile(root, pkg, folderName(pkg), null, LineIcon.FOLDER, null, false,
                        gridX(i), gridY(i), w, h);
                continue;
            }
            Intent li = launchIntent(pm, pkg);
            if (li == null) continue;
            AppTileData data=appArtworkCache.get(pkg);
            String signature=appArtworkSignature(pkg);
            if(data!=null&&!signature.equals(data.signature))data=null;
            String label=data==null?getSharedPreferences("app_labels",0).getString(pkg,Strings.t("Loading app…")):data.label;
            Drawable icon=data==null?new LineIcon(LineIcon.ALL_APPS):data.icon;
            int i=rowApps.size();rowApps.add(pkg);
            addTile(root,pkg,label,null,null,icon,false,gridX(i),gridY(i),w,h);
            if(data==null)loadAppArtwork(pkg,signature);
        }
        // Give the app grid explicit D-pad neighbors so focus moves by row and column instead
        // of jumping to whichever overlapping control Android happens to consider nearest.
        for (int i = 0; i < rowApps.size(); i++) {
            View appTile = tile(rowApps.get(i));
            if (appTile == null) continue;
            if (appTile.getId() == View.NO_ID) appTile.setId(View.generateViewId());
        }
        for (int i = 0; i < rowApps.size(); i++) {
            View appTile = tile(rowApps.get(i));
            if (appTile == null) continue;
            int column = i % appColumns;
            if (column > 0) appTile.setNextFocusLeftId(tile(rowApps.get(i - 1)).getId());
            if (column + 1 < appColumns && i + 1 < rowApps.size())
                appTile.setNextFocusRightId(tile(rowApps.get(i + 1)).getId());
            if (i - appColumns >= 0) appTile.setNextFocusUpId(tile(rowApps.get(i - appColumns)).getId());
            int nextRow = HomeUiPolicy.downIndex(i,rowApps.size(),appColumns);
            if (nextRow >= 0) appTile.setNextFocusDownId(tile(rowApps.get(nextRow)).getId());
        }
        // App browsing and task controls are utilities, so they follow the selected utility layout.
        int utilityX = utilitiesPlacement == 1 ? screenWidth - px(homeSafeInset() + UTILITY_BUTTON) : px(homeSafeInset());
        int utilityY = px(12);
        int utilityIndex = 0;
        List<Integer> quick = quickList();
        boolean store = !apps.contains(PLAY_STORE) && launchIntent(pm, PLAY_STORE) != null;
        List<String> utilityKeys = new ArrayList<>(), utilityLabels = new ArrayList<>();
        List<Drawable> utilityDrawables = new ArrayList<>();
        utilityKeys.add(NETWORK); utilityLabels.add(Strings.t("Network")); utilityDrawables.add(null);
        utilityKeys.add(QUICK_PANEL); utilityLabels.add(Strings.t("Quick controls")); utilityDrawables.add(new LineIcon(LineIcon.CONTROL_PANEL));
        for (int q : quick) { utilityKeys.add(QUICK + QUICK_NAMES[q]); utilityLabels.add(Strings.t(QUICK_LABELS[q])); utilityDrawables.add(new LineIcon(QUICK_ICONS[q])); }
        if (store) { utilityKeys.add(PLAY_STORE); utilityLabels.add("Play Store"); utilityDrawables.add(new LineIcon(LineIcon.PLAY)); }
        utilityKeys.add(SETTINGS); utilityLabels.add(Strings.t("Settings")); utilityDrawables.add(new LineIcon(LineIcon.SETTINGS));
        utilityKeys.add(ADD); utilityLabels.add(Strings.t("Add app")); utilityDrawables.add(new LineIcon(LineIcon.PLUS));
        utilityKeys.add("all-apps"); utilityLabels.add("All apps"); utilityDrawables.add(new LineIcon(LineIcon.ALL_APPS));
        netIcon=null;
        for(int i=utilityKeys.size()-1;i>=0;i--)if(!SETTINGS.equals(utilityKeys.get(i))&&getSharedPreferences(PREFS,0).getBoolean("utilityHidden_"+utilityKeys.get(i),false)){utilityKeys.remove(i);utilityLabels.remove(i);utilityDrawables.remove(i);}
        utilityFocusKeys = new ArrayList<>(utilityKeys);
        
        int utilityHeight=getResources().getDisplayMetrics().heightPixels;
        int railTop=px(homeSafeInset());
        int railBottom=Math.max(railTop+px(UTILITY_BUTTON),Math.min(utilityHeight-px(homeSafeInset()),lastAppTop-px(28)));
        UtilityUiPolicy.RailLayout rail=UtilityUiPolicy.rail(railTop,railBottom,px(UTILITY_BUTTON),utilityKeys.size(),px(12));
        utilityY=rail.top;
        int utilityStep=rail.step;
        if (utilitiesPlacement == 2 || utilitiesPlacement == 3) {
            int b = px(UTILITY_BUTTON), margin = px(homeSafeInset());
            int step = utilityKeys.size() > 1
                    ? Math.max(1, (screenWidth - 2 * margin - b) / (utilityKeys.size() - 1)) : 0;
            int startX = margin;
            utilityY = utilitiesPlacement == 2
                    ? lastAppTop + tileH + px(60)
                    : gridTop - px(UTILITY_BUTTON + (homeSectionHeadings()?120:62));
            for (int i = 0; i < utilityKeys.size(); i++) {
                ImageView icon = addRailIcon(utilityKeys.get(i), utilityLabels.get(i), utilityDrawables.get(i), startX + i * step, utilityY, 2);
                if (NETWORK.equals(utilityKeys.get(i))) netIcon = icon;
            }
        } else {
            for (int i = 0; i < utilityKeys.size(); i++) {
                ImageView icon = addRailIcon(utilityKeys.get(i), utilityLabels.get(i), utilityDrawables.get(i), utilityX, utilityY + utilityIndex++ * utilityStep, utilitiesPlacement);
                if (NETWORK.equals(utilityKeys.get(i))) netIcon = icon;
            }
        }
        addTopTvSettings(root, screenWidth);
        wireUtilityNavigation();

        netState = -1;
        refreshNetwork();

        root.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // An app that was uninstalled hands focus to the one now in its place, as Remove does.
        if (wantAt >= 0 && !rowApps.contains(want))
            want = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(wantAt, rowApps.size() - 1));
        View restore = null;
        for (View t : tiles) if (t.getTag().equals(want)) restore = t;
        if (restore == null && !rowApps.isEmpty()) restore = tile(rowApps.get(0));
        if (restore == null) restore = tile(ADD);
        if (restore != null) restore.requestFocus();
    }

    /** A short, damped tvOS-style focus spring, driven by frame callbacks so it still runs
     * when Android's global animator scale is zero. */
    private void animateFocus(View view, boolean focused, float focusedScale) {
        if(focused&&homeScroller!=null&&tiles.contains(view))view.postOnAnimation(()->{
            if(view.isAttachedToWindow()&&view.hasFocus()&&homeScroller!=null)view.requestRectangleOnScreen(homeFocusBounds(view),launcherAnimationMode()==2);
        });
        boolean calm=launcherAnimationMode()==1,reduced=launcherAnimationMode()==2;
        uiMotion.focus(view,focused,focusedScale,px(calm?2:4),px(calm?7:14),calm,reduced);
    }

    private Rect homeFocusBounds(View view){
        if(!rowApps.contains(view.getTag()))return new Rect(0,0,view.getWidth(),view.getHeight());
        int labels=HomeUiPolicy.bounded(getSharedPreferences(PREFS,0).getInt("homeAppLabels",0),0,2);
        return new Rect(0,HomeUiPolicy.focusTop(labels,scale),view.getWidth(),HomeUiPolicy.focusBottom(view.getHeight(),scale));
    }
    /** A lightly damped spring, clamped to a small overshoot so focus never looks bouncy. */
    private float springProgress(float t){return TvMotionPolicy.settle(t);}

    /** A gentle breathing halo makes the focused app clear without animating the whole screen. */
    private void animateFocusGlow(final View owner, final View glow, boolean focused) {
        Runnable old = focusGlowAnimations.remove(glow);
        if (old != null) glow.removeCallbacks(old);
        if (!focused) {
            glow.setAlpha(0f);
            glow.setScaleX(1f);
            glow.setScaleY(1f);
            return;
        }
        if (launcherAnimationMode() != 0 || !getSharedPreferences(PREFS, 0).getBoolean("homeFocusPulse", true)) {
            glow.setAlpha(0.32f);
            glow.setScaleX(1f);
            glow.setScaleY(1f);
            return;
        }
        final long start = SystemClock.uptimeMillis();
        final Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            // A queued frame from an older focus session must never remove a newer pulse.
            if (focusGlowAnimations.get(glow) != frame[0]) return;
            if (!activityResumed || !owner.isAttachedToWindow() || !glow.isAttachedToWindow()
                    || !owner.hasFocus() || !glow.isShown()) {
                focusGlowAnimations.remove(glow);
                glow.setAlpha(0f);
                glow.setScaleX(1f);
                glow.setScaleY(1f);
                return;
            }
            float phase = ((SystemClock.uptimeMillis() - start) % 1900L) / 1900f;
            float pulse = 0.5f - 0.5f * (float) Math.cos(phase * (float) (Math.PI * 2));
            glow.setAlpha(0.27f + pulse * 0.16f);
            float scale = 0.990f + pulse * 0.015f;
            glow.setScaleX(scale);
            glow.setScaleY(scale);
            glow.postDelayed(frame[0], 33L);
        };
        focusGlowAnimations.put(glow, frame[0]);
        glow.postDelayed(frame[0], 33L);
    }

    private void installAppTileKeyHandler(final View card, final String key, final boolean editable) {
        final Runnable[] longPressAction = new Runnable[1];
        card.setOnKeyListener((v, code, event) -> {
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            if (key.equals(movingKey)) {
                if (longPressAction[0] != null) v.removeCallbacks(longPressAction[0]);
                return moveKey(key, code, ok, down);
            }
            if (editable && code == KeyEvent.KEYCODE_MENU) {
                if (!down) showEditMenu(key);
                return true;
            }

            if (code == KeyEvent.KEYCODE_DPAD_DOWN && rowApps.contains(key)
                    && HomeUiPolicy.downIndex(rowApps.indexOf(key), rowApps.size(), appColumns) < 0
                    && !getSharedPreferences(PREFS,0).getString(HOME_DOWN_LAUNCH_PACKAGE,"@gaming").isEmpty()) {
                if (down && event.getRepeatCount() == 0 && !launchHomeDownShortcut(key)) return false;
                return true;
            }
            if (!ok) return false;
            if (down) {
                if (editable && !longPressed && (event.isLongPress()
                        || (event.getRepeatCount() > 0
                        && event.getEventTime() - event.getDownTime() >= ViewConfiguration.getLongPressTimeout()))) {
                    if (longPressAction[0] != null) v.removeCallbacks(longPressAction[0]);
                    longPressed = true;
                    okDownKey = null;
                    showEditMenu(key);
                } else if (event.getRepeatCount() == 0) {
                    longPressed = false;
                    okDownKey = key;
                    if (editable) {
                        longPressAction[0] = () -> {
                            if (key.equals(okDownKey) && v.hasFocus() && !longPressed) {
                                longPressed = true;
                                okDownKey = null;
                                showEditMenu(key);
                            }
                        };
                        v.postDelayed(longPressAction[0], ViewConfiguration.getLongPressTimeout());
                    }
                }
            } else {
                if (longPressAction[0] != null) v.removeCallbacks(longPressAction[0]);
                boolean pressed = key.equals(okDownKey) && !event.isCanceled();
                if (key.equals(okDownKey)) okDownKey = null;
                if (pressed && !longPressed) open(key);
                longPressed = false;
            }
            return true;
        });
    }


    private void addTile(ViewGroup parent, final String key, String label, Drawable banner, int[] lineIcon, Drawable appIcon, boolean customArtwork,
                         int x, int y, int w, int h) {
        FrameLayout card = new FrameLayout(this);
        final boolean appIconOnly = rowApps.contains(key) && !isFolderKey(key);
        final ImageView[] tileArtwork = new ImageView[1];
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(RADIUS));
        if (appIconOnly || isFolderKey(key)) {
            int style = homeTileStyle();
            bg.setColor(style==0?Color.TRANSPARENT:style==2?0x553C4B60:0xE51A2230);
            bg.setStroke(Math.max(1,px(1)),style==0?Color.TRANSPARENT:style==2?0x80DCEBFF:0x60596D84);
        }
        // Optional UI-only theme. Never changes wallpaper, tile geometry or artwork assets.
        int premium = homeThemeIndex();
        if (premium > 0 && (appIconOnly || isFolderKey(key)) && homeTileStyle() != 0) {
            bg.setColor(HomePremiumThemes.SURFACES[premium]);
            // A restrained two-stop sheen adds depth without blurring or touching video wallpaper.
            bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            bg.setColors(HomePremiumThemes.tileGradient(premium));
            bg.setStroke(Math.max(1, px(premium == 6 ? 2 : 1)), HomePremiumThemes.BORDERS[premium]);
            bg.setCornerRadius(px(HomePremiumThemes.RADII[premium]));
        }
        if (appIconOnly) {
            Drawable artwork = customArtwork ? banner : appIcon != null ? appIcon : banner;
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int padding=0;
            padding=homeTileStyle()==0?0:px(homeTileStyle()==1?12:9);iv.setPadding(padding,padding,padding,padding);
            GradientDrawable appArtworkClip = new GradientDrawable();
            appArtworkClip.setColor(Color.TRANSPARENT);
            appArtworkClip.setCornerRadius(px(22));
            iv.setBackground(appArtworkClip);
            iv.setClipToOutline(false);
            iv.setImageDrawable(new AppIconDrawable(artwork));
            iv.setAlpha(appArtworkIdlePercent() / 100f);
            iv.setTag("tile-artwork");
            tileArtwork[0] = iv;
            card.addView(iv, new FrameLayout.LayoutParams(h, h, Gravity.CENTER));
        } else if (banner != null) {
            bg.setColor(Color.TRANSPARENT);
            ImageView iv = new ImageView(this);
            GradientDrawable artworkClip = new GradientDrawable();
            artworkClip.setColor(Color.TRANSPARENT);
            artworkClip.setCornerRadius(px(RADIUS));
            iv.setBackground(artworkClip);
            iv.setClipToOutline(true);
            if (customArtwork) {
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setImageDrawable(banner);
                FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(Math.min(h, w), Math.min(h, w), Gravity.CENTER);
                iv.setLayoutParams(iconLp);
            } else iv.setImageDrawable(fit(banner, w, h));
            iv.setScaleX(0.89f);
            iv.setScaleY(0.89f);
            iv.setAlpha(0.46f);
            iv.setTag("tile-artwork");
            tileArtwork[0] = iv;
            if (customArtwork) {
                int iconSide = Math.min(h, w);
                card.addView(iv, new FrameLayout.LayoutParams(iconSide, iconSide, Gravity.CENTER));
            } else card.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            // Utility tile: keep the icon floating over the wallpaper, with a soft focus fill.
            if (!appIconOnly && !isFolderKey(key)) bg.setColor(Color.TRANSPARENT);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            ImageView iv = new ImageView(this);
            int size = lineIcon != null ? px(isFolderKey(key) ? 86 : 44) : px(77);
            iv.setScaleX(0.70f);
            iv.setScaleY(0.70f);
            if (lineIcon != null) iv.setImageDrawable(new LineIcon(lineIcon));
            else if (appIcon != null) iv.setImageDrawable(fit(appIcon, size, size));
            iv.setAlpha(0.46f);
            tileArtwork[0] = iv;
            if (appIconOnly) {
                iv.setBackground(null);
                iv.setClipToOutline(false);
            } else {
                GradientDrawable iconShadow = new GradientDrawable();
                iconShadow.setShape(GradientDrawable.RECTANGLE);
                iconShadow.setCornerRadius(px(16));
                iconShadow.setColor(Color.TRANSPARENT);
                iconShadow.setStroke(Math.max(1, px(2)), 0x35FFFFFF);
                iv.setBackground(iconShadow);
                iv.setClipToOutline(true);
            }
            iv.setElevation(0f);
            iv.setTranslationZ(0f);
            inner.addView(iv, new LinearLayout.LayoutParams(size, size));

            card.addView(inner, new FrameLayout.LayoutParams(-1, -1));
        }
        if(appIconOnly && tileArtwork[0]!=null){
            float iconScale=Math.max(.60f,Math.min(1f,getSharedPreferences(PREFS,0).getInt("appIconScalePercent",100)/100f));
            ViewGroup.LayoutParams artParams=tileArtwork[0].getLayoutParams();
            int baseWidth=artParams.width>0?artParams.width:w,baseHeight=artParams.height>0?artParams.height:h;
            FrameLayout.LayoutParams sized=new FrameLayout.LayoutParams(Math.max(1,Math.round(baseWidth*iconScale)),Math.max(1,Math.round(baseHeight*iconScale)),Gravity.CENTER);
            tileArtwork[0].setLayoutParams(sized);
        }
        if (premium > 0 && (appIconOnly || isFolderKey(key)) && homeTileStyle() != 0) {
            // Shader overlay is clipped to the existing tile bounds, not the video wallpaper.
            card.setBackground(new LayerDrawable(new Drawable[] {bg,
                    new PremiumReflectionDrawable(premium, getResources().getDisplayMetrics().density)}));
        } else card.setBackground(bg);
        card.setClipToOutline(!appIconOnly||homeTileStyle()!=0);
        card.setElevation(appIconOnly?0:px(4));
        card.setFocusable(true);
        card.setTag(key);
        card.setContentDescription(label);

        final int captionMode = HomeUiPolicy.bounded(getSharedPreferences(PREFS,0).getInt("homeAppLabels",0),0,2);
        final TextView caption = text(px(26),0xFFE6F0F8,semibold);
        caption.setTag("tile-caption");caption.setText(label);caption.setGravity(Gravity.CENTER);caption.setSingleLine(true);
        caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setIncludeFontPadding(false);
        caption.setShadowLayer(px(4),0,px(2),0xE0000000);
        caption.setVisibility(captionMode==1?View.VISIBLE:View.INVISIBLE);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        final GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(px(premium > 0 ? HomePremiumThemes.RADII[premium] : RADIUS));
        outline.setColor(Color.TRANSPARENT);
        outline.setStroke(Math.max(1, px(2)), utilityGlowColor(key));
        final View focusOutline = new View(this);
        focusOutline.setBackground(outline);
        focusOutline.setFocusable(false);
        focusOutline.setClickable(false);
        focusOutline.setVisibility(View.GONE);
        card.addView(focusOutline, new FrameLayout.LayoutParams(-1, -1));
        final View focusGlow = new View(this);
        GradientDrawable glow = new GradientDrawable();
        glow.setCornerRadius(px((premium > 0 ? HomePremiumThemes.RADII[premium] : RADIUS) + 12));
        glow.setOrientation(GradientDrawable.Orientation.TL_BR);
        glow.setColors(premium > 0 ? HomePremiumThemes.focusGradient(premium)
                : new int[] {0x3043CFFF, 0x3059C9FF, 0x302B8FFF});
        glow.setStroke(Math.max(1, px(2)), premium > 0 ? HomePremiumThemes.BORDERS[premium] : 0x667FEAFF);
        focusGlow.setBackground(glow);
        focusGlow.setVisibility(View.VISIBLE);
        focusGlow.setAlpha(0f);

        final boolean flatTile = banner == null;
        card.setOnFocusChangeListener((v, has) -> {
            boolean contrast = highContrastFocusEnabled();
            if (has) { focusedKey = key;  }
            caption.setVisibility(captionMode==1||(captionMode==0&&has)?View.VISIBLE:View.INVISIBLE);
            caption.setTextColor(has?Color.WHITE:0xD6E3EEF7);
            if (flatTile && !appIconOnly && !isFolderKey(key)) bg.setColor(Color.TRANSPARENT);
            if (tileArtwork[0] != null) {
                tileArtwork[0].setAlpha(has ? 1f : appIconOnly || isFolderKey(key)
                        ? appArtworkIdlePercent() / 100f : 0.46f);
                float fullArtworkScale = appIconOnly ? 1f : banner != null ? 1.05f : 1f;
                float artworkScale = appIconOnly || launcherAnimationMode()==2 ? 1f : has ? fullArtworkScale : fullArtworkScale * 0.9f;
                int appPosition = rowApps.indexOf(key);
                float parallaxX = 0f, parallaxY = 0f;
                if (banner != null && appPosition >= 0 && launcherAnimationMode()==0 && !appIconOnly) {
                    float horizontal = appColumns <= 1 ? 0f
                            : ((appPosition % appColumns) / (float) (appColumns - 1) - 0.5f);
                    int rows = Math.max(1, (rowApps.size() + appColumns - 1) / appColumns);
                    float vertical = rows <= 1 ? 0f
                            : ((appPosition / appColumns) / (float) (rows - 1) - 0.5f);
                    parallaxX = -horizontal * px(7);
                    parallaxY = -vertical * px(4);
                }
                tileArtwork[0].animate().scaleX(artworkScale).scaleY(artworkScale)
                        .translationX(has ? parallaxX : 0f)
                        .translationY(has && launcherAnimationMode()!=2 ? (appIconOnly ? -px(4) : -px(3) + parallaxY) : 0f).setDuration(motionDuration(190L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();
            }
            outline.setStroke(Math.max(1, px(has && contrast ? 6 : 2)),
                    has && contrast ? Color.WHITE : utilityGlowColor(key));
            glow.setStroke(Math.max(1, px(has && contrast ? 5 : 2)), has && contrast ? Color.WHITE
                    : premium > 0 ? HomePremiumThemes.BORDERS[premium] : 0x667FEAFF);
            focusOutline.setVisibility(has ? View.VISIBLE : View.GONE);
            focusGlow.setVisibility(has ? View.VISIBLE : View.GONE);
            animateFocusGlow(v, focusGlow, has);
            card.setElevation(has ? px(18) : 0);
            // Keep app card footprints identical so gaps stay even on the focused tile.
            animateFocus(v, has, HomeUiPolicy.safeFocusScale(launcherAnimationMode(),contrast,w,px(homeSafeInset()),px(GAP)));

        });
        final boolean editable = rowApps.contains(key);
        if ("all-apps".equals(key)) card.setOnClickListener(v -> {showAllInstalledApps();});
        else card.setOnClickListener(v -> {open(key);});
        if (editable) card.setOnLongClickListener(v -> { showEditMenu(key); return true; });
        installAppTileKeyHandler(card, key, editable);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.addView(focusGlow, at(-px(5), -px(5), w + px(10), h + px(10)));
        wrap.addView(card, new FrameLayout.LayoutParams(w, h));
        wrap.addView(caption,at(0,-px(58),w,px(46)));

        parent.addView(wrap, at(x, y, w, h));
        DetoxPointerFocus.install(card);
        tiles.add(card);
    }


    private void applyUtilityLabelStyle(TextView label) {
        label.setTypeface(semibold, Typeface.BOLD);
        label.getPaint().setFakeBoldText(true);
        label.setIncludeFontPadding(false);
        label.setLetterSpacing(0.004f);
        applyFontPreview(label);
        applyUtilityTextColor(label);
        label.setBackground(null);
        label.setShadowLayer(menuPx(2), 0, menuPx(1), 0xC0000000);
    }

    private void applyUtilityTextColor(TextView label) {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (!prefs.contains("utilityTextColor")) {
            label.setTextColor(withFontColor(0xFFF1F5FA));
            if (selectedFontColor() == RAINBOW_COLOR) applyRainbow(label);
            else label.getPaint().setShader(null);
            return;
        }
        int color = prefs.getInt("utilityTextColor", Color.WHITE);
        label.getPaint().setShader(null);
        label.setTextColor(color == WALLPAPER_MATCH_COLOR ? wallpaperAccentColor() : color);
    }

    private int utilityMotionLevel() {
        return Math.max(0,Math.min(2,getSharedPreferences(PREFS,0).getInt("utilityAppearanceMotion",1)));
    }
    private float utilityFocusScale(boolean artwork) {
        int motion=utilityMotionLevel();
        return motion==0?1f:motion==2?1.065f:artwork?1.055f:1.035f;
    }
    // Compact left-rail shortcut with a label revealed on focus.
    private ImageView addRailIcon(final String key, String label, Drawable icon, int x, int y) {
        return addRailIcon(key, label, icon, x, y, 0);
    }

    private ImageView addRailIcon(final String key, String label, Drawable icon, int x, int y, int placement) {
        int b=px(UTILITY_BUTTON),size=Math.max(px(22),Math.min(b-px(10),Math.round(b*.62f*utilityIconScale())));
        int labelGap = px(10);
        FrameLayout card = new FrameLayout(this);
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setElevation(0);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        int accent = utilityGlowColor(key);
        card.setBackground(null);
        Drawable officialIcon=BundledUtilityArtwork.home(this,key);
        if(officialIcon!=null)icon=officialIcon;
        final boolean officialArtwork=BundledUtilityArtwork.bundled(icon);
        final boolean preserveArtworkColors=BundledUtilityArtwork.preserveColors(icon);
        ImageView iv = new ImageView(this);
        if (icon instanceof LineIcon) {
            ((LineIcon) icon).setColor(utilityGlowColor(key));
            ((LineIcon) icon).setFitToContent(true);((LineIcon)icon).setFillFraction(.94f);
            ((LineIcon) icon).setStrokeScale(1.05f);
        }
        iv.setImageDrawable(icon);
        if(!preserveArtworkColors)iv.setColorFilter(accent,android.graphics.PorterDuff.Mode.SRC_IN);
        iv.setAlpha(utilityIconOpacity());iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setScaleX(1);iv.setScaleY(1);
        card.addView(iv, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));

        TextView name = text(menuPx(27), 0xE8FFFFFF, semibold);
        name.setText(label);
        applyUtilityLabelStyle(name);
        if (SETTINGS.equals(key)) settingsUtilityLabel = name;
        name.setBackground(null);
        name.setPadding(menuPx(14), 0, menuPx(14), 0);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int desiredLabelWidth = (int) Math.ceil(name.getPaint().measureText(label)) + 2 * menuPx(14);
        int maxLabelWidth = placement == 1 ? Math.max(px(1), x - labelGap - px(12))
                : placement == 2 ? screenWidth - 2 * px(12)
                : Math.max(px(1), screenWidth - x - b - labelGap - px(12));
        int labelWidth = Math.min(maxLabelWidth, Math.max(menuPx(52), desiredLabelWidth));
        name.setSingleLine(true);
        name.setIncludeFontPadding(false);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(placement == 1 ? Gravity.CENTER_VERTICAL | Gravity.RIGHT
                : placement == 2 ? Gravity.CENTER : Gravity.CENTER_VERTICAL | Gravity.LEFT);
        name.setVisibility(View.GONE);

        card.setFocusable(true);
        card.setTag(key);
        card.setContentDescription(label);
        card.setOnFocusChangeListener((v, has) -> {
            if (has) { focusedKey = key;  }
            iv.setAlpha(has?1:utilityIconOpacity());
            if(preserveArtworkColors)iv.clearColorFilter();else iv.setColorFilter(has ? brightenColor(accent, 1.12f) : accent,
                    android.graphics.PorterDuff.Mode.SRC_IN);
            iv.animate().cancel();iv.setScaleX(1);iv.setScaleY(1);
            
            name.animate().cancel();
            name.setBackground(null);
            if (has) {
                name.setVisibility(View.VISIBLE);
                name.setAlpha(0f);
                name.setTranslationX(placement == 1 ? -px(8) : px(8));
                name.animate().alpha(1f).translationX(0f).setDuration(motionDuration(180L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f)).start();
            } else {
                name.animate().alpha(0f).translationX(placement == 1 ? -px(8) : px(8))
                        .setDuration(motionDuration(120L)).withEndAction(() -> {
                            if (!card.hasFocus()) name.setVisibility(View.GONE);
                            name.setTranslationX(0f);
                        }).start();
            }
            applyUtilityTextColor(name);
            animateFocus(v, has, utilityFocusScale(officialArtwork));
        });
        if ("all-apps".equals(key)) card.setOnClickListener(v -> {showAllInstalledApps();});
        else card.setOnClickListener(v -> {open(key);});

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        if (placement == 1) {
            wrap.addView(name, at(0, 0, labelWidth, b));
            wrap.addView(card, at(labelWidth + labelGap, 0, b, b));
            root.addView(wrap, at(x - labelWidth - labelGap, y, labelWidth + labelGap + b, b));
        } else if (placement == 2) {
            name.setGravity(Gravity.CENTER);
            name.setPadding(menuPx(6), 0, menuPx(6), 0);
            wrap.addView(card, at(0, 0, b, b));
            int labelLeft = x + b / 2 - labelWidth / 2;
            labelLeft = Math.max(px(12), Math.min(labelLeft, screenWidth - labelWidth - px(12)));
            wrap.addView(name, at(labelLeft - x, -menuPx(50), labelWidth, menuPx(44)));
            root.addView(wrap, at(x, y, b, b));
        } else {
            wrap.addView(card, new FrameLayout.LayoutParams(b, b));
            wrap.addView(name, at(b + labelGap, 0, labelWidth, b));
            root.addView(wrap, at(x, y, b + labelGap + labelWidth, b));
        }
        DetoxPointerFocus.install(card);
        tiles.add(card);
        return iv;
    }

    private void addTopTvSettings(FrameLayout parent, int screenWidth) {
        final int b = px(topActionButtonSize()), gap = px(ICON_GAP);
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int placement = prefs.getInt("utilitiesPlacement", 0);
        boolean leftGroup = placement == 1;
        if (placement >= 2) {
            String saved = prefs.getString("clockPosition", "top-right");
            leftGroup = saved.endsWith("left");
        }
        String style = effectiveClockStyle(prefs);
        float clockScale = clockSizeScale();
        int clockWidth = px(Math.round(620f * clockScale));
        int clockHeight = px(Math.round(("digital".equals(style) ? 190f : 410f) * clockScale));
        int clockX = leftGroup ? 0 : Math.max(0, screenWidth - clockWidth - px(24));
        int clockY = 0;
        int actionY = clockY + clockHeight + px(4);
        int firstX = leftGroup ? clockX : clockX + clockWidth - 3 * b - 2 * gap;
        FrameLayout settingsAction = addTopAction(parent, TV_SETTINGS, "TV settings", LineIcon.SETTINGS,
                0xFFFFD166, firstX, actionY, leftGroup, () -> open(TV_SETTINGS));
        FrameLayout restartAction = addTopAction(parent, TV_RESTART, "Restart TV", LineIcon.RESTART,
                0xFFFF8B72, firstX + b + gap, actionY, leftGroup, this::requestTvRestart);
        FrameLayout screenAction = addTopAction(parent, SCREEN_SLEEP, "Screen off", LineIcon.SCREEN_OFF,
                0xFF8FE8FF, firstX + 2 * (b + gap), actionY, leftGroup, this::requestScreenSleep);
        final FrameLayout[] actionViews = {settingsAction, restartAction, screenAction};
        final View[] actionCards = {
                settingsAction.findViewWithTag(TV_SETTINGS),
                restartAction.findViewWithTag(TV_RESTART),
                screenAction.findViewWithTag(SCREEN_SLEEP)
        };
        for (View card : actionCards) if (card != null) card.setId(View.generateViewId());
        for (int i = 0; i < actionCards.length; i++) {
            View card = actionCards[i];
            if (card == null) continue;
            if (i > 0 && actionCards[i - 1] != null) card.setNextFocusLeftId(actionCards[i - 1].getId());
            if (i + 1 < actionCards.length && actionCards[i + 1] != null) card.setNextFocusRightId(actionCards[i + 1].getId());
        }
        parent.post(this::positionTopActionsUnderClock);
    }

    private void positionTopActionsUnderClock() {
        positionTopActionsUnderClock(0);
    }

    private void positionTopActionsUnderClock(int attempts) {
        if (root == null) return;
        View[] actionCards = {tile(TV_SETTINGS), tile(TV_RESTART), tile(SCREEN_SLEEP)};
        if (actionCards[0] == null || actionCards[1] == null || actionCards[2] == null) return;
        View activeClock = clockLayout;
        if (activeClock == null || activeClock.getWidth() <= 0 || activeClock.getHeight() <= 0) {
            if (attempts < 10) root.postDelayed(() -> positionTopActionsUnderClock(attempts + 1), 40L);
            return;
        }

        int[] rootLoc = new int[2], clockLoc = new int[2];
        root.getLocationOnScreen(rootLoc);
        activeClock.getLocationOnScreen(clockLoc);
        int button = px(topActionButtonSize()), gap = px(ICON_GAP);
        int groupWidth = 3 * button + 2 * gap;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float clockCenter = clockLoc[0] - rootLoc[0] + activeClock.getWidth() / 2f;
        int firstIconX = Math.round(clockCenter - groupWidth / 2f);
        firstIconX = Math.max(px(12), Math.min(firstIconX, screenWidth - groupWidth - px(12)));
        int clockBottom = clockLoc[1] - rootLoc[1] + activeClock.getHeight();
        if(homeConnectionStatus!=null)homeConnectionStatus.setVisibility(View.GONE);
        int actionY=clockBottom+px(18);int availableHeight=getResources().getDisplayMetrics().heightPixels;if(actionY+button+menuPx(44)>availableHeight-px(24))actionY=Math.max(px(16),clockLoc[1]-rootLoc[1]-button-menuPx(54));
        for (int i = 0; i < actionCards.length; i++) {
            View card = actionCards[i];
            View wrapper = (View) card.getParent();
            if (wrapper == null) continue;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) wrapper.getLayoutParams();
            lp.leftMargin = firstIconX + i * (button + gap) - card.getLeft();
            lp.topMargin = actionY - card.getTop();
            wrapper.setLayoutParams(lp);
        }

        List<View> firstRowApps = new ArrayList<>();
        int appRowTop = -1;
        for (String pkg : rowApps) {
            View app = tile(pkg);
            if (app == null) continue;
            int[] loc = new int[2];
            app.getLocationOnScreen(loc);
            if (appRowTop < 0 || loc[1] < appRowTop) {
                appRowTop = loc[1];
                firstRowApps.clear();
                firstRowApps.add(app);
            } else if (Math.abs(loc[1] - appRowTop) <= px(8)) firstRowApps.add(app);
        }

        List<View> topControls = new ArrayList<>();
        for (View card : actionCards) if (card != null) topControls.add(card);
        List<View> aboveAppUtilities = new ArrayList<>();
        if (getSharedPreferences(PREFS, 0).getInt("utilitiesPlacement", 0) == 3) {
            for (String key : utilityFocusKeys) {
                View utility = tile(key);
                if (utility != null) aboveAppUtilities.add(utility);
            }
            sortViewsByCenterX(aboveAppUtilities);
        }

        if (!firstRowApps.isEmpty() && !aboveAppUtilities.isEmpty()) {
            connectFocusRows(topControls, aboveAppUtilities);
            connectFocusRows(aboveAppUtilities, firstRowApps);
            for (int i = 0; i < aboveAppUtilities.size(); i++) {
                View v = aboveAppUtilities.get(i);
                if (v.getId() == View.NO_ID) v.setId(View.generateViewId());
                v.setNextFocusLeftId(i > 0 ? aboveAppUtilities.get(i - 1).getId() : View.NO_ID);
                v.setNextFocusRightId(i + 1 < aboveAppUtilities.size()
                        ? aboveAppUtilities.get(i + 1).getId() : View.NO_ID);
            }
        } else if (!firstRowApps.isEmpty()) {
            connectFocusRows(topControls, firstRowApps);
        }
        wireUtilityNavigation();
    }

    private void sortViewsByCenterX(List<View> views) {
        Collections.sort(views, (left, right) -> {
            int[] a = new int[2], b = new int[2];
            left.getLocationOnScreen(a);
            right.getLocationOnScreen(b);
            return Integer.compare(a[0] + left.getWidth() / 2, b[0] + right.getWidth() / 2);
        });
    }

    private void connectFocusRows(List<View> upper, List<View> lower) {
        if (upper.isEmpty() || lower.isEmpty()) return;
        for (View v : upper) if (v.getId() == View.NO_ID) v.setId(View.generateViewId());
        for (View v : lower) if (v.getId() == View.NO_ID) v.setId(View.generateViewId());
        for (View from : upper) {
            View nearest = nearestViewByCenterX(lower, from);
            if (nearest != null) from.setNextFocusDownId(nearest.getId());
        }
        for (View from : lower) {
            View nearest = nearestViewByCenterX(upper, from);
            if (nearest != null) from.setNextFocusUpId(nearest.getId());
        }
    }

    private View nearestViewByCenterX(List<View> candidates, View source) {
        if (candidates.isEmpty()) return null;
        int[] sourceLoc = new int[2];
        source.getLocationOnScreen(sourceLoc);
        int sourceCenter = sourceLoc[0] + source.getWidth() / 2;
        View nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (View candidate : candidates) {
            int[] loc = new int[2];
            candidate.getLocationOnScreen(loc);
            int distance = Math.abs(sourceCenter - (loc[0] + candidate.getWidth() / 2));
            if (distance < nearestDistance) { nearest = candidate; nearestDistance = distance; }
        }
        return nearest;
    }
    private FrameLayout addTopAction(FrameLayout parent, String key, String title, int[] iconOps,
            int color, int x, int y, boolean labelRight, Runnable action) {
        final int b=px(topActionButtonSize()),size=Math.max(px(24),Math.min(b-px(10),Math.round(b*.62f*utilityIconScale()))),labelWidth=px(210);
        final int labelHeight = menuPx(38), labelTop = b + px(5);
        final FrameLayout card = new FrameLayout(this);
        card.setFocusable(true);
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setElevation(0);
        card.setBackground(null);
        card.setTag(key);
        card.setContentDescription(Strings.t(title));

        ImageView icon = new ImageView(this);
        LineIcon drawable = new LineIcon(iconOps);
        drawable.setColor(utilityGlowColor(key));
        drawable.setStrokeScale(1.05f);drawable.setFitToContent(true);drawable.setFillFraction(.94f);
        Drawable officialIcon=BundledUtilityArtwork.home(this,key);
        final boolean officialArtwork=officialIcon!=null;
        final boolean preserveArtworkColors=BundledUtilityArtwork.preserveColors(officialIcon);
        icon.setImageDrawable(officialArtwork?officialIcon:drawable);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setAlpha(utilityIconOpacity());
        if(!preserveArtworkColors)icon.setColorFilter(utilityGlowColor(key),android.graphics.PorterDuff.Mode.SRC_IN);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.addView(icon, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));

        TextView name = text(menuPx(27), 0xE8FFFFFF, semibold);
        name.setText(Strings.t(title));
        applyUtilityLabelStyle(name);
        name.setBackground(null);
        name.setPadding(menuPx(14), 0, menuPx(14), 0);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        name.setVisibility(View.GONE);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        int iconLeftInGroup = (labelWidth - b) / 2;
        wrap.addView(card, at(iconLeftInGroup, 0, b, b));
        wrap.addView(name, at(0, labelTop, labelWidth, labelHeight));
        parent.addView(wrap, at(x - iconLeftInGroup, y, labelWidth, labelTop + labelHeight));

        // The full icon-and-label group is also a touch target, while the card stays focusable for remotes.
        wrap.setOnClickListener(v -> action.run());
        card.setOnFocusChangeListener((v, has) -> {
            if (has) { focusedKey = key;  }
            card.setElevation(0);icon.setAlpha(has?1:utilityIconOpacity());
            if(preserveArtworkColors)icon.clearColorFilter();else icon.setColorFilter(has ? brightenColor(utilityGlowColor(key), 1.12f) : utilityGlowColor(key),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            name.setBackground(null);
            name.animate().cancel();
            if (has) {
                name.setVisibility(View.VISIBLE);
                name.setAlpha(0f);
                int[] rootLoc = new int[2], cardLoc = new int[2];
                root.getLocationOnScreen(rootLoc);
                card.getLocationOnScreen(cardLoc);
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                float desiredLeft = cardLoc[0] - rootLoc[0] + b * 0.5f - labelWidth * 0.5f;
                float clampedLeft = Math.max(px(8), Math.min(desiredLeft, screenWidth - labelWidth - px(8)));
                name.setTranslationX(clampedLeft - desiredLeft);
                name.setTranslationY(-px(5));
                name.animate().alpha(1f).translationY(0f).setDuration(motionDuration(180L))
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f)).start();
            } else {
                name.animate().alpha(0f).translationY(-px(5)).setDuration(motionDuration(120L))
                        .withEndAction(() -> {
                            if (!card.hasFocus()) name.setVisibility(View.GONE);
                            name.setTranslationX(0f);
                            name.setTranslationY(0f);
                        }).start();
            }
            applyUtilityTextColor(name);
            animateFocus(v, has, utilityFocusScale(officialArtwork));
        });
        card.setOnClickListener(v -> {action.run();});
        DetoxPointerFocus.install(card);
        tiles.add(card);
        return wrap;
    }
    private void requestScreenSleep() {
        if (android.os.Build.VERSION.SDK_INT >= 28 && LauncherAccessibilityService.lockScreen()) return;
        detoxAlertBuilder().setTitle(Strings.t("Screen off"))
                .setMessage(Strings.t("Enable TV Detox Home Redirect in Accessibility settings to use this shortcut. Press the TV remote Power button to wake the screen."))
                .setNegativeButton(Strings.t("Cancel"), null)
                .setPositiveButton(Strings.t("Open settings"), (d, which) -> {
                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
                }).show();
    }

    private void requestTvRestart() {
        detoxAlertBuilder().setTitle(Strings.t("Restart TV?"))
                .setMessage(Strings.t("Continue to the TV power menu? You will need to choose Restart there."))
                .setNegativeButton(Strings.t("Cancel"), null)
                .setPositiveButton(Strings.t("Continue"), (d, which) -> {
                    if (LauncherAccessibilityService.showPowerDialog()) return;
                    detoxAlertBuilder().setTitle(Strings.t("Power menu unavailable"))
                            .setMessage(Strings.t("Use the TV remote's Power button and choose Restart. To open the power menu from Detox, enable TV Detox Home Redirect in Accessibility settings."))
                            .setNegativeButton(Strings.t("Close"), null)
                            .setPositiveButton(Strings.t("Open settings"), (dialog, choice) -> {
                                try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                                catch (Exception ignored) {}
                            }).show();
                }).show();
    }

    private int utilityColor(String key) {
        if (NETWORK.equals(key)) return 0xFF20C8F2;
        if (QUICK_PANEL.equals(key)) return 0xFF8CE6FF;
        if ((QUICK + "bluetooth").equals(key)) return 0xFF6B94FF;
        if ((QUICK + "picture").equals(key)) return 0xFF40E08B;
        if ((QUICK + "sound").equals(key)) return 0xFFFF9A45;
        if (PLAY_STORE.equals(key)) return 0xFFC49BFF;
        if (SETTINGS.equals(key) || TV_SETTINGS.equals(key)) return 0xFFFFD23F;
        if (TV_RESTART.equals(key)) return 0xFFFF8B72;
        if (SCREEN_SLEEP.equals(key)) return 0xFF8FE8FF;
        if ("all-apps".equals(key)) return 0xFF73E8FF;
        if (ADD.equals(key)) return 0xFFFF668E;
        return 0xFF7CB8FF;
    }

    private int utilityGlowColor(String key) {
        String preset = currentThemePreset();
        return brightenColor(THEME_NAMES[MULTICOLOR_THEME_INDEX].equals(preset) ? utilityColor(key) : uiAccentColor(), 1.18f);
    }

    private int homeThemeIndex() {
        return HomePremiumThemes.index(getSharedPreferences(PREFS, 0).getString("homePremiumTheme", "classic"));
    }

    private void showHomePremiumThemes() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        routeNextDialogBack(this::showThemesAndStyle);
        panel("Home UI themes", translated(HomePremiumThemes.NAMES), null, 0,
                homeThemeIndex(), null, selected -> {
                    prefs.edit().putString("homePremiumTheme", HomePremiumThemes.IDS[selected]).apply();
                    applyClockStyle();
                    buildTiles();
                    showThemesAndStyle();
                });
    }

    private int uiAccentColor(){
        int homeTheme = homeThemeIndex();
        int base;
        if (homeTheme > 0) base=HomePremiumThemes.ACCENTS[homeTheme];
        else {
            String saved=currentThemePreset();
            int index=Arrays.asList(THEME_NAMES).indexOf(saved);
            base=index==WALLPAPER_THEME_INDEX?wallpaperAccentColor():THEME_COLORS[index<0?0:index];
        }
        return base;
    }

    private static int brightenColor(int color, float factor) {
        return Color.rgb(Math.min(255, Math.round(Color.red(color) * factor)),
                Math.min(255, Math.round(Color.green(color) * factor)),
                Math.min(255, Math.round(Color.blue(color) * factor)));
    }

    private void open(String key) {
        Intent i;
        if ("dock:inputs".equals(key)) { showHomeInputs(); return; }
        else if ("dock:search".equals(key)) { showHomeSearchOptions(); return; }
        else if ("dock:settings".equals(key)) { showSettingsRoot(); return; }
        else if (isFolderKey(key)) {
            showFolderApps(key);
            return;
        } else if (ADD.equals(key)) {
            showAddMenu(-1);
            return;
        } else if (SETTINGS.equals(key)) {
            showSettingsRoot();
            return;
        } else if (isInput(key)) {
            openInput(key);
            return;
        } else if (NETWORK.equals(key)) {
            openNetworkSettings();
            return;
        } else if (QUICK_PANEL.equals(key)) {
            showQuickControls();
            return;
        } else if (key.startsWith(QUICK)) {
            openQuick(key.substring(QUICK.length()));
            return;
        } else if (TV_SETTINGS.equals(key)) {
            openTvSettings();
            return;
        } else {
            i = launchIntent(getPackageManager(), key);
            if (i == null) return;
        }
        launchApp(i, tile(key));
    }

    private void launchApp(Intent intent, View source) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String key = source != null && source.getTag() instanceof String ? (String) source.getTag() : null;
        String packageName = null;
        try {
            android.content.ComponentName component = intent.getComponent();
            if (component != null) packageName = component.getPackageName();
            else if (intent.resolveActivity(getPackageManager()) != null)
                packageName = intent.resolveActivity(getPackageManager()).getPackageName();
        } catch (Exception ignored) {}
        if (getPackageName().equals(packageName)) packageName = null;
        final String targetPackage = packageName;
        if (key != null) getSharedPreferences(PREFS, 0).edit()
                .putString("pendingAppReturnTransition", key).apply();
        boolean launched = false;
        try {
            if (source != null && android.os.Build.VERSION.SDK_INT >= 16) {
                ActivityOptions options = ActivityOptions.makeScaleUpAnimation(source,
                        source.getWidth() / 2, source.getHeight() / 2, 1, 1);
                startActivity(intent, options.toBundle());
            } else startActivity(intent);
            launched = true;
        } catch (Exception transitionError) {
            // Some TV app activities reject transition options; retry the normal launch.
            android.util.Log.w("DetoxLauncher", "Animated app launch failed; retrying without transition", transitionError);
            try { startActivity(intent); launched = true; }
            catch (Exception launchError) {
                DetoxDiagnostics.recordFailure("DetoxLauncher", "App launch", "could not start " + targetPackage, launchError);
            }
        }
        if (!launched) {
            getSharedPreferences(PREFS, 0).edit().remove("pendingAppReturnTransition")
                    .apply();
            detoxAlertBuilder().setTitle(Strings.t("App did not open"))
                    .setMessage(Strings.t("Android could not start this app. You can try again or open its app information."))
                    .setPositiveButton(Strings.t("Retry"), (d, w) -> {
                        try { startActivity(intent); }
                        catch (Exception retryError) {
                            DetoxDiagnostics.recordFailure("DetoxLauncher", "App launch", "retry failed for " + targetPackage, retryError);
                            Toast.makeText(this, Strings.t("App is unavailable"), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton(Strings.t("App info"), (d, w) -> openApplicationInfo(targetPackage))
                    .setNegativeButton(Strings.t("Close"), null).show();
        } else if (targetPackage != null) {
        }
    }

    private void openApplicationInfo(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxLauncher", "App management", "could not open details for " + packageName, error);
            Toast.makeText(this, Strings.t("App info is unavailable"), Toast.LENGTH_SHORT).show();
        }
    }

    /** On return, briefly spring the app tile back into place so the launch has a matching end. */
    private void animateReturnedApp() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String key = prefs.getString("pendingAppReturnTransition", "");
        if (key.isEmpty()) return;
        prefs.edit().remove("pendingAppReturnTransition").apply();
        View target = tile(key);
        if (target == null) return;
        final long start = SystemClock.uptimeMillis();
        final float baseScale = target.getScaleX();
        final long duration = motionDuration(300L);
        if (duration == 0L) return;
        final Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            float t = Math.min(1f, (SystemClock.uptimeMillis() - start) / (float) duration);
            float pulse = (float) Math.sin(Math.PI * t) * Math.min(1f, springProgress(t));
            float scale = baseScale + 0.035f * pulse;
            target.setScaleX(scale);
            target.setScaleY(scale);
            if (t < 1f) target.postOnAnimation(frame[0]);
            else {
                target.setScaleX(baseScale);
                target.setScaleY(baseScale);
            }
        };
        target.postOnAnimation(frame[0]);
    }

    private void showSettingsRoot(){if(!getSharedPreferences("settingsUndo",0).contains("snapshot"))SettingsWorkbench.checkpoint(this);pendingPanelOrigin=null;routeNextDialogBack(null);if(settingsRootDialog==null||!settingsRootDialog.isShowing())createSettingsPanel();else{populateSettingsCategory(-1,false);resizeSettingsPanelToCategory(-1,tile(SETTINGS));if(settingsRootRows!=null)settingsRootRows[Math.max(0,Math.min(SETTINGS_ITEMS.length-1,settingsRootSelection))].requestFocus();}}

    private void showSettingsCategory(int category){pendingPanelOrigin=null;int safe=Math.max(0,Math.min(SettingsMenuRoutes.categoryCount()-1,category));settingsRootSelection=safe;if(settingsRootDialog==null||!settingsRootDialog.isShowing())createSettingsPanel();populateSettingsCategory(safe,true);resizeSettingsPanelToCategory(safe,tile(SETTINGS));}

    private void returnToSettingsCategory(String id) {
        int index=SettingsMenuRoutes.categoryIndex(id);
        if(index<0)throw new IllegalArgumentException("Unknown Settings category: "+id);
        showSettingsCategory(index);
    }

    private void createSettingsPanel() {
        final View anchor = tile(SETTINGS);
        if (anchor == null) return;
        routeNextDialogBack(null);
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        int padding = menuPx(12);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackground(utilityMenuSurface());
        panel.setElevation(menuPx(18));

        settingsRootItems = new LinearLayout(this);
        settingsRootItems.setOrientation(LinearLayout.VERTICAL);
        settingsCategoryItems = new LinearLayout(this);
        settingsCategoryItems.setOrientation(LinearLayout.VERTICAL);
        settingsRootColumnActive = true;
        addSettingsColumnHeader(settingsRootItems, "Settings");
        addSettingsColumnHeader(settingsCategoryItems, "Choose a section");

        settingsRootRows = new TextView[SETTINGS_ITEMS.length];
        for (int i = 0; i < SETTINGS_ITEMS.length; i++) {
            final int category = i;
            TextView row = createSettingsRow(SETTINGS_ITEMS[i], true);
            row.setId(View.generateViewId());
            settingsRootRows[i] = row;
            row.setNextFocusRightId(row.getId());
            View.OnFocusChangeListener focus=row.getOnFocusChangeListener();row.setOnFocusChangeListener((v,has)->{if(focus!=null)focus.onFocusChange(v,has);if(has&&settingsPanelCategory>=0&&settingsPanelCategory!=category){populateSettingsCategory(-1,false);resizeSettingsPanelToCategory(-1,anchor);}});
            row.setOnClickListener(v -> {
                settingsRootSelection = category;
                populateSettingsCategory(category, true);
                resizeSettingsPanelToCategory(category, anchor);
            });
            settingsRootItems.addView(row, new LinearLayout.LayoutParams(-1, menuPx(58)));
        }
        populateSettingsCategory(-1,false);
        ScrollView rootScroll = settingsColumnScroll(settingsRootItems);
        ScrollView categoryScroll = settingsColumnScroll(settingsCategoryItems);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int available = Math.max(menuPx(280), screenWidth - menuPx(64));
        int desiredWidth = menuPx(700);
        int panelWidth = Math.min(desiredWidth, available);
        int innerWidth = Math.max(menuPx(220), panelWidth - padding * 3 - menuPx(1));
        int leftWidth = Math.min(menuPx(300), innerWidth / 2);
        int rightWidth = innerWidth - leftWidth;
        panel.addView(rootScroll, new LinearLayout.LayoutParams(leftWidth, -1));
        settingsCategoryScroll=categoryScroll;categoryScroll.setVisibility(View.GONE);
        View divider = new View(this);settingsDivider=divider;divider.setVisibility(View.GONE);
        divider.setBackgroundColor(0x26FFFFFF);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(menuPx(1), -1);
        dividerLp.leftMargin = padding / 2;
        dividerLp.rightMargin = padding / 2;
        panel.addView(divider, dividerLp);
        panel.addView(categoryScroll, new LinearLayout.LayoutParams(Math.max(menuPx(150), rightWidth), -1));

        dialog.setContentView(panel);
        dialog.setOnDismissListener(ignored -> {
            if (settingsRootDialog == dialog) {
                settingsFocusGeneration++;
                settingsRootDialog = null;
                settingsRootRows = null;
                settingsCategoryRows = null;
                settingsRootItems = null;
                settingsCategoryItems = null;
                settingsPanelCategory = -1;settingsCategoryScroll=null;settingsDivider=null;
                settingsPanelX = settingsPanelY = settingsPanelWidth = settingsPanelHeight = 0;
            }
            View settingsTile = tile(SETTINGS);
            if (settingsUtilityLabel != null)
                settingsUtilityLabel.setVisibility(settingsTile != null && settingsTile.hasFocus() ? View.VISIBLE : View.GONE);
        });
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if (android.os.Build.VERSION.SDK_INT >= 31) window.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false) ? menuPx(28) : 0);
        window.setGravity(Gravity.TOP | Gravity.LEFT);

        int[] at = new int[2];
        anchor.getLocationOnScreen(at);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int safeBottom = settingsMenuSafeBottom();
        int panelRows = SETTINGS_ITEMS.length;
        int panelHeight = Math.min(safeBottom - menuPx(24),
                menuPx(68 + panelRows * 58 + padding * 2));
        panelHeight = Math.max(menuPx(160), panelHeight);
        int placement = getSharedPreferences(PREFS, 0).getInt("utilitiesPlacement", 0);
        int anchorRight = at[0] + Math.round(anchor.getWidth() * anchor.getScaleX());
        int labelWidth = settingsUtilityLabel == null || settingsUtilityLabel.getWidth() <= 0
                ? menuPx(120) : settingsUtilityLabel.getWidth();
        int gap = menuPx(18), x;
        if (placement == 1) x = at[0] - gap - labelWidth - panelWidth;
        else x = anchorRight + gap + (placement == 2 ? labelWidth / 2 : labelWidth);
        x = Math.max(menuPx(16), Math.min(x, screenWidth - panelWidth - menuPx(16)));
        int y = at[1] + (anchor.getHeight() - panelHeight) / 2;
        y = Math.max(menuPx(16), Math.min(y, safeBottom - panelHeight));
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.x = x;
        lp.y = y;
        lp.width = panelWidth;
        lp.height = panelHeight;
        window.setAttributes(lp);
        settingsPanelX = x;
        settingsPanelY = y;
        settingsPanelWidth = panelWidth;
        settingsPanelHeight = panelHeight;

        final SettingsNavigationPolicy.BackGate settingsBackGate = new SettingsNavigationPolicy.BackGate();
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if(routeDialogShortcut(dialog,event))return true;
            if (keyCode != KeyEvent.KEYCODE_BACK) return false;
            if (!settingsBackGate.accept(keyCode, event.getAction(), event.getRepeatCount())) return true;
            if (!settingsRootColumnActive&&utilityMenuSelection.back()) {
                // Keep the adjacent column useful when backing out of a category. The old
                // empty "Choose a section" placeholder looked like a blank popup on TV.
                int row = Math.max(0, Math.min(settingsRootSelection, settingsRootRows.length - 1));
                populateSettingsCategory(-1, false);resizeSettingsPanelToCategory(-1,anchor);
                settingsRootColumnActive = true;
                settingsRootRows[row].requestFocus();
                settingsBackGate.reset(); // The same dialog remains open after closing the right column.
            } else animateDialogOut(dialog, null, true);
            return true;
        });
        dialog.show(); CompanionInput.dialog(dialog);
        settingsRootDialog = dialog;
        if (settingsUtilityLabel != null) settingsUtilityLabel.setVisibility(View.VISIBLE);
        resizeSettingsPanelToCategory(-1,anchor);animateDialogIn(window.getDecorView());
        int initial = Math.max(0, Math.min(settingsRootSelection, settingsRootRows.length - 1));
        final TextView initialRow=settingsRootRows[initial];
        final int focusGeneration=settingsFocusGeneration;
        initialRow.post(()->{
            if (!SettingsNavigationPolicy.canRestoreRootFocus(focusGeneration, settingsFocusGeneration, settingsPanelCategory)) return;
            if(settingsRootDialog!=dialog||!dialog.isShowing()||!initialRow.isAttachedToWindow())return;
            settingsRootColumnActive=true;initialRow.requestFocus();
        });
    }

    private void resizeSettingsPanelToCategory(int category,View anchor){
        Dialog dialog=settingsRootDialog;if(dialog==null||!dialog.isShowing()||anchor==null)return;Window window=dialog.getWindow();if(window==null)return;
        int available=getResources().getDisplayMetrics().widthPixels-menuPx(32),padding=menuPx(12),gap=menuPx(13);boolean expanded=category>=0;int left=settingsColumnWidth(settingsRootItems,menuPx(250),menuPx(460));int right=expanded?settingsColumnWidth(settingsCategoryItems,menuPx(280),menuPx(620)):0;int width=Math.min(available,left+padding*2+(expanded?gap+right:0));int inner=width-padding*2-(expanded?gap:0);left=expanded?Math.min(left,Math.max(menuPx(150),inner-menuPx(200))):inner;
        View rootScroll=(View)settingsRootItems.getParent();ViewGroup.LayoutParams rootLp=rootScroll.getLayoutParams();rootLp.width=left;rootScroll.setLayoutParams(rootLp);if(settingsCategoryScroll!=null){ViewGroup.LayoutParams childLp=settingsCategoryScroll.getLayoutParams();childLp.width=Math.max(menuPx(150),inner-left);settingsCategoryScroll.setLayoutParams(childLp);settingsCategoryScroll.setVisibility(expanded?View.VISIBLE:View.GONE);}if(settingsDivider!=null)settingsDivider.setVisibility(expanded?View.VISIBLE:View.GONE);
        int rows=Math.max(SETTINGS_ITEMS.length,expanded?settingsCategoryItemCount(category):0),height=Math.max(1,Math.min(settingsMenuSafeBottom()-menuPx(24),menuPx(68+rows*58+24)));int[] at=new int[2];anchor.getLocationOnScreen(at);int placement=getSharedPreferences(PREFS,0).getInt("utilitiesPlacement",0),label=settingsUtilityLabel!=null&&settingsUtilityLabel.getWidth()>0?settingsUtilityLabel.getWidth():menuPx(120);int x=placement==1?at[0]-menuPx(18)-label-width:at[0]+anchor.getWidth()+menuPx(18)+(placement==2?label/2:label);x=Math.max(menuPx(16),Math.min(x,getResources().getDisplayMetrics().widthPixels-width-menuPx(16)));int y=Math.max(menuPx(16),Math.min(at[1]+(anchor.getHeight()-height)/2,settingsMenuSafeBottom()-height));WindowManager.LayoutParams lp=window.getAttributes();lp.x=x;lp.y=y;lp.width=width;lp.height=height;window.setAttributes(lp);settingsPanelX=x;settingsPanelY=y;settingsPanelWidth=width;settingsPanelHeight=height;
    }
    private int settingsColumnWidth(LinearLayout column,int minimum,int maximum){int width=minimum;if(column!=null)for(int i=0;i<column.getChildCount();i++)if(column.getChildAt(i) instanceof TextView){TextView text=(TextView)column.getChildAt(i);width=Math.max(width,(int)Math.ceil(text.getPaint().measureText(text.getText().toString()))+text.getPaddingLeft()+text.getPaddingRight()+menuPx(12));}return Math.min(maximum,width);}

    private void populateSettingsCategory(int category, boolean requestFocus) {
        if(settingsCategoryItems==null)return;
        final int focusGeneration=++settingsFocusGeneration;
        if(category<0||category>=SettingsMenuRoutes.categoryCount()){utilityMenuSelection.open();settingsPanelCategory=-1;settingsCategoryRows=null;settingsCategoryItems.removeAllViews();if(settingsRootRows!=null)for(TextView root:settingsRootRows)root.setNextFocusRightId(root.getId());if(settingsCategoryScroll!=null)settingsCategoryScroll.setVisibility(View.GONE);if(settingsDivider!=null)settingsDivider.setVisibility(View.GONE);return;}
        utilityMenuSelection.click(category);settingsPanelCategory = category;
        if (settingsRootRows != null)
            for (TextView rootRow : settingsRootRows) rootRow.setNextFocusRightId(rootRow.getId());
        settingsCategoryItems.removeAllViews();
        SettingsMenuRoutes.Entry[] entries = SettingsMenuRoutes.children(SettingsMenuRoutes.categoryId(category));
        String[] items = new String[entries.length];
        for (int i=0;i<items.length;i++) items[i]=settingsEntryLabel(entries[i]);
        String heading = SETTINGS_ITEMS[category];
        addSettingsColumnHeader(settingsCategoryItems, heading);
        settingsCategoryRows = new TextView[items.length];
        for (int i = 0; i < items.length; i++) {
            final int choice = i;
            final int selectedCategory = category;
            TextView row = createSettingsRow(items[i], false);
            row.setId(View.generateViewId());
            row.setNextFocusLeftId(settingsRootRows[category].getId());
            settingsCategoryRows[i] = row;
            row.setOnClickListener(v -> {
                if(settingsRootDialog==null||!settingsRootDialog.isShowing())return;settingsCategorySelections[selectedCategory] = choice;
                int[] rowLocation = new int[2];
                row.getLocationOnScreen(rowLocation);
                Rect parentRow = new Rect(rowLocation[0], rowLocation[1],
                        rowLocation[0] + row.getWidth(), rowLocation[1] + row.getHeight());
                pendingPanelOrigin = parentRow;
                openSettingsItem(entries[choice].id);
                if (pendingPanelOrigin == parentRow) pendingPanelOrigin = null;
            });
            settingsCategoryItems.addView(row, new LinearLayout.LayoutParams(-1, menuPx(58)));
        }
        settingsRootRows[category].setNextFocusRightId(settingsCategoryRows[0].getId());
        if (requestFocus) {
            int selected = Math.max(0, Math.min(settingsCategorySelections[category], items.length - 1));
            final TextView target=settingsCategoryRows[selected];final Dialog owner=settingsRootDialog;target.post(()->{
                if (!SettingsNavigationPolicy.canRestoreCategoryFocus(focusGeneration, settingsFocusGeneration, category, settingsPanelCategory)) return;
                if(owner==null||settingsRootDialog!=owner||!owner.isShowing()||!target.isAttachedToWindow())return;
                settingsRootColumnActive=false;target.requestFocus();
            });
        }
    }
    private int settingsCategoryItemCount(int category) { return SettingsMenuRoutes.count(category); }

    /** Resize the two-column menu to its selected category instead of reserving space for the longest one. */


    /** Reserve the launcher app row so menus cannot cover or steal focus from app tiles. */
    private int settingsMenuSafeBottom() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int lowestRowY = -1;
        for (String pkg : rowApps) {
            View app = tile(pkg);
            if (app == null || app.getVisibility() != View.VISIBLE || app.getHeight() <= 0) continue;
            int[] location = new int[2];
            app.getLocationOnScreen(location);
            lowestRowY = Math.max(lowestRowY, location[1]);
        }
        if (lowestRowY < 0) return screenHeight - menuPx(24);
        int bottomRowTop = screenHeight;
        for (String pkg : rowApps) {
            View app = tile(pkg);
            if (app == null || app.getVisibility() != View.VISIBLE || app.getHeight() <= 0) continue;
            int[] location = new int[2];
            app.getLocationOnScreen(location);
            if (location[1] >= lowestRowY - px(12)) bottomRowTop = Math.min(bottomRowTop, location[1]);
        }
        return Math.max(menuPx(180), Math.min(screenHeight - menuPx(20), bottomRowTop - menuPx(18)));
    }

    private ScrollView settingsColumnScroll(View content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(false);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private void addSettingsColumnHeader(LinearLayout column, String titleText) {
        TextView title = text(menuPx(23), 0xC9D7E4F3, semibold);
        title.setText(Strings.t(titleText));
        title.setTypeface(semibold);
        title.getPaint().setShader(null);
        styleMenuHeading(title);
        title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setBackground(null);title.setPadding(menuPx(12),0,menuPx(12),0);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-2,menuPx(64));
        titleParams.leftMargin=menuPx(6);titleParams.bottomMargin=menuPx(5);
        column.addView(title,titleParams);
        View accent = new View(this);
        GradientDrawable accentBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x38FFFFFF, 0x18FFFFFF, 0x00FFFFFF});
        accentBg.setCornerRadius(menuPx(2));
        accent.setBackground(accentBg);
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(menuPx(150), menuPx(4));
        accentLp.leftMargin = menuPx(12);
        accentLp.bottomMargin = menuPx(9);
        column.addView(accent, accentLp);
    }

    private TextView createSettingsRow(String label, boolean rootColumn) {
        TextView row = text(menuPx(rootColumn?26:28), Color.WHITE, semibold);
        row.setText(Strings.t(label)+(rootColumn?"   ›":""));row.setContentDescription(Strings.t(label));
        row.setTypeface(semibold);
        row.getPaint().setShader(null);styleMenuText(row);
        row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(menuPx(18), 0, menuPx(18), 0);
        row.setBackground(settingsMenuFocusBackground(false,false));
        row.setFocusable(true);
        row.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) settingsRootColumnActive = rootColumn;
            boolean contrast = highContrastFocusEnabled();
            row.setBackground(settingsMenuFocusBackground(hasFocus,contrast));
            styleMenuText(row);
            animateFocus(row,hasFocus,contrast?1.015f:1.006f);
            // Settings columns scroll independently. Keep the focused row fully visible
            // after DPAD navigation, including when the right column has many entries.
            if (hasFocus) keepSettingsRowVisible(row);
        });
        return row;
    }


    /** Scroll the containing settings column only after layout has settled.
     *  The focus/attachment checks prevent a queued scroll from moving an old panel. */
    private void keepSettingsRowVisible(final View row) {
        row.post(() -> {
            if (!row.isAttachedToWindow() || !row.hasFocus()) return;
            if (settingsRootDialog == null || !settingsRootDialog.isShowing()) return;
            android.graphics.Rect bounds = new android.graphics.Rect(0, 0, row.getWidth(), row.getHeight());
            row.requestRectangleOnScreen(bounds, true);
        });
    }

    private void openSettingsItem(String action) {
        if (settingsRootDialog != null) settingsRootDialog.dismiss();
        openSettingsAction(action);
    }
    
    private void openSettingsAction(String action) {
        SettingsMenuRoutes.Entry entry=SettingsMenuRoutes.find(action);
        if(entry==null)throw new IllegalArgumentException("Unknown Settings action: "+action);
        if(entry.group()){showSettingsGroup(action);return;}
        routeNextDialogBack(()->showSettingsGroup(entry.parent));
        switch(action){
            case "wallpaper.choose": showWallpapers(); break;
            case "wallpaper.slideshow": showWallpaperSlideshowOptions(); break;
            case "wallpaper.fit": showWallpaperFraming(); break;
            case "video.builtin": showBuiltInLiveVideoPicker(); break;
            case "video.import": showVideoWallpaperPicker(); break;
            case "video.playback": showLiveWallpaperOptions(); break;
            case "effects.style": showWallpaperMotionStyle(); break;
            case "effects.enabled": toggleWallpaperMotion(); break;
            case "idle.motion": showIdleMotionStyle(); break;
            case "idle.preview": previewIdleWallpaper(); break;
            case "idle.enabled": toggleIdleWallpaper(); break;
            case "idle.timeout": showIdleTimeout(); break;
            case "idle.transition": showIdleTransition(); break;
            case "idle.welcome": toggleIdleWelcome(); break;
            case "idle.clock": toggleIdleClock(); break;
            case "idle.shade": showIdleShade(); break;
            case "idle.clockSize": showIdleClockSize(); break;
            case "idle.clockPosition": showIdleClockPosition(); break;
            case "idle.date": toggleIdleDate(); break;
            case "idle.alarm": toggleIdleAlarm(); break;
            case "idle.hint": toggleIdleHint(); break;
            case "font.family": showFontOptions(); break;
            case "font.size": showTextSizeOptions(); break;
            case "font.color": showFontColorOptions(); break;
            case "font.appNames": showAppNameTextOptions(); break;
            case "style.color": showThemeOptions(); break;
            case "style.homeTheme": showHomePremiumThemes(); break;
            case "style.iconPack": showIconPackOptions(); break;
            case "style.blur": toggleMenuBlur(); break;
            case "clock.blend": toggleClockBlend(); break;
            case "style.header": showHomeHeaderOptions(); break;
            case "apps.grid": showAppLayoutOptions(); break;
            case "apps.iconSize": showAppIconSizeOptions(); break;
            case "apps.manage": showManageHomeApps(); break;
            case "apps.add": showAddHomeApp(); break;
            case "apps.tiles": showAppTileStyleOptions(); break;
            case "apps.headings": toggleHomeHeadings(); break;
            case "apps.down": showHomeDownLaunchOptions(); break;
            case "apps.media": toggleHomeMediaCards(); break;
            case "animation.preset": showLauncherAnimationOptions(); break;
            case "animation.glow": showFocusGlowOptions(); break;
            case "animation.contrast": showHighContrastFocusOptions(); break;
            case "animation.artwork": showAppArtworkVisibilityOptions(); break;
            case "clock.style": showClockOptions(); break;
            case "clock.color": showClockColorOptions(); break;
            case "clock.size": showClockSizeOptions(); break;
            case "clock.position": showClockPositionOptions(); break;
            case "clock.visibility": showClockVisibilityOptions(); break;
            case "clock.date": showClockDateOptions(); break;
            case "clock.display": showClockDisplayOptions(); break;
            case "clock.tools": showClockUtilities(); break;
            case "utility.visibility": showUtilityVisibilityOptions(); break;
            case "utility.size": showUtilityIconSizeOptions(); break;
            case "utility.position": showUtilitiesPositionOptions(); break;
            case "utility.text": showUtilityTextColorOptions(); break;
            case "utility.opacity": showUtilityOpacityOptions(); break;
            case "utility.motion": showUtilityMotionOptions(); break;
            case "utility.showAll": showAllUtilityIcons(); break;
            case "utility.quick": showQuickControlCustomization(); break;
            case "utility.reset": showUtilityResetConfirmation(); break;
            case "performance.overview": showPerformanceOverview(); break;
            case "performance.preset": showPerformancePreset(); break;
            case "performance.frames": showPerformanceSampling(); break;
            case "backup.transfer": showBackupRestoreOptions(); break;
            case "backup.profiles": showHomeProfiles(); break;
            case "backup.checkpoint": saveSettingsCheckpoint(); break;
            case "backup.undo": restoreSettingsCheckpoint(); break;
            case "system.shortcuts": showRemoteShortcutOptions(); break;
            case "system.sleep": showSleepTimerOptions(); break;
            case "system.android": consumeNextDialogBackAction(); openTvSettings(); break;
            case "system.about": showAbout(); break;
            case "system.search": showSettingsSearch(); break;
            case "phone.pair": showPhoneCompanion(); break;
            default: throw new IllegalStateException("Unbound Settings action: "+action);
        }
    }


    private List<String> settingsChoiceLabels(List<SettingsChoice> rows){List<String> labels=new ArrayList<>();for(SettingsChoice row:rows)labels.add(Strings.t(row.label));return labels;}
    private String settingsEntryLabel(SettingsMenuRoutes.Entry entry){
        SharedPreferences p=getSharedPreferences(PREFS,0);String key=null;boolean fallback=true;
        switch(entry.id){
            case "effects.enabled":return "Wallpaper Motion · "+(p.getBoolean("wallpaperMotion",true)&&!"off".equals(normalizeMotionStyle(p.getString("wallpaperMotionStyle","ambient")))?"On":"Off");
            case "idle.enabled":key="idleWallpaperEnabled";break;
            case "idle.welcome":key="cinematicWelcome";break;
            case "idle.clock":key="ambientClockEnabled";break;
            case "idle.date":key="idleShowDate";break;
            case "idle.alarm":key="idleShowAlarm";break;
            case "idle.hint":key="idleShowHint";break;
            case "apps.headings":key="homeSectionHeadings";break;
            case "apps.media":key="homeMediaCards";fallback=false;break;
            case "style.blur":key="menuBlur";fallback=false;break;
            case "clock.blend":key="clockWallpaperBlend";fallback=false;break;
        }
        return key==null?entry.label:entry.label.replace(" On/Off", "")+" · "+(p.getBoolean(key,fallback)?"On":"Off");
    }
    private void showSettingsGroup(String group){
        SettingsMenuRoutes.Entry parent=SettingsMenuRoutes.find(group);
        if(parent==null||!parent.group())throw new IllegalArgumentException("Unknown Settings group: "+group);
        if(parent.parent.isEmpty()){returnToSettingsCategory(group);return;}
        List<SettingsMenuRoutes.Entry> visible=new ArrayList<>();
        String face=effectiveClockStyle(getSharedPreferences(PREFS,0));
        for(SettingsMenuRoutes.Entry entry:SettingsMenuRoutes.children(group))
            if(!"clock.display".equals(entry.id)||ClockFacePolicy.premium(face)||"digital".equals(face))visible.add(entry);
        SettingsMenuRoutes.Entry[] entries=visible.toArray(new SettingsMenuRoutes.Entry[0]);List<String> labels=new ArrayList<>();int selected=0;
        for(int i=0;i<entries.length;i++){labels.add(settingsEntryLabel(entries[i]));if(entries[i].id.equals(settingsGroupSelections.get(group)))selected=i;}
        routeNextDialogBack(()->showSettingsGroup(parent.parent));
        panel(Strings.t(parent.label),labels,null,0,selected,null,choice->{settingsGroupSelections.put(group,entries[choice].id);openSettingsAction(entries[choice].id);});
    }
    private void showThemesAndStyle(){showSettingsGroup(SettingsMenuRoutes.STYLE);}
    private void settingsBoolean(String key,boolean fallback,String parent,Runnable apply){
        SharedPreferences p=getSharedPreferences(PREFS,0);p.edit().putBoolean(key,!p.getBoolean(key,fallback)).apply();
        consumeNextDialogBackAction();if(apply!=null)apply.run();showSettingsGroup(parent);
    }
    private void settingsChoice(String title,String key,String[] labels,int[] values,int fallback,String parent,Runnable apply){
        SharedPreferences p=getSharedPreferences(PREFS,0);int current=p.getInt(key,fallback),selected=0;
        for(int i=0;i<values.length;i++)if(values[i]==current)selected=i;
        routeNextDialogBack(()->showSettingsGroup(parent));
        panel(title,translated(labels),null,0,selected,null,i->{p.edit().putInt(key,values[i]).apply();if(apply!=null)apply.run();showSettingsGroup(parent);});
    }
    private void showAppNameTextOptions(){settingsChoice("App Labels","homeAppLabels",new String[]{"On focus","Always visible","Hidden"},new int[]{0,1,2},0,SettingsMenuRoutes.ICONS,this::buildTiles);}
    private void showAppTileStyleOptions(){nextPanelHomeDiagram=HomeSettingsPreview.TILES;settingsChoice("App Tile Style","homeTileStyle",new String[]{"Floating artwork","Framed tiles","Soft glass"},new int[]{0,1,2},1,SettingsMenuRoutes.ICONS,this::buildTiles);}
    private void showHomeHeaderOptions(){
        nextPanelHomeDiagram=HomeSettingsPreview.HEADER;
        SharedPreferences p=getSharedPreferences(PREFS,0);routeNextDialogBack(this::showThemesAndStyle);
        panel("Home Screen Header",translated(new String[]{"Compact","Full"}),null,0,compactHomeHeader()?0:1,null,i->{p.edit().putBoolean("homeCompactHeader",i==0).apply();applyClockStyle();buildTiles();showThemesAndStyle();});
    }
    private void showFocusGlowOptions(){
        SharedPreferences p=getSharedPreferences(PREFS,0);routeNextDialogBack(()->returnToSettingsCategory(SettingsMenuRoutes.ANIMATIONS));
        panel("Focus Glow",translated(new String[]{"Steady","Breathing"}),null,0,p.getBoolean("homeFocusPulse",true)?1:0,null,i->{p.edit().putBoolean("homeFocusPulse",i==1).apply();buildTiles();returnToSettingsCategory(SettingsMenuRoutes.ANIMATIONS);});
    }
    private void toggleHomeHeadings(){settingsBoolean("homeSectionHeadings",true,SettingsMenuRoutes.APPS,this::buildTiles);}
    private void toggleHomeMediaCards(){settingsBoolean("homeMediaCards",false,SettingsMenuRoutes.APPS,this::refreshHomeMediaCards);}
    private void toggleMenuBlur(){settingsBoolean("menuBlur",false,SettingsMenuRoutes.STYLE,null);}
    private void toggleClockBlend(){settingsBoolean("clockWallpaperBlend",false,SettingsMenuRoutes.CLOCK_GROUP,this::applyClockStyle);}
    private void showAllUtilityIcons(){SharedPreferences.Editor edit=getSharedPreferences(PREFS,0).edit();for(String key:allUtilityVisibilityKeys())edit.remove("utilityHidden_"+key);edit.apply();buildTiles();showUtilitySettings();}
    private void showManageHomeApps(){
        List<String> apps=new ArrayList<>(rowApps);List<String> labels=new ArrayList<>();for(String key:apps)labels.add(isFolderKey(key)?folderName(key):labelOf(key));
        if(apps.isEmpty()){showAddHomeApp();return;}
        routeNextDialogBack(()->returnToSettingsCategory(SettingsMenuRoutes.APPS));
        panel("Manage Home Apps · hold OK also opens app options",labels,null,0,-1,null,i->{routeNextDialogBack(this::showManageHomeApps);showEditMenu(apps.get(i));});
    }
    private void showAddHomeApp(){routeNextDialogBack(()->returnToSettingsCategory(SettingsMenuRoutes.APPS));showAddMenu(-1);}
    private void showWallpaperMotionStyle(){
        SharedPreferences p=getSharedPreferences(PREFS,0);String saved=normalizeMotionStyle(p.getString("wallpaperMotionStyle","ambient"));int selected=0;
        for(int i=0;i<MOTION_KEYS.length;i++)if(MOTION_KEYS[i].equals(saved))selected=i;
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.EFFECTS));
        panel("Wallpaper Motion Style",translated(MOTION_NAMES),null,0,selected,i->setMotionPreview(MOTION_KEYS[i]),i->{p.edit().putString("wallpaperMotionStyle",MOTION_KEYS[i]).apply();previewMotionStyle=null;applyMotionPreferences();showSettingsGroup(SettingsMenuRoutes.EFFECTS);});
    }
    private void toggleWallpaperMotion(){
        SharedPreferences p=getSharedPreferences(PREFS,0);
        boolean next=!(p.getBoolean("wallpaperMotion",true)&&!"off".equals(normalizeMotionStyle(p.getString("wallpaperMotionStyle","ambient"))));
        SharedPreferences.Editor edit=p.edit().putBoolean("wallpaperMotion",next);
        if(next&&"off".equals(p.getString("wallpaperMotionStyle","ambient")))edit.putString("wallpaperMotionStyle","ambient");
        edit.apply();consumeNextDialogBackAction();applyMotionPreferences();showSettingsGroup(SettingsMenuRoutes.EFFECTS);
    }
    private void toggleIdleWallpaper(){settingsBoolean("idleWallpaperEnabled",true,SettingsMenuRoutes.IDLE_BEHAVIOR,this::resetIdleTimers);}
    private void showIdleTimeout(){settingsChoice("Start Idle Wallpaper After","idleWallpaperMinutes",new String[]{"1 minute","3 minutes","5 minutes","10 minutes","15 minutes","30 minutes"},new int[]{1,3,5,10,15,30},10,SettingsMenuRoutes.IDLE_BEHAVIOR,this::resetIdleTimers);}
    private void showIdleTransition(){
        SharedPreferences p=getSharedPreferences(PREFS,0);String[] values={"fade-zoom","fade","zoom","none"};int selected=Arrays.asList(values).indexOf(p.getString("idleWallpaperTransition","fade-zoom"));routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.IDLE_BEHAVIOR));
        panel("Idle Transition",translated(new String[]{"Fade and zoom","Fade","Zoom","None"}),null,0,Math.max(0,selected),null,i->{p.edit().putString("idleWallpaperTransition",values[i]).apply();showSettingsGroup(SettingsMenuRoutes.IDLE_BEHAVIOR);});
    }
    private void showIdleMotionStyle(){
        SharedPreferences p=getSharedPreferences(PREFS,0);List<String> values=new ArrayList<>(),labels=new ArrayList<>();values.add("same");labels.add("Same as Home");values.addAll(Arrays.asList(MOTION_KEYS));labels.addAll(translated(MOTION_NAMES));
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.IDLE));
        panel("Idle Motion Style",labels,null,0,Math.max(0,values.indexOf(p.getString("idleWallpaperMotionStyle","same"))),null,i->{p.edit().putString("idleWallpaperMotionStyle",values.get(i)).apply();applyMotionPreferences();showSettingsGroup(SettingsMenuRoutes.IDLE);});
    }
    private void previewIdleWallpaper(){consumeNextDialogBackAction();ambientPreviewPending=true;if(screenHost!=null)screenHost.post(this::tryAmbientPreview);}
    private void toggleIdleWelcome(){settingsBoolean("cinematicWelcome",true,"idle.settings",null);}
    private void toggleIdleClock(){settingsBoolean("ambientClockEnabled",true,"idle.settings",null);}
    private void toggleIdleDate(){settingsBoolean("idleShowDate",true,"idle.settings",null);}
    private void toggleIdleAlarm(){settingsBoolean("idleShowAlarm",true,"idle.settings",null);}
    private void toggleIdleHint(){settingsBoolean("idleShowHint",true,"idle.settings",null);}
    private void showIdleShade(){showIdleValueOptions("Wallpaper Shading","idleWallpaperShade",new int[]{0,20,35,54,70},54);}
    private void showIdleClockSize(){showIdleValueOptions("Idle Clock Size","idleClockScale",new int[]{70,85,100,115,130},100);}
    private void showIdleClockPosition(){settingsChoice("Idle Clock Position","idleClockPosition",new String[]{"Left","Center","Right"},new int[]{0,1,2},0,"idle.settings",null);}
    private void showWallpaperFraming(){consumeNextDialogBackAction();SettingsWorkbench.frame(this,()->returnToSettingsCategory(SettingsMenuRoutes.WALLPAPER));}
    private void saveSettingsCheckpoint(){consumeNextDialogBackAction();SettingsWorkbench.checkpoint(this);Toast.makeText(this,"Settings checkpoint saved",Toast.LENGTH_SHORT).show();showSettingsGroup(SettingsMenuRoutes.BACKUP);}
    private void restoreSettingsCheckpoint(){consumeNextDialogBackAction();SettingsWorkbench.undo(this,()->showSettingsGroup(SettingsMenuRoutes.BACKUP));}
    private void showAbout(){
        AlertDialog dialog=detoxAlertBuilder().setTitle(Strings.t("About Detox"))
                .setMessage(DetoxAboutInfo.summary(this))
                .setPositiveButton(Strings.t("Back"),(d,which)->returnToSettingsCategory(SettingsMenuRoutes.SYSTEM)).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
    private void showSettingsSearch(){
        consumeNextDialogBackAction();List<String> names=new ArrayList<>();List<Runnable> routes=new ArrayList<>();
        for(SettingsMenuRoutes.Entry entry:SettingsMenuRoutes.all())if(!entry.group()&&!"system.search".equals(entry.id)){
            String path=entry.label;SettingsMenuRoutes.Entry parent=SettingsMenuRoutes.find(entry.parent);while(parent!=null){path=parent.label+" / "+path;parent=SettingsMenuRoutes.find(parent.parent);}
            names.add(path);routes.add(()->openSettingsAction(entry.id));
        }
        SettingsWorkbench.search(this,names.toArray(new String[0]),routes.toArray(new Runnable[0]),()->returnToSettingsCategory(SettingsMenuRoutes.SYSTEM));
    }


    private void showHomeDownLaunchOptions() {
        PackageManager pm=getPackageManager();
        TreeMap<String,String> byLabel=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String,String> labelByPackage=new java.util.HashMap<>();
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo info:pm.queryIntentActivities(query,0)){
                String pkg=info.activityInfo.packageName;
                if(pkg.equals(getPackageName())||labelByPackage.containsKey(pkg)||launchIntent(pm,pkg)==null)continue;
                String label=labelOf(pkg),unique=label;
                for(int suffix=2;byLabel.containsKey(unique)&&!pkg.equals(byLabel.get(unique));suffix++)unique=label+" ("+suffix+")";
                byLabel.put(unique,pkg);labelByPackage.put(pkg,unique);
            }
        }
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        String current=prefs.getString(HOME_DOWN_LAUNCH_PACKAGE,"@gaming");
        List<String> appLabels=new ArrayList<>(),packages=new ArrayList<>();
        appLabels.add("Gaming screen");packages.add("@gaming");
        appLabels.add("Off");packages.add("");
        for(Map.Entry<String,String> entry:byLabel.entrySet()){appLabels.add(entry.getKey());packages.add(entry.getValue());}
        int selected=Math.max(0,packages.indexOf(current));
        routeNextDialogBack(()->returnToSettingsCategory(SettingsMenuRoutes.APPS));
        panel(Strings.t("Down from bottom row"),translated(appLabels),null,0,selected,null,which->{
            prefs.edit().putString(HOME_DOWN_LAUNCH_PACKAGE,packages.get(which)).apply();
            returnToSettingsCategory(SettingsMenuRoutes.APPS);
        });
    }

    private boolean launchHomeDownShortcut(String sourceKey) {
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        String packageName=prefs.getString(HOME_DOWN_LAUNCH_PACKAGE,"@gaming");
        if(packageName.isEmpty())return false;
        if("@gaming".equals(packageName)){openGamingScreen();return true;}
        Intent intent=launchIntent(getPackageManager(),packageName);
        if(intent==null){
            prefs.edit().remove(HOME_DOWN_LAUNCH_PACKAGE).apply();
            Toast.makeText(this,Strings.t("The selected app is no longer available"),Toast.LENGTH_LONG).show();
            return false;
        }
        launchApp(intent,tile(sourceKey));
        return true;
    }
    private void showAppIconSizeOptions(){
        SharedPreferences prefs=getSharedPreferences(PREFS,0);int[] sizes={60,75,90,100};String[] names={"Compact · 60%","Small · 75%","Medium · 90%","Full · 100%"};int current=prefs.getInt("appIconScalePercent",100),selected=3;for(int i=0;i<sizes.length;i++)if(sizes[i]==current)selected=i;
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.ICONS));
        panel("App icon size",translated(names),null,0,selected,null,i->{prefs.edit().putInt("appIconScalePercent",sizes[i]).apply();buildTiles();showSettingsGroup(SettingsMenuRoutes.ICONS);});
    }
    private void showAppLayoutOptions() {
        nextPanelHomeDiagram=HomeSettingsPreview.LAYOUT;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int selected = appLayoutMode();
        routeNextDialogBack(() -> returnToSettingsCategory(SettingsMenuRoutes.APPS));
        String[] layouts = {"Balanced · 6 across", "Comfortable · 4 larger tiles",
                "More apps · 8 across"};
        panel(Strings.t("App layout"), translated(layouts), null, 0, selected, null, which -> {
            prefs.edit().putInt("appLayoutMode", which).apply();
            buildTiles();
            returnToSettingsCategory(SettingsMenuRoutes.APPS);
        });
    }

    private void showVideoWallpaperMenu(){showSettingsGroup(SettingsMenuRoutes.VIDEO);}

    private void showBuiltInLiveVideoPicker() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int selected = BuiltInLiveVideoCatalog.indexOfAsset(prefs.getString("liveWallpaperVideoAsset", ""));
        routeNextDialogBack(this::showVideoWallpaperMenu);
        panel(Strings.t("Built-in live videos"), translated(BuiltInLiveVideoCatalog.NAMES),
                null, 0, selected, null, this::applyBuiltInLiveVideoWallpaper);
    }

    private void applyBuiltInLiveVideoWallpaper(int index) {
        String asset = BuiltInLiveVideoCatalog.assetAt(index);
        wallpaperPhotoImportGeneration++;
        videoCropGeneration++;
        videoImportToken++;
        stopVideoWallpaper();
        pendingVideoWallpaperName=BuiltInLiveVideoCatalog.nameAt(index);
        getSharedPreferences(PREFS, 0).edit()
                .putString("liveWallpaperVideoAsset", asset)
                .putString("liveWallpaperVideoName", BuiltInLiveVideoCatalog.nameAt(index))
                .remove("liveWallpaperVideoPath").remove("liveWallpaperVideoUri")
                .putString("liveWallpaperScene", "video").putBoolean("liveWallpaperEnabled", true)
                .putBoolean("liveWallpaperCrop", true).putFloat("liveWallpaperCropScale", 1f)
                .apply();
        configureWallpaperWindow();
        scheduleWallpaperSlideshow();
        Toast.makeText(this, Strings.t("Loading video: ") + BuiltInLiveVideoCatalog.nameAt(index), Toast.LENGTH_SHORT).show();
        showLiveWallpaperOptions();
    }

    private void showInterfaceMotionOptions(){returnToSettingsCategory(SettingsMenuRoutes.ANIMATIONS);}

    private boolean highContrastFocusEnabled() {
        return getSharedPreferences(PREFS, 0).getBoolean("highContrastFocus", false);
    }

    private int appArtworkIdlePercent() {
        return Math.max(20, Math.min(100,
                getSharedPreferences(PREFS, 0).getInt("appArtworkIdlePercent", 85)));
    }

    private void updateAppArtworkOpacity(int percent) {
        float alpha = Math.max(0.20f, Math.min(1f, percent / 100f));
        for (View appTile : tiles) {
            Object tag = appTile.getTag();
            if (!(tag instanceof String) || !rowApps.contains((String) tag) || isFolderKey((String) tag)) continue;
            View artwork = appTile.findViewWithTag("tile-artwork");
            if (artwork instanceof ImageView) ((ImageView) artwork).setAlpha(appTile.hasFocus() ? 1f : alpha);
        }
    }

    private void showAppArtworkVisibilityOptions() {
        final int[] values = {20, 25, 30, 40, 55, 70, 85, 100};
        String[] labels = new String[values.length];
        int selected = 5;
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i] + "% while idle · 100% focused";
            if (values[i] == appArtworkIdlePercent()) selected = i;
        }
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.ICONS));
        panel(Strings.t("Icon Visibility"), translated(labels), null, 0, selected, which -> {
            updateAppArtworkOpacity(values[which]);
        }, which -> {
            getSharedPreferences(PREFS, 0).edit().putInt("appArtworkIdlePercent", values[which]).apply();
            updateAppArtworkOpacity(values[which]);
            showSettingsGroup(SettingsMenuRoutes.ICONS);
        });
    }

    private void showHighContrastFocusOptions() {
        routeNextDialogBack(this::showInterfaceMotionOptions);
        boolean enabled = highContrastFocusEnabled();
        String[] choices = {"Off", "On · bright outline and stronger focus fill"};
        panel(Strings.t("High-contrast focus"), translated(choices), null, 0, enabled ? 1 : 0, null, which -> {
            getSharedPreferences(PREFS, 0).edit().putBoolean("highContrastFocus", which == 1).apply();
            buildTiles();
            showInterfaceMotionOptions();
        });
    }

    private void showIconPackOptions() {
        routeNextDialogBack(() -> showSettingsGroup(SettingsMenuRoutes.ICONS));
        List<AppIconPackCatalog.Pack> packs=AppIconPackCatalog.discover(this);
        List<String> labels=new ArrayList<>();for(AppIconPackCatalog.Pack pack:packs)labels.add(pack.name);
        int selected=AppIconPackCatalog.selected(packs,getSharedPreferences(PREFS,0).getString("iconPackPackage",""));
        panel(Strings.t("Icon Pack"),labels,pos->{
            if(pos==0)return new ColorDrawable(Color.TRANSPARENT);
            try{return fit(getPackageManager().getApplicationIcon(packs.get(pos).packageName),menuPx(48),menuPx(48));}
            catch(Exception ignored){return new ColorDrawable(Color.TRANSPARENT);}
        },px(48),selected,null,which->{
            getSharedPreferences(PREFS,0).edit().putString("iconPackPackage",packs.get(which).packageName).apply();
            buildTiles();showSettingsGroup(SettingsMenuRoutes.ICONS);
        });
    }

    private boolean isFolderKey(String key) { return key != null && key.startsWith(FOLDER_PREFIX); }
    private String folderNameKey(String folderKey) { return "folderName:" + folderKey; }
    private String folderAppsKey(String folderKey) { return "folderApps:" + folderKey; }
    private String folderName(String folderKey) {
        return getSharedPreferences(PREFS, 0).getString(folderNameKey(folderKey), Strings.t("Folder"));
    }
    private List<String> folderPackages(String folderKey) {
        String saved = getSharedPreferences(PREFS, 0).getString(folderAppsKey(folderKey), "");
        List<String> out = new ArrayList<>();
        for (String pkg : saved.split(",")) if (!pkg.trim().isEmpty() && !out.contains(pkg.trim())) out.add(pkg.trim());
        return out;
    }

    private void showRemoteShortcutOptions() {
        routeNextDialogBack(() -> returnToSettingsCategory(SettingsMenuRoutes.SYSTEM));
        SharedPreferences prefs=getSharedPreferences(PREFS,0);List<SettingsChoice> rows=new ArrayList<>();
        for(int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++)rows.add(new SettingsChoice("slot","Shortcut "+(slot+1)+" · "+remoteShortcutSummary(prefs,slot),Integer.toString(slot)));
        rows.add(new SettingsChoice("clear","Clear all shortcuts"));
        panel(Strings.t("Remote shortcuts · while using Detox"),settingsChoiceLabels(rows),null,0,-1,null,index->{
            SettingsChoice row=rows.get(index);
            if("slot".equals(row.id)){pendingShortcutSlot=Integer.parseInt(row.value);showRemoteShortcutActionPicker(pendingShortcutSlot);}
            else if("clear".equals(row.id)){
                SharedPreferences.Editor edit=prefs.edit();for(int slot=0;slot<REMOTE_SHORTCUT_SLOTS;slot++)edit.remove(remoteShortcutKeyKey(slot)).remove(remoteShortcutActionKey(slot)).remove(remoteShortcutPackageKey(slot)).remove("remoteShortcutGesture"+slot);
                edit.remove("remoteShortcutKeyCode").remove("remoteShortcutKind").remove("remoteShortcutPackage").apply();showRemoteShortcutOptions();
            }
        });
    }

    private String remoteShortcutSummary(SharedPreferences prefs, int slot) {
        int code = prefs.getInt(remoteShortcutKeyKey(slot), slot == 0
                ? prefs.getInt("remoteShortcutKeyCode", KeyEvent.KEYCODE_UNKNOWN) : KeyEvent.KEYCODE_UNKNOWN);
        if (code == KeyEvent.KEYCODE_UNKNOWN) return "Not assigned";
        String action = prefs.getString(remoteShortcutActionKey(slot),
                slot == 0 ? prefs.getString("remoteShortcutKind", "app") : "app");
        String target = "voice".equals(action) ? "Voice search" : "settings".equals(action) ? "Detox settings"
                : "gaming".equals(action) ? "Gaming screen" : "wallpaper".equals(action) ? "Choose wallpaper" : "all-apps".equals(action) ? "All apps"
                : labelOf(prefs.getString(remoteShortcutPackageKey(slot), prefs.getString("remoteShortcutPackage", "")));
        String gesture = prefs.getString("remoteShortcutGesture" + slot, "tap");
        return KeyEvent.keyCodeToString(code).replace("KEYCODE_", "") + " · " + target
                + " · " + ("hold".equals(gesture) ? "hold" : "tap");
    }

    private void showRemoteShortcutActionPicker(int slot) {
        pendingShortcutSlot=slot;routeNextDialogBack(this::showRemoteShortcutOptions);
        List<SettingsChoice> rows=Arrays.asList(new SettingsChoice("app","Launch an app"),new SettingsChoice("voice","Voice search"),new SettingsChoice("settings","Open Detox settings"),new SettingsChoice("wallpaper","Choose wallpaper"),new SettingsChoice("all-apps","Show all apps"),new SettingsChoice("gaming","Open gaming screen"),new SettingsChoice("test","Test this saved shortcut"));
        panel(Strings.t("Choose shortcut action"),settingsChoiceLabels(rows),null,0,-1,null,index->{
            String action=rows.get(index).id;
            if("test".equals(action)){fireRemoteShortcut(slot);return;}
            if("app".equals(action)){showRemoteShortcutAppPicker();return;}
            pendingShortcutPackage=null;pendingShortcutKind=action;showRemoteShortcutGesturePicker(action,null,slot);
        });
    }

    private void showRemoteShortcutGesturePicker(String kind, String packageName, int slot) {
        routeNextDialogBack(() -> showRemoteShortcutActionPicker(slot));
        String[] gestures = {"Short press", "Hold for 0.6 seconds"};
        panel(Strings.t("Choose remote gesture"), translated(gestures), null, 0, -1, null, which -> {
            pendingShortcutGesture = which == 1 ? "hold" : "tap";
            beginRemoteShortcutCapture(kind, packageName, slot);
        });
    }

    private void showRemoteShortcutAppPicker() {
        routeNextDialogBack(() -> showRemoteShortcutActionPicker(pendingShortcutSlot));
        PackageManager pm = getPackageManager();
        TreeMap<String, String> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || apps.containsValue(pkg) || launchIntent(pm, pkg) == null) continue;
                String label = labelOf(pkg), unique = label;
                for (int suffix = 2; apps.containsKey(unique) && !pkg.equals(apps.get(unique)); suffix++) unique = label + " (" + suffix + ")";
                apps.put(unique, pkg);
            }
        }
        List<String> labels = new ArrayList<>(apps.keySet());
        List<String> packages = new ArrayList<>(apps.values());
        if (labels.isEmpty()) {
            Toast.makeText(this, Strings.t("No launchable apps found"), Toast.LENGTH_SHORT).show();
            return;
        }
        panel(Strings.t("Choose an app"), labels, pos -> {
            try { return fit(pm.getApplicationIcon(packages.get(pos)), menuPx(48), menuPx(48)); }
            catch (Exception ignored) { return new ColorDrawable(Color.TRANSPARENT); }
        }, px(48), -1, null, pos -> showRemoteShortcutGesturePicker("app", packages.get(pos), pendingShortcutSlot));
    }

    private void beginRemoteShortcutCapture(String kind, String packageName, int slot) {
        pendingShortcutKind = kind;
        pendingShortcutPackage = packageName;
        pendingShortcutSlot=RemoteShortcutState.slot(slot);
        capturingRemoteShortcut = true;
        AlertDialog dialog = detoxAlertBuilder()
                .setTitle(Strings.t("Press a remote button"))
                .setMessage(Strings.t("Press the button you want to use. Direction, OK, Back, Home, Settings, volume and power buttons are reserved."))
                .setNegativeButton(Strings.t("Cancel"), (d, which) -> cancelRemoteShortcutCapture())
                .create();
        remoteShortcutCaptureDialog = dialog;
        dialog.setOnKeyListener((d, keyCode, event) -> captureRemoteShortcutKey(event));
        dialog.setOnCancelListener(d -> cancelRemoteShortcutCapture());
        dialog.show(); CompanionInput.dialog(dialog);
    }

    private void cancelRemoteShortcutCapture() {
        String kind=pendingShortcutKind,app=pendingShortcutPackage;int slot=pendingShortcutSlot;
        capturingRemoteShortcut=false;pendingShortcutKind=null;pendingShortcutPackage=null;remoteShortcutCaptureDialog=null;
        if(kind!=null)showRemoteShortcutGesturePicker(kind,app,slot);else showRemoteShortcutOptions();
    }

    private void launchVoiceSearch() {
        Intent speech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speech.putExtra(RecognizerIntent.EXTRA_PROMPT, Strings.t("Speak to search"));
        speech.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { startActivityForResult(speech, REQUEST_VOICE_SEARCH); }
        catch (Exception unavailable) {
            try { startActivity(new Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Exception ignored) {
                Toast.makeText(this, Strings.t("Voice search is not available on this TV."), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void searchForSpokenText(String query) {
        if (query == null || query.trim().isEmpty()) return;
        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
        search.putExtra("query", query.trim());
        try { startActivity(search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ignored) {
            Toast.makeText(this, Strings.t("No search app is available for voice results."), Toast.LENGTH_SHORT).show();
        }
    }

    private void promptFolderName(String folderKey) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX, menuPx(28));
        input.setText(folderKey == null ? "My folder" : folderName(folderKey));
        input.setSelection(input.length());
        input.setHint(Strings.t("Folder name"));
        detoxAlertBuilder().setTitle(Strings.t(folderKey == null ? "Name your folder" : "Rename folder"))
                .setView(input)
                .setNegativeButton(Strings.t("Cancel"), null)
                .setPositiveButton(Strings.t("Choose apps"), (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = Strings.t("My folder");
                    chooseFolderApps(folderKey, name);
                }).show();
    }

    private void chooseFolderApps(String folderKey, String name) {
        PackageManager pm = getPackageManager();
        TreeMap<String, String> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || launchIntent(pm, pkg) == null || apps.containsValue(pkg)) continue;
                String label = labelOf(pkg), unique = label;
                for (int suffix = 2; apps.containsKey(unique) && !pkg.equals(apps.get(unique)); suffix++)
                    unique = label + " (" + suffix + ")";
                apps.put(unique, pkg);
            }
        }
        if (apps.isEmpty()) {
            Toast.makeText(this, Strings.t("No launchable apps found"), Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> labels = new ArrayList<>(apps.keySet());
        List<String> packages = new ArrayList<>(apps.values());
        boolean[] checked = new boolean[packages.size()];
        if (folderKey != null) {
            Set<String> current = new HashSet<>(folderPackages(folderKey));
            for (int i = 0; i < packages.size(); i++) checked[i] = current.contains(packages.get(i));
        }
        detoxAlertBuilder().setTitle(Strings.t("Apps in ") + name)
                .setMultiChoiceItems(labels.toArray(new String[0]), checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(Strings.t("Cancel"), null)
                .setPositiveButton(Strings.t("Save folder"), (dialog, which) -> {
                    List<String> chosen = new ArrayList<>();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) chosen.add(packages.get(i));
                    if (chosen.isEmpty()) {
                        Toast.makeText(this, Strings.t("Choose at least one app"), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String target = folderKey;
                    if (target == null) target = FOLDER_PREFIX + System.currentTimeMillis();
                    getSharedPreferences(PREFS, 0).edit().putString(folderNameKey(target), name)
                            .putString(folderAppsKey(target), android.text.TextUtils.join(",", chosen)).apply();
                    if (!rowApps.contains(target)) rowApps.add(target);
                    saveRow();
                    focusedKey = target;
                    buildTiles();
                    returnToSettingsCategory(SettingsMenuRoutes.APPEARANCE);
                }).show();
    }

    private void showFolderApps(String folderKey) {
        List<String> packages = folderPackages(folderKey);
        if (packages.isEmpty()) {
            panel(folderName(folderKey), Collections.singletonList("Edit folder"), null, 0, 0, null,
                    which -> promptFolderName(folderKey));
            return;
        }
        List<String> labels = new ArrayList<>();
        for (String pkg : packages) labels.add(labelOf(pkg));
        PackageManager pm = getPackageManager();
        panel(folderName(folderKey), labels, pos -> {
            try { return fit(pm.getApplicationIcon(packages.get(pos)), menuPx(48), menuPx(48)); }
            catch (Exception ignored) { return new ColorDrawable(Color.TRANSPARENT); }
        }, px(48), -1, null, pos -> {
            Intent intent = launchIntent(pm, packages.get(pos));
            if (intent != null) launchApp(intent, tile(folderKey));
        });
    }

    private void removeFolder(String folderKey) {
        int oldIndex = rowApps.indexOf(folderKey);
        rowApps.remove(folderKey);
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        prefs.edit().remove(folderNameKey(folderKey)).remove(folderAppsKey(folderKey)).apply();
        saveRow();
        focusedKey = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(Math.max(0, oldIndex), rowApps.size() - 1));
        buildTiles();
    }

    private float utilityIconOpacity(){return UtilityUiPolicy.opacity(getSharedPreferences(PREFS,0).getInt("utilityIconOpacity",80))/100f;}
    private float utilityIconScale(){int percent=getSharedPreferences(PREFS,0).getInt("utilityIconScalePercent",100);return Math.max(.70f,Math.min(1.30f,percent/100f));}
    private void showUtilityIconSizeOptions(){SharedPreferences prefs=getSharedPreferences(PREFS,0);int[] values={75,90,100,115,130};String[] labels={"Compact · 75%","Small · 90%","Standard · 100%","Large · 115%","Extra large · 130%"};int selected=2;for(int i=0;i<values.length;i++)if(values[i]==prefs.getInt("utilityIconScalePercent",100))selected=i;routeNextDialogBack(this::showUtilitySettings);panel(Strings.t("Utility icon size"),translated(labels),null,0,selected,null,which->{prefs.edit().putInt("utilityIconScalePercent",values[which]).apply();buildTiles();showUtilitySettings();});}
    private static int utilityOpacitySelection(int current, int[] values) {
        int selected = 0;
        int smallestDifference = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            int difference = Math.abs(current - values[i]);
            if (difference < smallestDifference) {
                smallestDifference = difference;
                selected = i;
            }
        }
        return selected;
    }
    private void showUtilityOpacityOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int[] values = {30, 50, 65, 80, 100};
        String[] names = {"30%", "50%", "65%", "80%", "100%"};
        routeNextDialogBack(this::showUtilitySettings);
        panel(Strings.t("Unfocused icon opacity"), translated(names), null, 0,
                utilityOpacitySelection(prefs.getInt("utilityIconOpacity", 80), values), null, index -> {
                    prefs.edit().putInt("utilityIconOpacity", values[index]).apply();
                    buildTiles();
                    showUtilitySettings();
                });
    }

    private List<String> allUtilityVisibilityKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(NETWORK);
        keys.add(QUICK_PANEL);
        // Include all configured and future-available quick controls, not only currently shown shortcuts.
        for (String name : QUICK_NAMES) keys.add(QUICK + name);
        keys.add(PLAY_STORE);
        keys.add(ADD);
        keys.add("all-apps");
        return keys;
    }

    private void showUtilityVisibilityOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        List<String> keys = allUtilityVisibilityKeys();
        List<String> labels = new ArrayList<>();
        labels.add("Network");
        labels.add("Quick controls");
        for (String label : QUICK_LABELS) labels.add(label);
        labels.add("Play Store");
        labels.add("Add app");
        labels.add("All apps");
        List<String> options = new ArrayList<>();
        options.add("Hide optional icons");
        for (int i = 0; i < keys.size(); i++)
            options.add(labels.get(i) + " · " + (prefs.getBoolean("utilityHidden_" + keys.get(i), false) ? "Hidden" : "Shown"));
        routeNextDialogBack(this::showUtilitySettings);
        panel(Strings.t("Icon visibility"), translated(options), null, 0, -1, null, which -> {
            SharedPreferences.Editor edit = prefs.edit();
            if (which == 0) {
                // Keep network and Quick Controls accessible; Settings is never hideable.
                for (String key : keys)
                    if (!NETWORK.equals(key) && !QUICK_PANEL.equals(key))
                        edit.putBoolean("utilityHidden_" + key, true);
            } else if (which >= 1 && which < keys.size() + 1) {
                String key = keys.get(which - 1);
                edit.putBoolean("utilityHidden_" + key,
                        !prefs.getBoolean("utilityHidden_" + key, false));
            } else return;
            edit.apply();
            buildTiles();
            showUtilityVisibilityOptions();
        });
    }

    private void showUtilityTextColorOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        List<String> labels = new ArrayList<>();
        labels.add("Follow global font colour");
        labels.add("Wallpaper match");
        labels.addAll(translated(COLOR_NAMES));
        int saved = prefs.getInt("utilityTextColor", Color.WHITE);
        int selected = prefs.contains("utilityTextColor") ? (saved == WALLPAPER_MATCH_COLOR ? 1 : 2) : 0;
        if (prefs.contains("utilityTextColor"))
            for (int i = 0; i < FONT_COLORS.length; i++) if (FONT_COLORS[i] == saved) selected = i + 2;
        routeNextDialogBack(this::showUtilitySettings);
        panel(Strings.t("Utility text colour"), labels, which -> {
            GradientDrawable swatch = new GradientDrawable();
            swatch.setShape(GradientDrawable.OVAL);
            swatch.setColor(which == 0 ? withFontColor(Color.WHITE)
                    : which == 1 ? wallpaperAccentColor() : FONT_COLORS[which - 2]);
            swatch.setStroke(Math.max(1, px(2)), 0x80FFFFFF);
            return swatch;
        }, px(28), selected, null, which -> {
            SharedPreferences.Editor edit = prefs.edit();
            if (which == 0) edit.remove("utilityTextColor");
            else edit.putInt("utilityTextColor", which == 1 ? WALLPAPER_MATCH_COLOR : FONT_COLORS[which - 2]);
            edit.apply();
            buildTiles();
            showUtilitySettings();
        });
    }



    private void showUtilityResetConfirmation() {
        routeNextDialogBack(this::showUtilitySettings);
        detoxAlertBuilder().setTitle(Strings.t("Reset Utilities?"))
                .setMessage(Strings.t("Reset utility labels, layout, visibility, opacity and icon appearance? Global themes, fonts and clock colours are kept."))
                .setNegativeButton(Strings.t("Cancel"), (dialog, which) -> showUtilitySettings())
                .setPositiveButton(Strings.t("Reset"), (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences(PREFS, 0);
                    SharedPreferences.Editor edit = prefs.edit();
                    for(String key:SettingsPreferencePolicy.utilityResetKeys(prefs.getAll().keySet())) edit.remove(key);
                    edit.apply();
                    wallpaperAssets.invalidateAccent();
                    applyClockStyle();
                    applyClockPosition();
                    buildTiles();
                    showUtilitySettings();
                }).show();
    }

    private void showUtilityMotionOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String[] names = {"Off", "Subtle", "Standard"};
        routeNextDialogBack(this::showUtilitySettings);
        panel(Strings.t("Icon animations"), translated(names), null, 0,
                utilityMotionLevel(), null, which -> {
                    prefs.edit().putInt("utilityAppearanceMotion", which).apply();
                    buildTiles();
                    showUtilitySettings();
                });
    }
    private void showUtilitySettings(){showSettingsGroup(SettingsMenuRoutes.UTILITIES);}

    private void showUtilitiesPositionOptions() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        int selected = Math.max(0, Math.min(3, prefs.getInt("utilitiesPlacement", 0)));
        String[] options = {"Left strip", "Right strip", "Below apps", "Above apps"};
        routeNextDialogBack(this::showUtilitySettings);
        panel(Strings.t("Utilities position"), translated(options), null, 0, selected, null, which -> {
            prefs.edit().putInt("utilitiesPlacement", which).apply();
            applyClockPosition();
            buildTiles();
            showUtilitySettings();
        });
    }

    private void showLauncherAnimationOptions() {
        nextPanelHomeDiagram=HomeSettingsPreview.MOTION;
        int selected = launcherMotionPreset();
        String[] modes = {"Cinematic", "Playful spring", "Calm", "Reduced motion"};
        routeNextDialogBack(this::showInterfaceMotionOptions);
        panel(Strings.t("Animation preset"), translated(modes), null, 0, selected, null, which -> {
            int legacyMode = which == 2 ? 1 : which == 3 ? 2 : 0;
            getSharedPreferences(PREFS, 0).edit().putInt("launcherMotionPreset", which)
                    .putInt("launcherAnimationMode", legacyMode).apply();
            applyLauncherAnimationMode();
            buildTiles();
            showInterfaceMotionOptions();
        });
    }

    private void applyLauncherAnimationMode() {
        applyMotionPreferences();
        if (launcherAnimationMode() != 0) {
            for (Map.Entry<View, Runnable> entry : new ArrayList<>(focusGlowAnimations.entrySet())) {
                entry.getKey().removeCallbacks(entry.getValue());
                entry.getKey().setAlpha(launcherAnimationMode() == 1
                        && entry.getKey().getVisibility() == View.VISIBLE ? 0.82f : 0f);
                entry.getKey().setScaleX(1f);
                entry.getKey().setScaleY(1f);
            }
            focusGlowAnimations.clear();
        }
        if (launcherAnimationMode() == 2) {
            for (View tile : tiles) {
                Runnable focus = focusAnimations.remove(tile);
                if (focus != null) tile.removeCallbacks(focus);
                tile.animate().cancel();tile.setScaleX(1f);tile.setScaleY(1f);tile.setTranslationY(0f);tile.setTranslationZ(0f);
            }
        }
    }

    private void showSleepTimerOptions() {
        routeNextDialogBack(() -> returnToSettingsCategory(SettingsMenuRoutes.SYSTEM));
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        long deadline = prefs.getLong("sleepTimerDeadline", 0L);
        List<String> options = new ArrayList<>();
        if (deadline > 0L) {
            long remaining = Math.max(1L, (deadline - System.currentTimeMillis() + 59_999L) / 60_000L);
            options.add("Cancel timer · " + remaining + " min left");
        }
        options.add("Start after 15 minutes");
        options.add("Start after 30 minutes");
        options.add("Start after 60 minutes");
        options.add("Start after 90 minutes");
        options.add("Start after 120 minutes");
        panel(Strings.t("Sleep timer · opens idle wallpaper"), translated(options), null, 0, -1, null, which -> {
            if (deadline > 0L && which == 0) {
                if (sleepTimerRunnable != null) sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
                sleepTimerRunnable = null;
                prefs.edit().remove("sleepTimerDeadline").remove("sleepTimerExpired").apply();
                Toast.makeText(this, Strings.t("Sleep timer cancelled"), Toast.LENGTH_SHORT).show();
                returnToSettingsCategory(SettingsMenuRoutes.SYSTEM);
                return;
            }
            int durationIndex = which - (deadline > 0L ? 1 : 0);
            int[] minutes = {15, 30, 60, 90, 120};
            startSleepTimer(minutes[Math.max(0, Math.min(minutes.length - 1, durationIndex))]);
        });
    }

    private void startSleepTimer(int minutes) {
        long deadline = System.currentTimeMillis() + minutes * 60_000L;
        getSharedPreferences(PREFS, 0).edit().putLong("sleepTimerDeadline", deadline)
                .remove("sleepTimerExpired").apply();
        if (sleepTimerRunnable != null) sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimerRunnable = () -> finishSleepTimer();
        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60_000L);
        Toast.makeText(this, Strings.t("Sleep timer set for ") + minutes + Strings.t(" minutes"), Toast.LENGTH_SHORT).show();
        returnToSettingsCategory(SettingsMenuRoutes.SYSTEM);
    }

    private void checkSleepTimerOnResume() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (prefs.getBoolean("sleepTimerExpired", false)) {
            prefs.edit().remove("sleepTimerExpired").apply();
            idleHandler.post(this::showSleepTimerFinishedDialog);
            return;
        }
        long deadline = prefs.getLong("sleepTimerDeadline", 0L);
        if (deadline <= 0L) return;
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0L) {
            prefs.edit().remove("sleepTimerDeadline").apply();
            idleHandler.postDelayed(this::showSleepTimerFinishedDialog, 350L);
        }
        else {
            if (sleepTimerRunnable != null) sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = () -> finishSleepTimer();
            sleepTimerHandler.postDelayed(sleepTimerRunnable, remaining);
        }
    }

    private void finishSleepTimer() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        prefs.edit().remove("sleepTimerDeadline").putBoolean("sleepTimerExpired", true).apply();
        sleepTimerRunnable = null;
        if (activityResumed && hasWindowFocus()) {
            prefs.edit().remove("sleepTimerExpired").apply();
            showSleepTimerFinishedDialog();
        }
    }

    private void showSleepTimerFinishedDialog() {
        if (isFinishing()) return;
        detoxAlertBuilder().setTitle(Strings.t("Sleep timer finished"))
                .setMessage(Strings.t("Detox can show the full-screen idle wallpaper. To switch off the TV, use its built-in sleep timer in TV settings."))
                .setPositiveButton(Strings.t("Show idle wallpaper"), (d, w) -> enterFullscreenWallpaperIdle())
                .setNeutralButton(Strings.t("TV sleep settings"), (d, w) -> openTvSettings())
                .setNegativeButton(Strings.t("Keep watching"), null).show();
    }

    private void showPerformanceOverview() {
        routeNextDialogBack(()->showSettingsGroup(SettingsMenuRoutes.PERFORMANCE));
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) manager.getMemoryInfo(memory);
        android.os.StatFs storage = new android.os.StatFs(android.os.Environment.getDataDirectory().getAbsolutePath());
        long storageTotal = storage.getTotalBytes();
        long storageFree = storage.getAvailableBytes();
        android.os.StatFs launcherStorage = new android.os.StatFs(getFilesDir().getAbsolutePath());
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String homeApp = "Unknown";
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolved = getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved != null && resolved.activityInfo != null)
                homeApp = resolved.activityInfo.packageName + "/" + resolved.activityInfo.name;
        } catch (Exception ignored) {}
        String liveState = !prefs.getBoolean("liveWallpaperEnabled", false) ? "Off"
                : videoWallpaperPlayer != null && videoWallpaperPlayer.isPlaying() ? "Playing"
                : "Enabled · waiting for video surface";
        String liveUri = prefs.getString("liveWallpaperVideoUri", "");
        String liveAssetName = prefs.getString("liveWallpaperVideoName", "");
        String decoder = videoWallpaperPlayer == null ? "Not active"
                : videoWallpaperPlayer.getVideoWidth() + " × " + videoWallpaperPlayer.getVideoHeight();
        float motionFps = wallpaperMotion == null ? 0f : wallpaperMotion.observedFps();
        String message = Strings.t("Available RAM: ") + android.text.format.Formatter.formatFileSize(this, memory.availMem)
                + " / " + android.text.format.Formatter.formatFileSize(this, memory.totalMem)
                + "\n\n" + Strings.t("Internal storage free: ") + android.text.format.Formatter.formatFileSize(this, storageFree)
                + " / " + android.text.format.Formatter.formatFileSize(this, storageTotal)
                + "\n\n" + Strings.t("Detox wallpaper files: ")
                + android.text.format.Formatter.formatFileSize(this, launcherStorage.getTotalBytes() - launcherStorage.getAvailableBytes())
                + "\n\n" + Strings.t("Default Home: ") + homeApp
                + "\n\n" + Strings.t("Live wallpaper: ") + liveState
                + (!liveAssetName.isEmpty() ? "\n" + Strings.t("Video source: ") + liveAssetName
                    : liveUri.isEmpty() ? "" : "\n" + Strings.t("Video source: ") + Uri.parse(liveUri).getLastPathSegment())
                + "\n" + Strings.t("Video dimensions: ") + decoder
                + " · " + Strings.t(prefs.getBoolean("liveWallpaperCrop", true) ? "Crop to fill" : "Fit inside")
                + "\n\n" + Strings.t("Wallpaper effects: ") + (prefs.getBoolean("wallpaperMotion", true)
                    ? motionDisplayName(normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient"))) : "Off")
                + " · " + (prefs.getInt("wallpaperPerformance", 1) <= 0 ? "Lite" : prefs.getInt("wallpaperPerformance", 1) >= 2 ? "Full" : "Balanced")
                + "\n" + Strings.t("Observed wallpaper FPS: ") + (motionFps > 0f
                    ? String.format(java.util.Locale.US, "%.0f", motionFps) : "No sample yet")
                + "\n\n" + Strings.t("Phone companion: ") + phoneCompanionStatus()
                + "\n\n" + DetoxPerformanceProbe.report(this)
                + "\n\n" + Strings.t("Recent failures (this session): ") + DetoxDiagnostics.report()
                + "\n\n" + Strings.t("These figures are read-only. Android manages memory; this screen does not stop or disable apps.");
        detoxAlertBuilder()
                .setTitle(Strings.t("TV diagnostics & performance"))
                .setMessage(message)
                .setNeutralButton(Strings.t("Android storage"), (dialog, which) -> {
                    try { startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)); }
                    catch (Exception error) {
                        DetoxDiagnostics.recordFailure("DetoxLauncher", "Diagnostics", "could not open Android app storage settings", error);
                        openTvSettings();
                    }
                })
                .setPositiveButton(Strings.t("Copy report"), (dialog, which) -> copyLauncherDiagnostic(message))
                .setNegativeButton(Strings.t("Done"), null)
                .show();
    }

    private String phoneCompanionStatus() {
        PhoneCompanionServer server = phoneCompanionServer;
        if (server == null || !server.isRunning()) return "Off";
        return server.isPaired() ? "Paired session active" : "Waiting for pairing";
    }

    private void copyLauncherDiagnostic(String report) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Detox diagnostics", report));
            Toast.makeText(this, Strings.t("Diagnostic report copied"), Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            DetoxDiagnostics.recordFailure("DetoxLauncher", "Diagnostics", "could not copy report to clipboard", ignored);
            Toast.makeText(this, Strings.t("Could not copy report"), Toast.LENGTH_SHORT).show();
        }
    }

    private int appLayoutMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        return Math.max(0, Math.min(2, prefs.getInt("appLayoutMode", 0)));
    }
    private void showAllInstalledApps() {
        PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> labelByPackage = new java.util.HashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || labelByPackage.containsKey(pkg) || launchIntent(pm, pkg) == null) continue;
                String label = labelOf(pkg);
                String unique = label;
                for (int suffix = 2; byLabel.containsKey(unique) && !pkg.equals(byLabel.get(unique)); suffix++)
                    unique = label + " (" + suffix + ")";
                byLabel.put(unique, pkg);
                labelByPackage.put(pkg, unique);
            }
        }
        if (byLabel.isEmpty()) {
            panel(Strings.t("No launchable apps found"), Collections.singletonList("OK"), which -> {});
            return;
        }
        List<String> appLabels = new ArrayList<>(byLabel.keySet());
        List<String> appPackages = new ArrayList<>(byLabel.values());
        showAllAppsIconPanel(pm, appLabels, appPackages);
    }

    /** Readable, searchable app grid designed for remote control navigation. */
    private void showAllAppsIconPanel(PackageManager pm, List<String> labels, List<String> packages) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(menuPx(34), menuPx(26), menuPx(34), menuPx(24));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView head = text(menuPx(38), Color.WHITE, semibold);
        head.setText(Strings.t("All apps"));styleMenuHeading(head);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, menuPx(64), 1f);
        titleParams.leftMargin = menuPx(12);
        header.addView(head, titleParams);
        TextView countPillText = text(menuPx(18), 0xE6C9F5FF, semibold);
        countPillText.setText(packages.size() + "  APPS");
        countPillText.setGravity(Gravity.CENTER);
        countPillText.setPadding(menuPx(14), 0, menuPx(14), 0);
        GradientDrawable countPill = new GradientDrawable();
        countPill.setColor(0x26FFFFFF);
        countPill.setCornerRadius(menuPx(24));
        countPill.setStroke(Math.max(1, menuPx(1)), 0x55FFFFFF);
        countPillText.setBackground(countPill);
        header.addView(countPillText, new LinearLayout.LayoutParams(-2, menuPx(38)));
        box.addView(header, new LinearLayout.LayoutParams(-1, menuPx(68)));
        View accent = new View(this);
        GradientDrawable accentLine = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x38FFFFFF, 0x18FFFFFF, 0x00FFFFFF});
        accentLine.setCornerRadius(menuPx(3));
        accent.setBackground(accentLine);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(-1, menuPx(3));
        accentParams.leftMargin = menuPx(12);
        accentParams.rightMargin = menuPx(12);
        accentParams.topMargin = menuPx(8);
        box.addView(accent, accentParams);

        EditText search=new EditText(this);
        search.setId(View.generateViewId());search.setSingleLine(true);search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xAFC5D4E2);search.setHint(Strings.t("Search installed apps"));
        search.setTextSize(TypedValue.COMPLEX_UNIT_PX,fontPx(menuPx(25)));
        search.setPadding(menuPx(20),0,menuPx(20),0);search.setSelectAllOnFocus(false);
        GradientDrawable searchBackground=new GradientDrawable();searchBackground.setColor(0x28121418);
        searchBackground.setCornerRadius(menuPx(18));searchBackground.setStroke(Math.max(1,menuPx(1)),0x55FFFFFF);
        search.setBackground(searchBackground);search.setFocusable(true);search.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams searchLp=new LinearLayout.LayoutParams(-1,menuPx(62));searchLp.topMargin=menuPx(14);
        box.addView(search,searchLp);

        GridView grid = new GridView(this);
        final int gridColumns=5;
        grid.setId(View.generateViewId());grid.setNumColumns(gridColumns);
        grid.setColumnWidth(menuPx(208));
        grid.setHorizontalSpacing(menuPx(8));
        grid.setVerticalSpacing(menuPx(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setVerticalScrollBarEnabled(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setFocusable(true);
        final int iconSize = menuPx(88);
        final List<Integer> visiblePositions=new ArrayList<>();
        for(int i=0;i<packages.size();i++)visiblePositions.add(i);
        grid.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return visiblePositions.size(); }
            @Override public Object getItem(int position) { return packages.get(visiblePositions.get(position)); }
            @Override public long getItemId(int position) { return visiblePositions.get(position); }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                final int adapterPosition=position;
                final int appIndex=visiblePositions.get(position);
                LinearLayout cell = convertView instanceof LinearLayout
                        ? (LinearLayout) convertView : new LinearLayout(MainActivity.this);
                cell.removeAllViews();
                cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);
                cell.setPadding(menuPx(8),menuPx(8),menuPx(8),menuPx(8));
                cell.setFocusable(true);cell.setClickable(true);
                cell.setContentDescription(Strings.t(labels.get(appIndex)));
                cell.setBackground(new ColorDrawable(Color.TRANSPARENT));
                final GradientDrawable focus = new GradientDrawable();
                focus.setCornerRadius(menuPx(20));
                focus.setColor(Color.TRANSPARENT);
                focus.setStroke(Math.max(1, menuPx(2)), Color.TRANSPARENT);
                cell.setBackground(focus);
                ImageView image = new ImageView(MainActivity.this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                try { image.setImageDrawable(fit(pm.getApplicationIcon(packages.get(appIndex)), iconSize, iconSize)); }
                catch (Exception ignored) {}
                cell.addView(image,new LinearLayout.LayoutParams(iconSize,iconSize));
                TextView label=text(menuPx(23),0xFFEAF2F8,semibold);label.setText(Strings.t(labels.get(appIndex)));
                label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                label.setIncludeFontPadding(false);label.setTextSize(TypedValue.COMPLEX_UNIT_PX,fontPx(menuPx(23)));
                LinearLayout.LayoutParams labelLp=new LinearLayout.LayoutParams(-1,menuPx(34));labelLp.topMargin=menuPx(5);cell.addView(label,labelLp);
                cell.setOnFocusChangeListener((v, focused) -> {
                    focus.setColor(focused ? 0x4430BFFF : Color.TRANSPARENT);
                    focus.setStroke(Math.max(1, menuPx(2)), focused ? 0xB38FE8FF : Color.TRANSPARENT);
                    styleMenuText(label);
                    cell.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f)
                            .setDuration(motionDuration(170L)).start();
                    cell.setTranslationZ(focused ? menuPx(8) : 0f);
                });
                cell.setOnClickListener(v -> {
                    Intent launch = launchIntent(pm, packages.get(appIndex));
                    if (launch != null) dismissDialogAnimated(d, () -> launchApp(launch, tile("all-apps")));
                    else Toast.makeText(MainActivity.this, Strings.t("App is unavailable"), Toast.LENGTH_SHORT).show();
                });
                cell.setOnKeyListener((v,keyCode,event)->{
                    boolean activate = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
                    if (activate) {
                        if (event.getAction() == KeyEvent.ACTION_UP) cell.performClick();
                        return true;
                    }
                    if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                    if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
                        int next=adapterPosition+gridColumns;
                        if(next<grid.getCount())focusAllAppsGridPosition(grid,next);
                        return true;
                    }
                    if(keyCode==KeyEvent.KEYCODE_DPAD_UP){
                        if(adapterPosition<gridColumns){search.requestFocus();return true;}
                        focusAllAppsGridPosition(grid,adapterPosition-gridColumns);
                        return true;
                    }
                    return false;
                });
                return cell;
            }
        });
        TextView empty=text(menuPx(27),0xFFD5E3ED,regular);empty.setText(Strings.t("No matching apps"));empty.setGravity(Gravity.CENTER);
        box.addView(empty,new LinearLayout.LayoutParams(-1,menuPx(64)));empty.setVisibility(View.GONE);grid.setEmptyView(empty);
        search.setNextFocusDownId(grid.getId());grid.setNextFocusUpId(search.getId());
        search.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                String query=s.toString().trim().toLowerCase(java.util.Locale.ROOT);visiblePositions.clear();
                for(int i=0;i<labels.size();i++)if(query.isEmpty()||labels.get(i).toLowerCase(java.util.Locale.ROOT).contains(query))visiblePositions.add(i);
                countPillText.setText(visiblePositions.size()+" / "+packages.size()+"  APPS");((BaseAdapter)grid.getAdapter()).notifyDataSetChanged();grid.setSelection(0);
            }
            @Override public void afterTextChanged(android.text.Editable s){}
        });
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        gridParams.topMargin = menuPx(12);
        box.addView(grid, gridParams);

        box.setBackground(glassSurface(new int[] {0x664A5668, 0x80202C3D, 0x70131F30}, 0x70FFFFFF,
                Math.max(1, Math.round((RADIUS + 8) * MENU_SCALE))));
        box.setElevation(menuPx(22));
        d.setContentView(box);
        setAnimatedBack(d, () -> {
            View allApps = tile("all-apps");
            if (allApps != null) allApps.requestFocus();
        });
        Window window=d.getWindow();
        if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setDimAmount(.28f);if(android.os.Build.VERSION.SDK_INT>=31)window.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false)?menuPx(28):0);window.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);}
        int screenWidth=getResources().getDisplayMetrics().widthPixels,screenHeight=getResources().getDisplayMetrics().heightPixels;
        int width=Math.min(menuPx(1540),screenWidth-menuPx(64));
        int top=menuPx(24),bottom=Math.min(screenHeight-menuPx(16),settingsMenuSafeBottom()-menuPx(16));
        int height=Math.max(1,bottom-top);
        d.show(); CompanionInput.dialog(d);if(window!=null){WindowManager.LayoutParams lp=window.getAttributes();lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;lp.x=0;lp.y=top;lp.width=width;lp.height=height;window.setAttributes(lp);animateDialogIn(window.getDecorView());}
        grid.post(() -> {
            if(grid.getCount()>0){grid.setSelection(0);View first=grid.getChildAt(0);if(first!=null)first.requestFocus();}
            else search.requestFocus();
        });
    }

    private void focusAllAppsGridPosition(GridView grid,int position){
        if(grid==null||position<0||position>=grid.getCount())return;
        grid.setSelection(position);
        grid.smoothScrollToPosition(position);
        grid.post(()->{
            int childIndex=position-grid.getFirstVisiblePosition();
            if(childIndex>=0&&childIndex<grid.getChildCount()){
                View child=grid.getChildAt(childIndex);
                if(child!=null)child.requestFocus();
            }
        });
    }

    /** Keep full-height app panels below the top-right TV controls. */
    private void showSidePanelWindow(Dialog d, boolean preview) {
        showSidePanelWindow(d, preview, 0);
    }

    private void showSidePanelWindow(Dialog d, boolean preview, int requestedHeight) {
        Window w = d.getWindow();
        if (w == null) return;
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(preview ? 0f : 0.28f);
        if (android.os.Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false) ? menuPx(28) : 0);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int margin = menuPx(24);
        int top = menuPx(ICON_TOP + RAIL_BUTTON + ICON_GAP);
        int safeBottom = Math.min(screenHeight - margin, settingsMenuSafeBottom());
        top = Math.max(margin, Math.min(top, safeBottom - menuPx(180)));
        int width = Math.max(1, Math.min(menuPx(560), screenWidth - 2 * margin));
        int height = requestedHeight > 0
                ? Math.min(requestedHeight, Math.max(1, safeBottom - top))
                : Math.max(1, safeBottom - top);
        View anchor = getCurrentFocus();
        if (anchor == null || !anchor.isShown()) anchor = tile(SETTINGS);
        int x, y;
        if (anchor != null && anchor.isShown()) {
            int[] at = new int[2]; anchor.getLocationOnScreen(at);
            int gap = menuPx(18), right = screenWidth - margin;
            int rightX = at[0] + anchor.getWidth() + gap;
            int leftX = at[0] - gap - width;
            x = rightX + width <= right ? rightX : Math.max(margin, leftX);
            y = at[1] + (anchor.getHeight() - height) / 2;
        } else {
            x = screenWidth - width - margin;
            y = top;
        }
        y = Math.max(top, Math.min(y, safeBottom - height));
        x = Math.max(margin, Math.min(x, screenWidth - width - margin));
        w.setGravity(Gravity.TOP | Gravity.LEFT);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = x;
        lp.y = y;
        lp.width = width;
        lp.height = height;
        placeSettingsPicker(w,lp);
        d.show(); CompanionInput.dialog(d);
        animateDialogIn(w.getDecorView());
    }

    /** Place a settings picker beside the open two-column Settings panel, aligned to its active row. */
    private boolean showNestedSettingsPanelWindow(Dialog d, boolean preview, int requestedHeight) {
        if (settingsRootDialog == null || !settingsRootDialog.isShowing()
                || settingsPanelCategory < 0 || settingsPanelWidth <= 0) return false;
        Window w = d.getWindow();
        if (w == null) return false;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int margin = menuPx(24), gap = menuPx(18);
        int desiredWidth = Math.min(menuPx(560), screenWidth - 2 * margin);
        int leftSpace = Math.max(0, settingsPanelX - gap - margin);
        int rightSpace = Math.max(0, screenWidth - margin - settingsPanelX - settingsPanelWidth - gap);
        boolean useRight = rightSpace >= Math.min(desiredWidth, menuPx(320)) || rightSpace >= leftSpace;
        int space = useRight ? rightSpace : leftSpace;
        int width = space >= menuPx(220) ? Math.min(desiredWidth, space)
                : Math.min(desiredWidth, screenWidth - 2 * margin);
        int safeBottom = settingsMenuSafeBottom();
        int availableHeight = Math.max(1, safeBottom - margin);
        int height = Math.min(Math.max(menuPx(220), requestedHeight), availableHeight);
        int centerY = settingsPanelY + settingsPanelHeight / 2;
        if (settingsCategoryRows != null && settingsPanelCategory < settingsCategorySelections.length) {
            int index = settingsCategorySelections[settingsPanelCategory];
            if (index >= 0 && index < settingsCategoryRows.length && settingsCategoryRows[index] != null) {
                int[] rowLocation = new int[2];
                settingsCategoryRows[index].getLocationOnScreen(rowLocation);
                centerY = rowLocation[1] + settingsCategoryRows[index].getHeight() / 2;
            }
        }
        int x = useRight ? settingsPanelX + settingsPanelWidth + gap : settingsPanelX - gap - width;
        x = Math.max(margin, Math.min(x, screenWidth - width - margin));
        int y = Math.max(menuPx(16), Math.min(centerY - height / 2, safeBottom - height));
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        w.setDimAmount(preview ? 0f : 0.28f);
        if (android.os.Build.VERSION.SDK_INT >= 31) w.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false) ? menuPx(28) : 0);
        w.setGravity(Gravity.TOP | Gravity.LEFT);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = x;
        lp.y = y;
        lp.width = width;
        lp.height = height;
        placeSettingsPicker(w,lp);
        d.show(); CompanionInput.dialog(d);
        animateDialogIn(w.getDecorView());
        return true;
    }
    // Live TV apps an HDMI tile can open, in order. Droidlogic (Amlogic) TVs ship Live TV under their
    // own package, with TvActivity as its entry point.
    private static final String[][] LIVE_TV = {{"com.android.tv", "com.android.tv.MainActivity"},
            {"com.droidlogic.android.tv", "com.android.tv.TvActivity"}};
    private static final String INPUT_PLAYER = "com.google.android.tv.inputplayer";

    // Opens an HDMI input: the stock Live TV app first, then Droidlogic's, then whatever the TV picks.
    // Each one is tried only when the one before it can't start, so a TV where the stock app already
    // worked takes exactly the same path as before. The one exception: when the TV itself sends HDMI to
    // Google TV's input player, that goes first, because some Google TVs ship com.android.tv as a stub
    // that starts fine but only says "Hello World!" (issue #5).
    private void openInput(String id) {
        Intent view = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id));
        List<Intent> tries = new ArrayList<>();
        ResolveInfo def = getPackageManager().resolveActivity(view, PackageManager.MATCH_DEFAULT_ONLY);
        if (def != null && def.activityInfo != null && INPUT_PLAYER.equals(def.activityInfo.packageName))
            tries.add(new Intent(view).setClassName(INPUT_PLAYER, def.activityInfo.name));
        for (String[] live : LIVE_TV) {
            Intent t = new Intent(view).setClassName(live[0], live[1]);
            if (t.resolveActivity(getPackageManager()) != null) tries.add(t);
        }
        tries.add(view);
        for (Intent t : tries) {
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(t);
                return;
            } catch (Exception ignored) {}
        }
    }

    // The shortcuts picked with --es quick, in that order, as indexes into QUICK_NAMES. A shortcut
    // this TV has no screen for is left out, so it never shows a tile that does nothing.
    private List<Integer> quickList() {
        List<Integer> out = new ArrayList<>();
        String saved = getSharedPreferences(PREFS, 0).getString("quick", null);
        if (saved == null) saved = "bluetooth,picture,sound";
        for (String name : saved.split("[,\\s]+")) {
            int q = Arrays.asList(QUICK_NAMES).indexOf(name.toLowerCase(java.util.Locale.ROOT));
            if (q < 0 || out.contains(q) || quickIntents(QUICK_NAMES[q]).isEmpty()) continue;
            out.add(q);
        }
        return out;
    }

    // OFFLINE, 1 to 3 for Wi-Fi with that many bars, or ETHERNET (any other link, which on a TV is a cable).
    private int networkState() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return OFFLINE;
        try {
            int rssi = Integer.MIN_VALUE;
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities c = cm.getNetworkCapabilities(cm.getActiveNetwork());
                if (c == null) return OFFLINE;
                if (!c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ETHERNET;
                if (android.os.Build.VERSION.SDK_INT >= 29) rssi = c.getSignalStrength();
                android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni == null || !ni.isConnected()) return OFFLINE;
                if (ni.getType() != ConnectivityManager.TYPE_WIFI) return ETHERNET;
            }
            if (rssi == Integer.MIN_VALUE) { // Android 9 and older: ask the Wi-Fi service
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null && wm.getConnectionInfo() != null) rssi = wm.getConnectionInfo().getRssi();
            }
            return 1 + WifiManager.calculateSignalLevel(rssi, 3);
        } catch (Exception e) {
            return OFFLINE;
        }
    }

    private void refreshNetwork() {
        int state = networkState();
        if(homeConnectionStatus!=null)homeConnectionStatus.setText(state==OFFLINE?Strings.t("Offline"):state==ETHERNET?"Ethernet":"Wi-Fi");
        if (netIcon == null) return;
        if (state == netState) return;
        netState = state;
        if (state == ETHERNET) {
            LineIcon ethernet = new LineIcon(LineIcon.ETHERNET);
            ethernet.setColor(utilityGlowColor(NETWORK));
            ethernet.setStrokeScale(.9f);ethernet.setFitToContent(true);ethernet.setFillFraction(.94f);
            netIcon.setImageDrawable(ethernet);
        } else {
            // The whole Wi-Fi fan dim, with the bars it has bright on top. Offline is a slash instead.
            LineIcon fan = new LineIcon(LineIcon.WIFI);
            fan.setAlpha(0x47);
            fan.setColor(utilityGlowColor(NETWORK));
            fan.setStrokeScale(.9f);fan.setFitToContent(true);fan.setFillFraction(.94f);fan.setReferenceBounds(24,60,216,199);
            int[] top = state == OFFLINE ? LineIcon.SLASH : state == 1 ? LineIcon.WIFI_1
                    : state == 2 ? LineIcon.WIFI_2 : LineIcon.WIFI;
            LineIcon signal = new LineIcon(top);
            signal.setColor(utilityGlowColor(NETWORK));
            signal.setStrokeScale(.9f);signal.setFitToContent(true);signal.setFillFraction(.94f);signal.setReferenceBounds(24,60,216,199);
            netIcon.setImageDrawable(new LayerDrawable(new Drawable[] {fan, signal}));
        }
        ((View) netIcon.getParent()).setContentDescription(
                state == OFFLINE ? Strings.t("Offline") : state == ETHERNET ? "Ethernet" : "Wi-Fi");
    }

    // Wi-Fi settings (on Android TV that page lists Ethernet too), else the TV's network page, else Settings.
    private void openNetworkSettings() {
        for (String a : new String[] {Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS,
                Settings.ACTION_SETTINGS}) {
            try {
                startActivity(new Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (Exception ignored) {}
        }
    }

    // Screens a shortcut can open, best first, keeping only those this TV has.
    private void showHomeTools(){
        String[] ids={"system.search","backup.checkpoint","backup.undo","wallpaper.fit","apps.media"};
        List<String> labels=new ArrayList<>();for(String id:ids)labels.add(settingsEntryLabel(SettingsMenuRoutes.find(id)));
        panel("Home tools",labels,null,0,-1,null,i->openSettingsAction(ids[i]));
    }
    private void refreshHomeMediaCards(){if(screenHost!=null)HomeMediaCards.refresh(this,screenHost);}


    private void openGamingScreen(){startActivity(new Intent(this,GamingActivity.class));overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);}
    // Shared Detox glass treatment for standard Android alert dialogs.
    // Custom Settings/Quick Controls panels retain their existing layout and focus styling.
    private AlertDialog.Builder detoxAlertBuilder() {
        final Runnable parent=consumeNextDialogBackAction();
        return new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert){
            @Override public AlertDialog create(){
                AlertDialog dialog=super.create();
                dialog.setOnShowListener(ignored->DetoxDialogStyle.apply(dialog,getSharedPreferences(PREFS,0).getBoolean("menuBlur",false),menuPx(1)));
                final SettingsNavigationPolicy.BackGate gate=new SettingsNavigationPolicy.BackGate();
                dialog.setOnKeyListener((d,key,event)->{
                    if(routeDialogShortcut(dialog,event))return true;
                    if(parent==null||key!=KeyEvent.KEYCODE_BACK)return false;
                    if(gate.accept(key,event.getAction(),event.getRepeatCount()))animateDialogOut(dialog,parent,true);
                    return true;
                });
                return dialog;
            }
        };
    }

    private void showQuickControls() {
        routeNextDialogBack(null);pendingPanelOrigin=null;
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        List<String> visible = new ArrayList<>();
        for (String key : quickControlOrder(prefs))
            if (!quickControlHidden(prefs, key)) visible.add(key);
        String[] labels = new String[visible.size() + 3];
        for (int i = 0; i < visible.size(); i++) labels[i] = quickControlLabel(visible.get(i));
        labels[visible.size()] = "Customize quick controls";
        labels[visible.size()+1] = "Home tools & settings search";
        labels[visible.size()+2] = "Gaming screen";
        panel(Strings.t("Quick controls"), translated(labels), null, 0, -1, null, which -> {
            if (which == visible.size()+2) { openGamingScreen(); return; }
            if (which == visible.size()+1) { showHomeTools(); return; }
            if (which == visible.size()) {
                showQuickControlCustomization();
                return;
            }
            openQuickControl(visible.get(which));
        });
    }

    private List<String> quickControlOrder(SharedPreferences prefs) {
        List<String> order = new ArrayList<>();
        String saved = prefs.getString("quickControlsOrder", "");
        if (!saved.isEmpty()) for (String key : saved.split(","))
            if (Arrays.asList(QUICK_CONTROL_KEYS).contains(key) && !order.contains(key)) order.add(key);
        for (String key : QUICK_CONTROL_KEYS) if (!order.contains(key)) order.add(key);
        return order;
    }

    private boolean quickControlHidden(SharedPreferences prefs, String key) {
        String hidden = prefs.getString("quickControlsHidden", "");
        return Arrays.asList(hidden.split(",")).contains(key);
    }

    private String quickControlLabel(String key) {
        int index = Arrays.asList(QUICK_CONTROL_KEYS).indexOf(key);
        return index < 0 ? key : QUICK_CONTROL_LABELS[index];
    }

    private void openQuickControl(String key) {
        switch (key) {
            case "network": openNetworkSettings(); break;
            case "bluetooth": openQuick("bluetooth"); break;
            case "picture": openQuick("picture"); break;
            case "sound": openQuick("sound"); break;
            case "launcher": showSettingsRoot(); break;
            case "tv": openTvSettings(); break;
        }
    }

    private void showQuickControlCustomization() {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        List<String> order = quickControlOrder(prefs);
        List<String> choices = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            String key = order.get(i);
            choices.add((i + 1) + ". " + quickControlLabel(key) + " · "
                    + (quickControlHidden(prefs, key) ? "Hidden" : "Visible"));
        }
        if(nextDialogBackAction==null)routeNextDialogBack(this::showQuickControls);
        panel(Strings.t("Quick controls"), translated(choices), null, 0, -1, null,
                which -> showQuickControlItemOptions(order.get(which)));
    }

    private void showQuickControlItemOptions(String key) {
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        List<String> order = quickControlOrder(prefs);
        int position = order.indexOf(key);
        boolean hidden = quickControlHidden(prefs, key);
        String[] actions = {hidden ? "Show" : "Hide", "Move earlier", "Move later"};
        routeNextDialogBack(this::showQuickControlCustomization);
        panel(Strings.t(quickControlLabel(key)), translated(actions), null, 0, -1, null, which -> {
            if (which == 0) {
                java.util.Set<String> hiddenKeys = new java.util.LinkedHashSet<>(Arrays.asList(
                        prefs.getString("quickControlsHidden", "").split(",")));
                hiddenKeys.remove("");
                if (hidden) hiddenKeys.remove(key); else hiddenKeys.add(key);
                prefs.edit().putString("quickControlsHidden", android.text.TextUtils.join(",", hiddenKeys)).apply();
            } else {
                int target = which == 1 ? position - 1 : position + 1;
                if (target >= 0 && target < order.size()) Collections.swap(order, position, target);
                prefs.edit().putString("quickControlsOrder", android.text.TextUtils.join(",", order)).apply();
            }
            showQuickControlCustomization();
        });
    }

    private List<Intent> quickIntents(String name) {
        List<Intent> all = new ArrayList<>();
        String tclType = name.equals("sound") ? "SoundPreset" : name.equals("picture") ? "picture" : null;
        if (tclType != null)
            all.add(new Intent(TCL_SHOW_WINDOW).setClassName("com.tcl.settings", "com.tcl.settings.ShowWindowService")
                    .putExtra("Type", tclType));
        switch (name) {
            case "bluetooth":
                all.add(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                all.add(new Intent("com.google.android.intent.action.CONNECT_INPUT")); // Android TV's pairing screen
                break;
            case "sound": all.add(new Intent(Settings.ACTION_SOUND_SETTINGS)); break;
            case "picture": all.add(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); break;
        }
        PackageManager pm = getPackageManager();
        List<Intent> out = new ArrayList<>();
        for (Intent i : all) {
            boolean service = TCL_SHOW_WINDOW.equals(i.getAction());
            if (service ? pm.resolveService(i, 0) != null : i.resolveActivity(pm) != null) out.add(i);
        }
        return out;
    }

    // Opens a shortcut's screen: the first one that starts wins. False if none did.
    private boolean openQuick(String name) {
        for (Intent i : quickIntents(name)) {
            try {
                if (TCL_SHOW_WINDOW.equals(i.getAction())) startService(i);
                else startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    // The TV's own picture screen, where brightness lives: the Picture shortcut's screens first (TCL's

    private void openTvSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ignored) {}
    }

    private boolean performCompanionAction(CompanionUiAction.Action action, long timeoutMillis) {
        if (Looper.myLooper() == Looper.getMainLooper()) try { return action.getAsBoolean(); }
        catch (RuntimeException error) { DetoxDiagnostics.recordFailure("DetoxCompanion", "Remote input", "Command failed", error); return false; }
        CompanionUiAction request = new CompanionUiAction(action, timeoutMillis);
        if (!shortcutHandler.post(request)) return false;
        try {
            boolean result = request.await();
            if (request.failure() != null) DetoxDiagnostics.recordFailure("DetoxCompanion", "Remote input", "Command failed", request.failure());
            return result;
        }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
        finally { request.cancel(); shortcutHandler.removeCallbacks(request); }
    }

    private void showPhoneCompanion() {
        stopPhoneCompanion();
        try {
            final boolean[] keepPairingOnHide = {false};
            final PhoneCompanionServer[] serverRef = new PhoneCompanionServer[1];
            PhoneCompanionServer server = new PhoneCompanionServer(this, new PhoneCompanionServer.Listener() {
                @Override public String appsJson() { return phoneCompanionAppsJson(); }
                @Override public boolean onRemoteKey(String key) {
                    return performCompanionAction(() -> phoneCompanionServer == serverRef[0]
                            && !isFinishing() && !isDestroyed() && sendCompanionKey(key), 2000);
                }
                @Override public void onPaired() {
                    runOnUiThread(() -> {
                        if(!isCurrentPhoneSession(serverRef[0]))return;
                        keepPairingOnHide[0] = true;
                        if (phoneCompanionDialog != null && phoneCompanionDialog.isShowing()) phoneCompanionDialog.dismiss();
                    });
                }
                @Override public boolean onLaunch(String packageName) {
                    return performCompanionAction(() -> phoneCompanionServer == serverRef[0]
                            && !isFinishing() && !isDestroyed() && launchCompanionApp(packageName), 3000);
                }
                @Override public boolean onTextReceived(String text) {
                    return performCompanionAction(() -> phoneCompanionServer == serverRef[0]
                            && !isFinishing() && !isDestroyed() && copyCompanionText(text), 2000);
                }
                @Override public void onSessionStopped() {
                    runOnUiThread(() -> {
                        if (isCurrentPhoneSession(serverRef[0])) {
                            phoneCompanionServer = null;
                            Toast.makeText(MainActivity.this, Strings.t("Phone companion session stopped"), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override public void onWallpaperReceived(File image) {
                    runOnUiThread(() -> {
                        if (!isCurrentPhoneSession(serverRef[0])) { image.delete(); return; }
                        updatePhoneTransferStatus("Photo received · opening crop preview");
                        importCompanionWallpaper(image);
                    });
                }
                @Override public void onVideoReceived(File video) {
                    runOnUiThread(() -> {
                        if (!isCurrentPhoneSession(serverRef[0])) { video.delete(); return; }
                        updatePhoneTransferStatus("Video received · opening crop preview");
                        importCompanionVideo(video);
                    });
                }
            });
            serverRef[0] = server;
            server.start();
            phoneCompanionServer = server;
            LinearLayout pairing = new LinearLayout(this);
            pairing.setOrientation(LinearLayout.VERTICAL);
            pairing.setGravity(Gravity.CENTER_HORIZONTAL);
            int pairPadding = menuPx(22);
            pairing.setPadding(pairPadding, pairPadding, pairPadding, pairPadding);
            // The shared dialog owns the single glass surface, border, blur and focus buttons.
            pairing.setBackground(null);
            TextView instructions = text(menuPx(18), 0xFFE8F1FA, semibold);
            styleMenuText(instructions);
            instructions.setText(Strings.t("Scan to connect · or enter the code · same home network"));
            instructions.setGravity(Gravity.CENTER);
            pairing.addView(instructions, new LinearLayout.LayoutParams(-1, menuPx(42)));
            ImageView qr = new ImageView(this);
            qr.setScaleType(ImageView.ScaleType.CENTER);
            qr.setBackgroundColor(Color.WHITE);
            Bitmap qrBitmap = PhoneCompanionQr.make(server.pairingUrl(), px(370));
            if (qrBitmap != null) qr.setImageBitmap(qrBitmap);
            pairing.addView(qr, new LinearLayout.LayoutParams(px(370), px(370)));
            TextView address = text(menuPx(16), 0xF4FFFFFF, regular);
            styleMenuText(address);
            address.setText(server.url());
            address.setTextIsSelectable(true);
            address.setGravity(Gravity.CENTER);
            pairing.addView(address, new LinearLayout.LayoutParams(-1, menuPx(32)));
            TextView pin = text(menuPx(24), Color.WHITE, semibold);
            styleMenuText(pin);
            pin.setText(Strings.t("Code  ·  ") + server.pin());
            pin.setGravity(Gravity.CENTER);
            pairing.addView(pin, new LinearLayout.LayoutParams(-1, menuPx(38)));
            AlertDialog dialog = detoxAlertBuilder().setTitle(Strings.t("Phone companion"))
                    .setView(pairing)
                    .setPositiveButton(Strings.t("Stop pairing"), (d, w) -> d.dismiss())
                    .setNegativeButton(Strings.t("Hide · keep pairing"), (d, w) -> { keepPairingOnHide[0] = true; d.dismiss(); }).create();
            phoneCompanionDialog = dialog;
            dialog.setOnDismissListener(d -> {
                if (phoneCompanionServer == server && !keepPairingOnHide[0]) { server.stop(); phoneCompanionServer = null; }
                if (phoneCompanionDialog == dialog) phoneCompanionDialog = null;
            });
            dialog.show(); CompanionInput.dialog(dialog);
        } catch (Exception error) {
            DetoxDiagnostics.recordFailure("DetoxCompanion", "Phone companion", "could not start pairing", error);
            stopPhoneCompanion();
            detoxAlertBuilder().setTitle(Strings.t("Phone companion unavailable"))
                    .setMessage(error.getMessage() == null ? Strings.t("Connect the TV to your home network and try again.") : error.getMessage())
                    .setPositiveButton(Strings.t("OK"), null).show();
        }
    }

    private boolean isCurrentPhoneSession(PhoneCompanionServer expected){return CompanionSessionPolicy.accepts(expected,phoneCompanionServer,isFinishing(),isDestroyed());}
    private void stopPhoneCompanion() {
        PhoneCompanionServer server = phoneCompanionServer;
        phoneCompanionServer = null;
        if (server != null) server.stop();
    }

    private void updatePhoneTransferStatus(String status) {
        Toast.makeText(this, Strings.t(status), Toast.LENGTH_LONG).show();
    }

    private boolean sendCompanionKey(String key) {
        if (isFinishing()) return false;
        if ("home".equals(key)) {
            try {
                Intent home = new Intent(this,MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
                return true;
            } catch (Exception ignored) { return false; }
        }
        if ("launcher-settings".equals(key)) {
            try {
                if(CompanionInput.ready()&&CompanionInput.key(KeyEvent.KEYCODE_SETTINGS))return true;
                startActivity(new Intent(this,MainActivity.class).setAction(ACTION_OPEN_DETOX_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;
            } catch(Exception ignored){return false;}
        }
        if ("settings".equals(key)) {
            try { openTvSettings(); return true; }
            catch (Exception ignored) { return false; }
        }
        if ("play-pause".equals(key)) {
            AudioManager audio=(AudioManager)getSystemService(AUDIO_SERVICE);if(audio==null)return false;
            long now=SystemClock.uptimeMillis();audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));audio.dispatchMediaKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,0));return true;
        }
        if ("volume-up".equals(key) || "volume-down".equals(key) || "mute".equals(key)) {
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audio == null) return false;
            int adjustment = "mute".equals(key) ? AudioManager.ADJUST_TOGGLE_MUTE : "volume-up".equals(key) ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
            audio.adjustVolume(adjustment, AudioManager.FLAG_SHOW_UI);
            return true;
        }
        int code = "up".equals(key) ? KeyEvent.KEYCODE_DPAD_UP
                : "down".equals(key) ? KeyEvent.KEYCODE_DPAD_DOWN
                : "left".equals(key) ? KeyEvent.KEYCODE_DPAD_LEFT
                : "right".equals(key) ? KeyEvent.KEYCODE_DPAD_RIGHT
                : "ok".equals(key) ? KeyEvent.KEYCODE_DPAD_CENTER : "menu".equals(key)?KeyEvent.KEYCODE_MENU:KeyEvent.KEYCODE_BACK;
        if(CompanionInput.ready())return CompanionInput.key(code);
        if(code==KeyEvent.KEYCODE_BACK)return LauncherAccessibilityService.companionBack();
        if (!activityResumed || root == null) return false;
        long downAt = SystemClock.uptimeMillis();
        boolean down = dispatchKeyEvent(new KeyEvent(downAt, downAt, KeyEvent.ACTION_DOWN, code, 0));
        boolean up = dispatchKeyEvent(new KeyEvent(downAt, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, code, 0));
        return down || up;
    }


    private boolean launchCompanionApp(String packageName) {
        if (packageName == null || packageName.equals(getPackageName()) || isFinishing()) return false;
        Intent intent = launchIntent(getPackageManager(), packageName);
        if (intent == null) return false;
        launchApp(intent, null);
        return true;
    }

    private boolean copyCompanionText(String text) {
        if (text == null || text.trim().isEmpty() || text.length() > 2000 || isFinishing()) return false;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return false;
        clipboard.setPrimaryClip(ClipData.newPlainText("Phone companion", text));
        Toast.makeText(this, Strings.t("Text copied to TV clipboard. Focus a text field and paste."), Toast.LENGTH_LONG).show();
        return true;
    }

    private String phoneCompanionAppsJson() {
        TreeMap<String, String> apps = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        PackageManager pm = getPackageManager();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || launchIntent(pm, pkg) == null || apps.containsValue(pkg)) continue;
                String label = labelOf(pkg), unique = label;
                for (int suffix = 2; apps.containsKey(unique) && !pkg.equals(apps.get(unique)); suffix++) unique = label + " (" + suffix + ")";
                apps.put(unique, pkg);
            }
        }
        org.json.JSONArray result = new org.json.JSONArray();
        for (Map.Entry<String, String> entry : apps.entrySet()) {
            org.json.JSONObject item = new org.json.JSONObject();
            try {
                item.put("label", entry.getKey());
                item.put("package", entry.getValue());
                String icon = companionAppIcon(pm, entry.getValue());
                if (icon != null) item.put("icon", icon);
                result.put(item);
            }
            catch (Exception ignored) {}
        }
        return result.toString();
    }

    private String companionAppIcon(PackageManager pm, String packageName) {
        Bitmap bitmap = null;
        try {
            Drawable drawable = pm.getApplicationIcon(packageName);
            int size = 64;
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            int dw = drawable.getIntrinsicWidth(), dh = drawable.getIntrinsicHeight();
            if (dw <= 0 || dh <= 0) { dw = size; dh = size; }
            float scale = Math.min((size - 4f) / dw, (size - 4f) / dh);
            int w = Math.max(1, Math.round(dw * scale)), h = Math.max(1, Math.round(dh * scale));
            int left = (size - w) / 2, top = (size - h) / 2;
            drawable.setBounds(left, top, left + w, top + h);
            drawable.draw(new Canvas(bitmap));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)) return null;
            return "data:image/png;base64," + android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception ignored) { return null; }
        finally { if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle(); }
    }

    private void importCompanionVideo(File video) {
        if (video == null || !video.isFile() || video.length() < 1024L) {
            if (video != null) video.delete();
            updatePhoneTransferStatus("Video was received but could not be opened");
            Toast.makeText(this, Strings.t("Could not open the video from your phone."), Toast.LENGTH_LONG).show();
            return;
        }
        videoImportToken++;
        videoCropGeneration++;
        if (videoWallpaperCropDialog != null && videoWallpaperCropDialog.isShowing())
            videoWallpaperCropDialog.dismiss();
        cleanupCompanionVideo(null);
        pendingCompanionVideo = video;
        Uri uri = Uri.fromFile(video);
        if (phoneCompanionDialog != null && phoneCompanionDialog.isShowing()) phoneCompanionDialog.dismiss();
        showVideoWallpaperCrop(uri);
    }

    private void cleanupCompanionVideo(Uri source) {
        File pending = pendingCompanionVideo;
        if (pending == null) return;
        if (source != null && !"file".equalsIgnoreCase(source.getScheme())) return;
        if (source != null && source.getPath() != null && !pending.getAbsolutePath().equals(source.getPath())) return;
        pendingCompanionVideo = null;
        pending.delete();
    }

    private void cancelCompanionVideoImport(Uri source) {
        if (pendingCompanionVideo != null) updatePhoneTransferStatus("Video received but not applied");
        cleanupCompanionVideo(source);
    }

    private InputStream openVideoInput(Uri source) throws java.io.IOException {
        if ("file".equalsIgnoreCase(source.getScheme())) {
            String path = source.getPath();
            if (path == null) throw new java.io.IOException("The selected video could not be opened");
            return new FileInputStream(path);
        }
        InputStream input = getContentResolver().openInputStream(source);
        if (input == null) throw new java.io.IOException("The selected video could not be opened");
        return input;
    }

    private void importCompanionWallpaper(File image) {
        if (image == null) return;
        final int request = ++wallpaperPhotoImportGeneration;
        discardPendingWallpaper();
        pendingCompanionWallpaper = true;
        try {
            photoImportWorker.execute(() -> {
                Bitmap decoded = null;
                Exception failure = null;
                try { decoded = decodePhotoForCrop(Uri.fromFile(image), 1920, 1080); }
                catch (Exception error) { failure = error; }
                finally { image.delete(); }
                final Bitmap result = decoded;
                final Exception error = failure;
                wallpaperDecodeHandler.post(() -> {
                    if (request != wallpaperPhotoImportGeneration || isFinishing() || isDestroyed()) {
                        if (result != null && !result.isRecycled()) result.recycle();
                        return;
                    }
                    if (result == null) {
                        pendingCompanionWallpaper = false;
                        updatePhoneTransferStatus("Photo received but could not be opened");
                        Toast.makeText(this, Strings.t("Could not open the photo from your phone."), Toast.LENGTH_LONG).show();
                        if (error != null) DetoxDiagnostics.recordFailure("DetoxCompanion", "Photo import", "could not decode received photo", error);
                        return;
                    }
                    pendingWallpaper = result;
                    if (phoneCompanionDialog != null && phoneCompanionDialog.isShowing()) phoneCompanionDialog.dismiss();
                    showWallpaperCrop();
                });
            });
        } catch (java.util.concurrent.RejectedExecutionException closed) {
            image.delete();
            pendingCompanionWallpaper = false;
            updatePhoneTransferStatus("Photo received but could not be opened");
        }
    }
    interface Pick { void at(int which); }
    interface Icon {
        Drawable at(int which) throws Exception;
        default void loadAsync(int which, IconLoaded loaded) { }
    }
    interface IconLoaded { void ready(Drawable drawable); }

    private void panel(String title, List<String> labels, Pick pick) {
        panel(title, labels, null, 0, -1, null, pick);
    }

    // A side panel in the home screen's own look: a title, then one row per choice, with the same
    // glass focus as the tiles. Back closes it. icon (or null) puts an icon of that size by each name.
    // With preview, the list starts on selected and moving through it calls preview; Back calls it
    // with selected again, to put things back. Settings callers can provide a parent route for Back.
    private void panel(String title, final List<String> labels, final Icon icon, final int size,
                       final int selected, final Pick preview, final Pick pick) {
        final Dialog d = new Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(menuPx(34), menuPx(62), menuPx(34), menuPx(34));

        TextView head = text(menuPx(30), Color.WHITE, semibold);
        head.setText(title);styleMenuHeading(head);
        head.setSingleLine(true);
        head.setEllipsize(android.text.TextUtils.TruncateAt.END);
        styleUtilityTitle(head);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(-2,menuPx(64));
        titleParams.leftMargin=menuPx(8);box.addView(head,titleParams);

        View accent = new View(this);
        GradientDrawable accentLine = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0x38FFFFFF, 0x18FFFFFF, 0x00FFFFFF});
        accentLine.setCornerRadius(menuPx(3));
        accent.setBackground(accentLine);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(-1, menuPx(3));
        accentParams.leftMargin = menuPx(24);
        accentParams.topMargin = menuPx(18);
        box.addView(accent, accentParams);

        int previewKind=nextPanelHomeDiagram;nextPanelHomeDiagram=-1;
        final HomeSettingsPreview homeDiagram = previewKind < 0 ? null : new HomeSettingsPreview(this,previewKind,selected);
        if(homeDiagram!=null){
            LinearLayout.LayoutParams diagramParams=new LinearLayout.LayoutParams(-1,menuPx(196));
            diagramParams.topMargin=menuPx(16);box.addView(homeDiagram,diagramParams);
        }

        ListView list = new ListView(this);
        list.setBackgroundColor(Color.TRANSPARENT);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setFocusable(true);
        list.setItemsCanFocus(false);
        final MenuNavigationPolicy navigationPace=new MenuNavigationPolicy();
        list.setOnKeyListener((view,key,event)->{
            if(key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
            if(event.getAction()==KeyEvent.ACTION_UP){navigationPace.release();return true;}
            if(event.getAction()!=KeyEvent.ACTION_DOWN||!list.isEnabled())return true;
            long now=SystemClock.uptimeMillis();
            if(!navigationPace.move(key,event.getRepeatCount()==0,now,event.getDownTime()))return true;
            int current=list.getSelectedItemPosition();
            int next=current<0?0:Math.max(0,Math.min(list.getCount()-1,current+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));
            if(list.getCount()>0)list.setSelection(next);
            return true;
        });
        GradientDrawable focus = new GradientDrawable();
        focus.setCornerRadius(menuPx(8));
        focus.setColor(0x48FFFFFF);
        focus.setStroke(0, Color.TRANSPARENT);
        list.setSelector(focus);
        // Icons load as rows appear, drawn small to save RAM.
        final Drawable[] icons = new Drawable[labels.size()];
        final boolean[] iconLoading = new boolean[labels.size()];
        list.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return labels.size(); }
            @Override public Object getItem(int pos) { return labels.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public View getView(int pos, View convert, ViewGroup parent) {
                TextView t = convert != null ? (TextView) convert : text(menuPx(32), 0xFFF7F8FC, Typeface.create("sans-serif", Typeface.BOLD));
                t.setTag(pos);
                setMenuLabel(t,labels.get(pos));
                t.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPx(menuPx(28)));
                // The shared menu formatter owns the selected family/color and normal option weight.
                t.setTypeface(semibold, Typeface.NORMAL);
                styleMenuText(t);
                t.setSingleLine(true);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(menuPx(80));
                t.setPadding(menuPx(24), 0, menuPx(24), 0);
                if (icon != null && icons[pos] == null) {
                    try { icons[pos] = icon.at(pos); }
                    catch (Exception e) { icons[pos] = new ColorDrawable(Color.TRANSPARENT); }
                    icons[pos].setBounds(0, 0, size, size);
                }
                t.setCompoundDrawablesRelative(icons[pos], null, null, null);
                t.setCompoundDrawablePadding(menuPx(24));
                if (icon != null && !iconLoading[pos]) {
                    iconLoading[pos] = true;
                    final int iconIndex = pos;
                    icon.loadAsync(pos, loaded -> {
                        if (loaded == null) return;
                        loaded.setBounds(0, 0, size, size);
                        icons[iconIndex] = loaded;
                        if (t.isAttachedToWindow() && t.getTag() instanceof Integer
                                && ((Integer) t.getTag()) == iconIndex)
                            t.setCompoundDrawablesRelative(loaded, null, null, null);
                    });
                }
                return t;
            }
        });
        final boolean[] choiceCommitted={false};
        list.setOnItemClickListener((parent, v, pos, id) -> {
            if(choiceCommitted[0])return;
            choiceCommitted[0]=true;list.setEnabled(false);
            Rect parentBounds = dialogBounds(d);
            dismissDialogAnimated(d, () -> {
                pendingPanelOrigin = parentBounds;
                pick.at(pos);
                if (pendingPanelOrigin == parentBounds) pendingPanelOrigin = null;
            });
        });
        if (preview != null || homeDiagram != null) {
            list.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    if(homeDiagram!=null)homeDiagram.select(pos);
                    if(preview!=null)preview.at(pos);
                    list.invalidateViews();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }
        if (preview != null) {
            // Restore temporary previews on every uncommitted dismissal, including remote
            // shortcuts and system dismissal. A committed choice applies after the dialog closes.
            d.setOnDismissListener(x -> {
                if (!choiceCommitted[0]) preview.at(selected);
                restorePreviewSettings();
            });
        }
        if (selected >= 0) list.setSelection(selected);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = menuPx(36);
        box.addView(list, lp);

        box.setBackground(utilityMenuSurface());
        box.setElevation(menuPx(18));
        d.setContentView(box);
        int requestedPanelHeight = menuPx(224 + Math.min(labels.size(), 8) * 80 + (homeDiagram==null?0:212));
        Rect parentBounds = pendingPanelOrigin;
        pendingPanelOrigin = null;
        if (parentBounds == null || !showAdjacentSettingsPanelWindow(d, preview != null, requestedPanelHeight, parentBounds)) {
            if (!showNestedSettingsPanelWindow(d, preview != null, requestedPanelHeight))
                showSidePanelWindow(d, preview != null, requestedPanelHeight);
        }
        Runnable parentRoute = consumeNextDialogBackAction();
        if (parentRoute == null) setAnimatedBack(d, null);
        else setAdjacentPanelBack(d, parentRoute);
        if(title.equals(Strings.t("Quick controls")))d.setOnKeyListener((owner,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP){nextDialogBackAction=null;pendingPanelOrigin=null;d.cancel();}return true;}return routeDialogShortcut(d,event);});

        list.post(() -> {
            if (selected >= 0 && selected < labels.size()) list.setSelection(selected);
            list.requestFocus();
        });
    }

    private Rect dialogBounds(Dialog dialog) {
        Window window = dialog == null ? null : dialog.getWindow();
        if (window == null) return null;
        View decor = window.getDecorView();
        int[] location = new int[2];
        decor.getLocationOnScreen(location);
        int width = decor.getWidth(), height = decor.getHeight();
        return width > 0 && height > 0 ? new Rect(location[0], location[1], location[0] + width, location[1] + height) : null;
    }

    /** Every settings picker occupies one stable slot, outside the utility rail and its labels. */
    private void placeSettingsPicker(Window window,WindowManager.LayoutParams layout){
        int screenWidth=getResources().getDisplayMetrics().widthPixels;
        int margin=menuPx(24),left=margin,right=screenWidth-margin;
        int placement=getSharedPreferences(PREFS,0).getInt("utilitiesPlacement",0);
        int labelWidth=settingsUtilityLabel!=null&&settingsUtilityLabel.getWidth()>0?settingsUtilityLabel.getWidth():menuPx(140);
        int railReserve=px(homeSafeInset()+UTILITY_BUTTON)+labelWidth+menuPx(24);
        if(placement==0)left=Math.max(left,railReserve);
        else if(placement==1)right=Math.min(right,screenWidth-railReserve);
        if(right-left<menuPx(280)){left=margin;right=screenWidth-margin;}
        int width=Math.max(1,Math.min(menuPx(560),right-left));
        int safeBottom=Math.min(getResources().getDisplayMetrics().heightPixels-margin,settingsMenuSafeBottom());
        int top=Math.max(margin,Math.min(menuPx(72),safeBottom-menuPx(180)));
        int height=Math.max(1,Math.min(layout.height,safeBottom-top));
        int preferred=settingsPanelWidth>0?settingsPanelX:placement==1?right-width:left;
        layout.gravity=Gravity.TOP|Gravity.LEFT;layout.x=Math.max(left,Math.min(preferred,right-width));
        layout.y=top;layout.width=width;layout.height=height;
        window.setAttributes(layout);
    }
    private boolean showAdjacentSettingsPanelWindow(Dialog dialog, boolean preview, int requestedHeight, Rect parent) {
        Window window = dialog.getWindow();
        if (window == null || parent == null) return false;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int margin = menuPx(24), gap = menuPx(18), desiredWidth = Math.min(menuPx(560), screenWidth - 2 * margin);
        int leftSpace = Math.max(0, parent.left - margin - gap);
        int rightSpace = Math.max(0, screenWidth - margin - parent.right - gap);
        boolean useRight = rightSpace >= Math.min(desiredWidth, menuPx(280)) || rightSpace >= leftSpace;
        int space = useRight ? rightSpace : leftSpace;
        int width = parent.height()>menuPx(120) ? desiredWidth : space >= menuPx(220) ? Math.min(desiredWidth, space)
                : Math.min(desiredWidth, screenWidth - 2 * margin);
        int safeBottom = settingsMenuSafeBottom();
        int maxHeight = Math.max(1, safeBottom - margin);
        int height = Math.min(Math.max(menuPx(220), requestedHeight), maxHeight);
        boolean replacingPanel = parent.height() > menuPx(120);
        int x = replacingPanel ? parent.left : useRight ? parent.right + gap : parent.left - gap - width;
        // If neither side can hold the panel, keep it tied to the selected parent row and
        // clamp it onscreen. This may overlap the parent on very narrow displays, but avoids
        // jumping back to the utilities rail and breaking the menu hierarchy.
        x = Math.max(margin, Math.min(x, screenWidth - width - margin));
        int y = Math.max(menuPx(16), Math.min(replacingPanel ? parent.top : parent.centerY() - height / 2, safeBottom - height));
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(preview ? 0f : 0.20f);
        if (android.os.Build.VERSION.SDK_INT >= 31) window.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false) ? menuPx(30) : 0);
        window.setGravity(Gravity.TOP | Gravity.LEFT);
        WindowManager.LayoutParams layout = window.getAttributes();
        layout.gravity = Gravity.TOP | Gravity.LEFT;
        layout.x = x; layout.y = y; layout.width = width; layout.height = height;
        placeSettingsPicker(window,layout);
        dialog.show(); CompanionInput.dialog(dialog);
        animateDialogIn(window.getDecorView());
        return true;
    }

    // A small menu that drops down under a tile, in the same glass as the tiles. Opens above the
    // tile when there's no room below. Back closes it.
    private void dropdown(View anchor, String[] items, final Pick pick) {
        dropdown(anchor, items, pick, -1);
    }

    private void dropdown(View anchor, String[] items, final Pick pick, final int initialFocus) {
        if(anchor==null)return;
        final Dialog dialog=new Dialog(this,android.R.style.Theme_Material_Dialog_NoActionBar);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        int pad=menuPx(8),rowHeight=menuPx(56);box.setPadding(pad,pad,pad,pad);
        box.setBackground(utilityMenuSurface());box.setElevation(menuPx(18));
        TextView[] rows=new TextView[items.length];float widest=menuPx(280);
        final boolean[] committed={false};
        for(int i=0;i<items.length;i++){
            final int choice=i;TextView row=text(menuPx(26),0xD9FFFFFF,regular);
            row.setText(Strings.t(items[i]));applyFontPreview(row);styleMenuText(row);
            row.setSingleLine(true);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(menuPx(18),0,menuPx(18),0);row.setFocusable(true);
            widest=Math.max(widest,row.getPaint().measureText(row.getText().toString())+menuPx(36)+2*pad);
            row.setBackground(glassMenuRowBackground(false,false));
            row.setOnFocusChangeListener((v,focused)->{boolean contrast=highContrastFocusEnabled();row.setBackground(glassMenuRowBackground(focused,contrast));styleMenuText(row);animateFocus(row,focused,contrast?1.035f:1.018f);});
            row.setOnClickListener(v->{if(committed[0]||!dialog.isShowing())return;committed[0]=true;box.setEnabled(false);dismissDialogAnimated(dialog,()->pick.at(choice));});
            rows[i]=row;box.addView(row,new LinearLayout.LayoutParams(-1,rowHeight));
        }
        ScrollView scroll=settingsColumnScroll(box);dialog.setContentView(scroll);
        int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;
        int margin=menuPx(24),width=Math.min((int)Math.ceil(widest),sw-2*margin);
        int height=Math.min(items.length*rowHeight+2*pad,sh-2*margin);int[] at=new int[2];anchor.getLocationOnScreen(at);
        int y=at[1]+anchor.getHeight()+menuPx(64);if(y+height>sh-margin)y=at[1]-height-menuPx(16);
        Window win=dialog.getWindow();if(win==null)return;
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if(android.os.Build.VERSION.SDK_INT>=31)win.setBackgroundBlurRadius(getSharedPreferences(PREFS,0).getBoolean("menuBlur",false)?menuPx(28):0);
        win.setGravity(Gravity.TOP|Gravity.LEFT);WindowManager.LayoutParams lp=win.getAttributes();
        lp.x=Math.max(margin,Math.min(at[0],sw-width-margin));lp.y=Math.max(margin,Math.min(y,sh-height-margin));lp.width=width;lp.height=height;win.setAttributes(lp);
        dialog.show();CompanionInput.dialog(dialog);animateDialogIn(win.getDecorView());setAnimatedBack(dialog,consumeNextDialogBackAction());
        if(initialFocus>=0&&initialFocus<rows.length){TextView target=rows[initialFocus];target.post(()->{if(dialog.isShowing()&&target.isAttachedToWindow())target.requestFocus();});}
    }

    /** Modern TV-style panel entrance, driven by frames so it still runs when system animations are off. */
    private void animateDialogIn(View view) {
        MenuTypography.tree(view,true);
        view.animate().cancel();view.setAlpha(1f);view.setScaleX(1f);view.setScaleY(1f);
        long duration=motionDuration(230);view.setTranslationX(duration==0?0:px(launcherAnimationMode()==1?8:24));
        uiMotion.panel(view,0,duration,null);
    }

    /** Close menus with a short matching slide, keeping text opacity stable before applying the selected action. */
    private void dismissDialogAnimated(final Dialog dialog, final Runnable after) {
        animateDialogOut(dialog, after, false);
    }

    private void routeNextDialogBack(Runnable action) { nextDialogBackAction = action; }

    private Runnable consumeNextDialogBackAction() {
        Runnable action = nextDialogBackAction;
        nextDialogBackAction = null;
        return action;
    }

    private void animateDialogOut(final Dialog dialog,final Runnable after,final boolean cancel) {
        if(dialog==null||!dialog.isShowing())return;
        Runnable finish=()->{if(!dialog.isShowing())return;if(cancel)dialog.cancel();else dialog.dismiss();if(after!=null)after.run();};
        View view=dialog.getWindow()!=null?dialog.getWindow().getDecorView():null;
        if(view==null){finish.run();return;}
        uiMotion.panel(view,px(launcherAnimationMode()==1?8:20),motionDuration(170),finish,()->{if(dialog.isShowing())dialog.dismiss();});
    }

    /** Let Back use the same animated exit as selecting a menu item. */
    private void setAnimatedBack(final Dialog dialog) {
        setAnimatedBack(dialog, null);
    }

    private void setAnimatedBack(final Dialog dialog, final Runnable parentRoute) {
        final SettingsNavigationPolicy.BackGate backGate = new SettingsNavigationPolicy.BackGate();
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if(routeDialogShortcut(dialog,event))return true;
            if (keyCode != KeyEvent.KEYCODE_BACK) return false;
            if (backGate.accept(keyCode, event.getAction(), event.getRepeatCount())) {
                animateDialogOut(dialog, parentRoute, true);
            }
            return true;
        });
    }

    private void setAdjacentPanelBack(final Dialog dialog, final Runnable parentRoute) {
        final SettingsNavigationPolicy.BackGate backGate = new SettingsNavigationPolicy.BackGate();
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (routeDialogShortcut(dialog, event)) return true;
            if (keyCode != KeyEvent.KEYCODE_BACK) return false;
            if (backGate.accept(keyCode, event.getAction(), event.getRepeatCount())) {
                Rect childBounds = dialogBounds(dialog);
                animateDialogOut(dialog, () -> {
                    pendingPanelOrigin = childBounds;
                    parentRoute.run();
                    if (pendingPanelOrigin == childBounds) pendingPanelOrigin = null;
                }, true);
            }
            return true;
        });
    }

    // Hold OK (or Menu) on a top-row app: move it, swap it for another, or take it off the home
    // screen. Removing only takes the tile away: the app stays installed and can be added back.
    // Apps the user installed can also be uninstalled; Android asks to confirm, and the tile goes
    // once the app is gone. Apps that came with the TV can't be, so they get App info instead,
    // where Android offers to turn them off.
    private void showEditMenu(final String key) {
        final int index = rowApps.indexOf(key);
        if (index < 0) return;
        if (isFolderKey(key)) {
            dropdown(tile(key), AppIconMenuPolicy.folderLabels(), which -> {
                AppIconMenuPolicy.FolderAction action = AppIconMenuPolicy.folderAction(which);
                if (action == AppIconMenuPolicy.FolderAction.OPEN) showFolderApps(key);
                else if (action == AppIconMenuPolicy.FolderAction.EDIT) promptFolderName(key);
                else if (action == AppIconMenuPolicy.FolderAction.REMOVE) removeFolder(key);
            });
            return;
        }
        final boolean user = userInstalled(key);
        final String iconPreference = "customAppIcon:" + key;
        SharedPreferences iconPrefs = getSharedPreferences(PREFS, 0);
        final boolean hasCustomIcon = iconPrefs.contains(iconPreference)
                || iconPrefs.contains("customAppIconFile:" + key);
        dropdown(tile(key), AppIconMenuPolicy.appLabels(hasCustomIcon, user), which -> {
            AppIconMenuPolicy.AppAction action = AppIconMenuPolicy.appAction(which, hasCustomIcon, user);
            if (action == AppIconMenuPolicy.AppAction.MOVE) startMove(key);
            else if (action == AppIconMenuPolicy.AppAction.REPLACE) showAddMenu(index);
            else if (action == AppIconMenuPolicy.AppAction.CHANGE_ICON) chooseCustomAppIcon(key);
            else if (action == AppIconMenuPolicy.AppAction.CLOSE) closeAppFromMenu(key);
            else if (action == AppIconMenuPolicy.AppAction.RESET_ICON) {
                SharedPreferences prefs = getSharedPreferences(PREFS, 0);
                String customFile = prefs.getString("customAppIconFile:" + key, null);
                if (customFile != null) new File(customFile).delete();
                prefs.edit().remove(iconPreference).remove("customAppIconFile:" + key).apply();
                focusedKey = key;
                buildTiles();
            } else if (action == AppIconMenuPolicy.AppAction.REMOVE) {
                rowApps.remove(index);
                saveRow();
                focusedKey = rowApps.isEmpty() ? ADD : rowApps.get(Math.min(index, rowApps.size() - 1));
                buildTiles();
            } else if (action == AppIconMenuPolicy.AppAction.UNINSTALL
                    || action == AppIconMenuPolicy.AppAction.INFO) {
                Uri pkg = Uri.parse("package:" + key);
                try {
                    startActivity(action == AppIconMenuPolicy.AppAction.UNINSTALL
                            ? new Intent(Intent.ACTION_DELETE, pkg)
                            : new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg));
                } catch (Exception error) {
                    DetoxDiagnostics.recordFailure("DetoxLauncher", "App management", "could not open management for " + key, error);
                }
            }
        });
    }

    /** Android does not let ordinary launchers force-stop another app directly. */
    private void closeAppFromMenu(String packageName) {
        Toast.makeText(this, Strings.t("Choose Force stop to close the app"), Toast.LENGTH_LONG).show();
        openApplicationInfo(packageName);
    }

    private void chooseCustomAppIcon(String packageName) {
        pendingCustomIconPackage = packageName;
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(pick, PICK_APP_ICON); }
        catch (Exception e) { pendingCustomIconPackage = null; }
    }

    // Every launchable app that isn't on the top row yet, by name. The pick is added at the end,
    // or takes the place of the app at replaceIndex (-1 to add).
    private void showAddMenu(final int replaceIndex) {
        final PackageManager pm = getPackageManager();
        TreeMap<String, String> byLabel = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (rowApps.contains(p) || byLabel.containsValue(p) || p.equals(getPackageName())
                        || (NOT_APPS.contains(p) && !p.equals(PLAY_STORE))) continue;
                String label = labelOf(p);
                if (!byLabel.containsKey(label)) byLabel.put(label, p);
            }
        }
        if (byLabel.isEmpty()) {
            panel(Strings.t("Every app is already here"), Collections.singletonList("OK"), which -> {});
            return;
        }
        final List<String> pkgs = new ArrayList<>(byLabel.values());
        String title = replaceIndex < 0 ? Strings.t("Add an app")
                : String.format(Strings.t("Replace %s"), labelOf(rowApps.get(replaceIndex)));
        panel(title, new ArrayList<>(byLabel.keySet()), pos -> fit(pm.getApplicationIcon(pkgs.get(pos)), menuPx(48), menuPx(48)),
                px(48), -1, null, which -> {
            if (replaceIndex < 0) rowApps.add(pkgs.get(which));
            else rowApps.set(replaceIndex, pkgs.get(which));
            saveRow();
            focusedKey = pkgs.get(which);
            buildTiles();
        });
    }

    private String labelOf(String pkg) {
        PackageManager pm = getPackageManager();
        try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
        catch (Exception e) { return pkg; }
    }

    // Saves the top row as the app list, so it no longer follows apps.txt.
    private void saveRow() {
        getSharedPreferences(PREFS, 0).edit().putString("apps", android.text.TextUtils.join(",", rowApps)).apply();
    }

    // Where app number i sits in the selected grid.
    private int gridX(int i) {
        return gridLeftInsetPx + (i % appColumns) * (tileBaseW + px(GAP)) + (tileBaseW - tileW) / 2;
    }

    private int homeSafeInset() {
        return Math.max(12, Math.min(72, getSharedPreferences(PREFS, 0).getInt("homeSafeInset", 28)));
    }

    private int gridY(int i) {
        return homeLayout != null ? homeLayout.y(i) : gridTop + (i / appColumns) * tileRowStepPx;
    }

    private View tile(String key) {
        for (View t : tiles) if (t.getTag().equals(key)) return t;
        return null;
    }

    // Move mode: the tile gets an outline, the arrows move it through the grid, OK or Back finishes.
    private void startMove(String key) {
        View card = tile(key);
        if (card == null) return;
        movingKey = key;
        GradientDrawable outline = new GradientDrawable();
        outline.setCornerRadius(px(RADIUS));
        outline.setStroke(px(4), Color.WHITE);
        ((FrameLayout) card).setForeground(outline);
        moveHint.setVisibility(View.VISIBLE);
        card.requestFocus();
    }

    private boolean moveKey(String key, int code, boolean ok, boolean down) {
        int step = code == KeyEvent.KEYCODE_DPAD_LEFT ? -1
                : code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1
                : code == KeyEvent.KEYCODE_DPAD_UP ? -appColumns
                : code == KeyEvent.KEYCODE_DPAD_DOWN ? appColumns : 0;
        if (step != 0) {
            int from = rowApps.indexOf(key), to = from + step;
            if (down && to >= 0 && to < rowApps.size()) {
                // Slide the tiles into their new places: no rebuild, so it stays quick on slow TVs.
                rowApps.add(to, rowApps.remove(from));
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                    View wrap = (View) tile(rowApps.get(i)).getParent();
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) wrap.getLayoutParams();
                    lp.leftMargin = gridX(i);
                    lp.topMargin = gridY(i);
                    wrap.setLayoutParams(lp);
                }
                // Scroll along with it once it has moved.
                final View card = tile(key);
                card.post(() -> {if(card.isAttachedToWindow())card.requestRectangleOnScreen(homeFocusBounds(card));});
            }
            return true;
        }
        if (ok || code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) {
            if (!down) {
                movingKey = null;
                longPressed = false;
                ((FrameLayout) tile(key)).setForeground(null);
                moveHint.setVisibility(View.GONE);
                saveRow();
                refreshApps();
            }
            return true;
        }
        return true; // up/down and everything else stay put while moving
    }

    private static boolean isInput(String key) {
        return key.indexOf('/') > 0;
    }

    // The TV's own HDMI inputs (not devices behind them), in order, skipping hidden ones.
    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        TvInputManager tim = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        if (tim == null) return out;
        for (TvInputInfo in : tim.getTvInputList()) {
            if (in.getType() == TvInputInfo.TYPE_HDMI && in.getParentId() == null && !in.isHidden(this)) out.add(in);
        }
        Collections.sort(out, (a, b) -> a.getId().compareTo(b.getId()));
        return out.size() > MAX_INPUTS ? out.subList(0, MAX_INPUTS) : out;
    }

    // The name set in Settings > Inputs (like "Bedroom"), else the TV's own name (like "HDMI 1").
    private String inputLabel(TvInputInfo in) {
        CharSequence custom = in.loadCustomLabel(this);
        if (custom != null && custom.length() > 0) return withoutDeviceType(custom.toString());
        CharSequence label = in.loadLabel(this);
        return label != null && label.length() > 0 ? withoutDeviceType(label.toString()) : "HDMI";
    }

    // Philips stores a name as "name&DEVICE_TYPE" (like "Computer&COMPUTER"), so show the name only.
    // Only on Philips TVs: elsewhere a name like "AUDIO&VIDEO" is the name itself.
    private static String withoutDeviceType(String label) {
        if (!"Philips".equalsIgnoreCase(android.os.Build.MANUFACTURER)) return label;
        int amp = label.lastIndexOf('&');
        return amp > 0 && label.substring(amp + 1).matches("[A-Z][A-Z0-9_]{2,}") ? label.substring(0, amp).trim() : label;
    }

    private boolean userInstalled(String pkg) {
        try { return (getPackageManager().getApplicationInfo(pkg, 0).flags & ApplicationInfo.FLAG_SYSTEM) == 0; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private static Intent launchIntent(PackageManager pm, String pkg) {
        return AppGridCatalog.launchIntent(pm, pkg);
    }

    // Xiaomi TVs ship MiLan Pro but don't register it by name; every other TV gets Roboto.
    private void loadFonts() {
        MenuTypography.bind(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        String key = previewFontKey != null ? previewFontKey : prefs.getString("font", "nunito");
        if (!Arrays.asList(FONT_KEYS).contains(key)) {
            key = FONT_KEYS[0];
            if (previewFontKey == null) prefs.edit().putString("font", key).apply();
        }
        regular = fontTypeface(key, Typeface.NORMAL);
        semibold = fontTypeface(key, Typeface.BOLD);
        MenuTypography.refresh(this);
    }

    private Typeface fontTypeface(String key, int style) {
        return MenuTypography.typeface(this,key,style);
    }
    String menuFontKey(){return previewFontKey!=null?previewFontKey:getSharedPreferences(PREFS,0).getString("font","nunito");}
    int menuFontColor(){return selectedFontColor();}
    int menuWallpaperAccent(){return wallpaperAccentColor();}
    private static Typeface fromFile(String path, Typeface fallback) {
        try { return new File(path).exists() ? Typeface.createFromFile(path) : fallback; }
        catch (Exception e) { return fallback; }
    }

    // Draws a banner once into a tile-sized bitmap (center-cropped). Some apps ship huge banners
    // that Android 9 refuses to draw ("Canvas: trying to draw too large bitmap"); this also saves RAM.
    private Drawable fit(Drawable d, int w, int h) {
        try {
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
            if (iw <= 0 || ih <= 0) { iw = w; ih = h; }
            float s = Math.max((float) w / iw, (float) h / ih);
            int dw = Math.round(iw * s), dh = Math.round(ih * s);
            d.setBounds((w - dw) / 2, (h - dh) / 2, (w - dw) / 2 + dw, (h - dh) / 2 + dh);
            d.draw(new Canvas(b));
            return new BitmapDrawable(getResources(), b);
        } catch (Throwable t) {
            return d;
        }
    }

    /** A slow, dim colour glow over the selected wallpaper; pauses when the launcher is covered. */
    private final class WallpaperMotionView extends ProfessionalWallpaperView {
        WallpaperMotionView(Context context) { super(context); }
        @Override void setMotionStyle(String requested) {
            String style = normalizeMotionStyle(requested);
            super.setMotionStyle(style);
            setWallpaperTone(wallpaperAccentColor());
            SharedPreferences prefs = getSharedPreferences(PREFS, 0);
            setEffectTuning(prefs.getInt("motionSpeed_" + style, 0),
                    prefs.getInt("motionBrightness_" + style, 0), prefs.getInt("motionIntensity_" + style, 0));
        }
    }


    private TextClock clock(String f12, String f24, int sizePx, int color, Typeface tf) {
        TextClock c = new WallpaperBlendClock(this);
        c.setFormat12Hour(f12);
        c.setFormat24Hour(f24);
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        c.setTextColor(withFontColor(color));
        c.setTypeface(tf);
        return c;
    }

    private TextView text(int sizePx, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPx(sizePx));
        MenuTypography.recordScaledText(t,sizePx);
        t.setTextColor(withFontColor(color));
        t.setShadowLayer(px(2), 0f, px(1), 0x70000000);
        if (tf != null) t.setTypeface(tf);
        if (selectedFontColor() == RAINBOW_COLOR) applyRainbow(t);
        return t;
    }

    private void applyRainbow(TextView view) {
        Runnable shader = () -> {
            if(MenuTypography.managed(view))return;
            float width = Math.max(1, view.getWidth());
            view.getPaint().setShader(new LinearGradient(0, 0, width, 0,
                    RAINBOW_COLORS, null, Shader.TileMode.CLAMP));
            view.invalidate();
        };
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right > left) shader.run();
        });
        view.post(shader);
    }

    private int selectedFontColor() {
        if (previewFontColorOverride != null) return previewFontColorOverride;
        int saved = getSharedPreferences(PREFS, 0).getInt("fontColor", Color.WHITE);
        return saved == WALLPAPER_MATCH_COLOR ? wallpaperAccentColor() : saved;
    }

    private int withFontColor(int original) {
        int color = selectedFontColor();
        return (Color.alpha(original) << 24) | ((color == RAINBOW_COLOR ? original : color) & 0x00FFFFFF);
    }

    private void setMenuLabel(TextView view,String label){
        android.text.SpannableString styled=new android.text.SpannableString(label);
        int split=label.indexOf(" · ");
        if(split>=0){styled.setSpan(new android.text.style.RelativeSizeSpan(.91f),split,label.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        view.setText(styled);view.setContentDescription(label);
    }
    private void styleMenuText(TextView text){
        if(!stableMenuText.containsKey(text))stableMenuText.put(text,true);
        if(Boolean.FALSE.equals(stableMenuText.get(text)))MenuTypography.set(text,true,menuPx(34));
        else MenuTypography.adoptScaled(text,false);
    }
    private void styleMenuHeading(TextView text){stableMenuText.put(text,false);styleMenuText(text);}
    private void styleUtilityTitle(TextView title){title.setBackground(null);title.setPadding(menuPx(16),0,menuPx(16),0);title.setGravity(Gravity.CENTER_VERTICAL);title.setMaxWidth(menuPx(520));}
    private GradientDrawable utilityMenuSurface(){
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x304A5668,0x48202C3D,0x38131F30});
        bg.setCornerRadius(menuPx(28));bg.setStroke(Math.max(1,menuPx(1)),0x80FFFFFF);
        bg.setDither(true);return bg;
    }
    private GradientDrawable glassMenuRowBackground(boolean focused,boolean contrast){
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(focused?(contrast?0xCC182A3D:0x48FFFFFF):Color.TRANSPARENT);
        bg.setCornerRadius(menuPx(8));
        bg.setStroke(0,Color.TRANSPARENT);
        return bg;
    }
    private GradientDrawable settingsMenuFocusBackground(boolean focused,boolean contrast){return glassMenuRowBackground(focused,contrast);}

    private void applyFontPreview(View view) {
        if (view == null) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if(MenuTypography.managed(text))MenuTypography.refresh(text);
            else {
                String key=menuFontKey();Typeface base=text.getTypeface();
                text.setTypeface(fontTypeface(key,base==null?Typeface.NORMAL:base.getStyle()));
                text.setTextColor(withFontColor(text.getCurrentTextColor()));
                if(selectedFontColor()==RAINBOW_COLOR)applyRainbow(text);else text.getPaint().setShader(null);
            }
        }
        if (view instanceof NativeClockFaceView) {
            String faceFont = previewFontKey != null ? previewFontKey : getSharedPreferences(PREFS, 0).getString("font", "nunito");
            ((NativeClockFaceView) view).setClockTypeface(fontTypeface(faceFont, Typeface.NORMAL));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyFontPreview(group.getChildAt(i));
        }
    }

    private void applyFontScale(View view, float factor) {
        if (view == null || factor <= 0f) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, text.getTextSize() * factor);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyFontScale(group.getChildAt(i), factor);
        }
    }

    private void setMotionPreview(String style) {
        previewMotionStyle = style;
        if (wallpaperMotion != null) {
            wallpaperMotion.setMotionStyle(style);
            wallpaperMotion.setMotionEnabled(!"off".equals(style) && launcherAnimationMode() != 2);
            wallpaperMotion.setMotionTuning(effectiveMotionPreset());
        }
    }

    private void restorePreviewSettings() {
        wallpaperPickerPreviewing = false;
        updateVideoAwareMotion();
        previewFontKey = null;
        previewClockStyleOverride = null;
        previewFontColorOverride = null;
        previewClockColorOverride = null;
        previewMotionStyle = null;
        previewVideoCropOverride=null;
        applyVideoWallpaperTransform();
        loadFonts();
        applyFontPreview(root);
        if (wallpaperMotion != null) {
            SharedPreferences prefs = getSharedPreferences(PREFS, 0);
            String saved = normalizeMotionStyle(prefs.getString("wallpaperMotionStyle", "ambient"));
            wallpaperMotion.setMotionStyle(saved);
            wallpaperMotion.setMotionEnabled(motionEffectsEnabled());
            wallpaperMotion.setMotionTuning(effectiveMotionPreset());
        }
        applyClockStyle();
        updateAppArtworkOpacity(appArtworkIdlePercent());
    }


    private int px(int v) { return Math.round(v * scale); }
    private int menuPx(int v) { return Math.max(1, Math.round(px(v) * MENU_SCALE)); }
    private float px(float v) { return v * scale; }
    private float fontPx(float pixels) {
        return pixels * MenuTextPolicy.scale(getSharedPreferences(PREFS, 0).getInt("fontScalePercent", 120));
    }

    private static FrameLayout.LayoutParams at(int x, int y, int w, int h) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.leftMargin = x;
        lp.topMargin = y;
        return lp;
    }
}









