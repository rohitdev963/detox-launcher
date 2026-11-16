package com.ansub.tvhome;
import android.app.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.Map;

/** Isolated live preview; one decoder, muted video, no preference edits until explicit Apply. */
final class GamingThemePreview {
 interface Result {void applied(String id);void cancelled();}
 private final Activity host;private final GamingBackgroundPreviewState state;private final Result result;
 private AlertDialog dialog;private GamingVideoBackground video;private FrameLayout frame;
 private TextView status;private boolean closed,applied,allowReturn=true,ready,failed;
 GamingThemePreview(Activity host,AlertDialog.Builder builder,Map<String,?> prefs,String candidate,Result result){
  this.host=host;this.result=result;state=new GamingBackgroundPreviewState(GamingBackgroundCatalog.savedId(prefs),candidate);
  GamingBackgroundCatalog.Choice choice=GamingBackgroundCatalog.resolve(prefs,candidate);
  float density=host.getResources().getDisplayMetrics().density;
  LinearLayout body=new LinearLayout(host);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(Math.round(12*density),0,Math.round(12*density),Math.round(10*density));
  frame=new FrameLayout(host){@Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(video!=null)video.active(focused&&!closed);}};
  frame.setBackgroundColor(0xFF080B11);frame.setClipChildren(true);
  int width=Math.min(Math.round(490*density),host.getResources().getDisplayMetrics().widthPixels-Math.round(96*density));
  int height=Math.min(width*9/16,Math.round(host.getResources().getDisplayMetrics().heightPixels*.42f));
  body.addView(frame,new LinearLayout.LayoutParams(-1,height));
  status=new TextView(host);status.setText("Loading preview…");status.setTextColor(0xFFF4F6FB);status.setTextSize(15);status.setGravity(Gravity.CENTER);status.setPadding(0,Math.round(8*density),0,0);body.addView(status,new LinearLayout.LayoutParams(-1,-2));
  boolean fill=GamingBackgroundCatalog.fill(prefs,choice);float zoom=GamingBackgroundCatalog.zoom(prefs,choice);
  if(choice.video()){
   video=new GamingVideoBackground(host,choice.source,new GamingVideoBackground.Listener(){
    @Override public void onReady(){loaded();}
    @Override public void onFallback(String from,String to){unavailable();if(video!=null){video.active(false);video.close();}}
    @Override public void onUnavailable(String source){unavailable();}
   });video.framing(fill,zoom);frame.addView(video,new FrameLayout.LayoutParams(-1,-1));
  }else{
   GamingImageBackground image=new GamingImageBackground(host,choice,fill,zoom,GamingBackgroundCatalog.position(prefs,choice,true),GamingBackgroundCatalog.position(prefs,choice,false),new GamingImageBackground.Failure(){public void failed(String source){unavailable();}public void ready(){loaded();}});
   frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
  }
  View readability=new View(host);Object value=prefs.get("gamingBackgroundReadability");int level=value instanceof Number?((Number)value).intValue():0;
  readability.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,GamingReadabilityPolicy.gradient(level)));readability.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);frame.addView(readability,new FrameLayout.LayoutParams(-1,-1));
  dialog=builder.setTitle(choice.name).setView(body).setPositiveButton("Apply",null).setNegativeButton("Cancel",(d,w)->{}).create();
  SettingsNavigationPolicy.BackGate gate=new SettingsNavigationPolicy.BackGate();
  dialog.setOnKeyListener((d,key,event)->{
   if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER)&&event.getRepeatCount()>0)return true;
   if(key!=KeyEvent.KEYCODE_BACK&&key!=KeyEvent.KEYCODE_BUTTON_B)return false;if(gate.accept(KeyEvent.KEYCODE_BACK,event.getAction(),event.getRepeatCount()))dialog.cancel();return true;
  });
  dialog.setOnDismissListener(d->{if(closed)return;closed=true;CompanionInput.clearDialog(dialog);if(video!=null){video.active(false);video.close();video=null;}frame.removeAllViews();
   if(applied)result.applied(state.apply());else{state.cancel();if(allowReturn&&!host.isFinishing()&&!host.isDestroyed())result.cancelled();}});
  dialog.setCanceledOnTouchOutside(false);dialog.show();CompanionInput.dialog(dialog);
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(ready&&!failed&&!closed){applied=true;dialog.dismiss();}});
  DetoxPointerFocus.install(dialog.getButton(AlertDialog.BUTTON_POSITIVE));DetoxPointerFocus.install(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
  dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();if(video!=null)video.active(true);
 }
 private void loaded(){if(closed||failed)return;ready=true;status.setText("Preview only · Apply to Gaming. Home stays unchanged.");if(dialog!=null&&dialog.getButton(AlertDialog.BUTTON_POSITIVE)!=null)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);}
 private void unavailable(){if(closed)return;failed=true;ready=false;status.setText("Preview unavailable. Choose another background.");if(dialog!=null&&dialog.getButton(AlertDialog.BUTTON_POSITIVE)!=null)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);}
 void close(){allowReturn=false;if(dialog!=null&&dialog.isShowing())dialog.dismiss();}
}
