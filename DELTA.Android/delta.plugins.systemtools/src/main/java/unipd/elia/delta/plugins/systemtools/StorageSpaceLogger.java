package unipd.elia.delta.plugins.systemtools;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaBooleanOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.delta.sharedlib.StringOption;

/**
 * Created by Elia on 21/07/2015.
 */
@DeltaPluginMetadata(PluginName = "Storage Space monitor",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Monitors the amount of used and available space on the device's memory.",
        DeveloperDescription = "This plugin periodically polls the file system and logs the amount of used and free bytes/blocks on the device's storage units.\n" +
                "By default it will monitor the System, Internal Storage and External Storage partitions, but you can specify additional specific mount points in the options. Any mount point that " +
                "does not exist on the device will be ignored.\nNote that, no matter how fast you set the polling frequency, an update will only be logged if the statistics changed since the last poll")
@DeltaOptions(
        BooleanOptions = {
                @DeltaBooleanOption(ID = "MONITOR_SYSTEM_ROOT", defaultValue = true, Name = "Monitor system partition", Description = "If checked, the /system partition will be monitored"),
                @DeltaBooleanOption(ID = "MONITOR_INTERNAL", defaultValue = true, Name = "Monitor data partition", Description = "If checked, the /data partition (aka: Internal Storage) will be monitored"),
                @DeltaBooleanOption(ID = "MONITOR_EXTERNAL", defaultValue = true, Name = "Monitor external storage", Description = "If checked, the external storage (if available) will be monitored")
        },
        StringOptions = {
                @DeltaStringOption(ID = "ADDITIONAL_PATHS", Name = "Additional mount points to monitor", defaultValue = "%EXTERNAL_STORAGE%", Multiline = true,
                        Description = "A list of paths that represent additional mount points to monitor. Put one per line, any mount points not present on the device will be ignored"
        )
})
public class StorageSpaceLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    Map<String, StorageStats> myFsMonitors;
    String pluginName;

    private class StorageStats{
        public long availableBytes;
        public long availableBlocks;
        public long totalBytes;
        public long totalBlocks;
        public long blockSize;

        public boolean equals(StorageStats storageStats){
            if(storageStats == null)
                return false;

            return availableBytes == storageStats.availableBytes &&
                    availableBlocks == storageStats.availableBlocks &&
                    totalBytes == storageStats.totalBytes &&
                    totalBlocks == storageStats.totalBlocks &&
                    blockSize == storageStats.blockSize;
        }

        public StorageStats(long availableBytes, long availableBlocks, long totalBytes, long totalBlocks, long blockSize){
            this.availableBytes = availableBytes;
            this.availableBlocks = availableBlocks;
            this.totalBytes = totalBytes;
            this.totalBlocks = totalBlocks;
            this.blockSize = blockSize;
        }
    }

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        myFsMonitors = new HashMap<>();
        pluginName = getClass().getName();

        List<String> pathsToMonitor = new LinkedList<>();

        if(pluginConfiguration.getBooleanOption("MONITOR_SYSTEM_ROOT") != null && pluginConfiguration.getBooleanOption("MONITOR_SYSTEM_ROOT").Value)
            pathsToMonitor.add(Environment.getRootDirectory().getAbsolutePath());
        if(pluginConfiguration.getBooleanOption("MONITOR_INTERNAL") != null && pluginConfiguration.getBooleanOption("MONITOR_INTERNAL").Value)
            pathsToMonitor.add(Environment.getDataDirectory().getAbsolutePath());
        if(pluginConfiguration.getBooleanOption("MONITOR_EXTERNAL") != null && pluginConfiguration.getBooleanOption("MONITOR_EXTERNAL").Value)
            pathsToMonitor.add(Environment.getExternalStorageDirectory().getAbsolutePath());


        StringOption paths_to_monitor = pluginConfiguration.getStringOption("ADDITIONAL_PATHS");
        if(paths_to_monitor != null && paths_to_monitor.Value != null && !paths_to_monitor.Value.isEmpty()){
            for(String pathToMonitor : paths_to_monitor.Value.trim().split("\\r?\\n")){
                String trimmedPath = pathToMonitor.trim();
                File file = new File(trimmedPath);
                if(file.exists() && file.isDirectory() && !pathsToMonitor.contains(trimmedPath))
                    pathsToMonitor.add(trimmedPath);
            }
        }

        if(pathsToMonitor.size() == 0)
            throw new PluginFailedToInitializeException(this, "Bad configuration: no mount points to monitor have been set.");

        for(String path : pathsToMonitor){
            myFsMonitors.put(path, null);
        }
    }

    @Override
    public void Terminate() {
        myFsMonitors.clear();
    }

    @Override
    public void Poll() {
        for(String pathToMonitor : myFsMonitors.keySet()){
            StorageStats storageStats = getStats(pathToMonitor);
            if(storageStats != null && (myFsMonitors.get(pathToMonitor) == null || !myFsMonitors.get(pathToMonitor).equals(storageStats))){
                sendUpdate(pathToMonitor, storageStats);
                myFsMonitors.put(pathToMonitor, storageStats);
            }
        }
    }

    private void sendUpdate(String pathToMonitor, StorageStats storageStats) {
        try{
            JSONObject obj = new JSONObject();
            obj.put("path", pathToMonitor);
            obj.put("total_bytes", storageStats.totalBytes);
            obj.put("total_blocks", storageStats.totalBlocks);
            obj.put("available_bytes", storageStats.availableBytes);
            obj.put("available_blocks", storageStats.availableBlocks);
            obj.put("block_size", storageStats.blockSize);

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private StorageStats getStats(String path){
        try{
            StatFs statFs = new StatFs(path);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) { //this has changed in API 18
                return new StorageStats(
                        statFs.getAvailableBytes(),
                        statFs.getAvailableBlocksLong(),
                        statFs.getTotalBytes(),
                        statFs.getBlockCountLong(),
                        statFs.getBlockSizeLong()
                );
            }
            else {
                return new StorageStats(
                        (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize(),
                        (long) statFs.getAvailableBlocks(),
                        (long) statFs.getBlockCount() * (long) statFs.getBlockSize(),
                        (long) statFs.getBlockCount(),
                        (long) statFs.getBlockSize()
                );
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}