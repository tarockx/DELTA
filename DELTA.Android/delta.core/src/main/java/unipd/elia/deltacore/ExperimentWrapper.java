package unipd.elia.deltacore;

import java.io.Serializable;

import unipd.elia.delta.sharedlib.ExperimentConfiguration;

/**
 * Created by Elia on 15/05/2015.
 */
public class ExperimentWrapper implements Serializable {
    public ExperimentConfiguration experimentConfiguration;
    public boolean isCompatible = false;
    public boolean isInstalled = false;
    public boolean isRunning = false;
    public boolean isDownloading = false;
    public boolean isInStore = false;
}
