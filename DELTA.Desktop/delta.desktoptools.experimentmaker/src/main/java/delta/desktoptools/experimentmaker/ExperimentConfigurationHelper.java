package delta.desktoptools.experimentmaker;

import delta.desktoptools.library.DeltaManifestHelper;
import delta.desktoptools.library.IOHelpers;
import delta.desktoptools.library.PluginClassParser;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.PluginConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by Elia on 20/05/2015.
 */
public class ExperimentConfigurationHelper {
    public static ExperimentConfiguration currentExperimentConfiguration = new ExperimentConfiguration();

    public static Map<String, List<PluginConfiguration>> getAvailablePlugins(String path){
        //Parse directories in search of plugin projects
        if(path == null)
            return null;
        File pluginsDir = new File(path);
        if(!pluginsDir.exists())
            return null;

        Map<String, List<File>> allAndroidProjects = IOHelpers.getAllAndroidProjects(path);
        Map<String, List<PluginConfiguration>> pluginClasses = new HashMap<>();

        for(Map.Entry<String, List<File>> entry : allAndroidProjects.entrySet()){
            String packageID = DeltaManifestHelper.getPackageIdFromManifest(entry.getKey());
            PluginClassParser p = new PluginClassParser();
            List<PluginConfiguration> parseResult = p.Parse(entry.getValue());
            for(PluginConfiguration deltaPluginClass : parseResult)
                deltaPluginClass.PluginPackage = packageID;
            if(parseResult.size() > 0)
                pluginClasses.put(packageID, parseResult);
        }

        return pluginClasses;
    }

    public static Map<String, List<PluginConfiguration>> getAvailableExternalPlugins(String path){
        //Parse directories in search of plugin projects
        if(path == null)
            return null;
        File pluginsDir = new File(path);
        if(!pluginsDir.exists())
            return null;

        List<File> aarFiles = IOHelpers.getAllFilesInFolderByExtension(pluginsDir, "aar", false);
        Map<String, List<PluginConfiguration>> pluginClasses = new HashMap<>();

        for(File aarFile : aarFiles){
            try{
                ZipFile zipFile = new ZipFile(aarFile);
                ZipEntry deltaManifestEntry = zipFile.getEntry("assets/DeltaManifest.xml");
                ZipEntry androidManifestEntry = zipFile.getEntry("AndroidManifest.xml");
                if(deltaManifestEntry != null && androidManifestEntry != null){
                    InputStream deltaInputStream = zipFile.getInputStream(deltaManifestEntry);
                    List<PluginConfiguration> pluginConfigurations = DeltaManifestHelper.readPluginManifest(deltaInputStream);
                    if(pluginConfigurations != null){
                        String packageID = DeltaManifestHelper.getPackageIdFromManifest(zipFile.getInputStream(androidManifestEntry));
                        for(PluginConfiguration deltaPluginClass : pluginConfigurations)
                            deltaPluginClass.PluginPackage = packageID;
                        pluginClasses.put(packageID, pluginConfigurations);
                    }
                }
            }catch (Exception ex){
                continue;
            }
        }

        return pluginClasses;
    }

    public static boolean CompileExperiment(String outputPath, String deltaAndroidProjectRootPath){
        return CompileExperiment(currentExperimentConfiguration, outputPath, deltaAndroidProjectRootPath);
    }

    public static boolean CompileExperiment(ExperimentConfiguration experimentConfiguration, String outputPath, String deltaAndroidProjectRootPath){
        if(!experimentConfiguration.ValidateConfiguration() || !experimentConfiguration.ValidatePluginsConfiguration())
            return false;

        return true;
    }

    public static boolean isExperimentWakelocking(){
        for(PluginConfiguration pluginConfiguration : currentExperimentConfiguration.getAllPluginConfigurations(false)){
            if(pluginConfiguration.RequiresWakelock)
                return true;
        }
        return false;
    }
}
