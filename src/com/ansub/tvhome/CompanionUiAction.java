package com.ansub.tvhome;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** A queued remote action expires before execution, even when the UI thread is busy. */
final class CompanionUiAction implements Runnable {
 interface Action {boolean getAsBoolean();}
 interface Clock {long getAsLong();}
 private final Action action;
 private final Clock clock;
 private final long deadline;
 private final AtomicInteger state=new AtomicInteger(); // queued=0, started=1, complete=2, cancelled=3
 private final CountDownLatch done=new CountDownLatch(1);
 private volatile boolean result;
 private volatile RuntimeException failure;
 CompanionUiAction(Action action,long timeoutMillis){this(action,timeoutMillis,System::nanoTime);}
 CompanionUiAction(Action action,long timeoutMillis,Clock clock){this.action=action;this.clock=clock;deadline=clock.getAsLong()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);}
 @Override public void run(){
  if(clock.getAsLong()-deadline>=0){cancel();return;}
  if(!state.compareAndSet(0,1))return;
  try{result=action.getAsBoolean();}catch(RuntimeException error){failure=error;result=false;}finally{state.set(2);done.countDown();}
 }
 boolean await() throws InterruptedException {
  if(state.get()==2)return result;
  long remaining=deadline-clock.getAsLong();
  if(remaining>0&&done.await(remaining,TimeUnit.NANOSECONDS))return state.get()==2&&result;
  cancel();return false;
 }
 void cancel(){if(state.compareAndSet(0,3))done.countDown();}
 RuntimeException failure(){return failure;}
}
