package unipd.elia.delta.plugins.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.json.JSONException;
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
 * Created by Elia on 18/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Power Source logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Reports whether the device is connected to an external power source (charging) or disconnected.",
        DeveloperDescription = "Also logs the type of power source (AC adapter, usb, etc.). A log entry is made every time the device is connected to a power source or disconnected from it. NOTE: If you're using the 'Battery and Power monitor' plugin, this one is redundant.")
public class PowerSourceLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;

    BroadcastReceiver myPowerStatusBroadcastReceiver;
    int nullValue = -1;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        try {
            myPowerStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportBatteryStatus(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void reportBatteryStatus(Intent intent){

        JSONObject object = new JSONObject();
        try {
            if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){
                object.put("power_source", "not_connected");
            }
            else {
                Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if(batteryIntent != null) {
                    switch (batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, nullValue)) {
                        case 0:
                            object.put("power_source", "not_connected");
                            break;
                        case BatteryManager.BATTERY_PLUGGED_AC:
                            object.put("power_source", "AC_adapter");
                            break;
                        case BatteryManager.BATTERY_PLUGGED_USB:
                            object.put("power_source", "usb_port");
                            break;
                        case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                            object.put("power_source", "wireless_charger");
                            break;
                        default:
                            object.put("power_source", "N/A");
                            break;
                    }
                }
                else {
                    object.put("power_source", "N/A");
                }
            }

            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, object));

        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myPowerStatusBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.registerReceiver(myPowerStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
            context.registerReceiver(myPowerStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myPowerStatusBroadcastReceiver);
    }
}
