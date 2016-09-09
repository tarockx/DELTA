package unipd.delta.plugins.accessibilitytools;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.DialogHelper;

/**
 * Created by Elia on 09/07/2015.
 */
public class DeltaAccessibilityService extends AccessibilityService {
    private static Map<Integer, List<IDeltaAccessibilityServiceListener>> eventType2Listeners;

    public static void registerForEvent(int eventType, IDeltaAccessibilityServiceListener listener){
        if(eventType2Listeners == null)
            eventType2Listeners = new HashMap<>();

        List<IDeltaAccessibilityServiceListener> list;
        if(!eventType2Listeners.containsKey(eventType)) {
            list = new LinkedList<>();
            eventType2Listeners.put(eventType, list);
        }
        else {
            list = eventType2Listeners.get(eventType);
        }

        list.add(listener);
    }

    public static void unregisterListener(IDeltaAccessibilityServiceListener listener){
        if(eventType2Listeners == null)
            return;

        for(List<IDeltaAccessibilityServiceListener> list : eventType2Listeners.values()){
            list.remove(listener);
        }
    }


    public static boolean isAccessibilityServiceEnabled(Context context){
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        String id = context.getPackageName() + "/unipd.delta.plugins.accessibilitytools.DeltaAccessibilityService";

        List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runningServices) {
            if (id.equals(service.getId())) {
                return true;
            }
        }

        return false;
    }

    public static void requestServiceActivationToUser(Context context){
        DialogHelper dialogHelper = new DialogHelper(context, "To work properly, this experiment needs to be turned on in the 'Accessibility' menu.\n" +
                "If you press 'ENABLE NOW' you will be redirected to the Accessibility menu. Once there, please turn on the 'DELTA Accessibility Helper'," +
                " then restart this experiment",
                "Enable Now",
                "Cancel experiment",
                null);
        int res = dialogHelper.ShowMyModalDialog();
        if(res == AlertDialog.BUTTON_POSITIVE){
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    @Override
    public void onServiceConnected(){

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(eventType2Listeners == null || !eventType2Listeners.containsKey(event.getEventType()))
            return;

        int eventType = event.getEventType();

        List<IDeltaAccessibilityServiceListener> list = eventType2Listeners.get(eventType);
        for(IDeltaAccessibilityServiceListener listener : list)
            listener.update(event);
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent){
        return super.onUnbind(intent);
    }
}
