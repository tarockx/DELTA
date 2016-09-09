package unipd.elia.delta.plugins.geolocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 22/06/2015.
 */

@DeltaPluginMetadata(PluginName = "Location Providers logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs changes in the status of the location providers present on the device (e.g.: when the GPS is turned on or off, etc.). " +
                "It does NOT log the user's location.",
        DeveloperDescription = "An entry will be added every time the status of one of the location providers on the device changes, for example when the user switches GPS " +
                "or the Network-based geolocator off or on. Note that these changes may also be triggered by events other than direct intervention by the user " +
                "(e.g: the network-based geolocator will be reported as unavailable if there is no network connection).")
public class LocationProviderLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myProvidersChangedBroadcastReceiver;
    LocationManager myLocationManager;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Received NULL Context or UpdateListener");

        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        try {
            myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if(myLocationManager == null)
                throw new PluginFailedToInitializeException(this, "Failed to obtain the location manager service");

            myProvidersChangedBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateProvidersStatus(intent);
                }
            };
        } catch (PluginFailedToInitializeException ex){
            throw ex;
        }
        catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void updateProvidersStatus(Intent intent) {
        List<String> allProviders = myLocationManager.getAllProviders();

        try{
            JSONArray arr = new JSONArray();
            for(String provider : allProviders){
                boolean enabled = myLocationManager.isProviderEnabled(provider);
                JSONObject obj = new JSONObject();
                obj.put("provider", provider);
                obj.put("enabled", enabled);
                arr.put(obj);
            }

            DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), pluginName, arr);
            myDeltaMaster.Update(data);
        } catch (Exception ex){
            ex.printStackTrace();
        }

    }

    @Override
    public void Terminate() {
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);

        context.registerReceiver(myProvidersChangedBroadcastReceiver, intentFilter);
    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myProvidersChangedBroadcastReceiver);
    }

}
