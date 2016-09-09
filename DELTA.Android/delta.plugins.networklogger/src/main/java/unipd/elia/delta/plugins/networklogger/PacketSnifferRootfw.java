package unipd.elia.delta.plugins.networklogger;

import android.content.Context;

import com.spazedog.lib.rootfw4.Shell;
import com.spazedog.lib.rootfw4.ShellStreamer;
import com.spazedog.lib.rootfw4.utils.File;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;


/**
 * Created by Elia on 17/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Network packet sniffer (rootfw version)",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs all the traffic going through the network.",
        DeveloperDescription = "Logs network packets using the tcpdump tool. This plugin requires the phone to be rooted. " +
                "NOTE: Alternative version of the regular Packet Sniffer plugin, uses the root-fw library rather than the superuser-commands one. " +
                "Might work where the regular version fails.",
        RequiresRoot = true)
public class PacketSnifferRootfw implements IDeltaPlugin, IDeltaEventPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    ShellStreamer myShellStreamer;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        Shell rootShell = new Shell(true);
        Shell.Result obatinedSu = rootShell.execute("id");
        if(obatinedSu == null || !obatinedSu.wasSuccessful() || !obatinedSu.getLine().contains("uid=0")){
            //retry
            obatinedSu = rootShell.execute("id");
            if(obatinedSu == null || !obatinedSu.wasSuccessful() || !obatinedSu.getLine().contains("uid=0"))
                throw new PluginFailedToInitializeException(this, "Could not obtain root privileges");
        }


        //fails if we don't have root access
        if(!rootShell.isConnected())
            throw new PluginFailedToInitializeException(this, "Could not connect with the root shell");

        //fail if we can't install tcpdump to system partition
        if(!checkAndInstallTcpDump(rootShell))
            throw new PluginFailedToInitializeException(this, "Failed to install the tcpdump binary");

        //setup logging
        if(!setupShellStreamer())
            throw new PluginFailedToInitializeException(this, "Failed to setup ShellStreamer");


    }

    private boolean checkAndInstallTcpDump(Shell rootShell) {
        boolean success = false;

        File tcpdumpFile = rootShell.getFile("/system/bin/deltatcpdump");
        if(!tcpdumpFile.exists()){ //install tcpdump
            try {
                java.io.File tempFile = IOHelpers.inflateResourceToInternalStorageCache(context, R.raw.deltatcpdump, "deltatcpdump");
                if(tempFile == null || !tempFile.exists())
                    return false;

                //mounting system with R/W privileges for this shell
                Shell.Result executeResult = rootShell.execute("mount -o rw,remount /system");
                if(!executeResult.wasSuccessful())
                    return false;

                String command = "cp \"" + tempFile.getAbsolutePath() + "\" \"" + tcpdumpFile.getAbsolutePath() + "\"";
                Shell.Result copyResult = rootShell.execute(command);
                if(!copyResult.wasSuccessful())
                    return false;

            }catch (Exception ex){
                ex.printStackTrace();
                return false;
            }
        }

        //make executable
        try {
            Shell.Result chmodResult = rootShell.execute("chmod 777 \"" + tcpdumpFile.getAbsolutePath() + "\"");
            rootShell.execute("mount -o ro,remount /system"); //remount as RO

            return chmodResult.wasSuccessful();
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
    }

    private boolean setupShellStreamer() {
        myShellStreamer = new ShellStreamer();
        // Start the connection as root
        return myShellStreamer.connect(true);
    }

    @Override
    public void Terminate() {
        if(myShellStreamer != null)
            myShellStreamer.disconnect();
        myShellStreamer = null;

        uninstallTcpdump();
    }

    private boolean uninstallTcpdump(){
        boolean success = false;

        Shell rootShell = new Shell(true);
        Shell.Result obatinedSu = rootShell.execute("id");
        if(obatinedSu == null || !obatinedSu.wasSuccessful() || !obatinedSu.getLine().contains("uid=0")){
            success = false;
        } else {
            success = rootShell.execute("mount -o rw,remount /system").wasSuccessful();
            if (success) {
                success = rootShell.execute("rm /system/bin/deltatcpdump").wasSuccessful(); //uninstall tcpdump
                rootShell.execute("mount -o ro,remount /system"); //remount as RO
            }
        }
        return success;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {

        if(myShellStreamer == null || !myShellStreamer.isConnected())
            throw new PluginFailedToStartLoggingException(this, "Could not connect to root shell");

        myShellStreamer.startStream(new ShellStreamer.StreamListener(){
            @Override
            public void onStreamStart(ShellStreamer shell) {
                shell.writeLine("cd /system/bin");
                shell.writeLine("./deltatcpdump -vv -s 0");

                // Send a stop signal once the above command is finished
                //shell.stopStream();
            }

            @Override
            public void onStreamInput(ShellStreamer shell, String outputLine) {
                // New line has been received from the shell

                //myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), PacketSnifferRootfw.this.pluginName, "{\"tcpdump_line\":\"" + outputLine + "\"}"));
                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), PacketSnifferRootfw.this.pluginName, outputLine.getBytes()));
                //myDeltaMaster.Update(pluginName, outputLine.getBytes());
            }

            @Override
            public void onStreamStop(ShellStreamer shell, int resultCode) {
                shell.disconnect();
            }
        });
    }

    @Override
    public void StopLogging() {
        if(myShellStreamer != null) {

            //myShellStreamer.stopStream();
        }
        //myShellStreamer.disconnect();
    }

}
