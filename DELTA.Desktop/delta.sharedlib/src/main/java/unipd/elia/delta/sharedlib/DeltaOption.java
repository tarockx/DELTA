package unipd.elia.delta.sharedlib;

import java.io.Serializable;

/**
 * Created by Elia on 30/06/2015.
 */
public abstract class DeltaOption implements Serializable {
    private static final long serialVersionUID = 99911L;

    public String ID;
    public String Name;
    public String Description;

    public abstract DeltaOption getDeepCopy();
}
