package unipd.elia.deltacore;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.helpers.DELTAUtils;
import unipd.elia.deltacore.helpers.ExperimentHelpers;
import unipd.elia.deltacore.helpers.PackageHelper;

public class UploadSchedulerBroadcastReceiver extends BroadcastReceiver {
    public static void setupAlarm(Context context){
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UploadSchedulerBroadcastReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                AlarmManager.INTERVAL_HOUR,
                AlarmManager.INTERVAL_HOUR, alarmIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(Constants.DEBUGTAG_DELTAAPP, "Alarm triggered: time to send upload schedule request");
        if(DELTAUtils.context == null)
            DELTAUtils.context = context;

        for (ExperimentStoreEntry experimentStoreEntry : PackageHelper.getInstalledExperiments(context.getPackageManager())) {
            ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(experimentStoreEntry.getConfigurationPath());
            if(experimentConfiguration.DeltaServerUrl != null && !experimentConfiguration.DeltaServerUrl.isEmpty())
                ExperimentHelpers.sendUploadCommand(context, experimentStoreEntry.getPackageId(), experimentConfiguration.DeltaServerUrl);
        }
    }
}
