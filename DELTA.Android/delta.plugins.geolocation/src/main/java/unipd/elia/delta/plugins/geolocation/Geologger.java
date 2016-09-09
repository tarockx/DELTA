package unipd.elia.delta.plugins.geolocation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaIntegerOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;


@DeltaPluginMetadata(PluginName = "Location logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs the user's position using access point and cell tower IDs, and possibily the device's GPS antenna.",
        DeveloperDescription = "Use the settings menu to chose the precision level you desire. If you chose \"Precise\" the device's GPS antenna will be used, which drains " +
                "significantly more battery than the coarse mode (which uses low-precision cell tower data). You can also chose the update interval, remember however that this value " +
                "is just a suggestion to the location API, which will still send updates at its own pace.")
@DeltaOptions(
        StringOptions = {
                @DeltaStringOption(ID = "PRECISION_LEVEL", Name = "Precision level",
                        Description = "Coarse mode has lower precision but lower impact on the battery, Precise mode uses GPS to achieve maximum precision at the cost of higher power consumption",
                        AvailableChoices = {"Coarse", "Precise"},
                        defaultValue = "Coarse")},
        IntegerOptions = {
                @DeltaIntegerOption(ID = "FREQUENCY", Name = "Update interval (milliseconds",
                        Description = "How often to ask for updates. Note: this is just a hint for the location subsystem, absolute precision is not guaranteed",
                        MinValue = 1000, defaultValue = 1000
                )}
)
public class Geologger implements IDeltaPlugin, IDeltaEventPlugin {
    private Context context;
    private IDeltaPluginMaster myDeltaMaster;
    private LocationManager myLocationManager;
    private LocationListener myLocationListener;
    String pluginName;
    int interval;
    boolean highPrecision;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        pluginName = getClass() .getName();

        try {
            interval = pluginConfiguration.getIntegerOption("FREQUENCY").Value;
            highPrecision = pluginConfiguration.getStringOption("PRECISION_LEVEL").Value.equals("Precise");
        }catch (Exception ex){
            throw new PluginFailedToInitializeException(this, "Bad configuration: frequency and/or precision level parameters not set or invalid.");
        }

        if(interval < 1000)
            throw new PluginFailedToInitializeException(this, "Bad configuration: frequency setting out of range");

        myLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();

                JSONObject obj = new JSONObject();
                try{
                    obj.put("long", longitude);
                    obj.put("lat", latitude);
                    obj.put("accuracy", location.getAccuracy());
                    obj.put("bearing", location.getBearing());
                    obj.put("current_provider", location.getProvider());

                    myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj.toString()));
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        if(myLocationManager == null)
            throw new PluginFailedToInitializeException(this, "Could not obtain LocationManager");


    }

    @Override
    public void Terminate() {

    }

    @Override
    public void StartLogging() {
        // Register the listener with the Location Manager to receive location updates

        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                myLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0, myLocationListener);
                if(highPrecision)
                    myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, myLocationListener);
            }
        });
    }

    @Override
    public void StopLogging() {
        myLocationManager.removeUpdates(myLocationListener);
    }

}
