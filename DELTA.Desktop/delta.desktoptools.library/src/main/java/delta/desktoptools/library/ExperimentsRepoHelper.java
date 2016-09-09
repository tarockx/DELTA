package delta.desktoptools.library;

import org.apache.commons.io.IOUtils;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Elia on 08/06/2015.
 */
public class ExperimentsRepoHelper {
    public static List<ExperimentConfiguration> getAllExperimentsInRepo(){
        File repoDir = new File(SettingsHelper.PATH_EXPERIMENTS_REPO);
        if(!repoDir.exists())
            return null;

        File[] files = repoDir.listFiles();
        if(files == null)
            return  null;

        List<ExperimentConfiguration> experimentConfigurations = new LinkedList<>();
        for(File file : files){
            try{
                ZipFile zipFile = new ZipFile(file);
                ZipEntry configurationEntry = zipFile.getEntry("configuration.xml");
                InputStream stream = zipFile.getInputStream(configurationEntry);
                ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(stream);
                stream.close();
                zipFile.close();
                if(experimentConfiguration != null)
                    experimentConfigurations.add(experimentConfiguration);
            }catch (Exception ex){
            }
        }
        return experimentConfigurations;
    }

    public static List<String> getAllExperimentsInRepoRaw(){
        File repoDir = new File(SettingsHelper.PATH_EXPERIMENTS_REPO);
        if(!repoDir.exists())
            return null;

        File[] files = repoDir.listFiles();
        if(files == null)
            return  null;

        List<String> experimentConfigurations = new LinkedList<>();
        for(File file : files){
            try{
                ZipFile zipFile = new ZipFile(file);
                ZipEntry configurationEntry = zipFile.getEntry("configuration.xml");
                InputStream stream = zipFile.getInputStream(configurationEntry);
                String experimentRawString = IOUtils.toString(stream);
                stream.close();
                zipFile.close();
                if(experimentRawString != null && !experimentRawString.isEmpty())
                    experimentConfigurations.add(experimentRawString);
            }catch (Exception ex){
            }
        }
        return experimentConfigurations;
    }

   public static byte[] getExperiment(String packageID){
        File repoDir = new File(SettingsHelper.PATH_EXPERIMENTS_REPO);

        if(!repoDir.exists())
            return null;

        File[] files = repoDir.listFiles();
        if(files == null)
            return  null;

        for(File file : files){
            try{
                ZipFile zipFile = new ZipFile(file);
                ZipEntry configurationEntry = zipFile.getEntry("configuration.xml");
                InputStream stream = zipFile.getInputStream(configurationEntry);
                ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(stream);
                stream.close();
                zipFile.close();
                if(experimentConfiguration != null && experimentConfiguration.ExperimentPackage.equals(packageID)){
                    InputStream is = new FileInputStream(file);
                    byte[] res = IOUtils.toByteArray(is);
                    is.close();
                    return  res;
                }
            }catch (Exception ex){
            }
        }
        return null;
    }

}
