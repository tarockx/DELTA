package unipd.delta.plugins.accessibilitytools;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
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
        PluginName = "KeyLogger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs all the text you type, except for passwords, which are not logged for security reasons.",
        DeveloperDescription = "An entry is logged every time the user types text in a text field. The entry will contain the old text value and the new one, as well as additional info, " +
                "like the ID and package of the text field that is being edited."
)
public class KeyLogger implements IDeltaPlugin, IDeltaEventPlugin, IDeltaAccessibilityServiceListener {
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
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, this);
    }

    @Override
    public void StopLogging() {
        DeltaAccessibilityService.unregisterListener(this);
    }

    @Override
    public void update(AccessibilityEvent event) {
        try{
            JSONObject obj = new JSONObject();
            obj.put("event", "text_input_changed");
            obj.put("package", event.getPackageName());
            obj.put("class", event.getClassName());
            obj.put("id", event.getSource() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ? event.getSource().getViewIdResourceName() : "N/A");
            obj.put("old_text", event.isPassword() ? "N/A [password_field]" : event.getBeforeText());
            obj.put("new_text", event.isPassword() ? "N/A [password_field]" : event.getText());

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}