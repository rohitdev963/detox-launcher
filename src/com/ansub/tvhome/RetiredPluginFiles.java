package com.ansub.tvhome;
import java.io.File;
import java.io.IOException;
/** Deletes only the retired private plugin directory and its exact cache-file pattern. */
final class RetiredPluginFiles {
 static void remove(File privateFiles,File cache)throws IOException{
  File root=new File(privateFiles.getCanonicalFile(),"detox_extensions");
  if(root.exists())removeOwned(root,root.getAbsolutePath());
  File[] staged=cache.listFiles((dir,name)->name.startsWith("detox_")&&name.endsWith(".detoxpack"));
  if(staged!=null)for(File file:staged){
   if(!file.isDirectory()&&file.getCanonicalFile().equals(file.getAbsoluteFile())&&!file.delete()&&file.exists())throw new IOException("Could not delete retired plugin cache");
  }
 }
 private static void removeOwned(File file,String root)throws IOException{
  String absolute=file.getAbsolutePath(),canonical=file.getCanonicalPath();
  if(!absolute.equals(canonical)||!(canonical.equals(root)||canonical.startsWith(root+File.separator)))throw new IOException("Refusing plugin cleanup outside its private directory");
  if(file.isDirectory()){
   File[] children=file.listFiles();if(children==null)throw new IOException("Could not list retired plugins");
   for(File child:children)removeOwned(child,root);
  }
  if(!file.delete()&&file.exists())throw new IOException("Could not remove retired plugin data");
 }
 private RetiredPluginFiles(){}
}
