package delta.desktoptools.library.logtools;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by Elia on 12/06/2015.
 */
public class LogFileEntry implements Comparable<LogFileEntry> {
    public File logFile;

    public String userID;
    public String experimentID;
    public List<String> availableLogs;
    public List<String> availableRawFiles;
    public Map<String, Long> rawFilesStartTimes;
    public Map<String, Long> rawFilesEndTimes;
    public long firstTimestamp;
    public long lastTimestamp;
    public long experimentStartTime;

    private boolean isLoaded = false;

    public LogFileEntry(File logFile){
        this.logFile = logFile;
        //reload();
    }

    public void reload(){
        if(logFile == null || !logFile.exists())
            return;

        isLoaded = loadMetadata();
    }

    public boolean isLoaded(){
        return isLoaded;
    }

    public String getFileName(){
        return logFile.getName();
    }

    public String readEntry(String entryName){
        try{
            ZipFile zipFile = new ZipFile(logFile);
            ZipEntry zipEntry = zipFile.getEntry(entryName);
            InputStream zipInputStream = zipFile.getInputStream(zipEntry);
            return IOUtils.toString(zipInputStream);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public byte[] readRawEntry(String entryName){
        try{
            ZipFile zipFile = new ZipFile(logFile);
            ZipEntry zipEntry = zipFile.getEntry(entryName);
            InputStream zipInputStream = zipFile.getInputStream(zipEntry);
            return IOUtils.toByteArray(zipInputStream);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public long getTimestampFromFilename(File file){
        String name = file.getName();
        long timestamp = Long.parseLong(name.split(Pattern.quote("."))[0]);
        return timestamp;
    }

    private boolean loadMetadata(){
        try {
            ZipFile zipFile = new ZipFile(logFile);
            ZipEntry zipEntry = zipFile.getEntry("metadata");
            String extraString = new String(zipEntry.getExtra());
            String[] pieces = extraString.split(Pattern.quote("$"));
            userID = pieces[0];
            experimentID = pieces[1];
            firstTimestamp = Long.parseLong(pieces[2]);
            lastTimestamp = Long.parseLong(pieces[3]);
            if(pieces.length > 4)
                experimentStartTime = Long.parseLong(pieces[4]);
            else
                experimentStartTime = firstTimestamp;

            availableLogs = new LinkedList<>();
            availableRawFiles = new LinkedList<>();
            rawFilesEndTimes = new HashMap<>();
            rawFilesStartTimes = new HashMap<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                if(!entry.getName().equals("metadata")) {
                    String extraEntryString = new String(entry.getExtra());
                    String[] entryPieces = extraEntryString.split(Pattern.quote("$"));
                    if(entryPieces[0].equals("json_mode"))
                        availableLogs.add(entry.getName());
                    else {
                        availableRawFiles.add(entry.getName());
                        rawFilesStartTimes.put(entry.getName(), Long.parseLong(entryPieces[1]));
                        rawFilesEndTimes.put(entry.getName(), Long.parseLong(entryPieces[2]));
                    }
                }
            }

            zipFile.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int compareTo(LogFileEntry o) {
        if(!isLoaded)
            reload();
        if(!o.isLoaded)
            o.reload();

        return (int)(firstTimestamp - o.firstTimestamp);
    }
}
