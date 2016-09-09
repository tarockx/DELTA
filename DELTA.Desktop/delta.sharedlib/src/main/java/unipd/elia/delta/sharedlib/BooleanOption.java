package unipd.elia.delta.sharedlib;

import java.io.Serializable;

/**
 * Created by Elia on 30/06/2015.
 */
public class BooleanOption extends DeltaOption implements Serializable {
    private static final long serialVersionUID = 99913L;

    public boolean Value;
    public boolean defaultValue;

    @Override
    public DeltaOption getDeepCopy() {
        BooleanOption copy = new BooleanOption();
        copy.ID = ID;
        copy.Name = Name;
        copy.Description = Description;

        copy.Value = Value;
        copy.defaultValue = defaultValue;

        return copy;
    }
}
