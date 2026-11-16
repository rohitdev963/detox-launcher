package com.ansub.tvhome;
import java.io.File;
import java.nio.file.Files;
public final class RetiredPluginFilesTest {
 public static void main(String[] args)throws Exception{
  File sandbox=Files.createTempDirectory(new File("build").toPath(),"plugin-retirement-test-").toFile();
  File files=new File(sandbox,"files"),cache=new File(sandbox,"cache");files.mkdirs();cache.mkdirs();
  File plugin=new File(files,"detox_extensions/.rollback-demo/assets");plugin.mkdirs();new File(plugin,"wallpaper.jpg").createNewFile();
  File media=new File(files,"wallpaper.jpg"),userArchive=new File(cache,"user.detoxpack"),part=new File(cache,"detox_123.detoxpack");
  media.createNewFile();userArchive.createNewFile();part.createNewFile();
  RetiredPluginFiles.remove(files,cache);RetiredPluginFiles.remove(files,cache);
  if(new File(files,"detox_extensions").exists()||part.exists()||!media.exists()||!userArchive.exists())throw new AssertionError("cleanup boundaries");
  media.delete();userArchive.delete();files.delete();cache.delete();sandbox.delete();
  System.out.println("Retired plugins: exact private folder/cache removed, user media/archives preserved, repeat safe");
 }
}
