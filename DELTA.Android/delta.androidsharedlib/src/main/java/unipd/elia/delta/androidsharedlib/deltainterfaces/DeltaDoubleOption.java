package unipd.elia.delta.androidsharedlib.deltainterfaces;

/**
 * Created by Elia on 30/06/2015.
 */
public @interface DeltaDoubleOption {
    String ID();
    String Name();
    String Description();
    double MaxValue() default Double.MAX_VALUE;
    double MinValue() default Double.MIN_VALUE;

    double defaultValue();
}
