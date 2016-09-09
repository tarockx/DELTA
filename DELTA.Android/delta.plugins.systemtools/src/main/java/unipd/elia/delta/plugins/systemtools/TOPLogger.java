package unipd.elia.delta.plugins.systemtools;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaIntegerOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.IntegerOption;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 29/06/2015.
 */
@DeltaPluginMetadata(PluginName = "TOP process and CPU logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs the list of processes currently running on the device",
        DeveloperDescription = "Uses the 'top' command to log running processes. This includes system and user processes, and logs their name, CPU usage, " +
                "memory occupation, # of threads in use, PID and UID. NOTE: you can use the options menu to set the maximum number of processes to log " +
                "(default is all processes). If you set this option to X only the X processes that are using more CPU will be included in the logs.",
        MinPollInterval = 10 * 1000)
@DeltaOptions(
        IntegerOptions = {
                @DeltaIntegerOption(ID = "MAX_PROCESSES",
                        Name = "Max processes to log",
                        Description = "Maximum number of processes to log. If set to 0, all will be logged. " +
                                "Otherwise only the first X processes will be logged (the list is ordered by CPU power consumption)",
                        MinValue = 0, defaultValue = 0
                )
        }
)
public class TOPLogger implements IDeltaPlugin, IDeltaPollingPlugin{
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    int max = 0;
    String invocation;
    Runtime runtime;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");

        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        IntegerOption maxOption = pluginConfiguration.getIntegerOption("MAX_PROCESSES");
        if(maxOption != null)
            max = maxOption.Value;

        runtime = Runtime.getRuntime();

        invocation = "top -n 1 -s cpu";
        if (max > 0)
            invocation = invocation + "  -m " + max;
    }

    private String executeTop() {
        java.lang.Process p = null;
        BufferedReader in = null;
        String returnString = null;
        try {
            p = runtime.exec(invocation);
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (returnString == null || returnString.contentEquals("")) {
                returnString = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                p.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returnString;
    }

    @Override
    public void Terminate() {
    }

    @Override
    public void Poll() {
        String top = executeTop();
        if(top != null && !top.isEmpty()){
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), top.trim()));
        }
    }
}
