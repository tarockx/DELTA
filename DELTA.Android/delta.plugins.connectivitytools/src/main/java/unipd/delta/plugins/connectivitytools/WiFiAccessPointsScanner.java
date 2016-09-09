package unipd.delta.plugins.connectivitytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import org.json.JSONArray;
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
 * Created by Elia on 06/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "WiFi Access Points scanner",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Periodically logs information about the available WiFi HotSpots around the device. This will NOT log your WiFi credentials.",
        DeveloperDescription = "The plugin will trigger a WiFi scan at the specified interval. When the results are available, an entry is logged, " +
                "containing information about all the detected access points. This includes their SSID, BSSID, transmission frequency, " +
                "signal strength and network capabilities.",
        MinPollInterval = 30 * 1000 //limit max scanning frequency to twice per minute
)
public class WiFiAccessPointsScanner implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    WifiManager myWiFiManager;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        myWiFiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        if(myWiFiManager == null)
            throw new PluginFailedToInitializeException(this, "Could not obtain WiFi manager");
    }

    private void reportScanResults(Intent intent){
        try{
            List<ScanResult> scanResults = myWiFiManager.getScanResults();
            if(scanResults == null)
                return;

            JSONArray array = new JSONArray();

            for(ScanResult scanResult : scanResults){
                JSONObject obj = new JSONObject();

                obj.put("SSID", scanResult.SSID);
                obj.put("BSSID", scanResult.BSSID);
                obj.put("capabilities", scanResult.capabilities);
                obj.put("frequency_mhz", scanResult.frequency);
                obj.put("dBm_level", scanResult.level);

                array.put(obj);
            }

            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), array));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
    }


    @Override
    public void Poll() {
        try {
            BroadcastReceiver myStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportScanResults(intent);
                    context.unregisterReceiver(this);
                }
            };
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            myWiFiManager.startScan();

        }catch (Exception ex){
            ;
        }

    }

}