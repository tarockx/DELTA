package unipd.elia.deltacore.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.FontAwesomeText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import unipd.elia.deltacore.ExperimentWrapper;
import unipd.elia.deltacore.R;

/**
 * Created by Elia on 25/07/2015.
 */
public class StoredExperimentsArrayAdapter extends ArrayAdapter {
    private final Context context;
    private final List<ExperimentWrapper> values;
    private View.OnClickListener myOnClickListener;
    private final Map<ExperimentWrapper, Boolean> expandedStatusMap = new HashMap<>();


    public StoredExperimentsArrayAdapter(Context context, List<ExperimentWrapper> values, View.OnClickListener onClickListener) {
        super(context, R.layout.listitem_stored_experiment, values);
        this.context = context;
        this.values = values;
        myOnClickListener = onClickListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.listitem_stored_experiment, parent, false);

        TextView txtExperimentName = (TextView) rowView.findViewById(R.id.available_experiment_listitem_txtExperimentName);
        TextView txtExperimenAuthor = (TextView) rowView.findViewById(R.id.available_experiment_listitem_txtExperimenAuthor);
        CheckBox chkCompatible = (CheckBox) rowView.findViewById(R.id.stored_experiment_listitem_chkCompatible);
        CheckBox chkInstalled = (CheckBox) rowView.findViewById(R.id.stored_experiment_listitem_chkInstalled);
        CheckBox chkRunning = (CheckBox) rowView.findViewById(R.id.stored_experiment_listitem_chkRunning);


        BootstrapButton btnInfo = (BootstrapButton) rowView.findViewById(R.id.available_experiment_listitem_btnInfo);
        BootstrapButton btnStart = (BootstrapButton) rowView.findViewById(R.id.available_experiment_listitem_btnStartExperiment);
        BootstrapButton btnStop = (BootstrapButton) rowView.findViewById(R.id.available_experiment_listitem_btnStopExperiment);

        FontAwesomeText fontAwesomeTextPlayIcon = (FontAwesomeText) rowView.findViewById(R.id.listitem_stored_experiment_fontAwesomePlayIcon);


        final ExperimentWrapper experimentWrapper = values.get(position);
        txtExperimentName.setText(experimentWrapper.experimentConfiguration.ExperimentName);
        txtExperimenAuthor.setText(experimentWrapper.experimentConfiguration.ExperimentAuthor);

        chkCompatible.setChecked(experimentWrapper.isCompatible);
        chkInstalled.setChecked(experimentWrapper.isInstalled);
        chkRunning.setChecked(experimentWrapper.isRunning);

        fontAwesomeTextPlayIcon.setVisibility(experimentWrapper.isRunning ? View.VISIBLE : View.INVISIBLE);

        btnInfo.setTag(experimentWrapper);
        btnStart.setTag(experimentWrapper);
        btnStop.setTag(experimentWrapper);
        btnInfo.setOnClickListener(null);
        btnStart.setOnClickListener(null);
        btnStop.setOnClickListener(null);
        btnInfo.setOnClickListener(myOnClickListener);
        btnStart.setOnClickListener(myOnClickListener);
        btnStop.setOnClickListener(myOnClickListener);
        btnStart.setVisibility(experimentWrapper.isRunning ? View.GONE : View.VISIBLE);
        btnStop.setVisibility(experimentWrapper.isRunning ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(experimentWrapper.isInstalled);
        btnStop.setEnabled(experimentWrapper.isInstalled);

        final View layoutHeader = rowView.findViewById(R.id.stored_experiment_listitem_headerLayout);
        final View layoutBody = rowView.findViewById(R.id.stored_experiment_listitem_bodyLayout);
        //View layoutBody = rowView.findViewById(R.id.stored_experiment_listitem_bodyLayout);
        //layoutBody.setVisibility(View.GONE);
        if(!expandedStatusMap.containsKey(experimentWrapper)){ //first time
            expandedStatusMap.put(experimentWrapper, false);
        }

        layoutBody.setVisibility(expandedStatusMap.get(experimentWrapper) ? View.VISIBLE : View.GONE);

        layoutHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!expandedStatusMap.get(experimentWrapper)) {
                    layoutBody.setVisibility(View.VISIBLE);
                    expandedStatusMap.put(experimentWrapper, true);
                   //expand(view);
                } else {
                    layoutBody.setVisibility(View.GONE);
                    expandedStatusMap.put(experimentWrapper, false);
                    //collapse(view);
                }
            }
        });

        return rowView;
    }


    public static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int)(initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}
