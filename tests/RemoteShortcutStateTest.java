package com.ansub.tvhome;
import java.util.*;
public class RemoteShortcutStateTest {
 static class H implements RemoteShortcutState.Host { List<Integer> fired=new ArrayList<>(); List<Runnable> tasks=new ArrayList<>(); public void fire(int s){fired.add(s);} public void later(Runnable r,long ms){if(ms!=600)throw new AssertionError("hold duration");tasks.add(r);} public void remove(Runnable r){tasks.remove(r);} void tick(){List<Runnable> t=new ArrayList<>(tasks);tasks.clear();for(Runnable r:t)r.run();}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] x){H h=new H();RemoteShortcutState s=new RemoteShortcutState(h);
 s.event(100,0,0,false,2,false);s.event(100,0,1,false,2,false);s.event(100,1,0,false,2,false);check(h.fired.equals(Arrays.asList(2)),"tap only once");
 s.event(101,0,0,false,3,true);check(h.fired.size()==1,"hold not early");h.tick();check(h.fired.equals(Arrays.asList(2,3)),"hold fires");s.event(101,1,0,false,3,true);
 s.event(102,0,0,false,4,true);s.event(102,1,0,false,4,true);h.tick();check(h.fired.size()==2,"released hold cancelled");
 s.event(103,0,0,false,5,true);s.otherKey(104);h.tick();check(h.fired.size()==2,"other key cancels hold");
 s.event(105,0,0,true,6,true);h.tick();check(h.fired.size()==2,"cancelled down");
 s.event(106,0,0,false,7,false);check(h.fired.get(2)==7,"last slot");
 check(RemoteShortcutState.slot(-3)==0&&RemoteShortcutState.slot(99)==7,"slot clamp");
 System.out.println("PASS: 7 remote shortcut regression scenarios");}
}
