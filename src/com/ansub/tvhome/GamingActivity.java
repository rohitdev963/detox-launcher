package com.ansub.tvhome;
import android.app.*;import android.content.*;import android.content.pm.*;import android.graphics.*;import android.graphics.drawable.*;import android.media.*;import android.os.*;import android.view.*;import android.widget.*;import java.util.*;import java.io.*;

/** Local gaming library with remote navigation and the selected photo/video background. */
public final class GamingActivity extends Activity {
 private android.content.SharedPreferences prefs;private FrameLayout root;private LinearLayout library;private ToneGenerator tone;private boolean resumed;private float density;private TextView themeButton,musicButton;private GamingVideoBackground videoBackground;private GamingMusicPlayer music;private View gamingReadabilityScrim;private GameLaunchTransition launchTransition;private final Map<String,Boolean> gameFlags=new HashMap<>();private final Map<String,Drawable> gameArtworkCache=new HashMap<>();private final Set<String> gameArtworkPending=new HashSet<>();private final java.util.concurrent.ExecutorService gameArtworkWorker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"DetoxGamingArtwork");t.setDaemon(true);return t;});private LauncherAppArtworkResolver appArtworkResolver;private String focusedPackage="";private final int accent=0xFF75E9FF;
 private final LinkedHashMap<String,String> available=new LinkedHashMap<>();
 private DetoxPerformanceProbe performanceProbe;
 private FrameLayout backgroundLayer;private GamingThemePreview themePreview;
 private final Map<String,String> themeBrowseSelections=new HashMap<>();private String themeRootSelection="images";
 private final Handler backgroundHandler=new Handler(Looper.getMainLooper());
 private final Map<String,String> settingsFocus=new HashMap<>();
 private TextView settingsButton;private TextView[] gamingRailActions;private int gameArtworkGeneration;private boolean pendingIconReload;
 private final Runnable refreshIconRows=()->{if(!resumed)return;boolean reload=pendingIconReload;pendingIconReload=false;refreshGamingIcons(reload);};
 private final android.content.SharedPreferences.OnSharedPreferenceChangeListener iconPreferencesListener=(store,key)->{
  if(AppIconSettingsPolicy.presentationKey(key)){pendingIconReload|=AppIconSettingsPolicy.artworkKey(key);backgroundHandler.removeCallbacks(refreshIconRows);backgroundHandler.postDelayed(refreshIconRows,70);}
 };

 private final Runnable refreshHomeBackground=()->{if(resumed&&GamingBackgroundCatalog.SAME_HOME.equals(GamingBackgroundCatalog.savedId(prefs.getAll())))replaceSelectedBackground();};
 private final android.content.SharedPreferences.OnSharedPreferenceChangeListener homeBackgroundListener=(store,key)->{
  if(GamingBackgroundCatalog.homeKey(key)&&GamingBackgroundCatalog.SAME_HOME.equals(GamingBackgroundCatalog.savedId(store.getAll()))){backgroundHandler.removeCallbacks(refreshHomeBackground);backgroundHandler.postDelayed(refreshHomeBackground,90);}
 };

 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setFlags(1024,1024);configureGamingWindow();prefs=getSharedPreferences("config",0);clearRetiredGamingBackgroundSettings();appArtworkResolver=new LauncherAppArtworkResolver(this);density=getResources().getDisplayMetrics().density;music=GamingMusicCatalog.available()?new GamingMusicPlayer(this):null;try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,18);}catch(RuntimeException ignored){}overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.fade_out);}
 private void clearRetiredGamingBackgroundSettings(){
  Set<String> retired=GamingBackgroundRetirementPolicy.keysToRemove(prefs.getAll().keySet());
  if(retired.isEmpty())return;
  android.content.SharedPreferences.Editor editor=prefs.edit();for(String key:retired)editor.remove(key);editor.apply();
 }
 private void configureGamingWindow(){
  getWindow().setNavigationBarColor(Color.TRANSPARENT);
  View decor=getWindow().getDecorView();
  decor.setSystemUiVisibility(decor.getSystemUiVisibility()|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
 }
 private GamingBackgroundCatalog.Choice selectedBackground(){return GamingBackgroundCatalog.resolve(prefs.getAll(),null);}
 private void updateThemeDescription(){if(themeButton!=null)themeButton.setContentDescription("Theme. Current background: "+selectedBackground().name);}
 private int dp(float n){return Math.round(n*density);}
 // Keep Gaming submenus visually aligned with the launcher's existing glass Settings menus.
 // Centralizing this also covers dialogs opened from game cards.
 private AlertDialog.Builder menuBuilder(){return new GamingMenuBuilder();}
 private final class GamingMenuBuilder extends AlertDialog.Builder {
  GamingMenuBuilder(){super(GamingActivity.this,android.R.style.Theme_Material_Dialog_Alert);}
  @Override public AlertDialog create(){
   AlertDialog dialog=super.create();
   dialog.setOnShowListener(ignored->styleGamingMenu(dialog));
   return dialog;
  }
 }
 private void styleGamingMenu(AlertDialog dialog){
  DetoxDialogStyle.apply(dialog,prefs.getBoolean("menuBlur",false),dp(1));
 }
 private void discover(){available.clear();gameFlags.clear();PackageManager pm=getPackageManager();for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){if(info.activityInfo!=null&&!getPackageName().equals(info.activityInfo.packageName))available.put(info.activityInfo.packageName,info.loadLabel(pm).toString());}}
 private boolean game(String pkg){Boolean cached=gameFlags.get(pkg);if(cached!=null)return cached;try{ApplicationInfo app=getPackageManager().getApplicationInfo(pkg,0);boolean value=(Build.VERSION.SDK_INT>=26&&app.category==ApplicationInfo.CATEGORY_GAME)||(app.flags&0x02000000)!=0;gameFlags.put(pkg,value);return value;}catch(Exception e){return false;}}
 private Set<String> saved(String key){return new HashSet<>(prefs.getStringSet(key,Collections.emptySet()));}
 private void render(){
  discover();
  clearBackgroundMedia();root=new FrameLayout(this);root.setBackgroundColor(0xFF080B11);
  addGamingBackground();
  gamingReadabilityScrim=new View(this);applyGamingReadability();root.addView(gamingReadabilityScrim,new FrameLayout.LayoutParams(-1,-1));
  // Keep the selected background visible and the existing actions in their compact rail.
  LinearLayout content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(24),dp(14),dp(24),dp(6));root.addView(content,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=label("🎮  Detox Gaming",23);title.setTypeface(Typeface.create("sans-serif-medium",0));title.setShadowLayer(dp(4),0,dp(2),0xFF000000);
  header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  content.addView(header,new LinearLayout.LayoutParams(-1,dp(54)));
  content.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setClipToPadding(false);scroll.setFillViewport(true);scroll.setHorizontalScrollBarEnabled(false);scroll.setPadding(dp(6),dp(8),dp(6),0);
  library=new LinearLayout(this);library.setOrientation(LinearLayout.HORIZONTAL);library.setGravity(Gravity.BOTTOM);library.setClipChildren(false);library.setClipToPadding(false);scroll.addView(library,new android.widget.FrameLayout.LayoutParams(-2,-1));
  content.addView(scroll,new LinearLayout.LayoutParams(-1,dp(156)));
  scroll.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
   if(right-left!=oldRight-oldLeft)spreadGameCards(scroll,(LinearLayout)scroll.getChildAt(0));
  });
  List<String> sorted=gamingLibraryPackages();
  View first=null,restore=null;
  int layoutMode=Math.max(0,Math.min(2,prefs.getInt("appLayoutMode",0)));int cardWidth=dp(layoutMode==1?132:layoutMode==2?104:116),cardHeight=dp(layoutMode==1?132:layoutMode==2?106:118);
  for(String pkg:sorted){View card=gameCard(pkg,layoutMode);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(cardWidth,cardHeight);cp.setMargins(dp(4),dp(4),dp(6),dp(8));library.addView(card,cp);if(first==null)first=card;if(pkg.equals(focusedPackage))restore=card;}
  if(first==null){TextView message=label("No games in this view",18);message.setPadding(dp(5),dp(8),0,dp(12));library.addView(message);TextView empty=button("Manage library",this::gamingLibrarySettings);library.addView(empty,new LinearLayout.LayoutParams(-2,-2));first=empty;}
  LinearLayout rail=new LinearLayout(this);rail.setOrientation(LinearLayout.VERTICAL);rail.setGravity(Gravity.CENTER_HORIZONTAL);rail.setClipChildren(false);rail.setClipToPadding(false);rail.setBackgroundColor(Color.TRANSPARENT);
  FrameLayout.LayoutParams railLp=new FrameLayout.LayoutParams(dp(44),-2,Gravity.RIGHT|Gravity.CENTER_VERTICAL);railLp.rightMargin=dp(16);root.addView(rail,railLp);
  FrameLayout homeSlot=railAction("home","⌂","Home",this::finish);
  FrameLayout addSlot=railAction("add","＋","Add games",this::addApps);
  FrameLayout themeSlot=railAction("theme","◈","Theme",this::showThemePicker);
  themeButton=(TextView)themeSlot.getTag();updateThemeDescription();
  FrameLayout moreSlot=railAction("more","⋯","Settings",this::libraryMenu);settingsButton=(TextView)moreSlot.getTag();
  FrameLayout[] railSlots={homeSlot,addSlot,themeSlot,moreSlot};TextView[] railActions=new TextView[railSlots.length];
  for(int i=0;i<railSlots.length;i++){
   railActions[i]=(TextView)railSlots[i].getTag();
   LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(40));
   if(i>0)ap.topMargin=dp(2);
   rail.addView(railSlots[i],ap);
  }
  gamingRailActions=railActions;
  // Populate every action before linking forward focus targets.
  for(int i=0;i<railActions.length;i++){
   if(i>0)railActions[i].setNextFocusUpId(railActions[i-1].getId());
   if(i+1<railActions.length)railActions[i].setNextFocusDownId(railActions[i+1].getId());
  }
  LinearLayout headerControls=new LinearLayout(this);headerControls.setGravity(Gravity.CENTER_VERTICAL);headerControls.setClipChildren(false);headerControls.setClipToPadding(false);
  musicButton=null;if(GamingMusicCatalog.available()){musicButton=musicToggleButton();updateMusicToggle();LinearLayout.LayoutParams musicLp=new LinearLayout.LayoutParams(dp(64),dp(30));musicLp.rightMargin=dp(10);headerControls.addView(musicButton,musicLp);}
  TextClock clock=new TextClock(this);clock.setFormat12Hour("h:mm a");clock.setFormat24Hour("HH:mm");clock.setTextSize(14);clock.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));clock.setTextColor(0xFFF5F8FF);clock.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);clock.setShadowLayer(dp(2),0,dp(1),0xC0000000);headerControls.addView(clock,new LinearLayout.LayoutParams(-2,dp(34)));
  FrameLayout.LayoutParams headerControlsLp=new FrameLayout.LayoutParams(-2,dp(40),Gravity.TOP|Gravity.RIGHT);headerControlsLp.topMargin=dp(12);headerControlsLp.rightMargin=dp(16);root.addView(headerControls,headerControlsLp);
  View railTarget=library.getChildCount()>0?library.getChildAt(library.getChildCount()-1):first;if(railTarget!=null){railTarget.setNextFocusRightId(railActions[0].getId());for(TextView action:railActions)action.setNextFocusLeftId(railTarget.getId());}
  setContentView(root);if(resumed){if(videoBackground!=null)videoBackground.active(hasWindowFocus());}final View focus=restore!=null?restore:first;focus.post(()->{focus.requestFocus();focus.requestRectangleOnScreen(new Rect(0,0,focus.getWidth(),focus.getHeight()),true);});
 }
 private void spreadGameCards(HorizontalScrollView scroll,LinearLayout row){
  int count=row.getChildCount();if(count==0)return;
  for(int i=0;i<count;i++)if(!(row.getChildAt(i).getTag() instanceof String))return;
  int viewport=Math.max(0,scroll.getWidth()-scroll.getPaddingLeft()-scroll.getPaddingRight());
  int widths=0;for(int i=0;i<count;i++)widths+=row.getChildAt(i).getLayoutParams().width;
  int extra=Math.max(0,viewport-widths-dp(12)*count);
  for(int i=0;i<count;i++){
   View card=row.getChildAt(i);LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)card.getLayoutParams();
   int cellExtra=extra/count+(i<extra%count?1:0);
   int leftMargin=dp(6)+cellExtra/2,rightMargin=dp(6)+cellExtra-cellExtra/2;
   if(params.leftMargin!=leftMargin||params.rightMargin!=rightMargin||params.bottomMargin!=dp(2)){
    params.setMargins(leftMargin,dp(4),rightMargin,dp(2));card.setLayoutParams(params);
   }
  }
 }
 private void clearBackgroundMedia(){
  if(videoBackground!=null){videoBackground.active(false);videoBackground.close();videoBackground=null;}
  if(backgroundLayer!=null)backgroundLayer.removeAllViews();
 }
 private void addGamingBackground(){
  backgroundLayer=new FrameLayout(this);backgroundLayer.setClipChildren(true);backgroundLayer.setBackgroundColor(0xFF080B11);root.addView(backgroundLayer,0,new FrameLayout.LayoutParams(-1,-1));
  showSelectedBackground();
 }
 private void replaceSelectedBackground(){if(backgroundLayer==null||root==null)return;clearBackgroundMedia();showSelectedBackground();updateThemeDescription();}
 private void showSelectedBackground(){
  GamingBackgroundCatalog.Choice choice=selectedBackground();Map<String,?> snapshot=prefs.getAll();
  if(choice.video()){
   videoBackground=createVideoBackground(choice.source);videoBackground.framing(GamingBackgroundCatalog.fill(snapshot,choice),GamingBackgroundCatalog.zoom(snapshot,choice));
   backgroundLayer.addView(videoBackground,new FrameLayout.LayoutParams(-1,-1));videoBackground.active(resumed&&hasWindowFocus());
  }else{
   final FrameLayout owner=backgroundLayer;
   GamingImageBackground image=new GamingImageBackground(this,choice,GamingBackgroundCatalog.fill(snapshot,choice),GamingBackgroundCatalog.zoom(snapshot,choice),GamingBackgroundCatalog.position(snapshot,choice,true),GamingBackgroundCatalog.position(snapshot,choice,false),source->{
    if(owner!=backgroundLayer||isFinishing()||isDestroyed())return;
    Toast.makeText(this,"Selected image unavailable; showing the default wallpaper",Toast.LENGTH_SHORT).show();
    GamingBackgroundCatalog.Choice fallback=GamingBackgroundCatalog.entries(GamingBackgroundCatalog.IMAGES)[0];
    owner.removeAllViews();owner.addView(new GamingImageBackground(this,fallback,true,1,.5f,.5f,null),new FrameLayout.LayoutParams(-1,-1));
   });backgroundLayer.addView(image,new FrameLayout.LayoutParams(-1,-1));
  }
 }
 private GamingVideoBackground createVideoBackground(String source){
  final GamingVideoBackground[] owner={null};
  owner[0]=new GamingVideoBackground(this,source,new GamingVideoBackground.Listener(){
   @Override public void onFallback(String failed,String replacement){runOnUiThread(()->{
    if(isFinishing()||isDestroyed()||owner[0]!=videoBackground)return;
    // Temporary playback recovery must never replace the user's saved media or Home-follow mode.
    Toast.makeText(GamingActivity.this,"Selected video unavailable; playing a temporary Gaming fallback",Toast.LENGTH_SHORT).show();
   });}
   @Override public void onUnavailable(String lastFailed){runOnUiThread(()->showVideoFallback(owner[0]));}
  });
  return owner[0];
 }
 private void showVideoFallback(GamingVideoBackground owner){
  if(owner==null||owner!=videoBackground||backgroundLayer==null||isFinishing()||isDestroyed())return;
  clearBackgroundMedia();
  GamingBackgroundCatalog.Choice image=GamingBackgroundCatalog.entries(GamingBackgroundCatalog.IMAGES)[0];
  backgroundLayer.addView(new GamingImageBackground(this,image,true,1,.5f,.5f,null),new FrameLayout.LayoutParams(-1,-1));
  Toast.makeText(this,"Video unavailable on this device; showing an original still background",Toast.LENGTH_LONG).show();
 }

 private View gameCard(String pkg,int layoutMode){
  Map<String,?> iconSettings=prefs.getAll();
  int height=dp(layoutMode==1?132:layoutMode==2?106:118),captionHeight=dp(26),artHeight=height-captionHeight;
  FrameLayout card=new FrameLayout(this);card.setId(View.generateViewId());card.setTag(pkg);card.setFocusable(true);card.setClickable(true);card.setContentDescription(available.get(pkg));card.setClipChildren(false);card.setBackgroundColor(Color.TRANSPARENT);
  int tileStyle=Math.max(0,Math.min(2,prefs.getInt("homeTileStyle",1))),theme=HomePremiumThemes.index(prefs.getString("homePremiumTheme","classic"));
  int radius=theme>0?HomePremiumThemes.RADII[theme]:tileStyle==2?dp(20):dp(12);
  GradientDrawable surface=new GradientDrawable();surface.setCornerRadius(radius);
  if(tileStyle==0){surface.setColor(Color.TRANSPARENT);surface.setStroke(0,Color.TRANSPARENT);}
  else if(tileStyle==1){surface.setColor(0xE51A2230);surface.setStroke(dp(1),0x60596D84);}
  else{surface.setColor(0x553C4B60);surface.setStroke(dp(1),0x80DCEBFF);}
  if(theme>0&&tileStyle!=0){surface.setColors(HomePremiumThemes.tileGradient(theme));surface.setStroke(dp(1),HomePremiumThemes.BORDERS[theme]);}
  FrameLayout artPanel=new FrameLayout(this);artPanel.setClipChildren(false);artPanel.setBackground(surface);card.addView(artPanel,new FrameLayout.LayoutParams(-1,artHeight,Gravity.BOTTOM));
  GradientDrawable ambient=new GradientDrawable();ambient.setGradientType(GradientDrawable.RADIAL_GRADIENT);ambient.setGradientRadius(dp(165));ambient.setGradientCenter(.5f,.75f);ambient.setColors(new int[]{(accent&0x00FFFFFF)|0x58000000,(accent&0x00FFFFFF)|0x14000000,0x00000000});ambient.setCornerRadius(radius);ambient.setAlpha(0);artPanel.setForeground(ambient);
  ImageView image=new ImageView(this);image.setAlpha(AppIconSettingsPolicy.opacity(iconSettings,false));image.setTag("gaming-artwork");image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(available.get(pkg));
  int iconPercent=AppIconSettingsPolicy.read(iconSettings,AppIconSettingsPolicy.SIZE);int baseIcon=layoutMode==1?dp(84):layoutMode==2?dp(62):dp(74);int iconSide=Math.max(dp(36),Math.round(baseIcon*iconPercent/100f));
  FrameLayout.LayoutParams iconLp=new FrameLayout.LayoutParams(iconSide,iconSide,Gravity.CENTER);artPanel.addView(image,iconLp);Drawable cached=gameArtworkCache.get(pkg);if(cached!=null)image.setImageDrawable(new AppIconDrawable(cached));else requestGameArtwork(pkg);
  TextView name=label((saved("gamingFavorites").contains(pkg)?"★  ":"")+available.get(pkg),14);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));name.setShadowLayer(dp(3),0,dp(1),0xD0000000);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setIncludeFontPadding(false);name.setGravity(Gravity.CENTER);name.setPadding(dp(2),0,dp(2),0);name.setContentDescription(available.get(pkg));name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  int labelMode=Math.max(0,Math.min(2,prefs.getInt("homeAppLabels",0)));name.setVisibility(labelMode==1?View.VISIBLE:View.INVISIBLE);FrameLayout.LayoutParams nameLp=new FrameLayout.LayoutParams(-1,captionHeight,Gravity.TOP);card.addView(name,nameLp);
  int idleOpacity=AppIconSettingsPolicy.read(iconSettings,AppIconSettingsPolicy.VISIBILITY);
  GamingIconGlow iconGlow=new GamingIconGlow(card,artPanel,ambient);
  card.setOnFocusChangeListener((view,focused)->{
   int focusAccent=theme>0?HomePremiumThemes.ACCENTS[theme]:accent;boolean highContrast=prefs.getBoolean("highContrastFocus",false);surface.setStroke(dp(focused?(highContrast?6:2):tileStyle==0?0:1),focused?(highContrast?Color.WHITE:focusAccent):theme>0&&tileStyle!=0?HomePremiumThemes.BORDERS[theme]:tileStyle==2?0x80DCEBFF:0x60596D84);
   name.setVisibility(labelMode==1||(labelMode==0&&focused)?View.VISIBLE:View.INVISIBLE);name.setTextColor(focused?Color.WHITE:0xD6E3EEF7);image.setAlpha(focused?1f:idleOpacity/100f);card.setElevation(focused?dp(12):0);
   iconGlow.focus(focused,motionAllowed(),AppIconSettingsPolicy.breathing(prefs.getAll()));
   if(focused){focusedPackage=pkg;card.post(()->card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true));}focusScale(card,focused,1.035f,160);
  });
  DetoxPointerFocus.install(card);card.setOnClickListener(v->{sound(true);launch(pkg);});card.setOnLongClickListener(v->{gameOptions(pkg);return true;});card.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BUTTON_X||key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)gameOptions(pkg);return true;}if(key==KeyEvent.KEYCODE_BUTTON_A){if(event.getAction()==KeyEvent.ACTION_UP)launch(pkg);return true;}return false;});return card;
 }
 private void invalidateGamingArtwork(){
  gameArtworkGeneration++;gameArtworkCache.clear();gameArtworkPending.clear();
  try{gameArtworkWorker.execute(()->appArtworkResolver.invalidate());}catch(java.util.concurrent.RejectedExecutionException ignored){}
 }
 private void refreshGamingIcons(boolean reloadArtwork){
  if(reloadArtwork)invalidateGamingArtwork();if(library==null)return;
  List<String> packages=gamingLibraryPackages();
  int mode=AppIconSettingsPolicy.read(prefs.getAll(),AppIconSettingsPolicy.LAYOUT);int width=dp(mode==1?132:mode==2?104:116),height=dp(mode==1?132:mode==2?106:118);
  library.removeAllViews();View restore=null;
  if(packages.isEmpty()){TextView empty=button("No games in this view · Manage library",this::gamingLibrarySettings);library.addView(empty);restore=empty;}
  for(String pkg:packages){View card=gameCard(pkg,mode);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,height);lp.setMargins(dp(4),dp(4),dp(6),dp(8));library.addView(card,lp);if(pkg.equals(focusedPackage))restore=card;}
  if(gamingRailActions!=null&&gamingRailActions.length>0){View last=library.getChildAt(library.getChildCount()-1);last.setNextFocusRightId(gamingRailActions[0].getId());for(TextView action:gamingRailActions)action.setNextFocusLeftId(last.getId());}
  if(library.getParent() instanceof HorizontalScrollView){HorizontalScrollView scroll=(HorizontalScrollView)library.getParent();library.post(()->{if(library.getParent()==scroll)spreadGameCards(scroll,library);});}
  if(restore!=null)restore.requestFocus();else if(library.getChildCount()>0)library.getChildAt(0).requestFocus();
 }
 private void requestGameArtwork(String pkg){
  Drawable cached=gameArtworkCache.get(pkg);if(cached!=null){applyGameArtwork(pkg,cached);return;}
  if(!gameArtworkPending.add(pkg))return;final int generation=gameArtworkGeneration;
  try{gameArtworkWorker.execute(()->{Drawable loaded=null;try{loaded=appArtworkResolver.resolve(pkg);}catch(RuntimeException ignored){}final Drawable result=loaded;runOnUiThread(()->{
   if(generation!=gameArtworkGeneration||isFinishing()||isDestroyed())return;
   gameArtworkPending.remove(pkg);if(result!=null){gameArtworkCache.put(pkg,result);applyGameArtwork(pkg,result);}
  });});}catch(java.util.concurrent.RejectedExecutionException ignored){gameArtworkPending.remove(pkg);}
 }
 private void applyGameArtwork(String pkg,Drawable artwork){
  if(library==null)return;for(int i=0;i<library.getChildCount();i++){View card=library.getChildAt(i);if(!pkg.equals(card.getTag()))continue;View image=card.findViewWithTag("gaming-artwork");if(image instanceof ImageView){((ImageView)image).setImageDrawable(new AppIconDrawable(artwork));((ImageView)image).setAlpha(card.hasFocus()?1f:Math.max(20,Math.min(100,prefs.getInt("appArtworkIdlePercent",85)))/100f);}return;}
 }
 @Override public boolean onKeyUp(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_BUTTON_B){finish();return true;}return super.onKeyUp(key,event);}
 private FrameLayout railAction(String id,String glyph,String name,Runnable action){
  FrameLayout slot=new FrameLayout(this);slot.setClipChildren(false);slot.setClipToPadding(false);
  TextView hint=label(name,13);hint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));hint.setTextColor(0xFFF5F8FF);hint.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);hint.setPadding(dp(10),dp(4),dp(10),dp(4));hint.setBackground(glassSurface());hint.setVisibility(View.GONE);hint.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  FrameLayout.LayoutParams hintLp=new FrameLayout.LayoutParams(dp(116),dp(32),Gravity.LEFT|Gravity.CENTER_VERTICAL);hintLp.leftMargin=-dp(124);slot.addView(hint,hintLp);
  TextView icon=label("",20);icon.setText(BundledUtilityArtwork.railLabel(this,id,glyph,dp(28)));icon.setIncludeFontPadding(false);icon.setId(View.generateViewId());icon.setContentDescription(name);icon.setGravity(Gravity.CENTER);icon.setFocusable(true);icon.setClickable(true);
  GradientDrawable idle=new GradientDrawable();idle.setColor(0x1EFFFFFF);idle.setCornerRadius(dp(15));icon.setBackground(idle);
  icon.setOnFocusChangeListener((view,focused)->{
   GradientDrawable state=new GradientDrawable();state.setColor(focused?0x90A9C8E1:0x1EFFFFFF);state.setCornerRadius(dp(15));state.setStroke(focused?dp(1):0,focused?0xD8FFFFFF:Color.TRANSPARENT);icon.setBackground(state);
   if(focused){hint.setVisibility(View.VISIBLE);hint.animate().cancel();if(motionAllowed()){hint.setAlpha(0f);hint.setTranslationX(dp(8));hint.animate().alpha(1f).translationX(0f).setDuration(150L).start();}else{hint.setAlpha(1f);hint.setTranslationX(0f);}}
   else{hint.animate().cancel();hint.setVisibility(View.GONE);hint.setAlpha(0f);}focusScale(icon,focused,1.06f,150);
  });
  icon.setOnClickListener(view->{sound(true);action.run();});DetoxPointerFocus.install(icon);
  slot.addView(icon,new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.CENTER));slot.setTag(icon);return slot;
 }
 private TextView musicToggleButton(){
  TextView toggle=label("",13);toggle.setIncludeFontPadding(false);toggle.setId(View.generateViewId());toggle.setGravity(Gravity.CENTER);toggle.setSingleLine(true);toggle.setFocusable(true);toggle.setClickable(true);
  toggle.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));toggle.setPadding(dp(8),0,dp(8),0);
  toggle.setBackground(glassSurface());
  toggle.setOnFocusChangeListener((view,focused)->{
   GradientDrawable state=glassSurface();state.setColors(focused?new int[]{0xB8A3B6C9,0xA8647C98,0xA83B536D}:new int[]{0x304A5668,0x48202C3D,0x38131F30});state.setStroke(dp(1),focused?0xFFFFFFFF:0x80FFFFFF);toggle.setBackground(state);
   focusScale(toggle,focused,1.04f,150);
  });
  toggle.setOnClickListener(view->toggleGamingMusic());DetoxPointerFocus.install(toggle);return toggle;
 }
 private void updateMusicToggle(){
  if(musicButton==null||prefs==null)return;boolean enabled=prefs.getBoolean("gamingMusicEnabled",true);
  musicButton.setText(enabled?"♫  ON":"♫  OFF");musicButton.setContentDescription(enabled?"Turn gaming background music off":"Turn gaming background music on");
  musicButton.setAlpha(enabled?1f:.78f);
 }
 private void toggleGamingMusic(){
  if(!GamingMusicCatalog.available())return;
  boolean enabled=!prefs.getBoolean("gamingMusicEnabled",true);prefs.edit().putBoolean("gamingMusicEnabled",enabled).apply();
  if(music!=null)music.settingsChanged();updateMusicToggle();sound(true);
 }
 private GradientDrawable glassSurface(){GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0x304A5668,0x48202C3D,0x38131F30});glass.setCornerRadius(dp(10));glass.setStroke(dp(1),0x80FFFFFF);glass.setDither(true);return glass;}
 private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xFFE6F7FF);return t;}
 private TextView button(String text,Runnable action){TextView t=label(text,16);t.setId(View.generateViewId());t.setShadowLayer(dp(2),0,dp(1),0xC0000000);t.setGravity(Gravity.CENTER);t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setFocusable(true);t.setClickable(true);t.setMinHeight(dp(48));GradientDrawable bg=glassSurface();t.setBackground(bg);t.setOnFocusChangeListener((v,focus)->{bg.setColors(focus?new int[]{0xB8A3B6C9,0xA8647C98,0xA83B536D}:new int[]{0x304A5668,0x48202C3D,0x38131F30});t.setTextColor(Color.WHITE);bg.setStroke(dp(1),focus?0xFFFFFFFF:0x80FFFFFF);if(focus){Object tag=v.getTag();if(tag instanceof String&&available.containsKey((String)tag)){focusedPackage=(String)tag;}sound(false);}focusScale(v,focus,1.035f,160);});DetoxPointerFocus.install(t);t.setOnClickListener(v->{sound(true);action.run();});return t;}
 private void sound(boolean select){if(tone!=null&&prefs.getBoolean("gamingSound",false))try{tone.startTone(select?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,select?70:28);}catch(RuntimeException ignored){}}
 private void launch(String pkg){
  if(launchTransition!=null&&launchTransition.running())return;
  Intent target=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(target==null)target=getPackageManager().getLaunchIntentForPackage(pkg);
  if(target==null){Toast.makeText(this,"This app cannot be opened",Toast.LENGTH_SHORT).show();return;}
  final Intent intent=target;String name=available.containsKey(pkg)?available.get(pkg):pkg;
  launchTransition=new GameLaunchTransition(this,root,music,name,motionAllowed()&&prefs.getBoolean("gamingLaunchAnimation",true),prefs.getInt("detoxPerformancePreset",GamingPerformancePolicy.DEFAULT),()->{
   if(!resumed)return;
   try{startActivity(intent);prefs.edit().putLong("gamingPlayed_"+pkg,System.currentTimeMillis()).putString("gamingLastPlayed",pkg).apply();}
   catch(Exception error){if(launchTransition!=null)launchTransition.cancel(true);launchTransition=null;DetoxDiagnostics.recordFailure("DetoxGaming","Game launch",pkg,error);Toast.makeText(this,"This app cannot be opened",Toast.LENGTH_SHORT).show();}
  });
 }
 private List<String> gamingLibraryPackages(){
  Set<String> selected=saved("gamingApps"),hidden=saved("gamingHidden"),favorites=saved("gamingFavorites");
  for(String pkg:available.keySet())if(game(pkg)&&!hidden.contains(pkg))selected.add(pkg);selected.removeAll(hidden);
  return GamingLibraryPolicy.order(selected,available,prefs.getAll(),MainActivity.homeAppPlacementOrder(this),favorites);
 }
 private void addApps(){showGamingAppVisibility(null);}
 /** Every rebuilt menu shares the Home glass surface, stable row IDs and immediate-parent BACK. */
 private interface MenuAction {void accept(String id);}
 private interface ChoiceAction {void accept(int index);}
 private void gamingMenu(String title,List<SettingsChoice> rows,MenuAction action,Runnable parent){
  String[] labels=new String[rows.size()];for(int i=0;i<labels.length;i++)labels[i]=rows.get(i).label;
  AlertDialog dialog=menuBuilder().setTitle(title).setItems(labels,(d,index)->{String id=rows.get(index).id;settingsFocus.put(title,id);d.dismiss();action.accept(id);}).setNegativeButton(parent==null?"Close":"Back",(d,w)->{if(parent!=null)parent.run();else if(settingsButton!=null)settingsButton.requestFocus();}).create();themeMenu(dialog,parent!=null?parent:()->{if(settingsButton!=null)settingsButton.requestFocus();});
  for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(settingsFocus.get(title))){dialog.getListView().setSelection(i);break;}
 }
 private void gamingChoice(String title,String[] names,int selected,ChoiceAction save,Runnable parent){
  AlertDialog dialog=menuBuilder().setTitle(title).setSingleChoiceItems(names,Math.max(0,Math.min(names.length-1,selected)),(d,index)->{save.accept(index);d.dismiss();if(parent!=null)parent.run();}).setNegativeButton("Back",(d,w)->{if(parent!=null)parent.run();}).create();themeMenu(dialog,parent);
 }
 private void gamingToggle(String title,String key,boolean fallback,Runnable parent,Runnable apply){gamingChoice(title,new String[]{"Off","On"},prefs.getBoolean(key,fallback)?1:0,index->{prefs.edit().putBoolean(key,index==1).apply();if(apply!=null)apply.run();},parent);}

 private void gamingLibrarySettings(){
  gamingMenu("Library",Arrays.asList(new SettingsChoice("apps","Visible games"),new SettingsChoice("sort","Sort order · "+GamingLibraryPolicy.SORT_NAMES[GamingLibraryPolicy.clamp(prefs.getInt("gamingSortOrder",0),4)]),new SettingsChoice("filter","Show · "+GamingLibraryPolicy.FILTER_NAMES[GamingLibraryPolicy.clamp(prefs.getInt("gamingLibraryFilter",0),3)]),new SettingsChoice("launch","Launch animation · "+(prefs.getBoolean("gamingLaunchAnimation",true)?"On":"Off"))),id->{
   if("apps".equals(id))showGamingAppVisibility(this::gamingLibrarySettings);
   else if("sort".equals(id))gamingChoice("Sort games",GamingLibraryPolicy.SORT_NAMES,prefs.getInt("gamingSortOrder",0),index->{prefs.edit().putInt("gamingSortOrder",index).apply();refreshGamingIcons(false);},this::gamingLibrarySettings);
   else if("filter".equals(id))gamingChoice("Show games",GamingLibraryPolicy.FILTER_NAMES,prefs.getInt("gamingLibraryFilter",0),index->{prefs.edit().putInt("gamingLibraryFilter",index).apply();refreshGamingIcons(false);},this::gamingLibrarySettings);
   else gamingToggle("Launch animation","gamingLaunchAnimation",true,this::gamingLibrarySettings,null);
  },this::libraryMenu);
 }
 private void gamingBackgroundSettings(){
  gamingMenu("Background",Arrays.asList(new SettingsChoice("theme","Choose image or video"),new SettingsChoice("contrast","Background contrast")),id->{
   if("theme".equals(id))showThemePicker(this::gamingBackgroundSettings);
   else if("contrast".equals(id))backgroundReadabilityOptions();
  },this::libraryMenu);
 }

 private void gamingAudioSettings(){
  List<SettingsChoice> rows=new ArrayList<>();
  if(GamingMusicCatalog.available()){rows.add(new SettingsChoice("music","Background music · "+(prefs.getBoolean("gamingMusicEnabled",true)?"On":"Off")));rows.add(new SettingsChoice("volume","Music volume · "+prefs.getInt("gamingMusicVolume",25)+"%"));}
  rows.add(new SettingsChoice("sounds","Navigation sounds · "+(prefs.getBoolean("gamingSound",false)?"On":"Off")));
  gamingMenu("Audio",rows,id->{
   if("music".equals(id))gamingToggle("Background music","gamingMusicEnabled",true,this::gamingAudioSettings,()->{if(music!=null)music.settingsChanged();updateMusicToggle();});
   else if("sounds".equals(id))gamingToggle("Navigation sounds","gamingSound",false,this::gamingAudioSettings,null);
   else if("volume".equals(id)){int[] values={0,10,15,25,40,55,70};String[] names=new String[values.length];int selected=0;for(int i=0;i<values.length;i++){names[i]=values[i]+"%";if(Math.abs(values[i]-prefs.getInt("gamingMusicVolume",25))<Math.abs(values[selected]-prefs.getInt("gamingMusicVolume",25)))selected=i;}gamingChoice("Music volume",names,selected,index->{prefs.edit().putInt("gamingMusicVolume",values[index]).apply();if(music!=null)music.settingsChanged();},this::gamingAudioSettings);}
  },this::libraryMenu);
 }

 @Override protected void onResume(){super.onResume();configureGamingWindow();CompanionInput.resume(this);resumed=true;prefs.registerOnSharedPreferenceChangeListener(homeBackgroundListener);prefs.registerOnSharedPreferenceChangeListener(iconPreferencesListener);pendingIconReload=false;invalidateGamingArtwork();render();if(music!=null)music.active(true);if(performanceProbe==null)performanceProbe=new DetoxPerformanceProbe(this,"gaming");performanceProbe.start();}
 @Override protected void onPause(){if(performanceProbe!=null)performanceProbe.stop();CompanionInput.pause(this);resumed=false;prefs.unregisterOnSharedPreferenceChangeListener(homeBackgroundListener);prefs.unregisterOnSharedPreferenceChangeListener(iconPreferencesListener);backgroundHandler.removeCallbacks(refreshHomeBackground);backgroundHandler.removeCallbacks(refreshIconRows);if(themePreview!=null){themePreview.close();themePreview=null;}if(music!=null)music.active(false);if(launchTransition!=null){launchTransition.cancel(false);launchTransition=null;}if(videoBackground!=null){videoBackground.active(false);videoBackground.close();}if(tone!=null)tone.stopTone();super.onPause();}
 @Override protected void onDestroy(){backgroundHandler.removeCallbacksAndMessages(null);prefs.unregisterOnSharedPreferenceChangeListener(homeBackgroundListener);prefs.unregisterOnSharedPreferenceChangeListener(iconPreferencesListener);if(themePreview!=null){themePreview.close();themePreview=null;}clearBackgroundMedia();gameArtworkWorker.shutdownNow();gameArtworkPending.clear();if(music!=null)music.close();if(launchTransition!=null){launchTransition.cancel(false);launchTransition=null;}if(videoBackground!=null)videoBackground.close();if(tone!=null){tone.release();tone=null;}super.onDestroy();}
 private void libraryMenu(){
  gamingMenu("Gaming Settings",Arrays.asList(new SettingsChoice("library","Library"),new SettingsChoice("icons","Icons"),new SettingsChoice("background","Background"),new SettingsChoice("audio","Audio"),new SettingsChoice("performance","Performance")),id->{
   switch(id){case "library":gamingLibrarySettings();break;case "icons":showGamingIconSettings();break;case "background":gamingBackgroundSettings();break;case "audio":gamingAudioSettings();break;case "performance":performanceMenu();break;}
  },null);
 }

 private void showGamingIconSettings(){
  List<SettingsChoice> rows=new ArrayList<>();rows.add(new SettingsChoice("pack","Icon Pack"));
  for(AppIconSettingsPolicy.Option option:new AppIconSettingsPolicy.Option[]{AppIconSettingsPolicy.SIZE,AppIconSettingsPolicy.VISIBILITY,AppIconSettingsPolicy.LABELS,AppIconSettingsPolicy.TILES,AppIconSettingsPolicy.LAYOUT,AppIconSettingsPolicy.GLOW,AppIconSettingsPolicy.CONTRAST}){
   int value=AppIconSettingsPolicy.read(prefs.getAll(),option);String current=option==AppIconSettingsPolicy.SIZE||option==AppIconSettingsPolicy.VISIBILITY?value+"%":option.labels[option.selected(value)];rows.add(new SettingsChoice(option.id,option.title+" · "+current));
  }
  rows.add(new SettingsChoice("visible","Visible Apps · Gaming only"));
  gamingMenu("Icons · Home + Gaming",rows,id->{if("pack".equals(id))showGamingIconPacks();else if("visible".equals(id))showGamingAppVisibility();else showGamingIconOption(id);},this::libraryMenu);
 }
 private void showGamingIconOption(String id){
  AppIconSettingsPolicy.Option option=AppIconSettingsPolicy.find(id);if(option==null)return;
  int saved=AppIconSettingsPolicy.read(prefs.getAll(),option);
  AlertDialog dialog=menuBuilder().setTitle(option.title+" · Home + Gaming").setSingleChoiceItems(option.labels,option.selected(saved),(d,index)->{
   android.content.SharedPreferences.Editor edit=prefs.edit();int value=option.values[index];
   if(option==AppIconSettingsPolicy.GLOW||option==AppIconSettingsPolicy.CONTRAST)edit.putBoolean(option.key,value==1);else edit.putInt(option.key,value);
   edit.apply();d.dismiss();showGamingIconSettings();
  }).setNegativeButton("Back",(d,w)->showGamingIconSettings()).create();themeMenu(dialog,this::showGamingIconSettings);
 }
 private void showGamingIconPacks(){
  List<AppIconPackCatalog.Pack> packs=AppIconPackCatalog.discover(this);String[] names=new String[packs.size()];for(int i=0;i<names.length;i++)names[i]=packs.get(i).name;
  AlertDialog dialog=menuBuilder().setTitle("Icon Pack · Home + Gaming").setSingleChoiceItems(names,AppIconPackCatalog.selected(packs,prefs.getString("iconPackPackage","")),(d,index)->{
   prefs.edit().putString("iconPackPackage",packs.get(index).packageName).apply();d.dismiss();showGamingIconSettings();
  }).setNegativeButton("Back",(d,w)->showGamingIconSettings()).create();themeMenu(dialog,this::showGamingIconSettings);
 }
 private void showGamingAppVisibility(){showGamingAppVisibility(this::showGamingIconSettings);}
 private void showGamingAppVisibility(Runnable parent){
  List<String> keys=new ArrayList<>(available.keySet());Collections.sort(keys,(a,b)->available.get(a).compareToIgnoreCase(available.get(b)));
  Set<String> chosen=saved("gamingApps"),hidden=saved("gamingHidden");String[] labels=new String[keys.size()];boolean[] checked=new boolean[keys.size()];
  for(int i=0;i<keys.size();i++){String pkg=keys.get(i);labels[i]=available.get(pkg);checked[i]=!hidden.contains(pkg)&&(game(pkg)||chosen.contains(pkg));}
  AlertDialog dialog=menuBuilder().setTitle("Visible Apps · Gaming only").setMultiChoiceItems(labels,checked,(d,index,on)->checked[index]=on).setPositiveButton("Save",(d,w)->{
   for(int i=0;i<keys.size();i++)if(checked[i]){chosen.add(keys.get(i));hidden.remove(keys.get(i));}else{chosen.remove(keys.get(i));hidden.add(keys.get(i));}
   prefs.edit().putStringSet("gamingApps",chosen).putStringSet("gamingHidden",hidden).apply();refreshGamingIcons(false);if(parent!=null)parent.run();
  }).setNegativeButton("Cancel",(d,w)->{if(parent!=null)parent.run();}).create();themeMenu(dialog,parent);
 }
 private void performanceMenu(){
  gamingMenu("Performance",Arrays.asList(new SettingsChoice("preset","Preset · "+GamingPerformancePolicy.name(prefs.getInt("detoxPerformancePreset",GamingPerformancePolicy.DEFAULT))),new SettingsChoice("motion","Menu motion · "+(prefs.getBoolean("gamingMotion",true)?"On":"Off")),new SettingsChoice("sampling","Frame diagnostics")),id->{
   if("preset".equals(id))performancePresetOptions();else if("motion".equals(id))gamingToggle("Menu motion","gamingMotion",true,this::performanceMenu,()->refreshGamingIcons(false));else showPerformanceSampling();
  },this::libraryMenu);
 }

 private void focusScale(View target,boolean focused,float amount,long duration){target.animate().cancel();if(!motionAllowed()){target.setScaleX(1f);target.setScaleY(1f);return;}float scale=focused?amount:1f;target.animate().scaleX(scale).scaleY(scale).setDuration(motionDuration(duration)).start();}
 private long motionDuration(long base){int mode=GamingPerformancePolicy.normalize(prefs.getInt("detoxPerformancePreset",GamingPerformancePolicy.DEFAULT));float factor=mode==0?.78f:mode==2?1.12f:1f;return Math.max(1L,Math.round(base*factor));}

 private void performancePresetOptions(){
  gamingChoice("Preset · Home + Gaming",GamingPerformancePolicy.NAMES,GamingPerformancePolicy.normalize(prefs.getInt("detoxPerformancePreset",GamingPerformancePolicy.DEFAULT)),preset->{prefs.edit().putInt("detoxPerformancePreset",preset).apply();refreshGamingIcons(false);},this::performanceMenu);
 }
 private void showPerformanceSampling(){
  boolean enabled=prefs.getBoolean("detoxPerformanceProbe",false);AlertDialog dialog=menuBuilder().setTitle("Frame diagnostics").setMessage(DetoxPerformanceProbe.report(this)+"\n\nMeasures Detox UI frames, not video decoder FPS.").setPositiveButton(enabled?"Stop sampling":"Start sampling",(d,w)->{prefs.edit().putBoolean("detoxPerformanceProbe",!enabled).apply();if(performanceProbe!=null){performanceProbe.stop();if(!enabled&&resumed)performanceProbe.start();}showPerformanceSampling();}).setNeutralButton("Refresh",(d,w)->showPerformanceSampling()).setNegativeButton("Back",(d,w)->performanceMenu()).create();themeMenu(dialog,this::performanceMenu);
 }

 private void themeMenu(AlertDialog dialog,Runnable parent){
  SettingsNavigationPolicy.BackGate gate=new SettingsNavigationPolicy.BackGate();
  dialog.setOnKeyListener((d,key,event)->{if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER)&&event.getRepeatCount()>0)return true;if(key!=KeyEvent.KEYCODE_BACK&&key!=KeyEvent.KEYCODE_BUTTON_B)return false;
   if(gate.accept(KeyEvent.KEYCODE_BACK,event.getAction(),event.getRepeatCount())){dialog.dismiss();if(parent!=null)backgroundHandler.post(()->{if(resumed&&!isFinishing())parent.run();});else if(themeButton!=null)themeButton.requestFocus();}return true;});
  dialog.setOnDismissListener(d->CompanionInput.clearDialog(dialog));dialog.show();CompanionInput.dialog(dialog);
 }
 private Runnable themePickerParent;
 private void showThemePicker(){showThemePicker(null);}
 private void showThemePicker(Runnable parent){
  themePickerParent=parent;openThemePicker();
 }
 private void openThemePicker(){
  List<SettingsChoice> rows=new ArrayList<>();rows.add(new SettingsChoice("images","Images · "+BundledWallpaperCatalog.size()));rows.add(new SettingsChoice("live","Live videos · "+BuiltInLiveVideoCatalog.size()));rows.add(new SettingsChoice("gaming","Gaming videos · "+GamingThemeCatalog.NAMES.length));rows.add(new SettingsChoice("home","Same as Home"));
  if(!GamingBackgroundCatalog.SAME_HOME.equals(GamingBackgroundCatalog.savedId(prefs.getAll())))rows.add(new SettingsChoice("framing","Framing · "+(prefs.getBoolean("gamingBackgroundFill",true)?"Fill screen":"Fit whole background")));
  String selectedGroup=GamingBackgroundCatalog.groupOf(GamingBackgroundCatalog.savedId(prefs.getAll()));
  String[] labels=new String[rows.size()];for(int i=0;i<labels.length;i++)labels[i]=(rows.get(i).id.equals(selectedGroup)?"✓ ":"")+rows.get(i).label;
  AlertDialog picker=menuBuilder().setTitle("Gaming Theme").setItems(labels,(d,index)->{
   String id=rows.get(index).id;themeRootSelection=id;d.dismiss();if("home".equals(id))previewTheme(GamingBackgroundCatalog.SAME_HOME,null);else if("framing".equals(id))showGamingFraming();else showThemeChoices(id);
  }).setNegativeButton(themePickerParent==null?"Close":"Back",(d,w)->{if(themePickerParent!=null)themePickerParent.run();else if(themeButton!=null)themeButton.requestFocus();}).create();themeMenu(picker,themePickerParent);
  for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(themeRootSelection)){picker.getListView().setSelection(i);break;}
 }
 private void showThemeChoices(String group){
  GamingBackgroundCatalog.Choice[] choices=GamingBackgroundCatalog.entries(group);String[] names=new String[choices.length];String saved=GamingBackgroundCatalog.savedId(prefs.getAll());int selected=-1;
  for(int i=0;i<names.length;i++){names[i]=choices[i].name;if(choices[i].id.equals(saved)){selected=i;names[i]="✓ "+names[i];}}
  AlertDialog picker=menuBuilder().setTitle(GamingBackgroundCatalog.IMAGES.equals(group)?"Images":GamingBackgroundCatalog.LIVE.equals(group)?"Live videos":"Gaming videos")
   .setSingleChoiceItems(names,selected,(d,index)->{themeBrowseSelections.put(group,choices[index].id);d.dismiss();previewTheme(choices[index].id,group);})
   .setNegativeButton("Back",(d,w)->openThemePicker()).create();themeMenu(picker,this::openThemePicker);
  String browse=themeBrowseSelections.get(group);for(int i=0;i<choices.length;i++)if(choices[i].id.equals(browse)){picker.getListView().setSelection(i);break;}
 }
 private void previewTheme(String id,String group){
  if(themePreview!=null)themePreview.close();
  // A paused MediaPlayer still owns its hardware decoder. Release it before previewing another clip.
  if(videoBackground!=null){videoBackground.active(false);videoBackground.close();}
  themePreview=new GamingThemePreview(this,menuBuilder(),prefs.getAll(),id,new GamingThemePreview.Result(){
   public void applied(String selected){themePreview=null;prefs.edit().putString("gamingBackgroundSource",selected).apply();replaceSelectedBackground();if(themeButton!=null)themeButton.requestFocus();}
   public void cancelled(){themePreview=null;if(resumed){if(group==null)openThemePicker();else showThemeChoices(group);}}
  });
 }
 private void showGamingFraming(){
  AlertDialog dialog=menuBuilder().setTitle("Gaming background framing").setSingleChoiceItems(new String[]{"Fill screen","Fit whole background"},prefs.getBoolean("gamingBackgroundFill",true)?0:1,(d,index)->{
   prefs.edit().putBoolean("gamingBackgroundFill",index==0).apply();d.dismiss();replaceSelectedBackground();openThemePicker();
  }).setNegativeButton("Back",(d,w)->openThemePicker()).create();themeMenu(dialog,this::openThemePicker);
 }
 private void gameOptions(String pkg){
  Set<String> favorites=saved("gamingFavorites");
  gamingMenu(available.get(pkg),Arrays.asList(new SettingsChoice("launch","Play"),new SettingsChoice("favorite",favorites.contains(pkg)?"Remove favorite":"Add favorite"),new SettingsChoice("front","Pin to front"),new SettingsChoice("hide","Hide from library"),new SettingsChoice("info","Android app info"),new SettingsChoice("uninstall","Uninstall app")),id->{
   switch(id){
    case "launch":launch(pkg);break;
    case "favorite":if(!favorites.add(pkg))favorites.remove(pkg);prefs.edit().putStringSet("gamingFavorites",favorites).apply();refreshGamingIcons(false);break;
    case "front":int first=0;for(String key:available.keySet())first=Math.min(first,prefs.getInt("gamingOrder_"+key,0));prefs.edit().putInt("gamingOrder_"+pkg,first-1).putInt("gamingSortOrder",0).apply();refreshGamingIcons(false);break;
    case "hide":Set<String> apps=saved("gamingApps"),hide=saved("gamingHidden");apps.remove(pkg);hide.add(pkg);prefs.edit().putStringSet("gamingApps",apps).putStringSet("gamingHidden",hide).apply();refreshGamingIcons(false);break;
    case "info":case "uninstall":try{startActivity(new Intent("info".equals(id)?android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS:Intent.ACTION_UNINSTALL_PACKAGE,android.net.Uri.parse("package:"+pkg)));}catch(Exception error){Toast.makeText(this,"Android could not open this action",Toast.LENGTH_SHORT).show();}break;
   }
  },()->{if(library!=null)for(int i=0;i<library.getChildCount();i++)if(pkg.equals(library.getChildAt(i).getTag())){library.getChildAt(i).requestFocus();break;}});
 }

 // Readability is an overlay above user-provided videos, never a modification to the video itself.
 private void applyGamingReadability(){
  if(gamingReadabilityScrim==null)return;
  int level=GamingReadabilityPolicy.clamp(prefs.getInt("gamingBackgroundReadability",0));
  gamingReadabilityScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,GamingReadabilityPolicy.gradient(level)));
  gamingReadabilityScrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
 }
 private void backgroundReadabilityOptions(){gamingChoice("Background contrast",GamingReadabilityPolicy.LABELS,GamingReadabilityPolicy.clamp(prefs.getInt("gamingBackgroundReadability",0)),index->{prefs.edit().putInt("gamingBackgroundReadability",index).apply();applyGamingReadability();},this::gamingBackgroundSettings);}

 private boolean motionAllowed(){return prefs.getBoolean("gamingMotion",true)&&AppIconSettingsPolicy.motionMode(prefs.contains("launcherMotionPreset"),prefs.getInt("launcherMotionPreset",0),prefs.getInt("launcherAnimationMode",0))!=2&&(Build.VERSION.SDK_INT<26||android.animation.ValueAnimator.areAnimatorsEnabled());}

 @Override public boolean dispatchKeyEvent(KeyEvent event){
  if(event.getKeyCode()==KeyEvent.KEYCODE_SETTINGS){
   if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)libraryMenu();
   return true;
  }
  return super.dispatchKeyEvent(event);
 }
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)configureGamingWindow();if(videoBackground!=null)videoBackground.active(focus&&resumed&&themePreview==null);}

}

