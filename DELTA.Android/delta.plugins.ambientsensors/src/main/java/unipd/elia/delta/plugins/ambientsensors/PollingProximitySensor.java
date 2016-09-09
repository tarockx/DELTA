package unipd.elia.delta.plugins.ambientsensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 27/04/2015.
 */

@DeltaPluginMetadata(PluginName = "Proximity sensor logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs the device Proximity Sensor, measuring how far the front of the device is from any solid object.",
        DeveloperDescription = "Normally reports a floating-point value representing the distance between the front of the device from any solid objects, in centimeters. " +
                "Note however that the unit of measurement is not standard and that some devices may only report a binary value " +
                "(with '0' meaning 'there's something near the device' and any other value meaning 'there's nothing near the device')")
public class PollingProximitySensor implements IDeltaPlugin, IDeltaPollingPlugin, SensorEventListener {
    SensorManager mySensorManager;
    Sensor myProximitySensor;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    boolean sporadicMode;
    int frequency;
    boolean listenerRegistered = false;

    float proximity;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();
        sporadicMode = pluginConfiguration.PollingFrequency >= 1000;
        frequency = pluginConfiguration.PollingFrequency;

        mySensorManager = (SensorManager) androidContext.getSystemService(Context.SENSOR_SERVICE);
        if (mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null){
            myProximitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
        else {
            throw new PluginFailedToInitializeException(this, "Proximity Sensor is not available on this device, or sensor could not be accessed");
        }
    }

    @Override
    public void Terminate() {
        if(mySensorManager != null)
            mySensorManager.unregisterListener(this);

        mySensorManager = null;
        myDeltaMaster = null;
        myProximitySensor = null;
    }

    private void sendUpdate(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("proximity", proximity);
        }catch (Exception ex){}


        myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        proximity = event.values[0];

        if(sporadicMode) {
            mySensorManager.unregisterListener(this);
            sendUpdate();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not needed
    }

    @Override
    public void Poll() {
        if(sporadicMode || !listenerRegistered) {
            mySensorManager.registerListener(this, myProximitySensor, frequency * 1000); //millisec * 1000 = microsec
            listenerRegistered = true;
        }
        else
            sendUpdate();
    }
}
