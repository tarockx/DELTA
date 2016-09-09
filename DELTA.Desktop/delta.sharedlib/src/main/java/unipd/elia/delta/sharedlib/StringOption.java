package unipd.elia.delta.sharedlib;

import java.util.Arrays;

/**
 * Created by Elia on 30/06/2015.
 */
public class StringOption extends DeltaOption {
    private static final long serialVersionUID = 99912L;

    public String Value;
    public boolean Multiline = false;
    public String[] AvailableChoices = {};

    public String defaultValue;

    @Override
    public DeltaOption getDeepCopy(){
        StringOption copy = new StringOption();

        copy.ID = ID;
        copy.Name = Name;
        copy.Description = Description;

        copy.Value = Value;
        copy.Multiline = Multiline;
        copy.AvailableChoices = Arrays.copyOf(AvailableChoices, AvailableChoices.length);

        copy.defaultValue = defaultValue;

        return copy;
    }
}
