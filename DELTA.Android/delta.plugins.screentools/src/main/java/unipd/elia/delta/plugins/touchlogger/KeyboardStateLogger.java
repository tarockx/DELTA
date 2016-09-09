package unipd.elia.delta.plugins.touchlogger;

import android.content.Context;

import com.spazedog.lib.rootfw4.Shell;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 29/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Keyboard State logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs whether the soft keyboard is open (visible) or not.",
        DeveloperDescription = "This plugin periodically checks if the software keyboard is open (visible) or not. " +
                "Note that due to limitations of the Android API, this plugin requires root access.",
        RequiresRoot = true)
public class KeyboardStateLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    Shell rootShell;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        rootShell = new Shell(true);
        Shell.Result obtainedSu = rootShell.execute("id");
        if(obtainedSu == null || !obtainedSu.wasSuccessful() || !obtainedSu.getLine().contains("uid=0")){
            //retry
            obtainedSu = rootShell.execute("id");
            if(obtainedSu == null || !obtainedSu.wasSuccessful() || !obtainedSu.getLine().contains("uid=0"))
                throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
        }

        //fails if we don't have root access
        if(!rootShell.isConnected())
            throw new PluginFailedToInitializeException(this, "Could not connect with the root shell");

    }

    @Override
    public void Terminate() {
        rootShell.destroy();
    }

    @Override
    public void Poll() {
        try{
            Shell.Result result = rootShell.execute("dumpsys window InputMethod | grep \"mHasSurface\"");
            if(result.wasSuccessful()){
                boolean isOpen = result.getLine().contains("mHasSurface=true");
                DeltaDataEntry deltaDataEntry = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), "{\"keyboardOpen\"=\" "+ isOpen + "\"}");
                myDeltaMaster.Update(deltaDataEntry);
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
