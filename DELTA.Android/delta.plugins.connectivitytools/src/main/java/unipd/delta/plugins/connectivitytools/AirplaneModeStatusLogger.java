package unipd.delta.plugins.connectivitytools;

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
 * Created by Elia on 09/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Airplane Mode state logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs whether Airplane Mode is turned on or off",
        DeveloperDescription = "An entry is logged every time the Airplane Mode is turned on or off. " +
                "NOTE: Airplane Mode being off does not guarantee that secondary networks (i.e.: WiFi, Bluetooth) are turned off, it just means the mobile network is turned off."
)
public class AirplaneModeStatusLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myStatusBroadcastReceiver;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        try {
            myStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportStatus(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void reportStatus(Intent intent){
        try{
            boolean state = intent.getBooleanExtra("state", false);

            JSONObject obj = new JSONObject();
            obj.put("event", state ? "airplane_mode_on" : "airplane_mode_off");

            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myStatusBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myStatusBroadcastReceiver);
    }
}
