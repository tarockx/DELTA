package unipd.elia.delta.plugins.systemtools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 22/06/2015.
 */
@DeltaPluginMetadata(PluginName = "Installed packages logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs which applications the user has installed, as well as new installations and removals/updates",
        DeveloperDescription = "An entry is made when the experiment starts, containing ALL the installed packages. " +
                "This entry will have the column 'event' set to 'first_dump' and consists of an array containing all the packages' names and UIDs. " +
                "Subsequent entries are only made when a package is installed/updated/removed or its data is cleared, containing only the " +
                "interested package name and UID.")
public class InstalledPackagesPlugin implements IDeltaPlugin, IDeltaEventPlugin {
    private Context context;
    private IDeltaPluginMaster myDeltaMaster;
    private BroadcastReceiver myPackagesBroadcastReceiver;


    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Received NULL Context or UpdateListener");

        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        try {
            myPackagesBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    reportUpdate(intent);
                }
            };

        } catch (Exception e){
            e.printStackTrace();
            throw new PluginFailedToInitializeException(this, "Exception while registering broadcast receiver. Original exception message: " + e.getMessage());
        }

    }

    private void reportUpdate(Intent intent) {
        JSONObject obj = new JSONObject();

        try{
            switch (intent.getAction()){
                case Intent.ACTION_PACKAGE_ADDED :
                    obj.put("event", "package_added");
                    break;
                case Intent.ACTION_PACKAGE_REMOVED :
                    obj.put("event", "package_removed");
                    break;
                case Intent.ACTION_PACKAGE_REPLACED :
                    obj.put("event", "package_updated");
                    break;
                case Intent.ACTION_PACKAGE_DATA_CLEARED :
                    obj.put("event", "package_data_cleared");
                    break;
            }

            obj.put("package_name", intent.getDataString());
            obj.put("package_uid", intent.getIntExtra(Intent.EXTRA_UID, -1));
            obj.put("first_dump", "[]");

            DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj);
            myDeltaMaster.Update(data);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void reportFirstDump(){
        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.
        try {
            JSONObject obj = new JSONObject();
            obj.put("event", "first_dump");
            obj.put("package_name", "");
            obj.put("package_uid", "");

            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            JSONArray arr = new JSONArray();
            for (ApplicationInfo packageInfo : packages) {
                JSONObject in_obj = new JSONObject();
                in_obj.put("package_name", packageInfo.packageName);
                in_obj.put("package_uid", packageInfo.uid);
                arr.put(in_obj);
            }
            obj.put("first_dump", arr);

            DeltaDataEntry data = new DeltaDataEntry(System.currentTimeMillis(), getClass().getName(), obj);
            myDeltaMaster.Update(data);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
        myPackagesBroadcastReceiver = null;
    }


    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        context.registerReceiver(myPackagesBroadcastReceiver, intentFilter);


        //sporadically reports first dump
        reportFirstDump();
    }

    @Override
    public void StopLogging() {
        context.unregisterReceiver(myPackagesBroadcastReceiver);
    }


}
