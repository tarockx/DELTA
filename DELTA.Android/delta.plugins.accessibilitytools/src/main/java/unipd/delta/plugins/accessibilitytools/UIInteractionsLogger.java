package unipd.delta.plugins.accessibilitytools;

import android.content.Context;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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
        PluginName = "UI Interactions logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs interactions between the user and the app's UI (User Interface), logging actions like which buttons the user clicks, " +
                "which elements he scrolls, which windows are visible. This can potentially be used to track the user's actions inside an app.",
        DeveloperDescription = "An entry is logged every time: (a) A View is clicked or long-clicked (b) a View gets focus (c) A View is scrolled. " +
                "NOTE: this plugins requires that the user enables the 'DELTA Accessibility Helper' in the system Accessibility menu. " +
                "The user will be prompted to do so at first start."
)
public class UIInteractionsLogger implements IDeltaPlugin, IDeltaEventPlugin, IDeltaAccessibilityServiceListener {
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
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_VIEW_CLICKED, this);
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, this);
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED, this);
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED, this);
    }

    @Override
    public void StopLogging() {
        DeltaAccessibilityService.unregisterListener(this);
    }

    @Override
    public void update(AccessibilityEvent event) {
        try{
            JSONObject obj = new JSONObject();
            switch (event.getEventType()){
                case AccessibilityEvent.TYPE_VIEW_CLICKED :
                    obj.put("event", "view_clicked");
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED :
                    obj.put("event", "view_long_clicked");
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED :
                    obj.put("event", "view_focused");
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED :
                    obj.put("event", "view_scrolled");
                    break;
            }

            obj.put("package", event.getPackageName());
            obj.put("class", event.getClassName());

            AccessibilityNodeInfo source = event.getSource();
            obj.put("id", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && source != null ? source.getViewIdResourceName() : "N/A");
            obj.put("node_text", event.isPassword() ? "N/A [password node]" : event.getText());
            obj.put("isNodeEnabled", event.isEnabled());
            obj.put("isNodeChecked", event.isChecked());
            obj.put("horizontal_scroll", event.getScrollX());
            obj.put("vertical_scroll", event.getScrollY());

            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
