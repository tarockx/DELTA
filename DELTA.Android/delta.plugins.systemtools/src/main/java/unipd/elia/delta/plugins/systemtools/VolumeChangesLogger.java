package unipd.elia.delta.plugins.systemtools;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

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
 * Created by Elia on 22/07/2015.
 */
@DeltaPluginMetadata(PluginName = "Volume levels monitor",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Monitors changes in the volume levels of the various audio streams of the device (music volume, ringer volume, alarm clock volume, etc.)",
        DeveloperDescription = "An entry is made every time a volume level changes. The entry includes the type " +
                "(Alarm, DTMF, Media, Notifications, Ringtone, System_Sounds, Phone_Call) and the old and new volume levels")
public class VolumeChangesLogger implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;
    SettingsContentObserver mySettingsContentObserver;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        mySettingsContentObserver = new SettingsContentObserver(context, new Handler(Looper.getMainLooper()));
    }

    @Override
    public void Terminate() {

    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        try {
            context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mySettingsContentObserver );
        } catch (Exception ex){
            throw new PluginFailedToStartLoggingException(this, "Failed to register content observer: " + ex.getMessage());
        }
    }

    @Override
    public void StopLogging() {
        context.getContentResolver().unregisterContentObserver(mySettingsContentObserver);
    }

    private void reportVolumeChange(int oldLevel, int newLevel, String streamName) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("event", oldLevel > newLevel ? "volume_decrease" : "volume_increase");
            obj.put("stream", streamName);
            obj.put("old_volume", oldLevel);
            obj.put("new_volume", newLevel);

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Observer to listen for volume changes
     */
    private class SettingsContentObserver extends ContentObserver {
        Context context;
        AudioManager myAudioManager;

        int[] streams = {
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_DTMF,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_VOICE_CALL
        };
        String[] streamNames ={
                "Alarm",
                "DTMF",
                "Media",
                "Notifications",
                "Ringtone",
                "System_Sounds",
                "Phone_Call"
        };
        int[] levels = new int[streams.length];

        public SettingsContentObserver(Context c, Handler handler) {
            super(handler);
            context=c;
            myAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            for(int i = 0; i < streams.length; i++){
                levels[i] = myAudioManager.getStreamVolume(streams[i]);
            }
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            for(int i = 0; i < streams.length; i++){
                int level = myAudioManager.getStreamVolume(streams[i]);
                if(level != levels[i]){ //level changed
                    reportVolumeChange(levels[i], level, streamNames[i]); //old level, new level, stream name
                    levels[i] = level;
                }
            }
        }
    }


}
