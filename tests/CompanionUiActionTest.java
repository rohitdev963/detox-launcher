package com.ansub.tvhome;
public final class CompanionUiActionTest {
 public static void main(String[] args)throws Exception{
  long[] now={0};int[] calls={0};
  CompanionUiAction expired=new CompanionUiAction(()->{calls[0]++;return true;},2000,()->now[0]);
  now[0]=2000000000L;expired.run();if(expired.await()||calls[0]!=0)throw new AssertionError("expired command executed");
  CompanionUiAction cancelled=new CompanionUiAction(()->{calls[0]++;return true;},2000,()->now[0]);cancelled.cancel();cancelled.run();
  if(cancelled.await()||calls[0]!=0)throw new AssertionError("cancelled command executed");
  CompanionUiAction valid=new CompanionUiAction(()->{calls[0]++;return true;},2000,()->now[0]);valid.run();valid.run();
  if(!valid.await()||calls[0]!=1)throw new AssertionError("one-shot result");
  CompanionUiAction timeout=new CompanionUiAction(()->{calls[0]++;return true;},0,()->now[0]);if(timeout.await())throw new AssertionError();timeout.run();if(calls[0]!=1)throw new AssertionError("timed out action ran later");
  CompanionUiAction failed=new CompanionUiAction(()->{throw new IllegalStateException("test");},2000,()->now[0]);failed.run();if(failed.await()||failed.failure()==null)throw new AssertionError("failed action escaped or blocked completion");
  now[0]+=3000000000L;if(!valid.await())throw new AssertionError("completed action acknowledgement lost after deadline");
  System.out.println("Companion UI actions: queued expiry, cancellation, timeout and exactly-once delivery passed");
 }
}
