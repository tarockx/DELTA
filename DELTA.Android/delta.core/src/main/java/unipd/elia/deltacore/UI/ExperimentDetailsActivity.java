package unipd.elia.deltacore.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.bluejamesbond.text.DocumentView;

import java.io.File;
import java.security.cert.X509Certificate;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.deltacore.ExperimentStoreEntry;
import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;
import unipd.elia.deltacore.helpers.DELTAUtils;
import unipd.elia.deltacore.helpers.ExperimentHelpers;
import unipd.elia.deltacore.helpers.IOHelpers;
import unipd.elia.deltacore.helpers.PackageHelper;
import unipd.elia.deltacore.helpers.SettingsHelpers;
import unipd.elia.deltacore.serviceutils.DeltaServiceConnection;
import unipd.elia.deltacore.serviceutils.IDeltaServiceEventListener;
import unipd.elia.deltacore.ui.fragments.CertificateDetailsFragment;

public class ExperimentDetailsActivity extends AppCompatActivity implements View.OnClickListener, IDeltaServiceEventListener {
    private ExperimentWrapper myExperimentWrapper;
    private DeltaServiceConnection myServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DELTAUtils.context = this;

        setContentView(R.layout.activity_experiment_details);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(Constants.EXPERIMENT_WRAPPER)) {
            myExperimentWrapper = (ExperimentWrapper) extras.getSerializable(Constants.EXPERIMENT_WRAPPER);
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("ERROR: no experiment data found!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    protected void onStart(){
        super.onStart();

        //delete experiment if we just installed it
        new File(getExternalFilesDir(null), "experiment.apk").delete();

        //populate UI elements
        PopulateUI();

        //bind to experiment
        BindToExperiment();

    }

    @Override
    protected void onStop(){
        super.onStop();

        UnbindFromExperiment();
    }

    private void PopulateUI() {
        if(myExperimentWrapper != null){

            TextView txtExperimentName = (TextView) findViewById(R.id.experiment_details_txtExperimentName);
            txtExperimentName.setText(myExperimentWrapper.experimentConfiguration.ExperimentName);
            TextView txtExperimentAuthor = (TextView) findViewById(R.id.experiment_details_txtExperimentAuthor);
            txtExperimentAuthor.setText(myExperimentWrapper.experimentConfiguration.ExperimentAuthor);
            TextView txtExperimentPackageID = (TextView) findViewById(R.id.experiment_details_txtExperimentPackageID);
            txtExperimentPackageID.setText(myExperimentWrapper.experimentConfiguration.ExperimentPackage);
            DocumentView txtExperimentDescription = (DocumentView) findViewById(R.id.experiment_details_txtExperimentDescription);
            txtExperimentDescription.setCacheConfig(DocumentView.CacheConfig.NO_CACHE);
            txtExperimentDescription.setText(myExperimentWrapper.experimentConfiguration.ExperimentDescription);

            BootstrapButton btnInstall = (BootstrapButton) findViewById(R.id.experiment_details_btnInstall);
            btnInstall.setOnClickListener(this);
            BootstrapButton btnDelete = (BootstrapButton) findViewById(R.id.experiment_details_btnDelete);
            btnDelete.setOnClickListener(this);
            BootstrapButton btnRemove = (BootstrapButton) findViewById(R.id.experiment_details_btnRemove);
            btnRemove.setOnClickListener(this);
            BootstrapButton btnUpload = (BootstrapButton) findViewById(R.id.experiment_details_btnUploadNow);
            btnUpload.setOnClickListener(this);
            BootstrapButton btnDumpLogs = (BootstrapButton) findViewById(R.id.experiment_details_btnDumpLogs);
            btnDumpLogs.setOnClickListener(this);
            BootstrapButton btnStart = (BootstrapButton) findViewById(R.id.experiment_details_btnStart);
            btnStart.setOnClickListener(this);
            BootstrapButton btnStop = (BootstrapButton) findViewById(R.id.experiment_details_btnStop);
            btnStop.setOnClickListener(this);

            TextView txtUploadServerUrlHeader = (TextView) findViewById(R.id.experiment_details_txtUploadServerUrlHeader);
            if(myExperimentWrapper.experimentConfiguration.DeltaServerUrl == null || myExperimentWrapper.experimentConfiguration.DeltaServerUrl.isEmpty()){
                btnUpload.setEnabled(false);
                txtUploadServerUrlHeader.setText(getString(R.string.experiment_details_txtUploadServerHeader_NoServerConfigured));
            }
            else {
                btnUpload.setEnabled(true);
                TextView txtUploadServerUrl = (TextView) findViewById(R.id.experiment_details_txtUploadServerUrl);
                txtUploadServerUrl.setText(myExperimentWrapper.experimentConfiguration.DeltaServerUrl);
                txtUploadServerUrlHeader.setText(getString(R.string.experiment_details_txtUploadServerHeader));
            }

            if(PackageHelper.isApkInstalled(getPackageManager(), myExperimentWrapper.experimentConfiguration.ExperimentPackage)){
                btnInstall.setVisibility(View.GONE);
                btnRemove.setVisibility(View.VISIBLE);
                btnUpload.setVisibility(View.VISIBLE);
                btnDumpLogs.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.GONE);
            }
            else{
                btnInstall.setVisibility(View.VISIBLE);
                btnRemove.setVisibility(View.GONE);
                btnUpload.setVisibility(View.GONE);
                btnDumpLogs.setVisibility(View.GONE);


                if(PackageHelper.isExperimentInStore(myExperimentWrapper.experimentConfiguration.ExperimentPackage))
                    btnDelete.setVisibility(View.VISIBLE);
                else
                    btnDelete.setVisibility(View.GONE);
            }

            ExperimentStoreEntry experimentStoreEntry = PackageHelper.getExperimentFromStore(myExperimentWrapper.experimentConfiguration.ExperimentPackage);
            X509Certificate x509Certificate = PackageHelper.getCertificateFromAPK(experimentStoreEntry.getApkFile(), this);
            Fragment newFragment = CertificateDetailsFragment.newInstance(x509Certificate);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.experiment_details_cardViewCertificate, newFragment).commit();

            LinearLayout layoutPluginList = (LinearLayout) findViewById(R.id.experiment_details_layoutPluginList);
            layoutPluginList.removeAllViews();
            for(PluginConfiguration pluginConfiguration : myExperimentWrapper.experimentConfiguration.getAllPluginConfigurations(false)){
                LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View pluginDetailsView = vi.inflate(R.layout.plugin_details_checkable_layout, null);

                TextView txtPluginName = (TextView) pluginDetailsView.findViewById(R.id.plugin_details_txtPluginName);
                DocumentView txtPluginDescription = (DocumentView) pluginDetailsView.findViewById(R.id.plugin_details_txtPluginDescription);
                txtPluginDescription.setCacheConfig(DocumentView.CacheConfig.NO_CACHE);
                txtPluginName.setText(pluginConfiguration.PluginName);
                txtPluginDescription.setText(pluginConfiguration.PluginDescription);

                // insert into main view
                layoutPluginList.addView(pluginDetailsView);
            }

            SetStatus();
        }
    }

    private void SetStatus(){
        BootstrapButton btnStart = (BootstrapButton) findViewById(R.id.experiment_details_btnStart);
        BootstrapButton btnStop = (BootstrapButton) findViewById(R.id.experiment_details_btnStop);
        TextView txtExperimentStatus = (TextView) findViewById(R.id.experiment_details_txtExperimentStatus);

        if(PackageHelper.isApkInstalled(getPackageManager(), myExperimentWrapper.experimentConfiguration.ExperimentPackage)){
            btnStart.setVisibility(myExperimentWrapper.isRunning ? View.GONE : View.VISIBLE);
            btnStop.setVisibility(myExperimentWrapper.isRunning ? View.VISIBLE : View.GONE);
            if(myExperimentWrapper.isRunning){
                txtExperimentStatus.setText("Running");
                txtExperimentStatus.setTextColor(Color.GREEN);
            } else {
                txtExperimentStatus.setText("Not running");
                txtExperimentStatus.setTextColor(Color.RED);
            }
        }else {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            txtExperimentStatus.setText("Not installed");
            txtExperimentStatus.setTextColor(Color.DKGRAY);
        }

    }

    private void BindToExperiment(){
        UnbindFromExperiment();

        ExperimentConfiguration experimentConfiguration = myExperimentWrapper.experimentConfiguration;

        Intent bindingIntent = new Intent();
        bindingIntent.setComponent(new ComponentName(experimentConfiguration.ExperimentPackage, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        bindingIntent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_BIND_CORE);
        DeltaServiceConnection deltaServiceConnection = new DeltaServiceConnection(myExperimentWrapper, this);

        boolean bound = bindService(bindingIntent, deltaServiceConnection, 0);
        if(bound){
            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Successfully bound to experiment service: " + experimentConfiguration.ExperimentPackage);
            myServiceConnection = deltaServiceConnection;
        }
        else{
            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to bound to experiment service: " + experimentConfiguration.ExperimentPackage);
        }

    }

    private void UnbindFromExperiment(){
        if(myServiceConnection != null) {
            try {
                unbindService(myServiceConnection);
            } catch (Exception e){
                //we MIGHT receive an exception here if we just uninstallled the service. Just ignore it.
            }
        }
        myServiceConnection = null;
    }

    private void StartExperiment() {
        ExperimentHelpers.startExperiment(this, myExperimentWrapper.experimentConfiguration.ExperimentPackage);
        SettingsHelpers.addStartedExperiment(getApplicationContext(), myExperimentWrapper.experimentConfiguration.ExperimentPackage);
        BindToExperiment();
    }

    private void StopExperiment(){
        if(myServiceConnection != null){
            Message message = new Message();
            message.what = Constants.DELTASERVICE_STOPLOGGING;
            myServiceConnection.send(message);
        }
        SettingsHelpers.removeStartedExperiment(getApplicationContext(), myExperimentWrapper.experimentConfiguration.ExperimentPackage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_experiment_details, menu);
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
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.experiment_details_btnStart : {
                StartExperiment();
                break;
            }
            case R.id.experiment_details_btnStop : {
                StopExperiment();
                break;
            }
            case R.id.experiment_details_btnInstall : {
                ExperimentStoreEntry experimentStoreEntry = PackageHelper.getExperimentFromStore(myExperimentWrapper.experimentConfiguration.ExperimentPackage);
                if(experimentStoreEntry != null) {
                    File oldFile = experimentStoreEntry.getApkFile();
                    File newFile = new File(getExternalFilesDir(null), "experiment.apk");
                    if (IOHelpers.copy(oldFile, newFile)) {
                        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                        promptInstall.setDataAndType(Uri.fromFile(newFile), "application/vnd.android.package-archive");
                        startActivity(promptInstall);
                    }
                }
                break;
            }
            case R.id.experiment_details_btnRemove : {
                StopExperiment();
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + myExperimentWrapper.experimentConfiguration.ExperimentPackage));
                startActivity(intent);
                break;
            }
            case R.id.experiment_details_btnUploadNow : {
                ExperimentHelpers.sendUploadCommand(this, myExperimentWrapper.experimentConfiguration.ExperimentPackage,
                        myExperimentWrapper.experimentConfiguration.DeltaServerUrl);
                break;
            }
            case R.id.experiment_details_btnDelete : {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The experiment will be permanently deleted from the local storage. " +
                        "If you want to reinstall it you'll have to download/import it again.\n\n" +
                        "Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                PackageHelper.removeExperimentFromStore(myExperimentWrapper.experimentConfiguration.ExperimentPackage);
                                finish();
                            }
                        })
                        .setNegativeButton("No", null);
                AlertDialog alert = builder.create();
                alert.show();
                break;
            }
            case R.id.experiment_details_btnDumpLogs : {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("All logs currently in the experiment's local cache will be copied to" +
                        " [/sdcard/Download/" + myExperimentWrapper.experimentConfiguration.ExperimentPackage + "] folder.\n\n" +
                        "Do you want to delete them from the local cache if the operation is successful? " +
                        "(this will clear the experiment's cache and free internal storage space, the copy dumped in the Download folder will be the only one left)")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ExperimentHelpers.sendDumpLogsCommand(ExperimentDetailsActivity.this, myExperimentWrapper.experimentConfiguration.ExperimentPackage,
                                        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), myExperimentWrapper.experimentConfiguration.ExperimentPackage).getAbsolutePath(),
                                        true);
                            }
                        })
                        .setNegativeButton("No, just make a copy", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ExperimentHelpers.sendDumpLogsCommand(ExperimentDetailsActivity.this, myExperimentWrapper.experimentConfiguration.ExperimentPackage,
                                        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), myExperimentWrapper.experimentConfiguration.ExperimentPackage).getAbsolutePath(),
                                        false);
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                break;
                /*
                final DialogHelper dialogHelper = new DialogHelper(this, "All logs currently in the experiment's local cache will be copied to" +
                        " [/sdcard/Download/" + myExperimentWrapper.experimentConfiguration.ExperimentPackage + "] folder.\n\n" +
                        "Do you want to delete them from the local cache if the operation is successful? " +
                        "(this will clear the experiment's cache and free internal storage space, the copy dumped in the Download folder will be the only one left)",
                        "Yes", "No, just make a copy", null);
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int result = dialogHelper.ShowMyModalDialog();
                        ExperimentHelpers.sendDumpLogsCommand(ExperimentDetailsActivity.this, myExperimentWrapper.experimentConfiguration.ExperimentPackage,
                                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), myExperimentWrapper.experimentConfiguration.ExperimentPackage).getAbsolutePath(),
                                result == DialogInterface.BUTTON_POSITIVE);
                    }
                });
                t.start();
                break;
                */
            }
        }
    }

    @Override
    public void onServiceDisconnected(ExperimentWrapper experimentWrapper) {
        UnbindFromExperiment();
    }

    @Override
    public void onServiceStartedLogging() {
        SetStatus();
    }

    @Override
    public void onServiceStoppedLogging() {
        SetStatus();
    }
}
