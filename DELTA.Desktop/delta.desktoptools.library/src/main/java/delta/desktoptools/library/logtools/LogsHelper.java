package delta.desktoptools.library.logtools;

import delta.desktoptools.library.IOHelpers;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Created by Elia on 16/06/2015.
 */
public class LogsHelper {
    public static List<LogFileEntry> getAllLogEntries(File directory, boolean includeSubdirectories, boolean preloadAllEntries){
        List<File> zipFiles = IOHelpers.getAllFilesInFolderByExtension(directory, "zip", includeSubdirectories);
        List<LogFileEntry> logFileEntries = new LinkedList<>();

        for(File file : zipFiles){
            try {
                ZipFile zipFile = new ZipFile(file);
                if(zipFile.getEntry("metadata") != null){
                    LogFileEntry logFileEntry =new LogFileEntry(file);
                    if(preloadAllEntries)
                        logFileEntry.reload();
                    logFileEntries.add(logFileEntry);
                }
                zipFile.close();
            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }
        return logFileEntries;
    }


}
