package unipd.elia.deltacore.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;

import java.util.List;

import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;


/**
 * Created by Elia on 15/05/2015.
 */
public class DownloadableExperimentArrayAdapter extends ArrayAdapter {
    private final Context context;
    private final List<ExperimentWrapper> values;
    private View.OnClickListener myOnClickListener;

    public DownloadableExperimentArrayAdapter(Context context, List<ExperimentWrapper> values, View.OnClickListener onClickListener) {
        super(context, R.layout.listitem_downloadable_experiment, values);
        this.context = context;
        this.values = values;
        myOnClickListener = onClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            return getDownloadableExperimentsView(position, convertView, parent);
    }

    private View getDownloadableExperimentsView(int position, View convertView, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listitem_downloadable_experiment, parent, false);

        TextView txtExperimentName = (TextView) rowView.findViewById(R.id.downloadable_experiment_txtExperimentName);
        TextView txtExperimenAuthor = (TextView) rowView.findViewById(R.id.downloadable_experiment_txtExperimentAuthor);

        ProgressBar progressBarDownloading = (ProgressBar) rowView.findViewById(R.id.downloadable_experiment_progressBarDownloading);
        BootstrapButton btnDownload = (BootstrapButton) rowView.findViewById(R.id.downloadable_experiment_btnDownload);
        BootstrapButton btnManage = (BootstrapButton) rowView.findViewById(R.id.downloadable_experiment_btnManage);

        ExperimentWrapper experimentWrapper = values.get(position);
        txtExperimentName.setText(experimentWrapper.experimentConfiguration.ExperimentName);
        txtExperimenAuthor.setText(experimentWrapper.experimentConfiguration.ExperimentAuthor);

        btnDownload.setVisibility(View.GONE);
        btnManage.setVisibility(View.GONE);
        progressBarDownloading.setVisibility(View.GONE);

        if(experimentWrapper.isDownloading){
            progressBarDownloading.setVisibility(View.VISIBLE);
        } else{
            if(experimentWrapper.isInStore)
                btnManage.setVisibility(View.VISIBLE);
            else
                btnDownload.setVisibility(View.VISIBLE);
        }

        btnDownload.setTag(experimentWrapper);
        btnManage.setTag(experimentWrapper);
        btnDownload.setOnClickListener(null);
        btnManage.setOnClickListener(null);
        btnDownload.setOnClickListener(myOnClickListener);
        btnManage.setOnClickListener(myOnClickListener);

        return rowView;
    }
}
