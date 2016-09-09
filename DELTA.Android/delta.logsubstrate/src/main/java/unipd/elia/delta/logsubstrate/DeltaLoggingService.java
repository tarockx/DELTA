package unipd.elia.delta.logsubstrate;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.ExperimentConfigurationException;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToLoadException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.logsubstrate.helpers.DELTAUtils;
import unipd.elia.delta.logsubstrate.helpers.ExperimentHelper;
import unipd.elia.delta.logsubstrate.helpers.IOHelpers;
import unipd.elia.delta.logsubstrate.helpers.StorageHelper;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.MathHelpers;
import unipd.elia.delta.sharedlib.PluginConfiguration;


/**
 * Created by Elia on 27/04/2015.
 */
public class DeltaLoggingService extends Service implements IDeltaPluginMaster {
    private static int NotificationID = 8424;
    private static NotificationCompat.Builder myNotificationBuilder = null;


    private final Messenger myMessenger = new Messenger(new IncomingHandler());
    private BroadcastReceiver myShutdownReceiver;

    private Map<MathHelpers.CycleValues, Integer> pollCycles = new HashMap<>();
    private Messenger myClientMessenger;
    private PendingIntent pollPendingIntent;
    private BroadcastReceiver myPollingAlarmBroadcastReceiver;
    private Map<MathHelpers.CycleValues, ScheduledThreadPoolExecutor> pollingThreadPoolSchedulers = new HashMap<>();

    private PowerManager.WakeLock myWakeLock;

    private boolean isExperimentRunning = false;
    //private synchronized void setIsExperimentRunning(boolean val){isExperimentRunning = val;}
    //private synchronized boolean getIsExperimentRunning(){return isExperimentRunning;}

    @Override
    public IBinder onBind(Intent intent) {
        processIncomingIntent(intent, 0);
        return myMessenger.getBinder();
    }

    /*
     * INITIALIZATION PHASE (LoadPlugins, setup polling intervals, etc.)
     */
    @Override
    public void onCreate() {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Delta Logging Service : onCreate()");
        super.onCreate();

        DELTAUtils.context = this;
        DELTAUtils.loggingServiceInstance = this;
        Logger.DEBUG = BuildConfig.DEBUG; //Fix for Android bug: libraries will always return BuildConfig.DEBUG = false, even if in debug mode, must be called from an app/service method.
    }

    @Override
    public void onDestroy(){
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Delta Logging Service : onDestroy()");
        super.onDestroy();

        if(isExperimentRunning)
            stopLogging();

        DELTAUtils.context = null;
        DELTAUtils.loggingServiceInstance = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, flags, startId);

        //local intent copy to pass to the inner class (needs to be final)
        final Intent finalIntent = intent;

        //Process start command in another thread (so calling UI isn't blocked)
        Runnable r = new Runnable() {
            public void run() {
                processIncomingIntent(finalIntent, startId);
            }
        };

        Thread t = new Thread(r);
        t.start();

