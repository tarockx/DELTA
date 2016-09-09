package unipd.elia.delta.logsubstrate.helpers;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;

/**
 * Created by Elia on 15/04/2015.
 */
public class IOHelpers {

    public static String getPrivateStoragePath(){
        Context context = DELTAUtils.context;

        return context.getFilesDir().getAbsolutePath();
    }

    public static String createOrGetExperimentFolder(){
        File baseDir = new File(getPrivateStoragePath(), "log_cache");
        if(!baseDir.exists())
            baseDir.mkdir();

        return baseDir.getAbsolutePath();
    }

    public static String createOrGetRawLogsFolder(){
        File baseDir = new File(getPrivateStoragePath(), "raw_logs");
        if(!baseDir.exists())
            baseDir.mkdir();

        return baseDir.getAbsolutePath();
    }

    public static List<File> getLogFiles(){
        File baseDir = new File(createOrGetExperimentFolder());
        List<File> result = new LinkedList<>();
        for(File file : baseDir.listFiles())
            if(!file.isDirectory())
                result.add(file);

        return result;
    }

    public static List<File> getRawLogFiles(){
        File baseDir = new File(createOrGetRawLogsFolder());
        List<File> result = new LinkedList<>();
        for(File file : baseDir.listFiles())
            if(!file.isDirectory())
                result.add(file);

        return result;
    }

    public static void saveExperimentConfigurationToDisk(ExperimentConfiguration experimentConfiguration){
        try
        {
            ExperimentConfigurationIOHelpers.SerializeExperimentConfiguration(experimentConfiguration, getPrivateStoragePath() + "/CurrentConfiguration.xml");
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Current experiment configuration serialized to disk!");
        }catch(Exception i)
        {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ERROR: Failed to serialize experiment to disk! If the logging service is killed it won't be able to restart by itself!");
            i.printStackTrace();
        }
    }

    public static ExperimentConfiguration loadSavedExperimentConfigurationFromDisk(){
        ExperimentConfiguration experimentConfiguration = null;
        try
        {
            experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(getPrivateStoragePath() + "/CurrentConfiguration.xml");
        } catch(Exception c)
        {
            c.printStackTrace();
        }
        if(experimentConfiguration != null)
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Experiment succesfully reloaded from disk!");
        else {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ERROR: Failed to reload experiment from disk. File corrupted or inaccessible. Cannote resume logging automatically!");
        }

        return experimentConfiguration;
    }

    public static boolean copy(File oldFile, File newFile){
        if(!oldFile.exists())
            return false;

        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(oldFile);
            output = new FileOutputStream(newFile, false); //overwrite
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
            input.close();
            output.close();
            return true;
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    public static String inflateRawResourceToInternalStorageCache(int resourceID, String outputFileName){
        Context context = DELTAUtils.context;
        try {
            File cachedir = new File(getPrivateStoragePath());
            if(!(cachedir != null && cachedir.exists() && cachedir.isDirectory()))
                cachedir.mkdir();

            InputStream is = context.getResources().openRawResource(resourceID);
            File outputFile = new File(cachedir.getAbsolutePath(), outputFileName);
            OutputStream os = new FileOutputStream(outputFile);

            //copying data
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            //file copied
            os.close();
            is.close();
            return  cachedir + "/" + outputFileName;
        }
        catch (Exception e){
            e.printStackTrace();
            return  null;
        }

    }

}
