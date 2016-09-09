package unipd.elia.delta.plugins.systemtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.delta.sharedlib.StringOption;

/**
 * Created by Elia on 01/07/2015.
 */
@DeltaPluginMetadata(PluginName = "Custom Broadcast logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs a set of custom broadcasts set by the experiment developer",
        DeveloperDescription = "Use the settings menu to set a list of Intent Filters to log (separate them with a newline if you want to log multiple intents). " +
                "A log entry is made every time one of the monitored intents fires. If the intent includes a data string and/or extras, they will be logged as well.")
@DeltaOptions(StringOptions = {
        @DeltaStringOption(ID = "INTENTS_TO_LOG", Name = "Intent actions to log", defaultValue = "", Multiline = true,
                Description = "Enter the list of broadcast actions you want to log (one per line)"
        )
})
public class BroadcastLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myBroadcastReceiver;
    String[] broadcastsToLog;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        try {
            StringOption broadcastsOption = pluginConfiguration.getStringOption("INTENTS_TO_LOG");
            broadcastsToLog = broadcastsOption.Value.trim().split("\\r?\\n");
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, "Error while decoding actions list (malformed?)");
        }

        if(broadcastsToLog.length == 0)
            throw new PluginFailedToInitializeException(this, "Bad configuration: no broadcasts set, nothing to log");

        myBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                processIncomingIntent(intent);
            }
        };

    }

    private void processIncomingIntent(Intent intent) {
        JSONObject obj = new JSONObject();
        try{
            obj.put("action", intent.getAction());
            obj.put("data", intent.getData() == null ? "null" : intent.getDataString());
            Bundle extras = intent.getExtras();
            if(extras == null)
                obj.put("extras", "[]");
            else {
                JSONArray arr = new JSONArray();
                for(String key : extras.keySet())
                    arr.put("{\"key\":\"" + extras.get(key) + "\"}");
                obj.put("extras", arr);
            }
        }catch (JSONException ex){
            ex.printStackTrace();
        }
        DeltaDataEntry deltaDataEntry = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj);
        myDeltaMaster.Update(deltaDataEntry);
    }

    @Override
    public void Terminate() {
        myBroadcastReceiver = null;
        myDeltaMaster = null;
        context = null;
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            for (String filter : broadcastsToLog)
                context.registerReceiver(myBroadcastReceiver, new IntentFilter(filter));
        } catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, "Failed to register all the required filters. Bad format? Original exception: " + ex.getMessage());
        }
    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myBroadcastReceiver);
    }

}
