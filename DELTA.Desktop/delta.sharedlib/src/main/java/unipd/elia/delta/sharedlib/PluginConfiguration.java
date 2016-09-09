package unipd.elia.delta.sharedlib;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Elia on 07/05/2015.
 */
public class PluginConfiguration implements java.io.Serializable {
    private static final long serialVersionUID = 99000L;

    public String PluginName;
    public String PluginAuthor;
    public String PluginDescription;
    public String DeveloperDescription;

    public String PluginClassQualifiedName;
    public String PluginPackage;
    public int PollingFrequency = -1;
    public int MinPollingFrequency = 0;

    public boolean supportsPolling;
    public boolean supportsEvents;

    public boolean IsEnabled;
    public boolean AllowOptOut;
    public boolean RequiresRoot;
    public boolean RequiresWakelock;

    public int MinSDK = 15;

    public List<DeltaOption> Options;

    public PluginConfiguration(){}

    /**
     * Copy constructor. Will return a deep copy of the input object
     * @param original The original instance you want to deep-copy
     */
    public PluginConfiguration(PluginConfiguration original){
        PluginName = original.PluginName;
        PluginAuthor = original.PluginAuthor;
        PluginDescription = original.PluginDescription;
        DeveloperDescription = original.DeveloperDescription;

        PluginClassQualifiedName = original.PluginClassQualifiedName;
        PluginPackage = original.PluginPackage;
        PollingFrequency = original.PollingFrequency;
        MinPollingFrequency = original.MinPollingFrequency;

        supportsPolling = original.supportsPolling;
        supportsEvents = original.supportsEvents;

        IsEnabled = original.IsEnabled;
        AllowOptOut = original.AllowOptOut;
        RequiresRoot = original.RequiresRoot;
        RequiresWakelock = original.RequiresWakelock;

        if(original.Options != null){
            Options = new LinkedList<>();
            for (DeltaOption option : original.Options){
                Options.add(option.getDeepCopy());
            }
        }
    }

    public StringOption getStringOption(String id){
        if(Options == null || id == null)
            return null;

        for(DeltaOption option : Options){
            if(option.ID.equals(id) && StringOption.class.isInstance(option))
                return (StringOption)option;
        }
        return null;
    }

    public BooleanOption getBooleanOption(String id){
        if(Options == null || id == null)
            return null;

        for(DeltaOption option : Options){
            if(option.ID.equals(id) && BooleanOption.class.isInstance(option))
                return (BooleanOption)option;
        }
        return null;
    }

    public IntegerOption getIntegerOption(String id){
        if(Options == null || id == null)
            return null;

        for(DeltaOption option : Options){
            if(option.ID.equals(id) && IntegerOption.class.isInstance(option))
                return (IntegerOption)option;
        }
        return null;
    }

    public DoubleOption getDoubleOption(String id){
        if(Options == null || id == null)
            return null;

        for(DeltaOption option : Options){
            if(option.ID.equals(id) && DoubleOption.class.isInstance(option))
                return (DoubleOption)option;
        }
        return null;
    }
}
