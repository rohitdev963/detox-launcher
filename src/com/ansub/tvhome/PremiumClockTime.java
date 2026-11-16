package com.ansub.tvhome;
import java.text.SimpleDateFormat;
import java.util.*;

/** Locale-aware strings cached at second/minute boundaries, never formatted on every frame. */
final class PremiumClockTime {
 private final Date instant=new Date();
 private SimpleDateFormat time,date,period;
 private long slot=Long.MIN_VALUE;private String zoneId="";private Locale cachedLocale;private boolean cached24,cachedSeconds;
 String digits="",dateText="",meridiem="",previous="";long changedAt;
 private final String[] currentGlyphs=new String[8],oldGlyphs=new String[8];
 void update(long now,TimeZone zone,Locale locale,boolean use24,boolean seconds){
  boolean reset=!zoneId.equals(zone.getID())||!locale.equals(cachedLocale)||cached24!=use24||cachedSeconds!=seconds;
  if(reset){zoneId=zone.getID();cachedLocale=locale;cached24=use24;cachedSeconds=seconds;time=new SimpleDateFormat(use24?(seconds?"HH:mm:ss":"HH:mm"):(seconds?"hh:mm:ss":"hh:mm"),locale);
   date=new SimpleDateFormat("EEE, d MMM",locale);period=new SimpleDateFormat("a",locale);
   time.setTimeZone(zone);date.setTimeZone(zone);period.setTimeZone(zone);slot=Long.MIN_VALUE;}
  long next=Math.floorDiv(now,seconds?1000L:60000L);
  if(next!=slot){slot=next;instant.setTime(now);String value=time.format(instant);
   previous=reset||digits.isEmpty()?value:digits;digits=value;changedAt=now;
   for(int i=0;i<digits.length();i++){currentGlyphs[i]=digits.substring(i,i+1);oldGlyphs[i]=previous.substring(i,i+1);}
   dateText=date.format(instant);meridiem=use24?"":period.format(instant);}
 }
 String glyph(int index,boolean old){return old?oldGlyphs[index]:currentGlyphs[index];}
 boolean digitChanged(int index){return index<digits.length()&&index<previous.length()&&digits.charAt(index)!=previous.charAt(index);}
}
