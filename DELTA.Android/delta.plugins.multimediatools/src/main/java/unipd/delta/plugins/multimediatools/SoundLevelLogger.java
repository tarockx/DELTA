package unipd.delta.plugins.multimediatools;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 21/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Sound Level logger",
        PluginDescription = "Periodically logs the noise level (volume) from the device microphone. No audio data is actually logged.",
        DeveloperDescription = "This plugin will log the noise amplitude from the device's microphone at the specified interval. " +
                "Note that using this plugin requires exclusive access to the microphone. While the microphone is in use by some other app, the plugin will stop reporting updates",
        PluginAuthor = "Elia Dal Santo"
)
public class SoundLevelLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;

    private AudioRecord myAudioRecorder = null;
    private int minSize;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        minSize= AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        myAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,minSize);
        if(myAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
            throw new PluginFailedToInitializeException(this, "Failed to obtain access to the microphone.");
    }

    @Override
    public void Terminate() {
        if (myAudioRecorder != null) {
            myAudioRecorder.stop();
            myAudioRecorder.release();
            myAudioRecorder = null;
        }
    }

    public double getAmplitude() {
        short[] buffer = new short[minSize];
        int result = myAudioRecorder.read(buffer, 0, minSize);
        if(result != AudioRecord.ERROR_BAD_VALUE && result != AudioRecord.ERROR_INVALID_OPERATION) {
            int max = 0;
            for (short s : buffer) {
                if (Math.abs(s) > max) {
                    max = Math.abs(s);
                }
            }
            return max;
        }
        return -1;
    }

    @Override
    public void Poll() {
        myAudioRecorder.startRecording();
        if(myAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            double level = getAmplitude();
            if(level != -1){
                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, "{\"volume\":\"" + level + "\"}"));
            }
        }
        myAudioRecorder.stop();
    }
}
