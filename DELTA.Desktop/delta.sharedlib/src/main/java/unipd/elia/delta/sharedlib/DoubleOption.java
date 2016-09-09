package unipd.elia.delta.sharedlib;

import java.io.Serializable;

/**
 * Created by Elia on 30/06/2015.
 */
public class DoubleOption extends DeltaOption implements Serializable{
    private static final long serialVersionUID = 99915L;

    public double Value;
    public Double MinValue = null;
    public Double MaxValue = null;

    public double defaultValue;

    public DoubleOption(){}

    @Override
    public DeltaOption getDeepCopy() {
        DoubleOption copy = new DoubleOption();
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
