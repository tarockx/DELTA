package unipd.elia.delta.plugins.systemtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.json.JSONObject;

import java.util.Calendar;

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
@DeltaPluginMetadata(PluginName = "Clock and Timezone changes monitor",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs whenever the device's clock or timezone settings are changed",
        DeveloperDescription = "An entry is made every time the user or an application modifies the system clock. " +
                "An entry is also generated when the timezone setting is changed.")
public class DateTimeChangesLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myTimeBroadcastReceiver;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myTimeBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportTimeChange(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }
    }

    private void reportTimeChange(Intent intent) {
        JSONObject obj = new JSONObject();
        try{
            switch (intent.getAction()){
                case Intent.ACTION_TIME_CHANGED :
                    obj.put("event", "clock_changed");
                    break;
                case Intent.ACTION_TIMEZONE_CHANGED :
                    obj.put("event", "timezone_changed");
                    break;
            }
            Calendar calendar = Calendar.getInstance();
            obj.put("current_timezone", calendar.getTimeZone().getID());
            obj.put("current_time", calendar.getTime().toString());

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj.toString()));
        }
        catch (Exception ex){}



    }

    @Override
    public void Terminate() {
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED));
            context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_CHANGED));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myTimeBroadcastReceiver);
    }
}
