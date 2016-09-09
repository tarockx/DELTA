package unipd.elia.delta.androidsharedlib.deltainterfaces;

/**
 * Created by Elia on 30/06/2015.
 */
public @interface DeltaIntegerOption {
    String ID();
    String Name();
    String Description();
    int MaxValue() default Integer.MAX_VALUE;
    int MinValue() default Integer.MIN_VALUE;

    int defaultValue();
}
