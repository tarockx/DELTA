package unipd.elia.delta.plugins.systemtools;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.delta.sharedlib.StringOption;

/**
 * Created by Elia on 20/07/2015.
 */
@DeltaPluginMetadata(PluginName = "File System monitor",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs all changes to a set of files/directories specified by the experiment creator, for example when such files are read/written/moved/deleted/etc. " +
                "Note that the content of the files themselves will NOT be logged.",
        DeveloperDescription = "This plugin logs all changes made to a set of files and/or directories. You can use the Options menu to configure which files/directories to monitor " +
                "(put one absolute file path per line. You can also use the macro %EXTERNAL_STORAGE% if you need to monitor files in the external storage, which is recommended since " +
                "the path to the external storage is not the same on all devices).\nA log entry is recorded whenever one of the monitored files/directories (or a file inside a monitored directory) is " +
                "opened, closed, read from, written to, created, deleted, moved, changed attributes.\nIf you don't change any settings, by default the EXTERNAL_STORAGE directory is monitored.")
@DeltaOptions(StringOptions = {
        @DeltaStringOption(ID = "PATHS_TO_MONITOR", Name = "Files/directories to monitor", defaultValue = "%EXTERNAL_STORAGE%", Multiline = true,
                Description = "Enter the list of absolute paths to files/directories you want to monitor (one per line). You can use the %EXTERNAL_STORAGE% macro as a prefix to reference " +
                        "a path in the external storage (e.g.: %EXTERNAL_STORAGE%/Download)"
        )
})
public class FileSystemLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    List<FileObserver> myFileObservers;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        myFileObservers = new LinkedList<>();
        pluginName = getClass().getName();

        StringOption paths_to_monitor = pluginConfiguration.getStringOption("PATHS_TO_MONITOR");
        if(paths_to_monitor == null || paths_to_monitor.Value == null || paths_to_monitor.Value.isEmpty())
            throw new PluginFailedToInitializeException(this, "Bad configuration: no paths to monitor has been set.");

        String pathToExternalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();

        for(String pathToMonitor : paths_to_monitor.Value.trim().split("\\r?\\n")){
            final String pathToMonitorDecoded = pathToMonitor.trim().replace("%EXTERNAL_STORAGE%", pathToExternalStorage);
            final File pathFile = new File(pathToMonitorDecoded);
            if(pathFile.exists()){
                String basePath = null;
                try {
                    basePath = pathFile.getCanonicalPath();
                } catch (IOException e) {
                    basePath = pathFile.getAbsolutePath();
                }
                final String finalBasePath = basePath;
                myFileObservers.add(new FileObserver(pathToMonitorDecoded) {
                    @Override
                    public void onEvent(int event, String path) {
                            update(event, path, finalBasePath);
                    }
                });
            }
        }
    }

    private void update(int event, String path, String pathToMonitorDecoded) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("monitored_path", pathToMonitorDecoded);
            switch (FileObserver.ALL_EVENTS & event){
                case FileObserver.ACCESS :
                    obj.put("event", "read_from");
                    break;
                case FileObserver.ATTRIB :
                    obj.put("event", "metadata_changed");
                    break;
                case FileObserver.CLOSE_NOWRITE :
                    obj.put("event", "closed_without_writing");
                    break;
                case FileObserver.CLOSE_WRITE :
                    obj.put("event", "closed_after_write");
                    break;
                case FileObserver.DELETE :
                    obj.put("event", "file_deleted");
                    break;
                case FileObserver.DELETE_SELF :
                    obj.put("event", "monitored_file_deleted");
                    break;
                case FileObserver.MODIFY :
                    obj.put("event", "written_to");
                    break;
                case FileObserver.MOVED_FROM :
                    obj.put("event", "file_moved_here");
                    break;
                case FileObserver.MOVED_TO :
                    obj.put("event", "file_moved_to_another_location");
                    break;
                case FileObserver.MOVE_SELF :
                    obj.put("event", "monitored_path_moved");
                    break;
                case FileObserver.OPEN :
                    obj.put("event", "file_opened");
                    break;
                default:
                    obj.put("event", "unknown"); //apparently there are some events that are undocumented / not exposed by the API
            }

            obj.put("affected_file", path == null ? pathToMonitorDecoded : path);

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void Terminate() {
        myFileObservers.clear();
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        for(FileObserver observer : myFileObservers)
            observer.startWatching();
    }

    @Override
    public void StopLogging() {
        for(FileObserver observer : myFileObservers)
            observer.stopWatching();
    }
}
