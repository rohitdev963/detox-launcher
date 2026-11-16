package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import android.os.*;
import java.util.Random;

/** Two locally decoded tracks crossfade. No service, downloads, or playback outside Gaming. */
final class GamingMusicPlayer implements AudioManager.OnAudioFocusChangeListener {
    private final Context context;
    private final SharedPreferences prefs;
    private final AudioManager audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final GamingMusicQueue queue=new GamingMusicQueue(Math.max(1,GamingMusicCatalog.NAMES.length),new Random());
    private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private AudioFocusRequest focusRequest;
    private MediaPlayer current,next;
    private int currentIndex,nextIndex,resumePosition,failures;
    private boolean foreground,focused,currentReady,nextReady,fading,skip,ended;
    private long fadeStarted,fadeLength;
    private float launchGain=1f;
    private Runnable launchFade;

    GamingMusicPlayer(Context context){
        this.context=context.getApplicationContext();prefs=context.getSharedPreferences("config",0);
        audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        currentIndex=Math.max(0,Math.min(GamingMusicCatalog.NAMES.length-1,prefs.getInt("gamingMusicTrack",0)));
        resumePosition=Math.max(0,prefs.getInt("gamingMusicPosition",0));
    }
    String title(){return GamingMusicCatalog.available()?GamingMusicCatalog.NAMES[currentIndex]:"No bundled music";}
    void active(boolean value){
        foreground=value;
        if(!GamingMusicCatalog.available()||!value||!prefs.getBoolean("gamingMusicEnabled",true)){stop(true);return;}
        if(!focused){
            if(audio==null)return;
            int result;
            if(Build.VERSION.SDK_INT>=26){
                focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
                        .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(this,handler).build();
                result=audio.requestAudioFocus(focusRequest);
            }else result=audio.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            focused=result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        if(focused&&current==null)openCurrent();
    }
    private boolean playingAllowed(){return GamingMusicCatalog.available()&&foreground&&focused&&prefs.getBoolean("gamingMusicEnabled",true);}
    private float level(){return Math.max(0,Math.min(70,prefs.getInt("gamingMusicVolume",25)))/100f;}
    private int fadeMillis(){return Math.max(1,Math.min(5,prefs.getInt("gamingMusicFadeSeconds",3)))*1000;}
    private MediaPlayer prepare(int index,boolean incoming){
        MediaPlayer player=new MediaPlayer();player.setAudioAttributes(attributes);player.setVolume(0,0);
        if(!incoming&&GamingMusicCatalog.NAMES.length==1)player.setLooping(true);
        player.setOnPreparedListener(p->{
            if(p!=current&&p!=next)return;
            failures=0;
            if(p==current){
                currentReady=true;
                if(resumePosition>0&&resumePosition<p.getDuration()-500)p.seekTo(resumePosition);
                resumePosition=0;
                if(playingAllowed()){p.start();setVolume(p,level());prepareNext();scheduleTick();}
            }else nextReady=true;
        });
        player.setOnCompletionListener(p->{
            if(p==current){ended=true;if(nextReady)promote();else if(next==null)prepareNext();}
        });
        player.setOnErrorListener((p,what,extra)->{if(p==current||p==next)failed(p,new IllegalStateException("Decoder "+what+"/"+extra));return true;});
        // Assign ownership before prepareAsync; stale callbacks are ignored after release.
        if(incoming)next=player;else current=player;
        try(AssetFileDescriptor file=context.getAssets().openFd(GamingMusicCatalog.asset(index))){
            player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());player.prepareAsync();
        }catch(Exception error){handler.post(()->{if(player==current||player==next)failed(player,error);});}
        return player;
    }
    private void openCurrent(){
        if(!playingAllowed())return;currentReady=false;ended=false;fading=false;skip=false;
        prepare(currentIndex,false);
    }
    private void prepareNext(){
        if(GamingMusicCatalog.NAMES.length<2||next!=null||!playingAllowed())return;
        nextIndex=queue.next(currentIndex,prefs.getBoolean("gamingMusicShuffle",true));
        nextReady=false;prepare(nextIndex,true);
    }
    void next(){
        if(!GamingMusicCatalog.available()||!prefs.getBoolean("gamingMusicEnabled",true))return;
        if(GamingMusicCatalog.NAMES.length==1){
            if(currentReady&&playingAllowed())try{current.seekTo(0);if(!current.isPlaying())current.start();resumePosition=0;}
            catch(IllegalStateException ignored){}
            return;
        }
        active(foreground);skip=true;if(next==null&&currentReady)prepareNext();scheduleTick();
    }
    void select(int index){
        if(index<0||index>=GamingMusicCatalog.NAMES.length)return;
        if(index==currentIndex&&current!=null)return;
        queue.reset();
        if(current==null){currentIndex=index;resumePosition=0;prefs.edit().putInt("gamingMusicTrack",index).putInt("gamingMusicPosition",0).apply();active(foreground);return;}
        release(next);next=null;nextReady=false;
        // A new selection starts its own transition from the currently audible track.
        fading=false;setVolume(current,level());nextIndex=index;skip=true;
        prepare(index,true);scheduleTick();
    }
    void settingsChanged(){
        if(!prefs.getBoolean("gamingMusicEnabled",true)){stop(true);return;}
        active(foreground);if(!fading)setVolume(current,level());
    }
    void shuffleChanged(){queue.reset();if(!fading){release(next);next=null;nextReady=false;prepareNext();}}
    private void scheduleTick(){handler.removeCallbacks(tick);if(playingAllowed()&&queue.needsCrossfadePolling())handler.post(tick);}
    private final Runnable tick=new Runnable(){
        @Override public void run(){
            if(!playingAllowed()||current==null)return;
            try{
                if(currentReady){
                    if(ended&&nextReady){promote();}
                    else if(!fading&&nextReady){
                        int remaining=Math.max(0,current.getDuration()-current.getCurrentPosition());
                        int desired=Math.min(fadeMillis(),Math.max(100,current.getDuration()/3));
                        if(skip||remaining<=desired){
                            fading=true;skip=false;fadeStarted=SystemClock.uptimeMillis();fadeLength=Math.max(100,Math.min(desired,Math.max(100,remaining)));
                            next.start();
                        }
                    }
                    if(fading){
                        float progress=Math.max(0,Math.min(1,(SystemClock.uptimeMillis()-fadeStarted)/(float)fadeLength));
                        setVolume(current,level()*(1-progress));setVolume(next,level()*progress);
                        if(progress>=1)promote();
                    }
                }
            }catch(IllegalStateException error){failed(current,error);}
            if(playingAllowed()&&current!=null)handler.postDelayed(this,50);
        }
    };
    private void promote(){
        if(next==null||!nextReady)return;
        MediaPlayer old=current;current=next;currentIndex=nextIndex;next=null;
        currentReady=true;nextReady=false;fading=false;ended=false;skip=false;resumePosition=0;
        release(old);setVolume(current,level());
        if(playingAllowed()&&!current.isPlaying())current.start();
        prefs.edit().putInt("gamingMusicTrack",currentIndex).putInt("gamingMusicPosition",0).apply();prepareNext();
    }
    private void failed(MediaPlayer player,Exception error){
        DetoxDiagnostics.recordFailure("DetoxGamingMusic","Music track",GamingMusicCatalog.NAMES[player==next?nextIndex:currentIndex],error);
        if(player==next){release(next);next=null;nextReady=false;fading=false;setVolume(current,level());}
        else{release(current);current=null;currentReady=false;release(next);next=null;nextReady=false;currentIndex=queue.next(currentIndex,true);resumePosition=0;}
        if(++failures>=GamingMusicCatalog.NAMES.length){stop(true);return;}
        handler.postDelayed(()->{if(!playingAllowed())return;if(current==null)openCurrent();else prepareNext();},250);
    }
    private void setVolume(MediaPlayer player,float volume){if(player!=null)try{player.setVolume(volume*launchGain,volume*launchGain);}catch(IllegalStateException ignored){}}
    void fadeForLaunch(long duration){
        cancelLaunchFade();
        if(duration<=0){active(false);return;}
        long started=SystemClock.uptimeMillis();
        launchFade=new Runnable(){public void run(){
            float progress=Math.min(1,(SystemClock.uptimeMillis()-started)/(float)duration);
            launchGain=1-progress;if(!fading)setVolume(current,level());
            if(progress>=1){active(false);return;}
            handler.postDelayed(this,25);
        }};handler.post(launchFade);
    }
    void cancelLaunchFade(){
        if(launchFade!=null)handler.removeCallbacks(launchFade);launchFade=null;launchGain=1;
        if(!fading)setVolume(current,level());
    }
    private static void release(MediaPlayer player){if(player!=null){player.setOnPreparedListener(null);player.setOnCompletionListener(null);player.setOnErrorListener(null);player.release();}}
    private void stop(boolean abandon){
        handler.removeCallbacksAndMessages(null);
        launchFade=null;launchGain=1;
        if(current!=null&&currentReady)try{resumePosition=current.getCurrentPosition();}catch(IllegalStateException ignored){}
        if(GamingMusicCatalog.available())prefs.edit().putInt("gamingMusicTrack",currentIndex).putInt("gamingMusicPosition",resumePosition).apply();
        MediaPlayer old=current,queued=next;current=null;next=null;release(old);release(queued);
        currentReady=false;nextReady=false;fading=false;skip=false;
        if(abandon&&audio!=null){if(Build.VERSION.SDK_INT>=26&&focusRequest!=null)audio.abandonAudioFocusRequest(focusRequest);else audio.abandonAudioFocus(this);focusRequest=null;focused=false;}
    }
    @Override public void onAudioFocusChange(int change){
        if(change==AudioManager.AUDIOFOCUS_GAIN){focused=true;if(playingAllowed()&&current==null)openCurrent();}
        else if(change<0){focused=false;stop(change==AudioManager.AUDIOFOCUS_LOSS);}
    }
    void close(){foreground=false;stop(true);}
}
