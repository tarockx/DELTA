package unipd.elia.delta.logsubstrate;

import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.logsubstrate.helpers.DELTAUtils;
import unipd.elia.delta.logsubstrate.helpers.DeltaServiceHelper;
import unipd.elia.delta.logsubstrate.helpers.IOHelpers;


/**
 * Created by Elia on 04/06/2015.
 */
public class LogsUploader {
    private static Lock lock = new ReentrantLock();
    private static int uploadNotificationId = 22;

    private static boolean isConnectedToWiFi(){
        Context context = DELTAUtils.context;
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnected()) //We are connected... but to wifi?
            return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        else
            return false;
    }

    public static void uploadPendingLogs(String serverUrl){
        //abort if another upload is already in progress
        if(!lock.tryLock()){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "An upload operation is already in progress. Skipping this one.");
            return;
        }

        try{
            List<File> logFiles = IOHelpers.getLogFiles();
            String android_id = Settings.Secure.getString(DELTAUtils.context.getContentResolver(), Settings.Secure.ANDROID_ID);
            String experimentID = DELTAUtils.context.getPackageName();

            int consecutiveFails = 0;
            for(File logFile : logFiles){
                notifyUploadProgress(logFiles.size(), logFiles.indexOf(logFile));
                if(isConnectedToWiFi()){ //only upload if connected to wifi, to save precious mobile data :)
                    //Perform upload
                    if(DeltaServiceHelper.uploadLog(android_id, experimentID, serverUrl, logFile)) {
                        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Successfully uploaded log file: " + logFile.getName());
                        logFile.delete();
                    }
                    else {
                        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to upload file: " + logFile.getName());
                        consecutiveFails++;
                        if(consecutiveFails == 3) //3 consecutive failed uploads, server or connection problem probably occurring, will retry the next time
                            break;
                    }
                }
                else {
                    //No connection to WiFi, aborting operation
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "No WiFi connection available to upload logs. Will retry later...");
                    break;
                }
            }
        } finally {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Log upload operation complete!");
            dismissUploadProgressNotification();
            lock.unlock();
        }

    }

    private static void notifyUploadProgress(int total, int progress){
        NotificationManager myNotifyManager = (NotificationManager) DELTAUtils.context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(DELTAUtils.context);
        myBuilder.setContentTitle("DELTA: Log data is being uploaded...")
                .setContentText("File " + progress + " of " + total)
                .setSmallIcon(R.drawable.notification_icon_1);
        myBuilder.setProgress(total, progress, false);
        // Displays the progress bar for the first time.
        myNotifyManager.notify(uploadNotificationId, myBuilder.build());
    }

    private static void dismissUploadProgressNotification(){
        NotificationManager myNotifyManager = (NotificationManager) DELTAUtils.context.getSystemService(Context.NOTIFICATION_SERVICE);
        myNotifyManager.cancel(uploadNotificationId);
    }
}
