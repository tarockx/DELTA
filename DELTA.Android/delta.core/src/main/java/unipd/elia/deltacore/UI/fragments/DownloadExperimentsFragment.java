package unipd.elia.deltacore.ui.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.deltacore.ExperimentStoreEntry;
import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;
import unipd.elia.deltacore.helpers.DELTAUtils;
import unipd.elia.deltacore.helpers.DeltaServiceHelper;
import unipd.elia.deltacore.helpers.PackageHelper;
import unipd.elia.deltacore.ui.ExperimentDetailsActivity;
import unipd.elia.deltacore.ui.TabbedMainActivity;
import unipd.elia.deltacore.ui.adapters.DownloadableExperimentArrayAdapter;


public class DownloadExperimentsFragment extends Fragment implements View.OnClickListener {
    DownloadExperimentListAsyncHelper myDownloadExperimentListAsyncHelper = new DownloadExperimentListAsyncHelper();
    DownloadExperimentAsyncHelper myDownloadExperimentAsyncHelper = new DownloadExperimentAsyncHelper();

    private List<ExperimentWrapper> DownloadableExperiments = new LinkedList<>();
    private DownloadableExperimentArrayAdapter myExperimentWrapperArrayAdapter;
    private Menu myOptionsMenu;

    class DownloadExperimentListAsyncHelper extends AsyncTask<Object, Void, List<ExperimentConfiguration>> {

        @Override
        protected List<ExperimentConfiguration> doInBackground(Object... params) {
            return DeltaServiceHelper.getAvailableExperiments(
                    PreferenceManager.getDefaultSharedPreferences(DownloadExperimentsFragment.this.getActivity()).getString("deltaServerAddress", null)
            );
        }

        protected void onPostExecute(List<ExperimentConfiguration> result) {
            updateExperimentList(result);

            //reset (a task can only run once)
            myDownloadExperimentListAsyncHelper = new DownloadExperimentListAsyncHelper();
        }
    }

    class DownloadExperimentAsyncHelper extends AsyncTask<ExperimentWrapper, Void, ExperimentStoreEntry>{
        private ExperimentWrapper myExperimentWrapper;

        @Override
        protected ExperimentStoreEntry doInBackground(ExperimentWrapper... params) {
            myExperimentWrapper = params[0];
            return DeltaServiceHelper.downloadExperiment(myExperimentWrapper.experimentConfiguration.ExperimentPackage,
                    PreferenceManager.getDefaultSharedPreferences(DownloadExperimentsFragment.this.getActivity()).getString("deltaServerAddress", null));
        }

        protected void onPostExecute(ExperimentStoreEntry result) {
            experimentDownloadFinished(result, myExperimentWrapper);

            //reset (a task can only run once)
            myDownloadExperimentAsyncHelper = new DownloadExperimentAsyncHelper();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mainView = inflater.inflate(R.layout.fragment_download_experiments, container, false);

        DELTAUtils.context = getActivity();
        DELTAUtils.initializeSharedPreferencesIfFirstRun();

        setHasOptionsMenu(true);

        final ListView listViewAvailableExperiments = (ListView) mainView.findViewById(R.id.download_experiments_listViewAvailableExperiments);
        myExperimentWrapperArrayAdapter = new DownloadableExperimentArrayAdapter(getActivity(), DownloadableExperiments, this);
        listViewAvailableExperiments.setAdapter(myExperimentWrapperArrayAdapter);

        return mainView;
    }



    @Override
    public void onStart(){
        super.onStart();

        downloadExperimentList();
    }

    public void downloadExperimentList(){
        //show loading UI
        showLoadingUI();

        //download experiment list
        try{
            myDownloadExperimentListAsyncHelper.execute();
        }
        catch (IllegalStateException ex){
            //already running, we ignore it, catch is here just to avoid an app crash if this happens
        }
    }

    public void updateExperimentList(List<ExperimentConfiguration> downloadableExperiments){
        if(downloadableExperiments == null){
            showNoExperimentsAvailableUI(getString(R.string.download_experiments_txtNoExperiments_failed));
            return;
        }

        if(downloadableExperiments.size() == 0){
            showNoExperimentsAvailableUI(getString(R.string.download_experiments_txtNoExperiments_noResults));
            return;
        }

        //clear any previous experiments
        DownloadableExperiments.clear();

        for(ExperimentConfiguration experimentConfiguration : downloadableExperiments){
            ExperimentWrapper experimentWrapper = new ExperimentWrapper();
            experimentWrapper.experimentConfiguration = experimentConfiguration;
            experimentWrapper.isInstalled = PackageHelper.isApkInstalled(getActivity().getPackageManager(), experimentConfiguration.ExperimentPackage);
            experimentWrapper.isInStore = PackageHelper.isExperimentInStore(experimentConfiguration.ExperimentPackage);

            DownloadableExperiments.add(experimentWrapper);
        }
        myExperimentWrapperArrayAdapter.notifyDataSetChanged();
        showDownloadableExperimentsUI();
    }