        return Service.START_STICKY; //Auto-restart if killed
    }


    
    protected void processIncomingIntent(Intent intent, int startID) {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Delta Logging Service : processIncomingIntent()");

        if(intent == null){//The service was killed by the system and restarted automatically thanks to its "STICKY" flag. Resuming...
            if(!isExperimentRunning){ //the experiment is not already running
                Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Process was killed and is now restarting. Reloading experiment...");
                ExperimentConfiguration experimentConfiguration = IOHelpers.loadSavedExperimentConfigurationFromDisk();

                if(experimentConfiguration != null)
                    startLogging(experimentConfiguration);
                else
                    Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ERROR: could not restart the service, failed to retrieve configuration backup from disk.");
            }
            else { //bad invocation: someone tried to start the service without an intent. Ignoring...
                Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ERROR: Received a null-intent request but the experiment is already running. Ignoring...");
            }
        }
        else { //Normal message to the service
            int requestedAction = intent.getIntExtra(Constants.REQUEST_ACTION, -1);
            switch (requestedAction) {
                case Constants.DELTASERVICE_STARTLOGGING:
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "StartLogging intent received. Setting up experiment...");
                    startLogging((ExperimentConfiguration) intent.getSerializableExtra(Constants.EXPERIMENT_CONFIGURATION));
                    break;
                case Constants.DELTASERVICE_BIND_CORE:
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Core DELTA App is binding to the service...");
                    break;
                case Constants.DELTASERVICE_STOPLOGGING:
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Received request to stop logging. Stopping...");
                    terminateExperiment();
                    break;
                case Constants.DELTASERVICE_UPLOAD_LOGS:
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Received request to upload logged data. Executing...");
                    String serverAddress = intent.getStringExtra(Constants.DELTA_UPLOAD_SERVER);
                    if(serverAddress != null) {
                        LogsUploader.uploadPendingLogs(serverAddress);
                    }
                    break;
                case Constants.DELTASERVICE_DUMP_LOGS:
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Received request to dump collected logs to the sdcard. Executing...");
                    String directory = intent.getStringExtra(Constants.DUMP_LOGS_DIRECTORY);
                    boolean clearAfterDump = intent.getBooleanExtra(Constants.CLEAR_LOGS_AFTER_DUMP, false);
                    if(directory != null && !directory.isEmpty()) {
                        makeToast("Copying collected log data to disk...", Toast.LENGTH_SHORT);
                        LogsDumper.dumpPendingLogs(directory, clearAfterDump);
                    }
                    break;
                default:
                    Logger.w(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Unknown intent received. Ignoring...");
            }
        }
    }

    private synchronized boolean startLogging(ExperimentConfiguration experimentConfiguration){
        if(isExperimentRunning){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Received start request but experiment is already running! Ignoring...");
            return true;
        }

        try {
            //Check consistency
            if(experimentConfiguration == null || !getPackageName().equals(experimentConfiguration.ExperimentPackage))
                throw new ExperimentConfigurationException();

            //Sets up experiment
            ExperimentHelper.setupExperiment(experimentConfiguration);
            IOHelpers.saveExperimentConfigurationToDisk(experimentConfiguration); //backup experiment configuration, to recover it if the service is killed by the system

            //initialize plug-ins (allocate resources, etc.)
            initializePlugins();

            //Start event-based logging
            startEventLogging();

            //Setup polling-based logging cycles
            setupPollingScheduler(ExperimentHelper.sporadicExperimentCycles); //low-precision AlarmManager scheduler for plugins that only log at long intervals (lower battery impact, no wakelocks)
            setupPollingTimer(ExperimentHelper.strictExperimentCycles, experimentConfiguration.SuspendOnScreenOff); //high-precision timer-based scheduler for low-latency polling plugins (up to 10 seconds). Wakelocks device

            //Sets itself up as a foreground service (informs the user that the experiment is running via notification, and at the same time keeps it from being killed by the system)
            createNotification(experimentConfiguration);

            //Notify DELTA Core client (if connected) that the experiment has started
            if(myClientMessenger != null)
                notifyClient(Constants.MESSAGE_LOGGING_HAS_STARTED);

            //register for shutdown event (to properly close the experiment if the device is turning off)
            registerShutdownListener();

            isExperimentRunning = true;
            StorageHelper.currentExperimentStartTime = System.currentTimeMillis();
            return true;


        }catch (PluginFailedToLoadException e) { //Couldn't load a plugin class: configuration file incorrect, the plugin has no public parameter-less constructor or the constructor threw an exception
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to load: [" + e.getFaultyPluginID() + "]. Reason: " + e.getMessage() + ". Canceling experiment...");
            makeToast("Failed to start experiment: plugin [" + e.getFaultyPluginID() + "] failed to load. Reason: " + e.getMessage(), Toast.LENGTH_LONG);
            terminateExperiment();
            return false;
        }
        catch (PluginFailedToInitializeException e){ //Failed to initialize plugin: plugin could not allocate the needed resources or the user did not give the required permissions (e.g.: root permissions)
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to initialize plugin: [" + e.getPlugin().getClass().getAnnotation(DeltaPluginMetadata.class).PluginName() + "]. Reason: " + e.getMessage() +" Canceling experiment...");
            makeToast("Failed to start experiment: plugin [" + e.getPlugin().getClass().getAnnotation(DeltaPluginMetadata.class).PluginName() + "] failed to initialize. Reason: " + e.getMessage(), Toast.LENGTH_LONG);
            terminateExperiment();
            return false;
        }
        catch (PluginFailedToStartLoggingException e){ //An error occurred while starting logging operations
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to start logging on plugin: [" + e.getPlugin().getClass().getAnnotation(DeltaPluginMetadata.class).PluginName() + "]. Reason: " + e.getMessage() +" Canceling experiment...");
            makeToast("Failed to start experiment: plugin [" + e.getPlugin().getClass().getAnnotation(DeltaPluginMetadata.class).PluginName() + "] failed to start logging. Reason: " + e.getMessage(), Toast.LENGTH_LONG);
            terminateExperiment();
            return false;
        }
        catch (ExperimentConfigurationException e){ //Error loading or reading the configuration file. Configuration file is damaged or was not designed for this experiment package
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to start experiment: Experiment Configuration data error. The provided configuration is either corrupted or was not for this experiment package");
            makeToast("Failed to start experiment: Experiment Configuration data error. The provided configuration is either corrupted or was not made for this experiment package", Toast.LENGTH_LONG);
            terminateExperiment();
            return false;
        }
        catch (FailSilentlyException e){ //A plugin has requested that the experiment be silently terminated (probably because the user has to satisfy some requirement before the experiment can start, like installing a specific app or enabling a specific setting)
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Plugin required silent fail, terminating...");
            terminateExperiment();
            return false;
        }
        catch (Exception e) { //Other unknown error (should not happen...)
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Failed to start experiment: unknown error. Printing stack trace and terminating experiment...");
            makeToast("Failed to start experiment: unknown error. Experiment will be terminated...", Toast.LENGTH_LONG);
            e.printStackTrace();
            terminateExperiment();
            return false;
        }
    }


    private void initializePlugins() throws PluginFailedToInitializeException, FailSilentlyException {
        List<PluginConfiguration> failedToRemove = new LinkedList<>();

        for (Map.Entry<PluginConfiguration, IDeltaEventPlugin> entry : ExperimentHelper.getEventPlugins().entrySet()) { //all event plugins
            try {
                entry.getValue().Initialize(this, this, new PluginConfiguration(entry.getKey()));
            }catch (PluginFailedToInitializeException ex){
                if(entry.getKey().AllowOptOut) {
                    failedToRemove.add(entry.getKey());
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Plugin " + entry.getKey().PluginName + " failed to initialize, but it's an optional plugin. " +
                            "Excluding from the experiment an continuing...");
                }
                else {
                    throw ex;
                }
            }
        }

        for (Map.Entry<PluginConfiguration, IDeltaPollingPlugin> entry : ExperimentHelper.getPollingPlugins().entrySet()) { //all polling plugins
            try {
                entry.getValue().Initialize(this, this, new PluginConfiguration(entry.getKey()));
            }catch (PluginFailedToInitializeException ex){
                if(entry.getKey().AllowOptOut) {
                    failedToRemove.add(entry.getKey());
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Plugin " + entry.getKey().PluginName + " failed to initialize, but it's an optional plugin. " +
                            "Excluding from the experiment an continuing...");
                }
                else {
                    throw ex;
                }
            }
        }

        ExperimentHelper.removePluginsFromCurrentExperiment(failedToRemove);
    }

    /**
     * Sets up the alarm manager that regulates periodic polling of long-interval plugins
     * @param experimentCycles The info on the polling frequencies
     */
    private void setupPollingScheduler(MathHelpers.ExperimentCycles experimentCycles) {
        //Check if we need polling in the first place
        if(experimentCycles == null){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "No PollingPlugins configured for this experiment, skipping AlarmManager setup...");
            return;
        }

        int i = 0;
        for(final MathHelpers.CycleValues cycleValues : experimentCycles.cycle2plugins.keySet()){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Setting up polling alarm (Frequency set at: " + MathHelpers.getReadableTime(cycleValues.frequency) + ")");
            pollCycles.put(cycleValues, 1);

            //custom inner BroadCastReceiver to trigger polls. Declared programmatically so it lives and dies with the service instance
            myPollingAlarmBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Polling alarm triggered! Performing poll...");
                    pollSensors(cycleValues);
                }
            };

            //Registering Broadcast Receiver
            String pollingIntentAction = getApplicationContext().getPackageName() + ".PollingAlarmReceiver_" + i;
            IntentFilter pollingIntentFilter=new IntentFilter(pollingIntentAction);
            registerReceiver(myPollingAlarmBroadcastReceiver, pollingIntentFilter);

            //Setting up Alarm
            Intent pollIntent = new Intent(pollingIntentAction);
            pollPendingIntent = PendingIntent.getBroadcast(this, 0, pollIntent, 0);
            AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    0,
                    cycleValues.frequency,
                    pollPendingIntent);

            i++;
        }
    }

    /**
     * Sets up the timer-based scheduler that regulates periodic polling of frequently-firing polling plugins
     * @param experimentCycles Information on the polling frequencies
     * @param suspendOnScreenOff If set to false the service will maintain a wakelock so it keeps logging even when screen turns off. Increases battery consumption
     */
    private void setupPollingTimer(MathHelpers.ExperimentCycles experimentCycles, boolean suspendOnScreenOff){
        //Check if we need polling in the first place
        if(experimentCycles == null){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "No PollingPlugins configured for this experiment, skipping Timer setup...");
            return;
        }

        for (final MathHelpers.CycleValues cycleValues : experimentCycles.cycle2plugins.keySet()){
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Setting up polling timer (Frequency set at: " + cycleValues.frequency + "ms)");

            pollCycles.put(cycleValues, 1);

            Runnable pollingRunnableTask = new Runnable() {
                @Override
                public void run() {
                    pollSensors(cycleValues);
                }
            };

            ScheduledThreadPoolExecutor myExecutor =(ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
            pollingThreadPoolSchedulers.put(cycleValues, myExecutor);
            myExecutor.scheduleAtFixedRate(pollingRunnableTask, 0, cycleValues.frequency, TimeUnit.MILLISECONDS);
        }


        //if we're using timers, we need to wakelock the device (if the experiment requires logging even when screen off, otherwise it's not needed)
        if(!suspendOnScreenOff) {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Experiment requires continuous logging, WakeLocking device...");
            PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
            myWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + "_wakelock");
            myWakeLock.acquire();
        }
    }


    /**
     * Tells all event-driven plugins to start logging.
     * @throws PluginFailedToStartLoggingException if a non-optional plugin fails to start logging. In that case the experiment will be terminated
     */
    private void startEventLogging() throws PluginFailedToStartLoggingException {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Initializing and starting EvenPlugins...");
        List<PluginConfiguration> failedToRemove = new LinkedList<>();

        for (Map.Entry<PluginConfiguration, IDeltaEventPlugin> entry : ExperimentHelper.getEventPlugins().entrySet()){ //all event plugins
            IDeltaEventPlugin eventPlugin = entry.getValue();

            //Start plugin
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Sending command to start logging to plugin: [" + eventPlugin.getClass().getName() + "]...");
            try {
                eventPlugin.StartLogging();
            } catch (PluginFailedToStartLoggingException e) {
                if(entry.getKey().AllowOptOut){
                    eventPlugin.Terminate();
                    failedToRemove.add(entry.getKey());
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Plugin [" + entry.getKey().PluginName + "] failed to start logging, but it's marked as optional. " +
                            "Removing it from experiment and continuing...");
                }
                else {
                    throw e;
                }
            }
        }

        ExperimentHelper.removePluginsFromCurrentExperiment(failedToRemove);
    }


    /**
     * Listens for the SHUTDOWN event, in order to cleanly terminate the experiment if the device is turning off
     */
    private void registerShutdownListener(){
        myShutdownReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                terminateExperiment();
            }
        };

        registerReceiver(myShutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
    }

    private synchronized void terminateExperiment(){
        //Stops all logging operations
        stopLogging();

        //Cleaning up experiment, unloading plugins, etc.
        cleanExperiment();

        //Notify client
        if(myClientMessenger != null)
            notifyClient(Constants.MESSAGE_LOGGING_HAS_STOPPED);

        //stop listening for shutdown events
        if(myShutdownReceiver != null){
            unregisterReceiver(myShutdownReceiver);
            myShutdownReceiver = null;
        }

        //stops the service
        stopSelf();
    }


    private void stopLogging() {
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Stopping logging operations...");

        //Stop the alarm that triggers polling (if alarm-based)
        if(myPollingAlarmBroadcastReceiver != null)
            unregisterReceiver(myPollingAlarmBroadcastReceiver);

        if(pollPendingIntent != null) {
            AlarmManager alarmMgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(pollPendingIntent);
            pollPendingIntent = null;
        }

        //stop the timer (if timer-based)
        for(ScheduledThreadPoolExecutor myPollingThreadPoolScheduler : pollingThreadPoolSchedulers.values()){
            myPollingThreadPoolScheduler.shutdown();
            try {
                myPollingThreadPoolScheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            myPollingThreadPoolScheduler.shutdownNow();
        }
        pollingThreadPoolSchedulers.clear();

        //release the wakelock
        if(myWakeLock != null) {
            myWakeLock.release();
            myWakeLock = null;
        }

        //Tell Event-based plugins to stop logging
        if(ExperimentHelper.getEventPlugins() != null) {
            for (Map.Entry<PluginConfiguration, IDeltaEventPlugin> entry : ExperimentHelper.getEventPlugins().entrySet()) { //all event plugins
                entry.getValue().StopLogging();
            }
        }

        isExperimentRunning = false;
    }

    private void cleanExperiment(){
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Cleaning up...");

        //Tell plugins to terminate (release all resources, if any)
        if(ExperimentHelper.getEventPlugins() != null) {
            for (Map.Entry<PluginConfiguration, IDeltaEventPlugin> entry : ExperimentHelper.getEventPlugins().entrySet()) { //all event plugins
                entry.getValue().Terminate();
            }
        }

        if(ExperimentHelper.getPollingPlugins() != null) {
            for (Map.Entry<PluginConfiguration, IDeltaPollingPlugin> entry : ExperimentHelper.getPollingPlugins().entrySet()) { //all polling plugins
                entry.getValue().Terminate();
            }
        }

        //Flush remaining cached logging data to disk
        StorageHelper.flushAndCleanup();
        StorageHelper.currentExperimentStartTime = 0;

        //Unload experiment configuration
        ExperimentHelper.clearExperiment();

        pollCycles.clear();
    }

    /*
     Receives updates from Event Plugins
     */
    @Override
    public void Update(DeltaDataEntry data) {
        if(isExperimentRunning)
            StorageHelper.update(data);
    }

    public void pollSensors(MathHelpers.CycleValues cycleValues) {
        if(!isExperimentRunning) {
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "ERROR: Poll Cycle invoked while the experiment is NOT running. Ignoring poll request...");
            return;
        }

        Integer pollCycle = pollCycles.get(cycleValues);
        Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Starting Poll Cycle #" + pollCycle + " for the " + MathHelpers.getReadableTime(cycleValues.frequency) + " cycle");

        List<PluginConfiguration> pluginsToPoll;
        if(ExperimentHelper.strictExperimentCycles != null && ExperimentHelper.strictExperimentCycles.cycle2plugins.containsKey(cycleValues))
            pluginsToPoll = ExperimentHelper.strictExperimentCycles.cycle2plugins.get(cycleValues);
        else
            pluginsToPoll = ExperimentHelper.sporadicExperimentCycles.cycle2plugins.get(cycleValues);

        Map<PluginConfiguration, IDeltaPollingPlugin> pollingPlugins = ExperimentHelper.getPollingPlugins();
        for (PluginConfiguration pluginConfiguration : pluginsToPoll){ //all polling plugins
            //check if the plugin needs to be polled at this cycle
            IDeltaPollingPlugin pollingPlugin = pollingPlugins.get(pluginConfiguration);
            long pluginPollCycle = pluginConfiguration.PollingFrequency / cycleValues.frequency;

            //Poll plugin
            if(pollCycle % pluginPollCycle == 0){
                Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Polling plugin: " + pollingPlugin.getClass().getName());
                pollingPlugin.Poll();
            }
        }

        //Increment poll cycle counter
        pollCycle++;
        if(pollCycle > cycleValues.iterations)
            pollCycle = 1;
        pollCycles.put(cycleValues, pollCycle);
    }



    private void createNotification(ExperimentConfiguration experimentConfiguration){
        Intent deltaCoreIntent = new Intent();
        deltaCoreIntent.setComponent(new ComponentName("unipd.elia.deltacore", "unipd.elia.deltacore.ui.ManageExperimentsActivity"));
        deltaCoreIntent.putExtra(Constants.EXPERIMENT_PACKAGE_ID , experimentConfiguration.ExperimentPackage);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, deltaCoreIntent, 0);

        myNotificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon_1)
                .setContentTitle("DELTA experiment running: " + experimentConfiguration.ExperimentName)
                .setContentText("Tap to open DELTA manager app")
                .setContentIntent(pendingIntent);



        Notification notification = myNotificationBuilder.build();


        startForeground(NotificationID, notification);
    }

    private void notifyClient(int what){
        if(myClientMessenger != null){
            try {
                Message m = new Message();
                m.what = what;
                myClientMessenger.send(m);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void makeToast(final String text, final int length){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeltaLoggingService.this, text, length).show();
            }
        });
    }

    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.MESSAGE_REGISTER_CLIENT :
                    Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "Incoming message: MESSAGE_REGISTER_CLIENT. Registering Delta.App client...");
                    DELTAUtils.loggingServiceInstance.myClientMessenger = msg.replyTo;
                    DELTAUtils.loggingServiceInstance.notifyClient(DELTAUtils.loggingServiceInstance.isExperimentRunning ? Constants.MESSAGE_LOGGING_HAS_STARTED : Constants.MESSAGE_LOGGING_HAS_STOPPED);
                    break;
                case Constants.MESSAGE_STOP_LOGGING:
                    DELTAUtils.loggingServiceInstance.terminateExperiment();
                    break;
                default:
                    break;
            }
        }
    }


}
