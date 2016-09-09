package unipd.delta.plugins.connectivitytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

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
 * Created by Elia on 01/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Network connection status logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs whether the device is connected to a network or not (and the type of the network, i.e.: WiFi or Mobile).Does NOT log network traffic.",
        DeveloperDescription = "A log entry is generated whenever the connectivity status changes, i.e.: whenever the device connects to or disconnects from " +
                "a data network (WiFi, mobile, WiMax, etc.). The type of the network is also included in the report."
)
public class NetworkConnectionStatusLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myStatusBroadcastReceiver;
    String pluginName;

    String lastUpdate = null;

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
            final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo networkInfo;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                networkInfo = connectivityManager.getNetworkInfo(intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0));
            else
                networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            JSONObject obj = new JSONObject();
            String status;
            String networkType = networkInfo.getTypeName();
            switch (networkInfo.getState()){
                case CONNECTED:
                    status = "connected";
                    break;
                case CONNECTING:
                    status = "connecting";
                    break;
                case DISCONNECTED:
                    status = "disconnected";
                    break;
                case DISCONNECTING:
                    status = "disconnecting";
                    break;
                case SUSPENDED:
                    status = "suspended";
                    break;
                default:
                    status = "unknown";
                    break;
            }
            obj.put("status", status);
            obj.put("network_type", networkType);

            String update = obj.toString();

            //report
            if(lastUpdate == null || !lastUpdate.equals(update)) {
                lastUpdate = update;
                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, update));
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
            context.registerReceiver(myStatusBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myStatusBroadcastReceiver);
    }
}
