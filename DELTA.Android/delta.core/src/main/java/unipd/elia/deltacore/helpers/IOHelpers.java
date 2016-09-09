package unipd.elia.deltacore.helpers;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Elia on 15/04/2015.
 */
public class IOHelpers {

    public static String getPrivateStoragePath(){
        return DELTAUtils.context.getFilesDir().getAbsolutePath();
    }

    public static File getPrivateStorageDirectory(){
        return DELTAUtils.context.getFilesDir();
    }

    public static File getExperimentsStoragePath(){
        return DELTAUtils.context.getDir("experiments", Context.MODE_PRIVATE);
    }

    public static String inflateResourceToInternalStorageCache(int resourceID, String outputFileName){
        Context context = DELTAUtils.context;
        try {
            File cachedir = new File(getPrivateStoragePath());
            if(!(cachedir != null && cachedir.exists() && cachedir.isDirectory()))
                cachedir.mkdir();

            InputStream is = context.getResources().openRawResource(resourceID);
            File outputFile = new File(cachedir.getAbsolutePath(), outputFileName);
            OutputStream os = new FileOutputStream(outputFile);

            //copying data
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            //file copied
            os.close();
            is.close();
            return  cachedir + "/" + outputFileName;
        }
        catch (Exception e){
            e.printStackTrace();
            return  null;
        }

    }

    public static void deleteDirectoryRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteDirectoryRecursive(child);

        fileOrDirectory.delete();
    }

    public static boolean setWorldReadable(File file){
        if(file.exists())
            return file.setReadable(true, false);
        else
            return false;
    }

    public static boolean unsetWorldReadable(File file){
        if(file.exists())
            return file.setReadable(false, false);
        else
            return false;
    }

    public static boolean copy(File oldFile, File newFile){
        if(!oldFile.exists())
            return false;

        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(oldFile);
            output = new FileOutputStream(newFile);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
            input.close();
            output.close();
            return true;
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static void unzip(File zipFile, File targetDirectory, String entryName, String targetFileName) {
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));

            ZipEntry ze;
            int count;
            byte[] buffer = new byte[2048];
            while ((ze = zis.getNextEntry()) != null) {
                if(entryName == null || ze.getName().equals(entryName)) {

                    File file = new File(targetDirectory, entryName != null && targetFileName != null ? targetFileName : ze.getName());
                    File dir = ze.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " + dir.getAbsolutePath());
                    if (ze.isDirectory())
                        continue;
                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        fout.close();
                    }
                }
            }

            zis.close();

        }catch(Exception e){

        }
        finally {
            //zis.close();
        }
    }
}
