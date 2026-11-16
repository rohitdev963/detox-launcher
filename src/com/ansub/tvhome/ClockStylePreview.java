package com.ansub.tvhome;

import android.app.*;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;


/** One live renderer, not 17 running thumbnails. The Home preference is untouched until Save. */
final class ClockStylePreview {
 interface Commit { void accept(String id); }
 private interface Select { void accept(int index); }
 static void show(Activity host,AlertDialog.Builder builder,String saved,int color,Typeface font,int motion,int preset,Commit commit,Runnable parent){
  ClockPreviewState state=new ClockPreviewState(saved);
  float density=host.getResources().getDisplayMetrics().density;
  int screenHeight=host.getResources().getDisplayMetrics().heightPixels;
  LinearLayout body=new LinearLayout(host);body.setOrientation(LinearLayout.HORIZONTAL);body.setPadding(Math.round(12*density),Math.round(8*density),Math.round(12*density),Math.round(8*density));
  ListView list=new ListView(host);list.setId(View.generateViewId());list.setDivider(null);list.setVerticalScrollBarEnabled(false);list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
  String[] labels=ClockFacePolicy.NAMES.clone();int initial=0;
  for(int i=0;i<labels.length;i++)if(ClockFacePolicy.KEYS[i].equals(state.saved)){labels[i]="✓ "+labels[i];initial=i;}
  list.setAdapter(new ArrayAdapter<String>(host,android.R.layout.simple_list_item_1,labels){
   @Override public View getView(int pos,View old,ViewGroup container){
    TextView row=(TextView)super.getView(pos,old,container);MenuTypography.set(row,false,18*host.getResources().getDisplayMetrics().scaledDensity);row.setMinHeight(Math.round(56*density));row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);
    row.setOnHoverListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_HOVER_ENTER){list.setSelection(pos);}return false;});return row;
   }
  });
  android.graphics.drawable.GradientDrawable selected=new android.graphics.drawable.GradientDrawable();selected.setColor(0x426FDCEB);selected.setCornerRadius(8*density);list.setSelector(selected);
  body.addView(list,new LinearLayout.LayoutParams(0,-1,.46f));
  LinearLayout preview=new LinearLayout(host);preview.setOrientation(LinearLayout.VERTICAL);preview.setGravity(Gravity.CENTER);
  TextView name=new TextView(host);name.setTextColor(0xFFF4F5FA);name.setTextSize(20);name.setGravity(Gravity.CENTER);preview.addView(name,new LinearLayout.LayoutParams(-1,-2));
  NativeClockFaceView face=new NativeClockFaceView(host);preview.addView(face,new LinearLayout.LayoutParams(-1,0,1));
  TextView hint=new TextView(host);hint.setTextColor(0xFFD4DFEB);hint.setTextSize(13);hint.setGravity(Gravity.CENTER);hint.setText("Live preview · Save to apply\n✓ marks your saved clock");preview.addView(hint,new LinearLayout.LayoutParams(-1,-2));
  body.addView(preview,new LinearLayout.LayoutParams(0,-1,.54f));
  final boolean[] returnParent={false},save={false};
  AlertDialog dialog=builder.setTitle("Clock Style").setView(body)
   .setPositiveButton("Save",(d,w)->{
    // Read the final row directly: a rapid Right/OK may precede ListView's deferred selection callback.
    int selectedIndex=list.getSelectedItemPosition();
    if(selectedIndex>=0&&selectedIndex<ClockFacePolicy.KEYS.length)state.select(ClockFacePolicy.KEYS[selectedIndex]);
    save[0]=true;
   })
   .setNegativeButton("Cancel",(d,w)->{returnParent[0]=true;}).create();
  Select select=index->{if(index<0||index>=ClockFacePolicy.KEYS.length)return;String key=ClockFacePolicy.KEYS[index];state.select(key);
   name.setText(ClockFacePolicy.NAMES[index]+(state.saved.equals(key)?" · Selected":""));face.configure(key,color,font,motion,preset);face.setActive(true);
   hint.setText("premium_weather".equals(key)?"Live preview · Time-only until weather is supported":"premium_progress".equals(key)?"Ring: elapsed minutes in this hour":"premium_neon".equals(key)?"Decorative ambient bars · Save to apply":"Live preview · Save to apply\n✓ marks your saved clock");};
  list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){select.accept(i);}public void onNothingSelected(AdapterView<?> p){}});
  list.setOnItemClickListener((p,v,i,id)->{select.accept(i);dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();});
  dialog.setOnDismissListener(d->{face.setActive(false);if(save[0])commit.accept(state.save());else{state.cancel();if(returnParent[0]&&!host.isFinishing()&&!host.isDestroyed())parent.run();}});
  dialog.show();
  int width=Math.min(Math.round(840*density),host.getResources().getDisplayMetrics().widthPixels-Math.round(48*density));
  ViewGroup.LayoutParams bodySize=body.getLayoutParams();bodySize.height=Math.min(Math.round(330*density),Math.round(screenHeight*.57f));body.setLayoutParams(bodySize);
  if(dialog.getWindow()!=null)dialog.getWindow().setLayout(width,-2);
  MenuNavigationPolicy pace=new MenuNavigationPolicy();
  list.setOnKeyListener((v,key,event)->{
   if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&event.getAction()==KeyEvent.ACTION_DOWN){dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();return true;}
   if(key!=KeyEvent.KEYCODE_DPAD_UP&&key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
   if(event.getAction()==KeyEvent.ACTION_UP){pace.release();return true;}
   if(event.getAction()==KeyEvent.ACTION_DOWN&&pace.move(key,event.getRepeatCount()==0,SystemClock.uptimeMillis(),event.getDownTime())){
    int i=Math.max(0,list.getSelectedItemPosition());list.setSelection(Math.max(0,Math.min(labels.length-1,i+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1))));}return true;
  });
  for(int button:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE})dialog.getButton(button).setOnKeyListener((v,key,event)->{
   if(key==KeyEvent.KEYCODE_DPAD_UP&&event.getAction()==KeyEvent.ACTION_DOWN){list.requestFocus();return true;}return false;});
  list.requestFocus();list.setSelection(initial);select.accept(initial);
 }
}
