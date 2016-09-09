package unipd.delta.plugins.connectivitytools;

import android.bluetooth.BluetoothAdapter;
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
 * Created by Elia on 08/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Bluetooth connection status logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs the status of the bluetooth connection: whether the bluetooth adapter is turned on/off, " +
                "if it's connected or not and if the device is discoverable or not. No actual data passing through the Bluetooth adapter will be logged.",
        DeveloperDescription = "A log entry is generated: " +
                "(a) When the Bluetooth adapter is turned on or off " +
                "(b) When the device is connecting/connected to a Bluetooth device, and when it is disconnecting/disconnected " +
                "(c) When the scan mode changes (i.e: when the device becomes discoverable/connectable)"
)
public class BluetoothConnectionStatusLogger implements IDeltaPlugin, IDeltaEventPlugin {
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
            JSONObject obj = new JSONObject();

            String event = null;
            String status = null;
            if(intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)){
                int statusCode = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                event = "connection_status_changed";

                switch (statusCode){
                    case BluetoothAdapter.STATE_CONNECTED:
                        status = "connected";
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        status = "connecting";
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        status = "disconnected";
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTING:
                        status = "disconnecting";
                        break;
                    default:
                        status = "unknown";
                        break;
                }
            }
            else if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int statusCode = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                event = "adapter_status_changed";

                switch (statusCode){
                    case BluetoothAdapter.STATE_OFF:
                        status = "adapter_off";
                        break;
                    case BluetoothAdapter.STATE_ON:
                        status = "adapter_on";
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        status = "adapter_turning_off";
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        status = "adapter_turning_on";
                        break;
                    default:
                        status = "unknown";
                        break;
                }
            }
            else if(intent.getAction().equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){
                int statusCode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                event = "discoverability_mode_changed";

                switch (statusCode){
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        status = "none";
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        status = "connectable";
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        status = "discoverable_and_connectable";
                        break;
                    default:
                        status = "unknown";
                        break;
                }
            }

            if(event != null) {
                obj.put("event", event);
                obj.put("status", status);
                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
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
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myStatusBroadcastReceiver);
    }
}
