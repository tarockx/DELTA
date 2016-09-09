package unipd.elia.delta.logsubstrate.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToLoadException;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.MathHelpers;
import unipd.elia.delta.sharedlib.MathHelpers.ExperimentCycles;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 16/04/2015.
 */
public class ExperimentHelper {

    //Configuration and data for the currently running experiment
    private static HashMap<PluginConfiguration, IDeltaEventPlugin> EventPlugins;
    private static HashMap<PluginConfiguration, IDeltaPollingPlugin> PollingPlugins;
    public static ExperimentCycles strictExperimentCycles;
    public static ExperimentCycles sporadicExperimentCycles;

    public static Map<PluginConfiguration, IDeltaEventPlugin> getEventPlugins() {
        return EventPlugins == null ? null : Collections.unmodifiableMap(EventPlugins);
    }

    public static Map<PluginConfiguration, IDeltaPollingPlugin> getPollingPlugins() {
        return PollingPlugins == null ? null : Collections.unmodifiableMap(PollingPlugins);
    }

    public static void setupExperiment(ExperimentConfiguration experimentConfiguration) throws PluginFailedToLoadException {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Configuring experiment: " + experimentConfiguration.ExperimentName);

        EventPlugins = new HashMap<>();
        PollingPlugins = new HashMap<>();

        for (Map.Entry<String, List<PluginConfiguration>> entry : experimentConfiguration.Plugins.entrySet())
        {
           for(PluginConfiguration pc : entry.getValue()) {
               if (pc.IsEnabled) {
                   IDeltaPlugin dp = loadPlugin(pc.PluginClassQualifiedName);
                   if (dp != null) {
                       if (IDeltaEventPlugin.class.isInstance(dp)) {
                           EventPlugins.put(pc, (IDeltaEventPlugin) dp);

                           Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Successfully loaded EventPlugin: " + pc.PluginClassQualifiedName);
                       } else if (IDeltaPollingPlugin.class.isInstance(dp)) {
                           PollingPlugins.put(pc, (IDeltaPollingPlugin) dp);

                           Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Successfully loaded PollingPlugin: " + pc.PluginClassQualifiedName);
                       } else{
                           throw new PluginFailedToLoadException(pc.PluginPackage, "Plugin was loaded, but doesn't implement the required interfaces");
                       }
                   } else {
                       throw new PluginFailedToLoadException(pc.PluginPackage, "Could not load plugin class or instantiate plugin object");
                   }
               }
           }
        }

        recalculateCycles();

        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Experiment configured!! (EventPlugins: " + EventPlugins.size() +
        " - PollingPlugins: " + PollingPlugins.size() + ")");

    }

    private static void recalculateCycles(){
        List<PluginConfiguration> pollingPluginsConfiguration = new LinkedList<>();
       pollingPluginsConfiguration.addAll(PollingPlugins.keySet());

        strictExperimentCycles = MathHelpers.getStrictTimerCycles(pollingPluginsConfiguration);
        sporadicExperimentCycles = MathHelpers.getSporadicCycles(pollingPluginsConfiguration);
    }

    private static IDeltaPlugin loadPlugin(String pluginClass) {
        try {
            Class classToInvestigate = Class.forName(pluginClass);
            Object pluginInstance = classToInvestigate.newInstance();
            return  (IDeltaPlugin)pluginInstance;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void removePluginsFromCurrentExperiment(List<PluginConfiguration> plugins){
        boolean removedPollingPlugin = false;
        for(PluginConfiguration plugin : plugins){
            if(PollingPlugins.containsKey(plugin)) {
                PollingPlugins.remove(plugin);
                removedPollingPlugin = true;
            }
            else if(EventPlugins.containsKey(plugin)) {
                EventPlugins.remove(plugin);
            }
        }

        if(removedPollingPlugin)
            recalculateCycles();
    }



    public static void clearExperiment() {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ExperimentHelper: clearing experiment...");

        if(EventPlugins != null)
            EventPlugins.clear();
        if(PollingPlugins != null)
            PollingPlugins.clear();

        strictExperimentCycles = null;
        sporadicExperimentCycles = null;
    }
}
