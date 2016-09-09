package unipd.elia.delta.androidsharedlib;

/**
 * Created by Elia on 05/05/2015.
 */
public class Constants {

    //Intent extras tags
    public static final String REQUEST_ACTION = "RequestedAction";
    public static final String EXPERIMENT_CONFIGURATION = "ExperimentConfiguration";
    public static final String EXPERIMENT_WRAPPER = "ExperimentWrapper";
    public static final String DELTA_UPLOAD_SERVER = "DeltaUploadServer";
    public static final String EXPERIMENT_PACKAGE_ID = "ExperimentPackageID";
    public static final String DUMP_LOGS_DIRECTORY = "DumpLogsDirectory";
    public static final String CLEAR_LOGS_AFTER_DUMP = "ClearLogsAfterDump";

    //DeltaService Intent REQUEST_ACTION codes
    public static final int DELTASERVICE_STARTLOGGING = 1000;
    public static final int DELTASERVICE_STOPLOGGING = 1001;
    public static final int DELTASERVICE_BIND_CORE = 1003;
    public static final int DELTASERVICE_UPLOAD_LOGS = 1004;
    public static final int DELTASERVICE_DUMP_LOGS = 1005;

    //Messaging "whats"
    public static final int MESSAGE_REGISTER_CLIENT = 1000;
    public static final int MESSAGE_STOP_LOGGING = 1001;
    public static final int MESSAGE_LOGGING_HAS_STOPPED = 2001;
    public static final int MESSAGE_LOGGING_HAS_STARTED = 2002;

    //IDs and tags
    public static final String DEBUGTAG_DELTALOGSUBSTRATE = "Delta.Logsubstrate";
    public static final String DEBUGTAG_DELTAAPP = "Delta.App";

    //Preferences Keys
    public static final String PREF_STARTED_EXPERIMENTS = "started_experiments";
    public static final String PREFEREFNCES_FILE_ID_DELTACORE = "deltacore_prefs";

    //Delta Web Services methods and addresses
    public static final String DWS_METHOD_UPLOADLOGS = "uploadLogData";
    public static final String DWS_METHOD_GETAVAILABLEEXPERIMENTS = "getAvailableExperiments";
    public static final String DWS_METHOD_DOWNLOAD_EXPERIMENT = "downloadExperiment";
    public static final String DWS_NAMESPACE = "http://webservices.delta.elia.unipd/";
    public static final String DWS_SOAP_ACTION = "http://webservices.delta.elia.unipd/uploadLogData";
}
