package unipd.elia.deltacore.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.deltacore.R;

/**
 * Created by Elia on 27/05/2015.
 */
public class PluginConfigurationArrayAdapter extends ArrayAdapter {
    private final Context context;
    private final List<PluginConfiguration> values;

    public PluginConfigurationArrayAdapter(Context context, List<PluginConfiguration> values) {
        super(context, R.layout.plugin_details_checkable_layout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.plugin_details_checkable_layout, parent, false);

        TextView txtPluginName = (TextView) rowView.findViewById(R.id.plugin_details_txtPluginName);
        TextView txtPluginDescription = (TextView) rowView.findViewById(R.id.plugin_details_txtPluginDescription);

        PluginConfiguration pluginConfiguration = values.get(position);

        txtPluginName.setText(pluginConfiguration.PluginName);
        txtPluginDescription.setText(pluginConfiguration.PluginDescription);

        return rowView;
    }
}