    private void downloadExperiment(ExperimentWrapper experimentWrapper){
        experimentWrapper.isDownloading = true;
        try{
            myDownloadExperimentAsyncHelper.execute(experimentWrapper);
        }
        catch (IllegalStateException ex){
            //already running
            Toast.makeText(getActivity(), "Another download is already in progress, please wait for it to finish...", Toast.LENGTH_LONG).show();
        }

        myExperimentWrapperArrayAdapter.notifyDataSetChanged();
    }

    private void experimentDownloadFinished(ExperimentStoreEntry experimentStoreEntry, ExperimentWrapper experimentWrapper){
        experimentWrapper.isDownloading = false;
        experimentWrapper.isInStore = (experimentStoreEntry != null);
        if(experimentWrapper.isInStore) {
            if(TabbedMainActivity.class.isInstance(getActivity()))
                ((TabbedMainActivity) getActivity()).experimentDownloaded();
            Toast.makeText(getActivity(), "Experiment downloaded successfully!", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getActivity(), "ERROR: failed to download experiment", Toast.LENGTH_SHORT).show();
        }

        myExperimentWrapperArrayAdapter.notifyDataSetChanged();

    }

    private void showLoadingUI(){
        getView().findViewById(R.id.download_experiments_layoutContactingServer).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.download_experiments_layoutAvailableExperiments).setVisibility(View.GONE);
        getView().findViewById(R.id.download_experiments_txtNoExperiments).setVisibility(View.GONE);
        //getView().findViewById(R.id.downloadable_experiment_btnDownload_pullRefreshLayoutNoItems).setVisibility(View.GONE);

        if(myOptionsMenu != null){
            final MenuItem refreshItem = myOptionsMenu.findItem(R.id.action_refresh);
            if(refreshItem != null)
                refreshItem.setActionView(R.layout.action_refreshing_animation);
        }
    }

    private void showDownloadableExperimentsUI(){
        getView().findViewById(R.id.download_experiments_layoutContactingServer).setVisibility(View.GONE);
        getView().findViewById(R.id.download_experiments_layoutAvailableExperiments).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.download_experiments_txtNoExperiments).setVisibility(View.GONE);
        //getView().findViewById(R.id.downloadable_experiment_btnDownload_pullRefreshLayoutNoItems).setVisibility(View.GONE);

        if(myOptionsMenu != null){
            final MenuItem refreshItem = myOptionsMenu.findItem(R.id.action_refresh);
            if(refreshItem != null)
                refreshItem.setActionView(null);
        }
    }

    private void showNoExperimentsAvailableUI(String message){
        getView().findViewById(R.id.download_experiments_layoutContactingServer).setVisibility(View.GONE);
        getView().findViewById(R.id.download_experiments_layoutAvailableExperiments).setVisibility(View.GONE);
        TextView txtNoExperiments = (TextView) getView().findViewById(R.id.download_experiments_txtNoExperiments);
        txtNoExperiments.setVisibility(View.VISIBLE);
        txtNoExperiments.setText(message);
        //getView().findViewById(R.id.downloadable_experiment_btnDownload_pullRefreshLayoutNoItems).setVisibility(View.VISIBLE);

        if(myOptionsMenu != null){
            final MenuItem refreshItem = myOptionsMenu.findItem(R.id.action_refresh);
            if(refreshItem != null)
                refreshItem.setActionView(null);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.downloadable_experiment_btnDownload : {
                downloadExperiment((ExperimentWrapper)v.getTag());
                break;
            }
            case R.id.downloadable_experiment_btnManage : {
                Intent i = new Intent(getActivity().getApplicationContext(), ExperimentDetailsActivity.class);
                i.putExtra(Constants.EXPERIMENT_WRAPPER, (ExperimentWrapper)v.getTag());
                startActivity(i);
                break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_download_experiments, menu);
        myOptionsMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_refresh){
            downloadExperimentList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
