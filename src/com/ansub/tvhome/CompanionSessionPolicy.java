package com.ansub.tvhome;

/** Queued UI callbacks and staged uploads belong to one specific pairing session. */
final class CompanionSessionPolicy {
 private static final java.util.regex.Pattern STAGING=java.util.regex.Pattern.compile("phone-companion-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-(wallpaper|video)\\.upload");
 static boolean accepts(Object expected,Object current,boolean finishing,boolean destroyed){return expected!=null&&expected==current&&!finishing&&!destroyed;}
 static String newUploadPrefix(){return "phone-companion-"+java.util.UUID.randomUUID().toString();}
 static boolean stagedUpload(String name){return name!=null&&(name.equals("phone-companion-wallpaper.upload")||name.equals("phone-companion-video.upload")||STAGING.matcher(name).matches());}
 private CompanionSessionPolicy(){}
}
