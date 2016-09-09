package unipd.elia.delta.androidsharedlib.exceptions;

import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;

/**
 * Created by Elia on 18/06/2015.
 */
public class PluginFailedToStartLoggingException extends Exception {
    private IDeltaPlugin faultyPlugin;
    private String reason;

    public PluginFailedToStartLoggingException(IDeltaPlugin plugin, String reason){
        faultyPlugin = plugin;
        this.reason = reason;
    }

    public IDeltaPlugin getPlugin(){
        return faultyPlugin;
    }

    @Override
    public String getMessage(){
        return reason;
    }
}
