package unipd.elia.delta.androidsharedlib.deltainterfaces;

/**
 * Created by Elia on 30/06/2015.
 */
public @interface DeltaOptions {
    DeltaStringOption[] StringOptions() default {};
    DeltaBooleanOption[] BooleanOptions() default {};
    DeltaIntegerOption[] IntegerOptions() default {};
    DeltaDoubleOption[] DoubleOptions() default {};
}
