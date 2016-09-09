package unipd.elia.deltacore.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import unipd.elia.delta.androidsharedlib.Constants;

/**
 * Created by Elia on 29/05/2015.
 */
public class SettingsHelpers {
    public static Set<String> getStartedExperiments(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFEREFNCES_FILE_ID_DELTACORE, Context.MODE_MULTI_PROCESS);
        return sharedPreferences.getStringSet(Constants.PREF_STARTED_EXPERIMENTS, null);
    }

    public static void addStartedExperiment(Context context, String packageID){
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFEREFNCES_FILE_ID_DELTACORE, Context.MODE_MULTI_PROCESS);
        Set<String> experiments =  sharedPreferences.getStringSet(Constants.PREF_STARTED_EXPERIMENTS, null);

        if(experiments == null)
            experiments = new HashSet<>();

        experiments.add(packageID);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Constants.PREF_STARTED_EXPERIMENTS);
        editor.commit();

        editor.putStringSet(Constants.PREF_STARTED_EXPERIMENTS, experiments);
        editor.commit();
    }

    public static void removeStartedExperiment(Context context, String packageID){
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.PREFEREFNCES_FILE_ID_DELTACORE, Context.MODE_MULTI_PROCESS);
        Set<String> experiments =  sharedPreferences.getStringSet(Constants.PREF_STARTED_EXPERIMENTS, null);

        if(experiments == null)
            return;

        experiments.remove(packageID);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Constants.PREF_STARTED_EXPERIMENTS);
        editor.commit();

        if(experiments.size() != 0)
            editor.putStringSet(Constants.PREF_STARTED_EXPERIMENTS, experiments);
        editor.commit();
    }
}
