package unipd.elia.delta.androidsharedlib.deltainterfaces;

import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;

/**
 * Created by Elia on 24/04/2015.
 */
public interface IDeltaEventPlugin extends IDeltaPlugin {
    /**
     * This method is called when the experiment logging phase begins, but always after the Initialize() method has been called.
     * You should start the actual logging operations here (for example: register a BroadcastReceiver to start receiving events). This method must be non blocking!
     * @throws PluginFailedToStartLoggingException if the logging operations failed to start. In this case the experiment is aborted, unless the plugin is marked as Optional
     */
    void StartLogging() throws PluginFailedToStartLoggingException;

    /**
     * This method is called when the experiment is stopping logging operations.
     * From the moment this is called, you should stop any logging operations and stop sending log Updates to the DeltaService.
     * You should also unregister listeners and receivers you have previously hooked up in the StartLogging method.
     */
    void StopLogging();
}
