package unipd.elia.delta.plugins.systemtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 13/07/2015.
 */
@DeltaPluginMetadata(PluginName = "Startup and Shutdown logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs the times at which the device is turned on and off",
        DeveloperDescription = "An entry is made every time the the device turns off and every time is turned on. Timestamps are in milliseconds since epoch.")
public class StartupShutdownLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myShutdownBroadcastReceiver;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myShutdownBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportShutdownEvent(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }
    }

    private void reportShutdownEvent(Intent intent) {
        JSONObject obj = new JSONObject();
        try{
            obj.put("event", "shutdown");
            obj.put("timestamp", System.currentTimeMillis());

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj.toString()));
        }
        catch (Exception ex){}
    }

    private void reportStartupEvent() {
        JSONObject obj = new JSONObject();
        try{
            long bootTime = java.lang.System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime();
            obj.put("event", "startup");
            obj.put("timestamp", bootTime);

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj.toString()));
        }
        catch (Exception ex){}
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myShutdownBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            //First entry, since we are not running when the system starts
            reportStartupEvent();

            context.registerReceiver(myShutdownBroadcastReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myShutdownBroadcastReceiver);
    }
}
