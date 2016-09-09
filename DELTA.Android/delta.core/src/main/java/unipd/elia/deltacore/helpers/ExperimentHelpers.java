package unipd.elia.deltacore.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.ExperimentStoreEntry;

/**
 * Created by Elia on 03/06/2015.
 */
public class ExperimentHelpers {
    public static boolean startExperiment(Context context, String experimentPackageID){
        ExperimentStoreEntry experimentStoreEntry = PackageHelper.getExperimentFromStore(experimentPackageID);
        if(experimentStoreEntry == null)
            return false;

        ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(experimentStoreEntry.getConfigurationPath());
        if(experimentConfiguration == null)
            return false;

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(experimentConfiguration.ExperimentPackage, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_STARTLOGGING);
        intent.putExtra(Constants.EXPERIMENT_CONFIGURATION, experimentConfiguration);

        context.startService(intent);
        return true;
    }


    public static boolean sendUploadCommand(Context context, String experimentPackageID, String uploadServerUrl){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(experimentPackageID, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_UPLOAD_LOGS);
        intent.putExtra(Constants.DELTA_UPLOAD_SERVER, uploadServerUrl);

        context.startService(intent);
        return true;
    }

    public static boolean sendDumpLogsCommand(Context context, String experimentPackageID, String directory, boolean clearAfterDump){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(experimentPackageID, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_DUMP_LOGS);
        intent.putExtra(Constants.DUMP_LOGS_DIRECTORY, directory);
        intent.putExtra(Constants.CLEAR_LOGS_AFTER_DUMP, clearAfterDump);

        context.startService(intent);
        return true;
    }
}
