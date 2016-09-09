package unipd.delta.plugins.connectivitytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaBooleanOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.BooleanOption;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 01/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "WiFi statistics logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs information about the currently connected WiFi access point, including SSID, local IP address, channel frequency, etc.",
        DeveloperDescription = "An entry is logged every time the device connects to a WiFi access point. Logged information include all the available statistics " +
                "about the connection: SSID, BSSID, link speed, local IP address, MAC address of the adapter, network ID and signal strength. " +
                "Optionally, if the appropriate setting is selected, an entry will be also logged every time the signal strength changes. " +
                "NOTE: due to limitations in the Android API, the 'frequency' value is only available on Android 5.0 or later. If running on a lower version the field will be null."
)
@DeltaOptions(
        BooleanOptions = {
                @DeltaBooleanOption( ID = "LOG_SIGNAL_STRENGTH", Name = "Also log changes in signal strength?", defaultValue = false,
                        Description = "If this option is set to true, a new log entry will be generated not only when a connection is established, but also when the signal strength of the currently connected network changes. Note that this could happen relatively often, only enable if needed.")
        }
)
public class WiFiInfoLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myStatusBroadcastReceiver;
    boolean logSignalStrengthChanges = false;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

            BooleanOption option = pluginConfiguration.getBooleanOption("LOG_SIGNAL_STRENGTH");
            if(option != null && option.Value)
                logSignalStrengthChanges = true;


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
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try{
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if(networkInfo == null)
                return;

            if(networkInfo.isConnected()){
                final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    JSONObject obj = new JSONObject();

                    obj.put("SSID", connectionInfo.getSSID());
                    obj.put("isSSIDHidden", connectionInfo.getHiddenSSID());
                    obj.put("BSSID", connectionInfo.getBSSID());
                    obj.put("frequency", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Integer.toString(connectionInfo.getFrequency()) : "N/A");
                    obj.put("IP", connectionInfo.getIpAddress());
                    obj.put("link_speed", connectionInfo.getLinkSpeed());
                    obj.put("MAC", connectionInfo.getMacAddress());
                    obj.put("network_ID", connectionInfo.getNetworkId());
                    obj.put("signal_strength_dBm", connectionInfo.getRssi());

                    //report
                    myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj));
                }
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
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            if(logSignalStrengthChanges)
                context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myStatusBroadcastReceiver);
    }
}
