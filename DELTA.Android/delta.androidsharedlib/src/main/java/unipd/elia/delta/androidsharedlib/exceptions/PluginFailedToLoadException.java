package unipd.elia.delta.androidsharedlib.exceptions;

/**
 * Created by Elia on 22/06/2015.
 */
public class PluginFailedToLoadException extends Exception {
    private String faultyPluginID;
    private String reason;

    public PluginFailedToLoadException(String faultyPluginID, String reason){
        this.faultyPluginID = faultyPluginID;
        this.reason = reason;
    }

    public String getFaultyPluginID(){
        return faultyPluginID;
    }

    @Override
    public String getMessage(){
        return reason;
    }
}
