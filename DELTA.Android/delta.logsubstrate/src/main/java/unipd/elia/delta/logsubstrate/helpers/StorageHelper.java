package unipd.elia.delta.logsubstrate.helpers;

import android.provider.Settings;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.Logger;

/**
 * Created by Elia on 18/05/2015.
 */
public class StorageHelper {
    private static final Map<String, List<DeltaDataEntry>> cachedData = new HashMap<>();
    public static long currentExperimentStartTime = 0;
    private static long cumulativeSize = 0;
    private static long lastFlushTimestamp = 0;

    private static Lock dumpLock = new ReentrantLock();

    private final static int MAX_CACHE_SIZE = 5 * 1024 * 1024; //5 MB
    private final static int MAX_FLUSH_INTERVAL_MILLISECONDS = 5 * 60 * 1000; //5 minutes

    /**
     * Stores logged data into a temporary RAM cache. When the amount of stored data exceeds the amount set by MAX_CACHE_SIZE
     * or when more than MAX_FLUSH_INTERVAL_MILLISECONDS milliseconds have passed since the last flush, the data is cleared from RAM
     * and flushed to disk.
     * @param data The data to store
     */
    public static void update(DeltaDataEntry data){
        //caches data
        synchronized (cachedData){

            if(lastFlushTimestamp == 0)
                lastFlushTimestamp = System.currentTimeMillis();

            String pluginID = data.pluginID;

            if(!cachedData.containsKey(pluginID))
                cachedData.put(pluginID, new LinkedList<DeltaDataEntry>());
            cachedData.get(pluginID).add(data);

            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Update from plugin: " + pluginID + "Data: {" + data.toString() + "}");

            cumulativeSize += data.getSize(); //approximate size in bytes
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Storing new data, cache size is now:" + cumulativeSize);

            if(cumulativeSize > MAX_CACHE_SIZE || (System.currentTimeMillis() - lastFlushTimestamp) > MAX_FLUSH_INTERVAL_MILLISECONDS){
                flushData(true);
            }
        }
    }

    /**
     * Flushes all cached data to disk and closes any opened file streams. This has to be called when the experiment is terminating,
     * AFTER all plugins have stopped sending updates, otherwise errors may occur.
     */
    public static void flushAndCleanup(){
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Service requested an explicit flush of logged data (probably terminating). Executing synchronously...");
        flushData(false); //We do NOT run this async, because it is called when the service is terminating. If we did, the service would be destroyed before we had a chance to dump data
    }


    @SuppressWarnings("unchecked")
    private static void flushData(boolean async) {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Flushing data from cache to zip files...");

        final Map.Entry<String, List<DeltaDataEntry>>[] entries;
        final long timestamp;
        synchronized (cachedData){
            timestamp = System.currentTimeMillis();
            entries = new Map.Entry[cachedData.size()];
            cachedData.entrySet().toArray(entries);
            cachedData.clear();
            cumulativeSize = 0;
            lastFlushTimestamp = timestamp;
        }

        if(entries.length == 0)
            return;

        if(async){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Flushing to disk now (on separate thread). Entry count: " + entries.length);

            //Runs IO operations in a separate thread
            Runnable r = new Runnable() {
                public void run() {
                    dumpDataToFiles(entries, timestamp);
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
        else {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Flushing to disk now. Entry count: " + entries.length);
            dumpDataToFiles(entries, timestamp);
        }
    }

    private static void dumpDataToFiles(Map.Entry<String, List<DeltaDataEntry>>[] entries, long timestamp){
        try {
            dumpLock.lock();

            String path = IOHelpers.createOrGetExperimentFolder();
            String fileOutPath = path + "/ExperimentStartedAt " + currentExperimentStartTime + " - PieceStartedAt " + timestamp + ".zip";

            // out put file
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Preparing to output data to: " + fileOutPath);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileOutPath)));
            out.setMethod(ZipOutputStream.DEFLATED);
            out.setLevel(Deflater.BEST_COMPRESSION);

            long start = 0, finish = 0;

            for(Map.Entry<String, List<DeltaDataEntry>> entry : entries){
                if(entry.getValue().size() > 0) {
                    boolean rawMode = entry.getValue().get(0).rawMode;
                    ZipEntry newEntry = new ZipEntry(entry.getKey() + (rawMode ? ".bin" : ".txt"));
                    newEntry.setExtra(
                            ((rawMode ? "raw_mode" : "json_mode") + "$" + entry.getValue().get(0).timestamp + "$" + entry.getValue().get(entry.getValue().size() - 1).timestamp ).getBytes()
                    );
                    out.putNextEntry(newEntry);

                    for (DeltaDataEntry deltaDataEntry : entry.getValue()) {
                        if(start == 0 || deltaDataEntry.timestamp < start)
                            start = deltaDataEntry.timestamp;
                        if(finish == 0 || deltaDataEntry.timestamp > finish)
                            finish = deltaDataEntry.timestamp;

                        out.write(deltaDataEntry.toByteArray());
                    }
                }
            }

            //set metadata for zip decoding later on
            String android_id = Settings.Secure.getString(DELTAUtils.context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String experimentID = DELTAUtils.context.getPackageName();
            ZipEntry extraEntry = new ZipEntry("metadata");
            extraEntry.setExtra((android_id + "$" + experimentID + "$" + start + "$" + finish + "$" + currentExperimentStartTime).getBytes());
            out.putNextEntry(extraEntry);
            out.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        } finally {
            dumpLock.unlock();
        }
    }


}
