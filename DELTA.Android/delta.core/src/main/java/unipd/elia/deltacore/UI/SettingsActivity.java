package unipd.elia.deltacore.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import unipd.elia.deltacore.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
