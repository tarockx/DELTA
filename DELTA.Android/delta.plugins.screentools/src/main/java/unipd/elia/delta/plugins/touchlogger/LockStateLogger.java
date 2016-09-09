package unipd.elia.delta.plugins.touchlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 29/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Screen State logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Reports when the device screen is turned on/off. Additionally, it also reports when the user unlocks the device",
        DeveloperDescription = "A log entry is added every time that: (a) the screen turns on, (b) the screen turns off, " +
                "(c) the user unlocks the device (ie: when the keyguard is gone).")
public class LockStateLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myLockStatusBroadcastReceiver;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myLockStatusBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportLockStatus(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void reportLockStatus(Intent intent){

        try{
            String event = "unknown";
            switch (intent.getAction()){
                case Intent.ACTION_SCREEN_ON :
                    event = "screen_on";
                    break;
                case Intent.ACTION_SCREEN_OFF :
                    event = "screen_off";
                    break;
                case Intent.ACTION_USER_PRESENT :
                    event = "screen_unlock";
                    break;
            }
            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), "{\"event\":\"" + event + "\"}"));

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myLockStatusBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.registerReceiver(myLockStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
            context.registerReceiver(myLockStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            context.registerReceiver(myLockStatusBroadcastReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myLockStatusBroadcastReceiver);
    }
}
