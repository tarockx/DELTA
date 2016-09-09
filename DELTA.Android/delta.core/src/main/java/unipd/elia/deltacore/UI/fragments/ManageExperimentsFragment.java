package unipd.elia.deltacore.ui.fragments;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.ExperimentStoreEntry;
import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;
import unipd.elia.deltacore.helpers.DELTAUtils;
import unipd.elia.deltacore.helpers.PackageHelper;
import unipd.elia.deltacore.helpers.SettingsHelpers;
import unipd.elia.deltacore.serviceutils.DeltaServiceConnection;
import unipd.elia.deltacore.serviceutils.IDeltaServiceEventListener;
import unipd.elia.deltacore.ui.ExperimentDetailsActivity;
import unipd.elia.deltacore.ui.SettingsActivity;
import unipd.elia.deltacore.ui.adapters.StoredExperimentsArrayAdapter;

public class ManageExperimentsFragment extends Fragment implements IDeltaServiceEventListener, View.OnClickListener {
    private List<ExperimentWrapper> AvailableExperiments;
    private Map<ExperimentWrapper, DeltaServiceConnection> ServiceConnections;

    private StoredExperimentsArrayAdapter myStoredExperimentsArrayAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View mainView = inflater.inflate(R.layout.fragment_manage_experiments, container, false);

        DELTAUtils.context = getActivity();
        DELTAUtils.initializeSharedPreferencesIfFirstRun();

        ServiceConnections = new HashMap<>();
        AvailableExperiments = new LinkedList<>();

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ListView listViewAvailableExperiments = (ListView) mainView.findViewById(R.id.manage_experiments_activity_listViewAvailableExperiments);
        myStoredExperimentsArrayAdapter = new StoredExperimentsArrayAdapter(getActivity(), AvailableExperiments, this);
        listViewAvailableExperiments.setAdapter(myStoredExperimentsArrayAdapter);

