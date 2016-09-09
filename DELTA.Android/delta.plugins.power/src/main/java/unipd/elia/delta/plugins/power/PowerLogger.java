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
import unipd.elia.delta.sharedlib.PluginConfiguration;


/**
 * Created by Elia on 18/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Battery and Power monitor",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Monitors the status of the battery (% of charge left, if it's charging, etc.)",
        DeveloperDescription = "Monitors changes in the level and charging status of the battery. This includes the current battery statistics (level, voltage, temperature, health) as well as which power source, if any, is connected (usb, AC adapter, wireless charger, etc.). A log entry is made every time one of the above mentioned values changes. NOTE: if you only need to log when the device is connected or not to a power source, but you're not interested in battery statistics, prefer the 'Power Source Logger' plugin, as it consumes less energy.")
public class PowerLogger implements IDeltaEventPlugin, IDeltaPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myBatteryBroadcastReceiver;
    int nullValue = -1;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        myBatteryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reportBatteryStatus(intent);
            }
        };

    }

    private void reportBatteryStatus(Intent intent){
        JSONObject object = new JSONObject();
        try {
            object.put("battery_level", intent.getIntExtra(BatteryManager.EXTRA_LEVEL, nullValue));
            object.put("battery_max_level", intent.getIntExtra(BatteryManager.EXTRA_SCALE, nullValue));
            object.put("battery_present", intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false));
            object.put("battery_temperature", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, nullValue));
            object.put("battery_voltage", intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, nullValue));

            switch (intent.getIntExtra(BatteryManager.EXTRA_STATUS, nullValue)){
                case BatteryManager.BATTERY_STATUS_CHARGING :
                    object.put("battery_status", "charging");
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING :
                    object.put("battery_status", "discharging");
                    break;
                case BatteryManager.BATTERY_STATUS_FULL :
                    object.put("battery_status", "full");
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING :
                    object.put("battery_status", "not_charging");
                    break;
                case BatteryManager.BATTERY_HEALTH_UNKNOWN :
                    object.put("battery_status", "unknown");
                    break;
                default:
                    object.put("battery_status", "N/A");
                    break;
            }

            switch (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, nullValue)){
                case BatteryManager.BATTERY_HEALTH_COLD :
                    object.put("battery_health", "cold");
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD :
                    object.put("battery_health", "dead");
                    break;
                case BatteryManager.BATTERY_HEALTH_GOOD :
                    object.put("battery_health", "good");
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT :
                    object.put("battery_health", "overheated");
                    break;
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE :
                    object.put("battery_health", "overvolted");
                    break;
                case BatteryManager.BATTERY_HEALTH_UNKNOWN :
                    object.put("battery_health", "unknown");
                    break;
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE :
                    object.put("battery_health", "unspecified_failure");
                    break;
                default:
                    object.put("battery_health", "N/A");
                    break;
            }

            switch (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, nullValue)){
                case 0 :
                    object.put("power_source", "not_connected");
                    break;
                case BatteryManager.BATTERY_PLUGGED_AC :
                    object.put("power_source", "AC_adapter");
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB :
                    object.put("power_source", "usb_port");
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS :
                    object.put("power_source", "wireless_charger");
                    break;
                default:
                    object.put("power_source", "N/A");
                    break;
            }

            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), object));

        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void Terminate() {
    }


    @Override
    public void StartLogging() {
        context.registerReceiver(myBatteryBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myBatteryBroadcastReceiver);
    }
}
