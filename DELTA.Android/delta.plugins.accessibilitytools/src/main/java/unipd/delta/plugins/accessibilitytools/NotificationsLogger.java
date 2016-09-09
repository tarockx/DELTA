package unipd.delta.plugins.accessibilitytools;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
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
 * Created by Elia on 13/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Notifications logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "This plugin logs all the notifications that the Apps and Android System generate. This includes the text of the notification and the ID of the app that generated it",
        DeveloperDescription = "BETA / Not working very well at the moment."
)
public class NotificationsLogger implements IDeltaPlugin, IDeltaEventPlugin, IDeltaAccessibilityServiceListener {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName = getClass().getName();

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException, FailSilentlyException {
        if (androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;

        if (!DeltaAccessibilityService.isAccessibilityServiceEnabled(context)) {
            DeltaAccessibilityService.requestServiceActivationToUser(context);
            throw new FailSilentlyException();
        }
    }

    @Override
    public void Terminate() {

    }

    @Override
    public void StartLogging() throws PluginFailedToStartLoggingException {
        DeltaAccessibilityService.registerForEvent(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED, this);
    }

    @Override
    public void StopLogging() {
        DeltaAccessibilityService.unregisterListener(this);
    }

    @Override
    public void update(AccessibilityEvent event) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("package", event.getPackageName());
            obj.put("class", event.getClassName());

            obj.put("notification_text", event.getText()); //?

            Notification notification = (Notification) event.getParcelableData();
            obj.put("notification_id", notification.number);
            obj.put("notification_category", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? notification.category : "N/A");
            obj.put("click_intent", notification.contentIntent);
            obj.put("dismiss_intent", notification.deleteIntent);
            obj.put("LED_color_argb", notification.ledARGB);
            obj.put("LED_on_ms", notification.ledOnMS);
            obj.put("LED_off_ms", notification.ledOffMS);
            obj.put("notification_priority", Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ? notification.priority : "N/A");
            //TODO: we could add more stuff here, lots of things can be extracted...


            myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, obj));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}