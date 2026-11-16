"""Exercise production CompanionInput with deterministic Android stand-ins, not a TV input stack."""
from pathlib import Path
import argparse,subprocess
p=argparse.ArgumentParser();p.add_argument('--java-home',type=Path,required=True);args=p.parse_args()
root=Path(__file__).resolve().parents[1];out=root/'build/companion-input-runtime';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
sources={
'android/os/SystemClock.java':'package android.os;public class SystemClock{public static long uptimeMillis(){return 100;}}',
'android/graphics/Rect.java':'package android.graphics;public class Rect{public Rect(int a,int b,int c,int d){}}',
'android/view/KeyEvent.java':'package android.view;public class KeyEvent{public static final int ACTION_DOWN=0,ACTION_UP=1,KEYCODE_DPAD_UP=19,KEYCODE_DPAD_DOWN=20,KEYCODE_DPAD_LEFT=21,KEYCODE_DPAD_RIGHT=22,KEYCODE_DPAD_CENTER=23,KEYCODE_BACK=4;public int action;public KeyEvent(long a,long b,int c,int d,int e){action=c;}}',
'android/view/View.java':'''package android.view;public class View{public static final int FOCUS_UP=33,FOCUS_DOWN=130,FOCUS_LEFT=17,FOCUS_RIGHT=66;public ViewGroup root;public View next;public boolean touch,window=true;public int requests,scrolls;public boolean hasWindowFocus(){return window;}public View findFocus(){return this instanceof ViewGroup?((ViewGroup)this).focused:null;}public View focusSearch(int direction){return next;}public boolean isInTouchMode(){return touch;}public boolean requestFocusFromTouch(){touch=false;return requestFocus(0);}public boolean requestFocus(int d){requests++;if(root!=null)root.focused=this;return true;}public boolean requestRectangleOnScreen(android.graphics.Rect r,boolean immediate){scrolls++;return true;}public int getWidth(){return 100;}public int getHeight(){return 50;}}''',
'android/view/ViewGroup.java':'package android.view;public class ViewGroup extends View{public View focused,first;}',
'android/view/FocusFinder.java':'package android.view;public class FocusFinder{public static FocusFinder getInstance(){return new FocusFinder();}public View findNextFocus(ViewGroup r,View v,int d){return r.first;}}',
'android/view/Window.java':'package android.view;public class Window{public ViewGroup root=new ViewGroup();public View getDecorView(){return root;}}',
'android/app/Activity.java':'''package android.app;public class Activity{public android.view.Window window=new android.view.Window();public boolean finishing,consumeDown,consumeUp;public int downs,ups;public boolean isFinishing(){return finishing;}public android.view.Window getWindow(){return window;}public boolean dispatchKeyEvent(android.view.KeyEvent e){if(e.action==0){downs++;return consumeDown;}ups++;return consumeUp;}}''',
'android/app/Dialog.java':'''package android.app;public class Dialog{public android.view.Window window=new android.view.Window();public boolean showing=true,consumeDown,consumeUp;public int downs,ups;public boolean isShowing(){return showing;}public android.view.Window getWindow(){return window;}public boolean dispatchKeyEvent(android.view.KeyEvent e){if(e.action==0){downs++;return consumeDown;}ups++;return consumeUp;}}''',
'com/ansub/tvhome/CompanionInputRuntimeTest.java':'''package com.ansub.tvhome;import android.app.*;import android.view.*;
public class CompanionInputRuntimeTest{
static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
static View child(ViewGroup root){View v=new View();v.root=root;return v;}
public static void main(String[] args){
Activity a=new Activity();ViewGroup root=a.window.root;View first=child(root),second=child(root);root.focused=first;first.next=second;CompanionInput.resume(a);
check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_RIGHT),"unhandled arrow accepted");check(root.focused==second&&second.requests==1,"arrow performs missing ViewRoot focus fallback");check(a.downs==1&&a.ups==1,"one DOWN/UP pair");check(second.scrolls==1,"new focus scrolls into view");
check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_RIGHT),"focus edge must not report offline");check(second.requests==1,"edge does not refocus repeatedly");
root.focused=first;a.consumeDown=true;check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_DOWN),"custom key handler accepted");check(root.focused==first,"custom navigation never moves twice");a.consumeDown=false;
root.focused=null;root.first=second;second.touch=true;check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_LEFT),"pointer to remote handoff");check(root.focused==second&&!second.touch,"initial focus exits touch mode");
Dialog d=new Dialog();View modalFirst=child(d.window.root),modalSecond=child(d.window.root);d.window.root.focused=modalFirst;modalFirst.next=modalSecond;CompanionInput.dialog(d);int before=a.downs;
check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_DOWN),"modal arrow accepted");check(d.window.root.focused==modalSecond,"focus stays in the modal");check(a.downs==before,"modal keys do not leak to Home");
d.consumeUp=true;check(CompanionInput.key(KeyEvent.KEYCODE_BACK),"modal Back delivered");check(d.ups==2,"modal Back has matching UP");d.showing=false;
root.focused=first;check(CompanionInput.key(KeyEvent.KEYCODE_DPAD_RIGHT),"closed modal ignored");check(root.focused==second,"activity receives later arrows");
CompanionInput.pause(a);check(!CompanionInput.ready()&&!CompanionInput.key(KeyEvent.KEYCODE_DPAD_LEFT),"external apps cannot receive injected input");
CompanionInput.resume(a);a.finishing=true;check(!CompanionInput.key(KeyEvent.KEYCODE_DPAD_LEFT),"finishing activity rejected");a.finishing=false;root.window=false;check(!CompanionInput.key(KeyEvent.KEYCODE_DPAD_LEFT),"unfocused window cannot gain remote focus");
System.out.println("Companion input runtime: "+checks+" assertions passed (Android stand-ins)");}}
'''}
files=[]
for name,data in sources.items():
 path=out/'stubs'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(data,encoding='utf-8');files.append(str(path))
files.append(str(root/'src/com/ansub/tvhome/CompanionInput.java'))
subprocess.run([str(args.java_home/'bin/javac.exe'),'-encoding','UTF8','-d',str(classes),*files],check=True)
subprocess.run([str(args.java_home/'bin/java.exe'),'-cp',str(classes),'com.ansub.tvhome.CompanionInputRuntimeTest'],check=True)
