package unipd.elia.deltacore.serviceutils;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.deltacore.ExperimentWrapper;

/**
 * Created by Elia on 25/06/2015.
 */
public class DeltaServiceConnection implements ServiceConnection {
    public ExperimentWrapper experimentWrapper;
    private Messenger outgoingMessenger;
    private Messenger incomingMessenger;
    private IDeltaServiceEventListener deltaServiceEventListener;

    public DeltaServiceConnection(ExperimentWrapper ew, IDeltaServiceEventListener eventListener){
        experimentWrapper = ew;
        deltaServiceEventListener = eventListener;
    }

    public boolean send(Message message){
        if(outgoingMessenger != null) {
            try {
                outgoingMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void onServiceConnected(ComponentName className, IBinder service) {
        outgoingMessenger = new Messenger(service);

        //Registers to the service, so it can reply to messages. myIncomingMessenger will receive the messages from this service instance.
        try{
            Message m = new Message();
            m.what = Constants.MESSAGE_REGISTER_CLIENT;
            incomingMessenger = new Messenger(new DeltaServiceMessageHandler(experimentWrapper, this));
            m.replyTo = incomingMessenger;
            outgoingMessenger.send(m);
        }
        catch (Exception ex){}
    }



    public void onServiceDisconnected(ComponentName className) {
        Logger.d(Constants.DEBUGTAG_DELTAAPP, "Unbound from service (it has stopped or was killed)");
        //UnbindFromExperiment(experimentWrapper);
        deltaServiceEventListener.onServiceDisconnected(experimentWrapper);
        onServiceStoppedLogging();
    }

    public void onServiceStartedLogging(){
        experimentWrapper.isRunning = true;
        deltaServiceEventListener.onServiceStartedLogging();
        //myExperimentWrapperArrayAdapter.notifyDataSetChanged();
    }

    public void onServiceStoppedLogging(){
        experimentWrapper.isRunning = false;
        //myExperimentWrapperArrayAdapter.notifyDataSetChanged();
        deltaServiceEventListener.onServiceStoppedLogging();
    }
};
