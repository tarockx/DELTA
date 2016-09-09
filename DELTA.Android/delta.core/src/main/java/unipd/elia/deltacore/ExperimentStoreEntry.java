package unipd.elia.deltacore;

import java.io.File;

import unipd.elia.deltacore.helpers.IOHelpers;

/**
 * Created by Elia on 26/05/2015.
 */
public class ExperimentStoreEntry {
    private String packageId;

    public ExperimentStoreEntry(String packageId){
        this.packageId = packageId;
    }

    public String getPackageId(){
        return packageId;
    }

    public String getConfigurationPath(){
        return IOHelpers.getExperimentsStoragePath() + "/" + packageId + "/configuration.xml";
    }

    public String getApkPath(){
        return IOHelpers.getExperimentsStoragePath() + "/" + packageId + "/experiment.apk";
    }

    public File getApkFile(){
        return new File(getApkPath());
    }

    public File getConfigurationFile(){
        return new File(getConfigurationPath());
    }
}

