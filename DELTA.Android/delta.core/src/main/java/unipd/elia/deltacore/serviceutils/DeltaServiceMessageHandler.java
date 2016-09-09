package unipd.elia.deltacore.serviceutils;

import android.os.Handler;
import android.os.Message;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.deltacore.ExperimentWrapper;

/**
 * Created by Elia on 25/06/2015.
 */
class DeltaServiceMessageHandler extends Handler {
    public ExperimentWrapper experimentWrapper;
    public DeltaServiceConnection deltaServiceConnection;

    public DeltaServiceMessageHandler(ExperimentWrapper ew, DeltaServiceConnection connection){
        experimentWrapper = ew;
        deltaServiceConnection = connection;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case Constants.MESSAGE_LOGGING_HAS_STOPPED:
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Service [" + (experimentWrapper.experimentConfiguration.ExperimentPackage) + "] confirms to have stopped. Notifying activity...");
                deltaServiceConnection.onServiceStoppedLogging();
                break;
            case Constants.MESSAGE_LOGGING_HAS_STARTED:
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Service [" + (experimentWrapper.experimentConfiguration.ExperimentPackage) + "] confirms to have started logging. Notifying activity..." );
                deltaServiceConnection.onServiceStartedLogging();
                break;
            default:
                Logger.d(Constants.DEBUGTAG_DELTAAPP, "Message from service! [" + (experimentWrapper.experimentConfiguration.ExperimentPackage) + "]" );
        }
    }
}