        return mainView;
    }

    @Override
    public void onStart(){
        super.onStart();

        reloadAndRebindExperiments();
    }

    @Override
    public void onStop(){
        super.onStop();

        unbindAndClearFromExperiments();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

    public void reloadAndRebindExperiments(){
        unbindAndClearFromExperiments();

        LoadAvailableExperiments();
        BindToRunningExperiments();
    }

    private void unbindAndClearFromExperiments(){
        UnbindFromAllBoundExperiments();
        AvailableExperiments.clear();
        ServiceConnections.clear();
    }

    private void LoadAvailableExperiments(){
        List<ExperimentStoreEntry> storedExperiments = PackageHelper.getStoredExperiments(true);

        if(storedExperiments == null || storedExperiments.size() == 0){
            getActivity().findViewById(R.id.fragment_manage_experiments_tvNoExperiments).setVisibility(View.VISIBLE);
        }
        else {
            getActivity().findViewById(R.id.fragment_manage_experiments_tvNoExperiments).setVisibility(View.GONE);
            for (ExperimentStoreEntry storedExperiment : storedExperiments) {
                ExperimentConfiguration experimentConfiguration = ExperimentConfigurationIOHelpers.DeserializeExperiment(storedExperiment.getConfigurationPath());
                if (experimentConfiguration != null) {
                    ExperimentWrapper experimentWrapper = new ExperimentWrapper();
                    experimentWrapper.experimentConfiguration = experimentConfiguration;
                    //experimentWrapper.isCompatible = PackageHelper.isApkCompatible(getActivity().getPackageManager(), storedExperiment.getApkPath());
                    experimentWrapper.isCompatible = Build.VERSION.SDK_INT >= experimentConfiguration.getMinSDK();
                    experimentWrapper.isInstalled = PackageHelper.isApkInstalled(getActivity().getPackageManager(), experimentConfiguration.ExperimentPackage);
                    experimentWrapper.isRunning = false; //we need to try to bind to figure this out, leave false until we know

                    AvailableExperiments.add(experimentWrapper);
                }
            }

            //Notifies the listview
            myStoredExperimentsArrayAdapter.notifyDataSetChanged();
        }
    }

    private void BindToRunningExperiments(){
        for(ExperimentWrapper ew : AvailableExperiments){
            if(ew.isInstalled)
                BindToExperiment(ew);
        }
    }

    private void BindToExperiment(ExperimentWrapper experimentWrapper){
        if(ServiceConnections.containsKey(experimentWrapper))
            UnbindFromExperiment(experimentWrapper);

        ExperimentConfiguration experimentConfiguration = experimentWrapper.experimentConfiguration;

        Intent bindingIntent = new Intent();
        bindingIntent.setComponent(new ComponentName(experimentConfiguration.ExperimentPackage, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        bindingIntent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_BIND_CORE);
        DeltaServiceConnection deltaServiceConnection = new DeltaServiceConnection(experimentWrapper, this);

        boolean bound = getActivity().bindService(bindingIntent, deltaServiceConnection, 0);
        if(bound){
            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Successfully bound to experiment service: " + experimentConfiguration.ExperimentPackage);
            ServiceConnections.put(experimentWrapper, deltaServiceConnection);
        }
        else{
            Logger.d(Constants.DEBUGTAG_DELTAAPP, "Failed to bound to experiment service: " + experimentConfiguration.ExperimentPackage);
        }

    }

    private void UnbindFromAllBoundExperiments(){
        for(ExperimentWrapper ew : AvailableExperiments){
            if(ServiceConnections.containsKey(ew)){
                UnbindFromExperiment(ew);
            }
        }
    }

    private void UnbindFromExperiment(ExperimentWrapper experimentWrapper){
        ServiceConnection serviceConnection = ServiceConnections.remove(experimentWrapper);
        if(serviceConnection != null) {
            getActivity().unbindService(serviceConnection);
        }
    }

    private void StartExperiment(ExperimentWrapper experimentWrapper) {
        ExperimentConfiguration experimentConfiguration = experimentWrapper.experimentConfiguration;
        SettingsHelpers.addStartedExperiment(getActivity().getApplicationContext(), experimentWrapper.experimentConfiguration.ExperimentPackage);

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(experimentConfiguration.ExperimentPackage, "unipd.elia.delta.logsubstrate.DeltaLoggingService"));
        intent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_STARTLOGGING);
        intent.putExtra(Constants.EXPERIMENT_CONFIGURATION, experimentConfiguration);

        getActivity().startService(intent);
        BindToExperiment(experimentWrapper);
    }

    private void StopExperiment(ExperimentWrapper experimentWrapper){
        /*Intent stoppingIntent = new Intent();
        stoppingIntent.setComponent(new ComponentName(experimentConfiguration.ExperimentPackage, experimentConfiguration.ExperimentPackage + ".DeltaLoggingService"));
        stoppingIntent.putExtra(Constants.REQUEST_ACTION, Constants.DELTASERVICE_STOPLOGGING);

        startService(stoppingIntent);
        */

        DeltaServiceConnection serviceConnection = ServiceConnections.get(experimentWrapper);
        boolean disconnected = false;
        if(serviceConnection != null){
            Message message = new Message();
            message.what = Constants.DELTASERVICE_STOPLOGGING;
            disconnected = serviceConnection.send(message);
        }
        SettingsHelpers.removeStartedExperiment(getActivity().getApplicationContext(), experimentWrapper.experimentConfiguration.ExperimentPackage);
        //UnbindFromExperiment(experimentWrapper);
    }



    @Override
    public void onClick(View v) {
        View clickedButton = v;
        if(clickedButton != null){
            switch (clickedButton.getId()){
                case R.id.available_experiment_listitem_btnInfo:
                    Intent i = new Intent(getActivity().getApplicationContext(), ExperimentDetailsActivity.class);
                    i.putExtra(Constants.EXPERIMENT_WRAPPER, (ExperimentWrapper)clickedButton.getTag());
                    startActivity(i);
                    break;
                case R.id.available_experiment_listitem_btnStartExperiment:
                    StartExperiment((ExperimentWrapper) clickedButton.getTag());
                    //myStoredExperimentsArrayAdapter.notifyDataSetChanged();
                    break;
                case R.id.available_experiment_listitem_btnStopExperiment:
                    StopExperiment((ExperimentWrapper) clickedButton.getTag());
                    //myStoredExperimentsArrayAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            case R.id.action_settings:
                Intent i = new Intent(getActivity(), SettingsActivity.class);
                startActivity(i);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceDisconnected(ExperimentWrapper experimentWrapper) {
        UnbindFromExperiment(experimentWrapper);
    }

    @Override
    public void onServiceStartedLogging() {
        myStoredExperimentsArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onServiceStoppedLogging() {
        myStoredExperimentsArrayAdapter.notifyDataSetChanged();
    }

}
