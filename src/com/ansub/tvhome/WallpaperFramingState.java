package com.ansub.tvhome;
/** Unsaved framing session. The caller persists it only after explicit confirmation. */
final class WallpaperFramingState {
    float zoom,x,y;
    WallpaperFramingState(float zoom,float x,float y){this.zoom=bound(zoom,1,2);this.x=bound(x,0,1);this.y=bound(y,0,1);}
    private static float bound(float value,float min,float max){return Float.isNaN(value)?min:Math.max(min,Math.min(max,value));}
    void adjust(String action){
        switch(action){
            case "zoomIn":zoom=bound(zoom+.1f,1,2);break;
            case "zoomOut":zoom=bound(zoom-.1f,1,2);break;
            case "left":x=bound(x-.1f,0,1);break;
            case "right":x=bound(x+.1f,0,1);break;
            case "up":y=bound(y-.1f,0,1);break;
            case "down":y=bound(y+.1f,0,1);break;
            case "reset":zoom=1;x=y=.5f;break;
            default:throw new IllegalArgumentException("Unknown framing action: "+action);
        }
    }
}
