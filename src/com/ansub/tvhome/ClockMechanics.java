package com.ansub.tvhome;

/** Pure time geometry and English word layout; no Android or background worker. */
final class ClockMechanics {
 static final String[] GRID={"ITLISASTIME","ACQUARTERDC","TWENTYFIVEX","HALFBTENFTO","PASTERUNINE","ONESIXTHREE","FOURFIVETWO","EIGHTELEVEN","SEVENTWELVE","TENSEOCLOCK"};
 static final String[] HOUR_WORDS={"ONE","TWO","THREE","FOUR","FIVE","SIX","SEVEN","EIGHT","NINE","TEN","ELEVEN","TWELVE"};
 private static final int[][] HOURS={{5,0,3},{6,8,3},{5,6,5},{6,0,4},{6,4,4},{5,3,3},{8,0,5},{7,0,5},{4,7,4},{9,0,3},{7,5,6},{8,5,6}};
 static float hourAngle(int h,int m,float seconds){return (h%12)*30f+m*.5f+seconds/120f;}
 static float minuteAngle(int m,float seconds){return m*6f+seconds*.1f;}
 static float secondAngle(int seconds,int millis){return seconds*6f+millis*.006f;}
 static long nextFrameTime(long previous,long frameTime,long interval){return GamingPerformancePolicy.nextFrameDeadline(previous,frameTime,interval);}
 static long nextDelay(String face,long now,boolean reduced,int frameMillis){
  if("words".equals(face))return 60000L-Math.floorMod(now,60000L);
  return reduced?1000L-Math.floorMod(now,1000L):Math.max(1,frameMillis);
 }
 static final class Words {
  final boolean[] lit=new boolean[110];final int remainder;String phrase;
  Words(int m){remainder=m%5;}
  void mark(int r,int c,int n){for(int i=0;i<n;i++)lit[r*11+c+i]=true;}
 }
 static Words words(int h,int m){
  Words w=new Words(m);w.mark(0,0,2);w.mark(0,3,2);
  int bucket=m/5;int amount=bucket>6?12-bucket:bucket;
  String number="";
  if(amount==1||amount==5){w.mark(2,6,4);number="FIVE";}
  if(amount==2){w.mark(3,5,3);number="TEN";}
  if(amount==3){w.mark(1,2,7);number="QUARTER";}
  if(amount==4||amount==5){w.mark(2,0,6);number="TWENTY"+(amount==5?" FIVE":"");}
  if(amount==6){w.mark(3,0,4);number="HALF";}
  if(bucket>6)w.mark(3,9,2);else if(bucket>0)w.mark(4,0,4);else w.mark(9,5,6);
  int hour=Math.floorMod(h+(bucket>6?1:0)-1,12);int[] token=HOURS[hour];w.mark(token[0],token[1],token[2]);
  w.phrase="IT IS "+(bucket==0?"":number+(bucket>6?" TO ":" PAST "))+HOUR_WORDS[hour]+(bucket==0?" O'CLOCK":"");
  return w;
 }
}
