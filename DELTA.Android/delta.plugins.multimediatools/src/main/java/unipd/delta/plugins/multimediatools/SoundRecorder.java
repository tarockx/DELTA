package unipd.delta.plugins.multimediatools;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 21/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Audio Recorder",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Continuously records audio from the microphone.",
        DeveloperDescription = "This plugin records all audio from the device microphone. WARNING: file size can get quite big (~2.5mb per minute at the maximum quality). " +
                "Also note that this will take exclusive control of the microphone, other apps will not be able to access it while the experiment is running.\n" +
                "Audio will be saved as raw PCM bytes. To turn it into a usable wave file you will have to convert it, for example with the following ffmpeg command-line: " +
                "\"ffmpeg -f s<bits>le -ar <sample_rate> -ac 1 -i input.bin output.wav\" (or any other tool of your choice)"
)
@DeltaOptions(
        StringOptions = {
                @DeltaStringOption(ID="SAMPLE_RATE", Name = "Sampling Rate (hz)", defaultValue = "44100",
                        Description = "The sampling rate, in Hz (how many audio samples are captured per second). Higher values will result in higher quality but bigger files. NOTE: according to the official Android documentation 44100 is the only rate guaranteed to work on all devices",
                        AvailableChoices = {"44100", "32000", "22050", "16000", "11025", "8000"}),
                @DeltaStringOption(ID="AUDIO_FORMAT", Name = "Bit depth", defaultValue = "PCM_16bit",
                        Description = "The bit depth of the audio stream (how many bits are used to encode each audio sample). 16 bit format gives better quality, but will take up twice as much storage space as the 8 bit format",
                        AvailableChoices = {"PCM_16bit", "PCM_8bit"})
        }
)
public class SoundRecorder implements IDeltaPlugin, IDeltaEventPlugin {
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

        try {
            int sampleRate = Integer.parseInt(pluginConfiguration.getStringOption("SAMPLE_RATE").Value);
            String formatString = pluginConfiguration.getStringOption("AUDIO_FORMAT").Value;
            int format;
            if(formatString.equals("PCM_16bit"))
                format = AudioFormat.ENCODING_PCM_16BIT;
            else
                format = AudioFormat.ENCODING_PCM_8BIT;

            minSize= AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, format);
            myAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, format, minSize);

        }catch (Exception ex){
            throw new PluginFailedToInitializeException(this, "Bad configuration: parameters missing or invalid");
        }

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

    private void storeAudio() {
        while (myAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
            try {
                byte[] buffer = new byte[minSize];
                int result = myAudioRecorder.read(buffer, 0, minSize);
                if(result != AudioRecord.ERROR_BAD_VALUE && result != AudioRecord.ERROR_INVALID_OPERATION) {
                    myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, buffer));
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        myAudioRecorder.startRecording();
        if(myAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
            throw new PluginFailedToStartLoggingException(this, "Failed to obtain access to the microphone.");
        }

        //start storing audio data (on separate thread, or the rest of the experiment will be blocked)
        Runnable r = new Runnable() {
            @Override
            public void run() {
                storeAudio();
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void StopLogging() {
        if(myAudioRecorder != null)
            myAudioRecorder.stop();
    }
}
