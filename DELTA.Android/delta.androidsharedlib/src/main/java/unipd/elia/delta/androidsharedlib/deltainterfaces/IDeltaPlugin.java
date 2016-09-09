package unipd.elia.delta.androidsharedlib.deltainterfaces;

import android.content.Context;

import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 13/04/2015.
 */
public interface IDeltaPlugin {

    /**
     * Will be called when the experiment starts and after the plugin instance is created.
     * Do any initialization here, and remember to keep a reference to the UpdateListener and Context (if you need it).
     * If you need to do some operation here that requires user input (i.e: requesting ROOT, showing a popup with a question, etc.) do it here,
     * but remember to BLOCK the call until you're done (otherwise the experiment might start while your plugin is not ready)
     * @param androidContext A copy of the Context. This will allow you to call Android APIs.
     * @param deltaPluginUpdateListener The instance of the logging service, to which you report updates.
     * @param pluginConfiguration configuration parameters for this plugin (includes Options and other config values)
     * @throws PluginFailedToInitializeException, FailSilentlyException
     */
    void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException;

    /**
     * Called when the experiment ends. In this method you should clean up any extra files you left in the system and release all resources you were using.
     */
    void Terminate();
}
