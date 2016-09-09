package unipd.elia.delta.androidsharedlib.deltainterfaces;

import android.os.Build;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Elia on 20/05/2015.
 */
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DeltaPluginMetadata {

    /**
     * Human-readable short name for the plugin
     */
    String PluginName();

    /**
     * Name of the plugin's creator
     */
    String PluginAuthor();

    /**
     * Simple description of what the plugin does (i.e.: what it logs). Will be shown to the user, don't make it too technical
     */
    String PluginDescription();

    /**
     * [OPTIONAL] Detailed technical description of what kind of data the plugin logs, in which circumstances and in which format. Will only be shown to experiment creators, not to the users.
     */
    String DeveloperDescription() default "N/A";

    /**
     * [OPTIONAL] Set to true if your plugin requires superuser (ROOT) access to work.
     */
    boolean RequiresRoot() default false;

    /**
     * [OPTIONAL] Set to true if your plugin requires the device to be wakelocked. If set to true, the experiment will keep the device always awake
     */
    boolean RequiresWakelock() default false;

    /**
     * [OPTIONAL] If your plugin is only compatible with an Android version higher than SDK_15 (ICS 4.0.3), set this option to the minimum SDK level required by your plugin
     */
    int MinSDK() default Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;

    /**
     * Minimum polling interval (in milliseconds) that this Polling Plugin can log at.
     * The configurator utility will not allow the experiment creator to to set a polling interval value lower than this.
     */
    int MinPollInterval() default 0;
}
