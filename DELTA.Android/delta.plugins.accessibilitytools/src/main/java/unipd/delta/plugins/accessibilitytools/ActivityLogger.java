package unipd.delta.plugins.accessibilitytools;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONObject;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaEventPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.exceptions.FailSilentlyException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToStartLoggingException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 09/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Activity Logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs which Activity is currently in the foreground. (i.e.: which App is currently visible, including which 'subsection' of the App is active at the moment).",
        DeveloperDescription = "An entry is logged every time the foreground activity changes. " +
                "NOTE: this plugins requires that the user enables the 'DELTA Accessibility Helper' in the system Accessibility menu. " +
                "The user will be prompted to do so at first start."
)
public class ActivityLogger implements IDeltaPlugin, IDeltaEventPlugin, IDeltaAccessibilityServiceListener {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName = getClass().getName();

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        if(!DeltaAccessibilityService.isAccessibilityServiceEnabled(context)){
            DeltaAccessibilityService.requestServiceActivationToUser(context);
            throw new FailSilentlyException();
        }
    }

    @Override
    public void Terminate() {

    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, this);
    }

    @Override
    public void StopLogging() {
        DeltaAccessibilityService.unregisterListener(this);
    }

    @Override
    public void update(AccessibilityEvent event) {
        try{
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            ActivityInfo activityInfo = context.getPackageManager().getActivityInfo(componentName, 0);
            if (activityInfo != null) {
                JSONObject obj = new JSONObject();
                obj.put("event", "activity_switched");
                obj.put("activity", componentName.flattenToString());
                //NOTE: can potentially be extended to include a lot more info...

                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}