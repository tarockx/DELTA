package delta.desktoptools.library;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Created by Elia on 03/06/2015.
 */
public class SettingsHelper {
    //Generic path settings
    public static String PATH_LOG_REPOSITORY;
    public static String PATH_ANDROID_PROJECT;
    public static String PATH_EXPERIMENTS_REPO;
    public static String PATH_EXTERNAL_PLUGINS;

    //DeltaWebServer specific settings
    public static String DELTA_SERVICE_ENDPOINT;

    public static void loadSettings(File ini) throws Exception {
        Wini prefs = new Wini(ini);

        //Logs directory
        String logsDirPath = prefs.get("Paths", "LogRepositoryDir");
        File logsDir = new File(logsDirPath);
        if(logsDir.exists() || logsDir.mkdirs())
            PATH_LOG_REPOSITORY = logsDir.getCanonicalPath();
        else
            throw new Exception("ERROR: directory for storing log data [" + logsDir.getAbsolutePath() + "] does not exist and cannot be created");

        //Android project directory
        String projectDirPath = prefs.get("Paths", "AndroidProjectDir");
        File projectDir = new File(projectDirPath);
        if(projectDir.exists() && (new File(projectDir, "gradlew")).exists())
            PATH_ANDROID_PROJECT = projectDir.getCanonicalPath();
        else
            throw new Exception("ERROR: directory [" + projectDir.getAbsolutePath() + "] does not exist or doesn't seem to contain the DELTA Android project!");

        //Default dir where DeltaWebServer will look for available experiments
        String exprepoDirPath = prefs.get("Paths", "ExperimentsRepositoryDir");
        File exprepoDir = new File(exprepoDirPath);
        if(exprepoDir.exists() || exprepoDir.mkdirs())
            PATH_EXPERIMENTS_REPO = exprepoDir.getCanonicalPath();
        else
            throw new Exception("ERROR: directory [" + exprepoDir.getAbsolutePath() + "] does not exist and cannot be created!");

        //Default dir where DeltaWebServer will look for available experiments
        String externalPluginsDirPath = prefs.get("Paths", "ExternalPluginsDir");
        File externalPluginsDir = new File(externalPluginsDirPath);
        if(externalPluginsDir.exists())
            PATH_EXTERNAL_PLUGINS = externalPluginsDir.getCanonicalPath();
    }

    public static void loadWebServerSettings(File ini) throws Exception{
        Wini prefs = new Wini(ini);

        //WebServer Endpoint
        String endpoint = prefs.get("DeltaWebServer", "ServiceEndpoint");
        if(endpoint != null || endpoint.isEmpty())
            DELTA_SERVICE_ENDPOINT = endpoint;
        else
            throw new Exception("ERROR: service endpoint [" + endpoint + "] not set in preferences file. Cannot start the DELTA WebService!");
    }
}
