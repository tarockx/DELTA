package unipd.elia.delta.plugins.networklogger;

import android.content.Context;

import org.sufficientlysecure.rootcommands.RootCommands;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.command.Command;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.io.IOException;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

@DeltaPluginMetadata(PluginName = "Network packet sniffer",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs all the traffic going through the network.",
        DeveloperDescription = "Logs network packets using the tcpdump tool. This plugin requires the phone to be rooted.",
        RequiresRoot = true)
public class PacketSniffer implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;

    String pluginName;
    Shell loggingShell;
    TcpDumpCommand loggingCommand;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        if (RootCommands.rootAccessGiven()) {
            try {
                Shell shell = Shell.startRootShell();

            if(!checkAndInstallTcpDump(shell))
                throw new PluginFailedToInitializeException(this, "Failed to install the tcpdump binary");

            } catch (IOException e) {
                e.printStackTrace();
                throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
            }
        }
        else {
            throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
        }
    }

    private boolean checkAndInstallTcpDump(Shell rootShell) {
        boolean success = false;

        Toolbox toolbox = new Toolbox(rootShell);
        if(!toolbox.remount("/system", "RW"))
            return false;

        String tcpdumpFilePath = "/system/bin/deltatcpdump";
        try {
            if (!toolbox.fileExists(tcpdumpFilePath)) { //install tcpdump

                java.io.File tempFile = IOHelpers.inflateResourceToInternalStorageCache(context, R.raw.deltatcpdump, "deltatcpdump");
                if (tempFile == null || !tempFile.exists())
                    return false;

                //mounting system with R/W privileges for this shell
                if (!toolbox.copyFile(tempFile.getAbsolutePath(), tcpdumpFilePath, false, false))
                    return false;
            }
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }

        try{
            success = toolbox.setFilePermissions(tcpdumpFilePath, "777");
            return success;
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
    }


    @Override
    public void Terminate() {
        uninstallTcpdump();
    }

    private boolean uninstallTcpdump() {
        boolean success = false;

        if (RootCommands.rootAccessGiven()) {
            try {
                Shell shell = Shell.startRootShell();

                String tcpdumpFilePath = "/system/bin/deltatcpdump";
                Toolbox toolbox = new Toolbox(shell);
                if (!toolbox.fileExists(tcpdumpFilePath))
                    return true;

                toolbox.remount("/system", "RW");
                SimpleCommand deleteCommand = new SimpleCommand("rm /system/bin/deltatcpdump");
                shell.add(deleteCommand).waitForFinish();
                shell.close();
                return deleteCommand.getExitCode() == 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        else {
            return false;
        }
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {

        String[] c = {"cd /system/bin", "./deltatcpdump -i any -tt -vv -s 0"};
        try {
            loggingShell = Shell.startRootShell();
            loggingCommand = new TcpDumpCommand(c);
            loggingShell.add(loggingCommand);

        } catch (IOException e) {
            e.printStackTrace();
            throw new PluginFailedToStartLoggingException(this, "Could not obtain root privileges");
        }
    }

    @Override
    public void StopLogging() {
        //Are there ANY root libraries that don't break when using daemons??
        try {
            Shell shell = Shell.startRootShell();
            Toolbox toolbox = new Toolbox(shell);
            if(toolbox.isProcessRunning("deltatcpdump")){
                toolbox.killAll("deltatcpdump");
            }
            if(toolbox.isProcessRunning("./deltatcpdump")) {
                toolbox.killAll("./deltatcpdump");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(loggingCommand != null){
            loggingCommand.terminate("Experiment stopping");
        }

        if(loggingShell != null)
            try {
                loggingShell.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }


    private class TcpDumpCommand extends Command {

        public TcpDumpCommand(String[] commands) {
            super(commands);
        }

        @Override
        public void output(int id, String line) {
            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, (line + "\n").getBytes()));
        }

        @Override
        public void afterExecution(int id, int exitCode) {
            Logger.d(Constants.DEBUGTAG_DELTALOGSUBSTRATE, "TCPdump finished!");
        }

    }


}
