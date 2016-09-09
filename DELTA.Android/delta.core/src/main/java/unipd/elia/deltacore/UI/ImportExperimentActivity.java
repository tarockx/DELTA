package unipd.elia.deltacore.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.ExperimentStoreEntry;
import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;
import unipd.elia.deltacore.helpers.PackageHelper;

public class ImportExperimentActivity extends AppCompatActivity {

    class ImportExperimentAsyncHelper extends AsyncTask<File, Void, ExperimentStoreEntry> {

        @Override
        protected ExperimentStoreEntry doInBackground(File... params) {
            return PackageHelper.storeExperiment(params[0], true);
        }

        protected void onPostExecute(ExperimentStoreEntry result) {
            importExperimentCompleted(result);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_experiment);

    }

    @Override
    protected void onStart(){
        super.onStart();

        findViewById(R.id.import_experiment_layoutImporting).setVisibility(View.GONE);

        try{
            Intent i = getIntent();
            Uri fileUri = i.getData();
            importExperiment(new File(fileUri.getPath()));
        } catch (Exception ex){
            fail("Invocation error");
            return;
        }
    }

    private void importExperiment(final File experimentFile){
        if(!experimentFile.exists()) {
            fail("File not found or inaccessible");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Import experiment from external file: " + experimentFile.getName() + "?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        findViewById(R.id.import_experiment_layoutImporting).setVisibility(View.VISIBLE);
                        new ImportExperimentAsyncHelper().execute(experimentFile);
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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

    private void importExperimentCompleted(ExperimentStoreEntry entry){
        findViewById(R.id.import_experiment_layoutImporting).setVisibility(View.GONE);
        if(entry == null){
            fail("failed to import experiment. File is damaged or unrecognized.");
            return;
        }
        else {
            try {
                ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(entry.getConfigurationPath());
                ExperimentWrapper experimentWrapper = new ExperimentWrapper();
                experimentWrapper.experimentConfiguration = experimentConfiguration;
                experimentWrapper.isInStore = true;
                experimentWrapper.isInstalled = PackageHelper.isApkInstalled(getPackageManager(), experimentConfiguration.ExperimentPackage);

                Intent i = new Intent(getApplicationContext(), ExperimentDetailsActivity.class);
                i.putExtra(Constants.EXPERIMENT_WRAPPER, experimentWrapper);
                startActivity(i);
            } catch (Exception ex){
                fail("unknown error");
            }
        }
    }

    private void fail(String reason){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("ERROR: " + reason)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_import_experiment, menu);
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
}
