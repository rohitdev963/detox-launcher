package com.ansub.tvhome;
/** Gaming background choices must match packaged MP4 assets. */
final class GamingThemeCatalog {
    static final String[] NAMES={"Ember Drift"};
    private static final String[] FILES={"ember-drift.mp4"};
    static String source(int index){return FILES.length==0?"":"asset:public/videos/"+FILES[Math.max(0,Math.min(FILES.length-1,index))];}
    static int indexOfSource(String source){for(int i=0;i<FILES.length;i++)if(source(i).equals(source))return i;return -1;}
    static int selected(String saved){
        for(int i=0;i<FILES.length;i++)if(source(i).equals(saved))return i;
        return 0;
    }
    private GamingThemeCatalog(){}
}
