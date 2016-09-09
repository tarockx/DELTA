package unipd.delta.plugins.telephonytools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;

import org.json.JSONException;
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
 * Created by Elia on 06/07/2015.
 */

@DeltaPluginMetadata(PluginName = "Phone Calls logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs every incoming and outgoing phone call, storing information like duration and call type (outgoing/incoming). " +
                "A caller ID will also be stored, which allows the experiment creator to identify calls from the same caller. " +
                "Note, however, that it will NOT disclose any phone number (values are hashed). This will also NOT record the audio of your phone calls.",
        DeveloperDescription = "A new entry will be logged every time the state of the phone changes (idle, ringing or off_hook)." +
                "Entries will include a timestamp, type of call (incoming or outgoing) and a hash of the caller phone number for unique identification of callers. " +
                "For privacy reasons, the phone number itself is not included.")
public class PhoneCallsLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    BroadcastReceiver myPhoneCallsBroadcastReceiver;
    int nullValue = -1;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myPhoneCallsBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportBatteryStatus(intent);
                }
            };
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }

    private void reportBatteryStatus(Intent intent){

        JSONObject object = new JSONObject();
        try {
            if(intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                if (intent.hasExtra(TelephonyManager.EXTRA_STATE)) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state.equals(TelephonyManager.EXTRA_STATE_IDLE))
                        object.put("phone_state", "idle");
                    else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING))
                        object.put("phone_state", "ringing");
                    else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
                        object.put("phone_state", "off_hook");
                    else
                        object.put("phone_state", "unknown");
                } else {
                    object.put("phone_state", "unknown");
                }

                if(intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)){
                    String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    object.put("caller_id_hash", number.hashCode());
                }
                else {
                    object.put("caller_id_hash", "N/A");
                }

            }

            //report
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), object));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myPhoneCallsBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.registerReceiver(myPhoneCallsBroadcastReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
        }catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, ex.getMessage());
        }

    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myPhoneCallsBroadcastReceiver);
    }
}