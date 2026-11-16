package com.ansub.tvhome;
/** Key-edge and hold state shared with the Home dispatcher; no Android dependency. */
final class RemoteShortcutState {
 static final int SLOTS=8;interface Host{void fire(int slot);void later(Runnable task,long delay);void remove(Runnable task);}
 private final Host host;private int key=-1,slot=-1;private Runnable pending;private long generation;
 RemoteShortcutState(Host host){this.host=host;}
 static int slot(int value){return Math.max(0,Math.min(SLOTS-1,value));}
 boolean event(int code,int action,int repeat,boolean cancelled,int selected,boolean hold){if(action==0){if(repeat>0||key==code)return true;if(cancelled){cancel();return true;}cancel();key=code;slot=slot(selected);if(hold){long token=generation;int target=slot;pending=()->{if(generation!=token||key!=code)return;pending=null;host.fire(target);};host.later(pending,600);}else{int target=slot;host.fire(target);}}else if(action==1&&key==code)cancel();return true;}
 void otherKey(int code){if(key>=0&&key!=code)cancel();}
 void cancel(){generation++;if(pending!=null)host.remove(pending);pending=null;key=-1;slot=-1;}
}
