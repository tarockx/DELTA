package unipd.elia.delta.logsubstrate;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.IOException;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.logsubstrate.helpers.DELTAUtils;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ExperimentConfiguration currentExperiment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DELTAUtils.context = this;

        LinearLayout layoutButtons = (LinearLayout) findViewById(R.id.main_layoutButtons);
        try {
            for(String asset : getAssets().list("")){
                try {
                    if(!asset.endsWith(".xml"))
                        continue;

                    ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(getAssets().open(asset));
                    if(experimentConfiguration != null){
                        experimentConfiguration.ExperimentPackage = getPackageName(); //For debug purposes, bypasses package name check
                        Button button = new Button(this);
                        button.setText(experimentConfiguration.ExperimentName);
                        button.setTag(experimentConfiguration);
                        button.setOnClickListener(this);
                        layoutButtons.addView(button);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Button buttonStop = (Button)findViewById(R.id.buttonStopExperiment);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentExperiment != null)
                    stopService(currentExperiment);

            }
        });

        Button buttonUpload = (Button)findViewById(R.id.buttonUploadLogs);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentExperiment != null)
                    uploadData(currentExperiment);

            }
        });

        Button buttonDump = (Button)findViewById(R.id.buttonDumpLogs);
        buttonDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentExperiment != null)
                    dumpData(currentExperiment);
            }
        });
    }

    private void startDeltaService(ExperimentConfiguration experimentConfiguration){

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(), "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_STARTLOGGING);
        intent.putExtra(Constants.EXPERIMENT_CONFIGURATION, experimentConfiguration);

        startService(intent);
    }

    private void dumpData(ExperimentConfiguration experimentConfiguration){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(), "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_DUMP_LOGS);
        intent.putExtra(Constants.DUMP_LOGS_DIRECTORY, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), experimentConfiguration.ExperimentPackage).getAbsolutePath());
        intent.putExtra(Constants.CLEAR_LOGS_AFTER_DUMP, true);

        startService(intent);
    }

    private void uploadData(ExperimentConfiguration experimentConfiguration){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(), "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_UPLOAD_LOGS);
        intent.putExtra(Constants.DELTA_UPLOAD_SERVER, experimentConfiguration.DeltaServerUrl);

        startService(intent);
    }

    private void stopService(ExperimentConfiguration experimentConfiguration){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getPackageName(), "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_STOPLOGGING);
        intent.putExtra(Constants.EXPERIMENT_CONFIGURATION, experimentConfiguration);

        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        currentExperiment = (ExperimentConfiguration) v.getTag();
        startDeltaService(currentExperiment);
    }
}
