package unipd.delta.plugins.accessibilitytools;

import android.view.accessibility.AccessibilityEvent;

/**
 * Created by Elia on 09/07/2015.
 */
public interface IDeltaAccessibilityServiceListener {
    void update(AccessibilityEvent event);
}
