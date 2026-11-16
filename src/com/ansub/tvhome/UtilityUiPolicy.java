package com.ansub.tvhome;
final class UtilityUiPolicy {
 static int opacity(int value){return Math.max(30,Math.min(100,value));}
 static final class RailLayout {final int top,step;RailLayout(int top,int step){this.top=top;this.step=step;}}
 static RailLayout rail(int top,int bottom,int button,int count,int gap){
  int available=Math.max(0,bottom-top-button);
  int step=count<=1?0:Math.min(button+Math.max(0,gap),available/(count-1));
  int span=Math.max(0,count-1)*step+button;
  return new RailLayout(top+Math.max(0,(bottom-top-span)/2),step);
 }
 static final class Selection {int category=-1;void open(){category=-1;}void focus(int index){if(category!=index)category=-1;}void click(int index){category=index>=0&&index<SettingsMenuRoutes.categoryCount()?index:-1;}boolean back(){if(category<0)return false;category=-1;return true;}}
}
