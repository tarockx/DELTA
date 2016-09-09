package unipd.elia.delta.plugins.systemtools;

import android.app.ActivityManager;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 03/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Tasks and Memory logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs which tasks (applications and services) are running on the device, and statistics on their usage of RAM memory.",
        DeveloperDescription = "Logged data includes information on which apps and services are running at any given time, including detailed memory usage statistics. " +
                "Also reports the apps that have foreground priority, possibly allowing to determine which app the user is actively using at any given time.")
public class RunningTasksLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    ActivityManager myActivityManager;
    private Context context;
    private IDeltaPluginMaster myDeltaMaster;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myActivityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
        }
        catch (Exception ex){
            throw new PluginFailedToInitializeException(this, "Failed to obtain an ActivityManager instance");
        }

        if(myActivityManager == null)
            throw new PluginFailedToInitializeException(this, "Could not obtain ActivityManager");
    }

    @Override
    public void Poll() {
        JSONArray jsonArray = new JSONArray();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = myActivityManager.getRunningAppProcesses();
        for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
            JSONObject jsonObject = new JSONObject();

            try {
                jsonObject.put("processName", appProcess.processName);
                jsonObject.put("pid", appProcess.pid);
                jsonObject.put("uid", appProcess.uid);
                jsonObject.put("isForeground", appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND);
                int[] pids = new int[1];
                pids[0] = appProcess.pid;
                android.os.Debug.MemoryInfo[] MI = myActivityManager.getProcessMemoryInfo(pids);
                jsonObject.put("mem_dalvik_private", MI[0].dalvikPrivateDirty);
                jsonObject.put("mem_dalvik_shared", MI[0].dalvikSharedDirty);
                jsonObject.put("mem_dalvik_pss", MI[0].dalvikPss);
                jsonObject.put("mem_native_private", MI[0].nativePrivateDirty);
                jsonObject.put("mem_native_shared", MI[0].nativeSharedDirty);
                jsonObject.put("mem_native_pss", MI[0].nativePss);
                jsonObject.put("mem_other_private", MI[0].otherPrivateDirty);
                jsonObject.put("mem_other_shared", MI[0].otherSharedDirty);
                jsonObject.put("mem_other_pss", MI[0].otherPss);

                jsonObject.put("mem_total_private_KB", MI[0].getTotalPrivateDirty());
                jsonObject.put("mem_total_shared_KB", MI[0].getTotalSharedDirty());
                jsonObject.put("meme_total_shared_KB", MI[0].getTotalPss());

                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), jsonArray.toString());
        myDeltaMaster.Update(data);
    }



    @Override
    public void Terminate() {
        myActivityManager = null;
        myDeltaMaster = null;
        context = null;
    }
}
