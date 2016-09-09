package unipd.elia.deltacore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Set;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.deltacore.helpers.DELTAUtils;
import unipd.elia.deltacore.helpers.ExperimentHelpers;
import unipd.elia.deltacore.helpers.SettingsHelpers;

/**
 * Created by Elia on 03/06/2015.
 */
public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        DELTAUtils.context = context;

        restartExperiments(context);

        UploadSchedulerBroadcastReceiver.setupAlarm(context);
    }

    private void restartExperiments(Context context){
         Set<String> experiments = SettingsHelpers.getStartedExperiments(context);
        if(experiments != null){
            for(String experimentPackageID : experiments){
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Restarting previously started experiment: " + experimentPackageID);
                ExperimentHelpers.startExperiment(context, experimentPackageID);
            }
        }
    }
}
