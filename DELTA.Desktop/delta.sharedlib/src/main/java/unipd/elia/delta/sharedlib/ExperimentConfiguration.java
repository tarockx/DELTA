package unipd.elia.delta.sharedlib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Elia on 07/05/2015.
 */
public class ExperimentConfiguration implements java.io.Serializable {
    private static final long serialVersionUID = 99901L;

    public String ExperimentName;
    public String ExperimentAuthor;
    public String ExperimentPackage;
    public String ExperimentDescription;
    public String DeltaServerUrl;
    public boolean SuspendOnScreenOff;

    public Map<String, List<PluginConfiguration>> Plugins = new HashMap<String, List<PluginConfiguration>>();

    public void AddPlugin(PluginConfiguration pluginConfiguration){
        if(!Plugins.containsKey(pluginConfiguration.PluginPackage))
            Plugins.put(pluginConfiguration.PluginPackage, new LinkedList<PluginConfiguration>());

        Plugins.get(pluginConfiguration.PluginPackage).add(pluginConfiguration);
    }

    public void RemovePlugin(PluginConfiguration pluginConfiguration) {
        RemovePlugin(pluginConfiguration.PluginPackage, pluginConfiguration.PluginClassQualifiedName);
    }

    public void RemovePlugin(String pluginPackage, String pluginClassQualifiedName){
        if(Plugins.containsKey(pluginPackage)){
            List<PluginConfiguration> pluginConfigurations = Plugins.get(pluginPackage);
            for(int i = 0; i < pluginConfigurations.size(); i++){
                if(pluginConfigurations.get(i).PluginClassQualifiedName.equals(pluginClassQualifiedName)){
                    pluginConfigurations.remove(i);
                    break;
                }
            }

            if(pluginConfigurations.size() == 0)
                Plugins.remove(pluginPackage);
        }
    }

    public List<PluginConfiguration> getAllPluginConfigurations(boolean pollingOnly){
        List<PluginConfiguration> plugins = new LinkedList<>();
        for(List<PluginConfiguration> pluginsInPackage : Plugins.values())
            for (PluginConfiguration pluginConfiguration : pluginsInPackage)
                if(!pollingOnly || pluginConfiguration.PollingFrequency > 0)
                    plugins.add(pluginConfiguration);

        return plugins;
    }

    public boolean ValidateConfiguration(){
        boolean isValid = true;

        Pattern packagePattern = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$");
        Matcher packageMatcher = packagePattern.matcher(ExperimentPackage);

        isValid = packageMatcher.find();
        isValid = isValid && ExperimentName != null && !ExperimentName.isEmpty();
        isValid = isValid && ExperimentAuthor != null && !ExperimentAuthor.isEmpty();
        isValid = isValid && ExperimentDescription != null && !ExperimentDescription.isEmpty();

        return  isValid;
    }

    public boolean ValidatePluginsConfiguration(){
        boolean isValid = false;
        for(PluginConfiguration pluginConfiguration : getAllPluginConfigurations(false)){
            if(!pluginConfiguration.AllowOptOut) {
                isValid = true;
                break;
            }
        }
        return isValid;
    }

    public int getMinSDK(){
        int minSdk = 0;
        for(PluginConfiguration pluginConfiguration : getAllPluginConfigurations(false)){
            if(minSdk == 0 || minSdk < pluginConfiguration.MinSDK)
                minSdk = pluginConfiguration.MinSDK;
        }
        return minSdk;
    }

}
