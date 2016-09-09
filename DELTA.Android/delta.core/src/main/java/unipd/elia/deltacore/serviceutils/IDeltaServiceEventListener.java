package unipd.elia.deltacore.serviceutils;

import unipd.elia.deltacore.ExperimentWrapper;

/**
 * Created by Elia on 25/06/2015.
 */
public interface IDeltaServiceEventListener {
    void onServiceDisconnected(ExperimentWrapper experimentWrapper);
    void onServiceStartedLogging();
    void onServiceStoppedLogging();
}
