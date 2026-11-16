package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
public final class SettingsIntegrationContractTest {
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static String method(String source,String name){Matcher match=Pattern.compile("(?:private )?[^\\n]*\\b"+name+"\\([^\\n]*\\)\\s*\\{").matcher(source);require(match.find(),"method "+name);int a=match.start(),b=source.indexOf("\n    private ",match.end());return source.substring(a,b<0?source.length():b);}
 public static void main(String[] args)throws Exception{
  Path main=Path.of(args[0]);String s=Files.readString(main);
  String dispatch=method(s,"openSettingsAction");Set<String> ids=new HashSet<>();
  Matcher cases=Pattern.compile("case \"([^\"]+)\":").matcher(dispatch);while(cases.find())require(ids.add(cases.group(1)),"duplicate dispatched action");
  int leaves=0;for(SettingsMenuRoutes.Entry e:SettingsMenuRoutes.all())if(!e.group()){
   leaves++;require(ids.remove(e.id),"visible route not dispatched: "+e.id);
   require(Pattern.compile("\\bvoid\\s+"+e.handler+"\\s*\\(").matcher(s).find(),"missing implementation: "+e.handler);
  }
  require(ids.isEmpty(),"stale dispatch: "+ids);
  require(s.contains("entries[choice].id"),"menu must dispatch captured ID");
  require(s.contains("settingsGroupSelections.put(group,entries[choice].id)"),"focus restoration must remember named item");
  String video=method(s,"showLiveWallpaperOptions");int start=video.indexOf("previewVideoCropOverride=choice==0"),end=video.indexOf("}, choice -> {",start);
  require(start>=0&&end>start&&!video.substring(start,end).contains(".edit()"),"video preview must not write saved framing");
  require(method(s,"restorePreviewSettings").contains("previewVideoCropOverride=null"),"video preview cancel restoration");
  require(method(s,"showAppArtworkVisibilityOptions").contains("70, 85, 100"),"default 85% must reopen selected");
  require(!method(s,"showAppLayoutOptions").contains("remove(\"appListSort\")"),"layout picker must preserve independent sorting preference");
  require(!method(s,"showQuickControlCustomization").contains("routeNextDialogBack(this::showQuickControlCustomization)"),"Quick Controls Back loop");
  require(method(s,"detoxAlertBuilder").contains("consumeNextDialogBackAction()"),"standard dialogs must consume parent route");
  require(s.contains("clockUtilities.show(consumeNextDialogBackAction())"),"Clock Utilities parent return");
  require(s.contains("nextPanelHomeDiagram=HomeSettingsPreview.TILES")&&s.contains("nextPanelHomeDiagram=HomeSettingsPreview.HEADER"),"existing Home diagrams preserved after menu relabeling");
  require(!s.contains("int previewKind = title.equals"),"preview behavior must not depend on translated display labels");
  require(s.contains("if(code==KeyEvent.KEYCODE_SETTINGS||code==KeyEvent.KEYCODE_INFO)"),"Settings/Info shortcuts in nested dialogs");
  String workbench=Files.readString(main.getParent().resolve("SettingsWorkbench.java"));
  int click=workbench.indexOf("setOnItemClickListener"),clickEnd=workbench.indexOf("dialog.getWindow()",click);
  require(click>=0&&clickEnd>click&&!workbench.substring(click,clickEnd).contains(".edit()"),"static framing preview must not persist unsaved edits");
  require(workbench.contains("dialog.setOnDismissListener(d->clear.run())"),"framing rollback on any dismissal");
  require(!s.contains("WALLPAPER_SETTINGS")&&!s.contains("TV_DISPLAY_SETTINGS"),"obsolete positional menu tables");
  List<SettingsChoice> rows=new ArrayList<>(Arrays.asList(new SettingsChoice("speed","Playback speed"),new SettingsChoice("crop","Crop imported video")));
  Collections.reverse(rows);require(rows.get(0).id.equals("crop")&&rows.get(0).label.equals("Crop imported video"),"optional/reordered row identity");
  UtilityUiPolicy.Selection selection=new UtilityUiPolicy.Selection();for(int i=0;i<7;i++){selection.click(i);require(selection.category==i,"category inaccessible "+i);require(selection.back(),"Back closes category");require(!selection.back(),"Back exits once");}selection.click(7);require(selection.category==-1,"invalid category");
  System.out.println("Settings integration contracts: "+leaves+" action bindings, preview persistence, reset/layout isolation, Back and reordered contextual rows passed (source/host checks only)");
 }
}
