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

@DeltaPluginMetadata(PluginName = "Pressure sensor logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs the device's Pressure Sensor, measuring the atmospheric pressure around the device.",
        DeveloperDescription = "Logged values are in millibars. Note: few devices have this sensor, consider marking this as optional if possible.")
public class PollingPressureSensor implements IDeltaPlugin, IDeltaPollingPlugin, SensorEventListener {
    SensorManager mySensorManager;
    Sensor myPressureSensor;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    boolean sporadicMode;
    int frequency;
    boolean listenerRegistered = false;

    float millibars;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();
        sporadicMode = pluginConfiguration.PollingFrequency >= 1000;
        frequency = pluginConfiguration.PollingFrequency;

        mySensorManager = (SensorManager) androidContext.getSystemService(Context.SENSOR_SERVICE);
        if (mySensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            myPressureSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        }
        else {
            throw new PluginFailedToInitializeException(this, "Pressure Sensor is not available on this device, or sensor could not be accessed");
        }
    }

    @Override
    public void Terminate() {
        if(mySensorManager != null)
            mySensorManager.unregisterListener(this);

        mySensorManager = null;
        myDeltaMaster = null;
        myPressureSensor = null;
    }

    private void sendUpdate(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("millibars", millibars);
        }catch (Exception ex){}


        myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        millibars = event.values[0];

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
            mySensorManager.registerListener(this, myPressureSensor, frequency * 1000); //millisec * 1000 = microsec
            listenerRegistered = true;
        }
        else
            sendUpdate();
    }
}
