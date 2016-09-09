package unipd.elia.delta.plugins.systemtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.json.JSONObject;

import java.util.Arrays;

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
@DeltaPluginMetadata(PluginName = "Alarm clock logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs whenever the alarm clock goes off",
        DeveloperDescription = "An entry is made every time the an alarm goes off, or if an alarm is snoozed/dismissed. " +
                "NOTE: this is NOT a standardized API, this plugin will only work if the user uses Android's default Alarm Clock. " +
                "Some limited support is present for major manufacturer's Clock apps, such as Samsung, HTC and Sony, " +
                "but you'll have to test on the specific device yourself to check if it works properly. Don't count on it blindly.")
public class AlarmsLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myTimeBroadcastReceiver;

    private final static String[] alert_intents = {
        "com.android.deskclock.ALARM_ALERT",
        //"com.android.deskclock.ALARM_DISMISS",
        //"com.android.deskclock.ALARM_DONE",
        //"com.android.deskclock.ALARM_SNOOZE",

        // Samsung
        "com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT",
        // HTC
        "com.htc.android.worldclock.ALARM_ALERT",
        // Sony
        "com.sonyericsson.alarm.ALARM_ALERT",
        // ZTE
        "zte.com.cn.alarmclock.ALARM_ALERT",
        // Motorola
        "com.motorola.blur.alarmclock.ALARM_ALERT"
    };

    private final static String[] dismiss_intents = {
            "com.android.deskclock.ALARM_DISMISS",
    };

    private final static String[] done_intents = {
            "com.android.deskclock.ALARM_DONE",
    };

    private final static String[] snooze_intents = {
            "com.android.deskclock.ALARM_SNOOZE",
    };



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
                    reportAlarm(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }
    }

    private void reportAlarm(Intent intent) {
        JSONObject obj = new JSONObject();
        String action = intent.getAction();
        try{
            if(Arrays.asList(alert_intents).contains(action))
                obj.put("event", "alarm_ringing");
            if(Arrays.asList(dismiss_intents).contains(action))
                obj.put("event", "alarm_dismissed");
            if(Arrays.asList(done_intents).contains(action))
                obj.put("event", "alarm_done");
            if(Arrays.asList(snooze_intents).contains(action))
                obj.put("event", "alarm_snoozed");

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj.toString()));
        }
        catch (Exception ex){}
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myTimeBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            for(String intent : alert_intents){
                context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(intent));
            }
            for(String intent : dismiss_intents){
                context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(intent));
            }
            for(String intent : done_intents){
                context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(intent));
            }
            for(String intent : snooze_intents){
                context.registerReceiver(myTimeBroadcastReceiver, new IntentFilter(intent));
            }
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myTimeBroadcastReceiver);
    }
}
