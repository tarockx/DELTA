package unipd.elia.deltacore.helpers;

import android.content.Context;
import android.preference.PreferenceManager;

import unipd.elia.deltacore.R;

/**
 * Created by Elia on 15/04/2015.
 */
public class DELTAUtils {
    public static Context context;

    public static void initializeSharedPreferencesIfFirstRun(){
        String justForTest = PreferenceManager.getDefaultSharedPreferences(context).getString("deltaServerAddress", null);
        if(justForTest == null) //not initialized
            PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
    }
}
