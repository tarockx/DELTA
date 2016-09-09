package unipd.elia.delta.logsubstrate;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.logsubstrate.helpers.DELTAUtils;
import unipd.elia.delta.logsubstrate.helpers.IOHelpers;

/**
 * Created by Elia on 25/06/2015.
 */
public class LogsDumper {
    private static Lock lock = new ReentrantLock();
    private static int dumpNotificationId = 23;

    public static void dumpPendingLogs(String directory, boolean clearAfter){
        //abort if another dump is already in progress
        if(!lock.tryLock()){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "A dump operation is already in progress, skipping this one.");
            return;
        }

        try{
            List<File> logFiles = IOHelpers.getLogFiles();
            List<File> rawLogFiles = IOHelpers.getRawLogFiles();

            File dumpDir = new File(directory);
            if(dumpDir.isDirectory() || dumpDir.mkdirs()){

                //dump regular log files
                for (File logFile : logFiles) {
                    notifyDumpProgress(logFiles.size(), logFiles.indexOf(logFile));
                    //Perform copy
                    boolean success = IOHelpers.copy(logFile, new File(dumpDir, logFile.getName()));
                    if (success) {
                        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Successfully copied log file: " + logFile.getName());
                        if(clearAfter)
                            logFile.delete();
                    } else {
                        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to copy file: " + logFile.getName());
                    }
                }

                //move raw files
                //TODO
            }
            else {
                Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Could not create output directory, dump operation failed.");
            }
        } finally {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Log dump operation complete!");
            dismissDumpProgressNotification();
            lock.unlock();
        }

    }

    private static void notifyDumpProgress(int total, int progress){
        NotificationManager myNotifyManager = (NotificationManager) DELTAUtils.context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder myBuilder = new NotificationCompat.Builder(DELTAUtils.context);
        myBuilder.setContentTitle("DELTA: Log data is being copied to disk...")
                .setContentText("File " + progress + " of " + total)
                .setSmallIcon(R.drawable.notification_icon_1);
        myBuilder.setProgress(total, progress, false);
        // Displays the progress bar for the first time.
        myNotifyManager.notify(dumpNotificationId, myBuilder.build());
    }

    private static void dismissDumpProgressNotification(){
        NotificationManager myNotifyManager = (NotificationManager) DELTAUtils.context.getSystemService(Context.NOTIFICATION_SERVICE);
        myNotifyManager.cancel(dumpNotificationId);
    }
}
