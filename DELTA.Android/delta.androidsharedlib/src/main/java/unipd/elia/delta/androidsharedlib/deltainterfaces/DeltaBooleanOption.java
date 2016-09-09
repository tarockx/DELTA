package unipd.elia.delta.androidsharedlib.deltainterfaces;

/**
 * Created by Elia on 30/06/2015.
 */
public @interface DeltaBooleanOption {
    String ID();
    String Name();
    String Description();

    boolean defaultValue();
}
