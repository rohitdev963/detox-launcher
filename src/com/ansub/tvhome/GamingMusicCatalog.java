package com.ansub.tvhome;

final class GamingMusicCatalog {
    static final String[] NAMES={};
    static boolean available(){return NAMES.length>0;}
    static String asset(int index){
        throw new IllegalArgumentException("The public edition has no bundled music");
    }
    private GamingMusicCatalog(){}
}
