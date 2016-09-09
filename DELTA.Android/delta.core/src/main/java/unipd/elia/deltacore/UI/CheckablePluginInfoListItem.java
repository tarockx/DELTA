package unipd.elia.deltacore.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * Created by Elia on 27/05/2015.
 */
public class CheckablePluginInfoListItem extends LinearLayout implements Checkable {
    public CheckablePluginInfoListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private boolean checked;


    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;

        //change ui if needed
    }

    @Override
    public void toggle() {
        this.checked = !this.checked;
    }
}
