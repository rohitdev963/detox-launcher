package com.ansub.tvhome;
import java.util.*;

/** Shuffled rounds cover every track before repeating, without a repeat across round boundaries. */
final class GamingMusicQueue {
    private final int count;
    private final Random random;
    private final ArrayList<Integer> bag=new ArrayList<>();
    GamingMusicQueue(int count,Random random){if(count<1)throw new IllegalArgumentException("Empty playlist");this.count=count;this.random=random;}
    int next(int current,boolean shuffle){
        if(!shuffle)return (Math.max(-1,current)+1)%count;
        if(bag.isEmpty()){for(int i=0;i<count;i++)bag.add(i);Collections.shuffle(bag,random);}
        int last=bag.size()-1;
        if(bag.get(last)==current&&last>0)Collections.swap(bag,last,0);
        return bag.remove(last);
    }
    void reset(){bag.clear();}
    boolean needsCrossfadePolling(){return count>1;}
}
