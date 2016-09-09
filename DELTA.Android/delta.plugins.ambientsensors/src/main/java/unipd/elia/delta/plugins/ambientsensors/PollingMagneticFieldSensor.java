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

@DeltaPluginMetadata(PluginName = "Magnetic Field sensor logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin measures the magnetic field around the device.",
        DeveloperDescription = "The values are measured in microtesla (uT).")
public class PollingMagneticFieldSensor implements IDeltaPlugin, IDeltaPollingPlugin, SensorEventListener {
    SensorManager mySensorManager;
    Sensor myMagneticSensor;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    boolean sporadicMode;
    int frequency;
    boolean listenerRegistered = false;

    float magX;
    float magY;
    float magZ;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();
        sporadicMode = pluginConfiguration.PollingFrequency >= 1000;
        frequency = pluginConfiguration.PollingFrequency;

        mySensorManager = (SensorManager) androidContext.getSystemService(Context.SENSOR_SERVICE);
        if (mySensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            myMagneticSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        else {
            throw new PluginFailedToInitializeException(this, "Magnetic Field Sensor is not available on this device, or sensor could not be accessed");
        }
    }

    @Override
    public void Terminate() {
        if(mySensorManager != null)
            mySensorManager.unregisterListener(this);

        mySensorManager = null;
        myDeltaMaster = null;
        myMagneticSensor = null;
    }

    private void sendUpdate(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("magX", magX);
            obj.put("magY", magY);
            obj.put("magZ", magZ);
        }catch (Exception ex){}


        myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        magX = event.values[0];
        magY = event.values[1];
        magZ = event.values[2];

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
            mySensorManager.registerListener(this, myMagneticSensor, frequency * 1000); //millisec * 1000 = microsec
            listenerRegistered = true;
        }
        else
            sendUpdate();
    }
}
