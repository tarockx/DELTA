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

@DeltaPluginMetadata(PluginName = "Gravity sensor logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs the device's Gravity Sensor.",
        DeveloperDescription = "This is similar to the Accelerometer plugin, but only measures the influence of gravity. Values are in m/s^2")
public class PollingGravitySensor implements IDeltaPlugin, IDeltaPollingPlugin, SensorEventListener {
    SensorManager mySensorManager;
    Sensor myGravitySensor;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    boolean sporadicMode;
    int frequency;
    boolean listenerRegistered = false;

    float gravX;
    float gravY;
    float gravZ;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();
        sporadicMode = pluginConfiguration.PollingFrequency >= 1000;
        frequency = pluginConfiguration.PollingFrequency;

        mySensorManager = (SensorManager) androidContext.getSystemService(Context.SENSOR_SERVICE);
        if (mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null){
            myGravitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        }
        else {
            throw new PluginFailedToInitializeException(this, "Gravity Sensor is not available on this device, or sensor could not be accessed");
        }
    }

    @Override
    public void Terminate() {
        if(mySensorManager != null)
            mySensorManager.unregisterListener(this);

        mySensorManager = null;
        myDeltaMaster = null;
        myGravitySensor = null;
    }

    private void sendUpdate(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("gravX", gravX);
            obj.put("gravY", gravY);
            obj.put("gravZ", gravZ);
        }catch (Exception ex){}


        myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        gravX = event.values[0];
        gravY = event.values[1];
        gravZ = event.values[2];

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
            mySensorManager.registerListener(this, myGravitySensor, frequency * 1000); //millisec * 1000 = microsec
            listenerRegistered = true;
        }
        else
            sendUpdate();
    }
}
