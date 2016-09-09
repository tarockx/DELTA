package unipd.elia.delta.sharedlib;

import java.io.Serializable;

/**
 * Created by Elia on 30/06/2015.
 */
public class IntegerOption extends DeltaOption implements Serializable{
    private static final long serialVersionUID = 99914L;

    public int Value;
    public Integer MinValue = null;
    public Integer MaxValue = null;

    public int defaultValue;

    @Override
    public DeltaOption getDeepCopy() {
        IntegerOption copy = new IntegerOption();
        copy.ID = ID;
        copy.Name = Name;
        copy.Description = Description;
        copy.MinValue = MinValue;
        copy.MaxValue = MaxValue;

        copy.Value = Value;
        copy.defaultValue = defaultValue;

        return copy;
    }
}
