package com.ansub.tvhome;
import java.util.ArrayList;
import java.util.LinkedHashMap;
/** One named catalogue owns labels, dispatch IDs and immediate parents. */
final class SettingsMenuRoutes {
 static final String WALLPAPER="wallpaper", APPEARANCE="appearance", APPS="apps", ANIMATIONS="animations", CLOCK="clockUtilities", SYSTEM="system", PHONE="phone";
 static final String FONTS="appearance.fonts", ICONS="apps.icons", STYLE="appearance.style", CLOCK_GROUP="clockUtilities.clock", UTILITIES="clockUtilities.utilities", PERFORMANCE="system.performance", BACKUP="system.backup", VIDEO="wallpaper.video", EFFECTS="wallpaper.effects", IDLE="wallpaper.idle", IDLE_BEHAVIOR="wallpaper.idle.behavior";
 static final class Entry {
  final String id,label,parent,handler;
  Entry(String id,String label,String parent,String handler){this.id=id;this.label=label;this.parent=parent;this.handler=handler;}
  boolean group(){return handler.isEmpty();}
 }
 private static final LinkedHashMap<String,Entry> ENTRIES=new LinkedHashMap<>();
 private static final String[] CATEGORIES={WALLPAPER,APPEARANCE,APPS,ANIMATIONS,CLOCK,SYSTEM,PHONE};
 static {
  group(WALLPAPER,"Wallpaper & Ambient","");group(APPEARANCE,"Appearance & Fonts","");group(APPS,"Apps & Home Layout","");group(ANIMATIONS,"Animations & Focus","");group(CLOCK,"Clock & Utilities","");group(SYSTEM,"System & Tools","");group(PHONE,"Phone Companion","");
  action("wallpaper.choose","Choose Wallpaper",WALLPAPER,"showWallpapers");action("wallpaper.slideshow","Photo Slideshow",WALLPAPER,"showWallpaperSlideshowOptions");group(VIDEO,"Video Wallpaper",WALLPAPER);group(EFFECTS,"Wallpaper Effects",WALLPAPER);action("wallpaper.fit","Wallpaper Fit & Position",WALLPAPER,"showWallpaperFraming");group(IDLE,"Idle Wallpaper",WALLPAPER);
  action("video.builtin","Built-in Live Videos",VIDEO,"showBuiltInLiveVideoPicker");action("video.import","Choose or Import a Video",VIDEO,"showVideoWallpaperPicker");action("video.playback","Playback & Framing",VIDEO,"showLiveWallpaperOptions");
  action("effects.style","Motion Style",EFFECTS,"showWallpaperMotionStyle");action("effects.enabled","Wallpaper Motion On/Off",EFFECTS,"toggleWallpaperMotion");
  group(IDLE_BEHAVIOR,"Idle Timeout & Behavior",IDLE);group("idle.settings","Idle Wallpaper Settings",IDLE);action("idle.motion","Idle Motion Style",IDLE,"showIdleMotionStyle");action("idle.preview","Preview Idle Wallpaper",IDLE,"previewIdleWallpaper");
  action("idle.enabled","Fullscreen Wallpaper On/Off",IDLE_BEHAVIOR,"toggleIdleWallpaper");action("idle.timeout","Start After",IDLE_BEHAVIOR,"showIdleTimeout");action("idle.transition","Idle Transition",IDLE_BEHAVIOR,"showIdleTransition");
  action("idle.welcome","Welcome Animation On/Off","idle.settings","toggleIdleWelcome");action("idle.clock","Idle Clock On/Off","idle.settings","toggleIdleClock");
  action("idle.shade","Wallpaper Shading","idle.settings","showIdleShade");action("idle.clockSize","Idle Clock Size","idle.settings","showIdleClockSize");action("idle.clockPosition","Idle Clock Position","idle.settings","showIdleClockPosition");
  action("idle.date","Show Idle Date On/Off","idle.settings","toggleIdleDate");action("idle.alarm","Show Next Alarm On/Off","idle.settings","toggleIdleAlarm");action("idle.hint","Return Hint On/Off","idle.settings","toggleIdleHint");
  group(FONTS,"Fonts & Text",APPEARANCE);group(STYLE,"Themes & Style",APPEARANCE);group(ICONS,"Icons",APPS);
  action("font.family","Font Family",FONTS,"showFontOptions");action("font.size","Font Size",FONTS,"showTextSizeOptions");action("font.color","Font Color",FONTS,"showFontColorOptions");action("font.appNames","App Labels",ICONS,"showAppNameTextOptions");
  action("style.color","Color Theme",STYLE,"showThemeOptions");action("style.homeTheme","Home UI Theme",STYLE,"showHomePremiumThemes");action("style.iconPack","Icon Pack",ICONS,"showIconPackOptions");action("style.header","Home Screen Header",STYLE,"showHomeHeaderOptions");action("style.blur","Glass Blur On/Off",STYLE,"toggleMenuBlur");
  action("apps.grid","App Grid Layout",APPS,"showAppLayoutOptions");action("apps.iconSize","Icon Size",ICONS,"showAppIconSizeOptions");action("apps.manage","Manage Home Apps",APPS,"showManageHomeApps");action("apps.add","Add App to Home",APPS,"showAddHomeApp");action("apps.tiles","Tile Style",ICONS,"showAppTileStyleOptions");action("apps.headings","Row Headings On/Off",APPS,"toggleHomeHeadings");action("apps.down","App Launch on DOWN",APPS,"showHomeDownLaunchOptions");action("apps.media","Home Media Cards On/Off",APPS,"toggleHomeMediaCards");
  action("animation.preset","Interface Animation Preset",ANIMATIONS,"showLauncherAnimationOptions");action("animation.glow","Focus Glow",ANIMATIONS,"showFocusGlowOptions");action("animation.contrast","High-contrast Focus",ANIMATIONS,"showHighContrastFocusOptions");action("animation.artwork","Icon Visibility",ICONS,"showAppArtworkVisibilityOptions");
  group(CLOCK_GROUP,"Clock",CLOCK);group(UTILITIES,"Utilities",CLOCK);
  action("clock.style","Clock Style",CLOCK_GROUP,"showClockOptions");action("clock.color","Clock Color",CLOCK_GROUP,"showClockColorOptions");action("clock.size","Clock Size",CLOCK_GROUP,"showClockSizeOptions");action("clock.position","Clock Position",CLOCK_GROUP,"showClockPositionOptions");action("clock.visibility","Clock Visibility",CLOCK_GROUP,"showClockVisibilityOptions");action("clock.date","Date Display",CLOCK_GROUP,"showClockDateOptions");action("clock.display","Clock Display & Effects",CLOCK_GROUP,"showClockDisplayOptions");action("clock.tools","Clock Utilities",CLOCK_GROUP,"showClockUtilities");action("clock.blend","Wallpaper Clock Blend On/Off",CLOCK_GROUP,"toggleClockBlend");
  action("utility.visibility","Utility Icon Visibility",UTILITIES,"showUtilityVisibilityOptions");action("utility.size","Utility Icon Size",UTILITIES,"showUtilityIconSizeOptions");action("utility.position","Utility Icon Position",UTILITIES,"showUtilitiesPositionOptions");action("utility.text","Utility Label Color",UTILITIES,"showUtilityTextColorOptions");action("utility.opacity","Idle Utility Opacity",UTILITIES,"showUtilityOpacityOptions");action("utility.motion","Utility Icon Animations",UTILITIES,"showUtilityMotionOptions");action("utility.showAll","Show All Utility Icons",UTILITIES,"showAllUtilityIcons");action("utility.quick","Quick Control Shortcuts",UTILITIES,"showQuickControlCustomization");action("utility.reset","Reset Utilities",UTILITIES,"showUtilityResetConfirmation");
  group(PERFORMANCE,"Performance & Diagnostics",SYSTEM);group(BACKUP,"Backup & Restore",SYSTEM);
  action("performance.overview","Diagnostics Report",PERFORMANCE,"showPerformanceOverview");action("performance.preset","Performance Preset",PERFORMANCE,"showPerformancePreset");action("performance.frames","Frame Sampling",PERFORMANCE,"showPerformanceSampling");
  action("backup.transfer","Settings & Media Transfer",BACKUP,"showBackupRestoreOptions");action("backup.profiles","Home Profiles",BACKUP,"showHomeProfiles");action("backup.checkpoint","Save Settings Checkpoint",BACKUP,"saveSettingsCheckpoint");action("backup.undo","Restore Settings Checkpoint",BACKUP,"restoreSettingsCheckpoint");
  action("system.shortcuts","Remote Shortcuts",SYSTEM,"showRemoteShortcutOptions");action("system.sleep","Sleep Timer",SYSTEM,"showSleepTimerOptions");action("system.android","Android TV Settings",SYSTEM,"openTvSettings");action("system.search","Find a Setting",SYSTEM,"showSettingsSearch");action("system.about","About",SYSTEM,"showAbout");
  action("phone.pair","Pair Phone / QR & Remote",PHONE,"showPhoneCompanion");
  for(Entry e:ENTRIES.values())if(!e.parent.isEmpty()&&!ENTRIES.containsKey(e.parent))throw new IllegalStateException("Missing Settings parent: "+e.id);
 }
 private static void group(String id,String label,String parent){add(new Entry(id,label,parent,""));}
 private static void action(String id,String label,String parent,String handler){add(new Entry(id,label,parent,handler));}
 private static void add(Entry e){if(ENTRIES.put(e.id,e)!=null)throw new IllegalStateException("Duplicate Settings action: "+e.id);}
 static Entry find(String id){return ENTRIES.get(id);}
 static Entry[] children(String parent){ArrayList<Entry> rows=new ArrayList<>();for(Entry e:ENTRIES.values())if(e.parent.equals(parent))rows.add(e);return rows.toArray(new Entry[0]);}
 static Entry[] all(){return ENTRIES.values().toArray(new Entry[0]);}
 static String[] categoryNames(){String[] names=new String[CATEGORIES.length];for(int i=0;i<names.length;i++)names[i]=find(CATEGORIES[i]).label;return names;}
 static int categoryCount(){return CATEGORIES.length;}
 static String categoryId(int index){return index>=0&&index<CATEGORIES.length?CATEGORIES[index]:null;}
 static int categoryIndex(String id){for(int i=0;i<CATEGORIES.length;i++)if(CATEGORIES[i].equals(id))return i;return -1;}
 static String categoryOf(String id){Entry e=find(id);for(int i=0;e!=null&&i<ENTRIES.size();i++){if(e.parent.isEmpty())return e.id;e=find(e.parent);}return null;}
 static int count(int category){String id=categoryId(category);return id==null?0:children(id).length;}
 static String actionAt(int category,int index){String id=categoryId(category);if(id==null)return null;Entry[] rows=children(id);return index>=0&&index<rows.length?rows[index].id:null;}
 private SettingsMenuRoutes(){}
}
