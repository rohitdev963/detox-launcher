package com.ansub.tvhome;
import java.nio.file.*;
import java.util.*;

public final class CompanionSessionIsolationTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  Object old=new Object(),current=new Object();
  check(CompanionSessionPolicy.accepts(current,current,false,false),"current callback rejected");
  check(!CompanionSessionPolicy.accepts(old,current,false,false),"old pairing callback must not close new dialog or apply a file");
  check(!CompanionSessionPolicy.accepts(current,null,false,false),"stopped session callback accepted");
  check(!CompanionSessionPolicy.accepts(null,null,false,false),"missing session accepted");
  check(!CompanionSessionPolicy.accepts(current,current,true,false)&&!CompanionSessionPolicy.accepts(current,current,false,true),"closing/destroyed host callback accepted");
  Set<String> prefixes=new HashSet<>();for(int i=0;i<200;i++){String prefix=CompanionSessionPolicy.newUploadPrefix();check(prefixes.add(prefix),"staging identity collision");for(String kind:new String[]{"wallpaper","video"})check(CompanionSessionPolicy.stagedUpload(prefix+"-"+kind+".upload"),"own private cache namespace not recognized");}
  check(CompanionSessionPolicy.stagedUpload("phone-companion-wallpaper.upload")&&CompanionSessionPolicy.stagedUpload("phone-companion-video.upload"),"legacy private staging cleanup compatibility");
  for(String other:new String[]{null,"wallpaper.jpg","movie.mp4","phone-companion-not-a-session-video.upload","phone-companion-wallpaper.upload.zip","../phone-companion-video.upload","phone-companion-user-notes.upload"})check(!CompanionSessionPolicy.stagedUpload(other),"unrelated file classified as private upload "+other);
  Path source=Path.of(args[0]),root=source.getParent().getParent().getParent().getParent().getParent(),parent=root.resolve("build/change-review");Files.createDirectories(parent);Path folder=Files.createTempDirectory(parent,"session-isolation-");
  Path oldFile=folder.resolve(CompanionSessionPolicy.newUploadPrefix()+"-video.upload"),newFile=folder.resolve(CompanionSessionPolicy.newUploadPrefix()+"-video.upload"),userFile=folder.resolve("user-wallpaper.mp4");
  try{Files.writeString(oldFile,"old session");Files.writeString(newFile,"new upload");Files.writeString(userFile,"user media");Files.delete(oldFile);check(Files.readString(newFile).equals("new upload")&&Files.readString(userFile).equals("user media"),"old session cleanup changed active/unrelated files");}
  finally{Files.deleteIfExists(oldFile);Files.deleteIfExists(newFile);Files.deleteIfExists(userFile);Files.delete(folder);}
  String main=Files.readString(source),server=Files.readString(source.getParent().resolve("PhoneCompanionServer.java"));
  for(String callback:new String[]{"onPaired()","onSessionStopped()","onWallpaperReceived(File image)","onVideoReceived(File video)"}){
   int a=main.indexOf("@Override public void "+callback),b=main.indexOf("\n                }",a);check(a>=0&&b>a&&main.substring(a,b).contains("isCurrentPhoneSession(serverRef[0])"),"unguarded delayed callback "+callback);
  }
  check(server.contains("String stagingPrefix=CompanionSessionPolicy.newUploadPrefix()")&&server.contains("stagingPrefix+\"-wallpaper.upload\"")&&server.contains("stagingPrefix+\"-video.upload\""),"server still shares staging files");
  check(server.indexOf("socket.bind(")<server.indexOf("File[] staleUploads="),"cleanup must occur only after exclusive listener bind succeeds");
  System.out.println("Companion session isolation: callback ordering/host teardown, 200 staging identities, scoped legacy cleanup, actual file isolation and server integration passed (host checks)");
 }
}
