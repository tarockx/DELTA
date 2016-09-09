package unipd.elia.delta.androidsharedlib.deltainterfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Elia on 30/06/2015.
 */
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DeltaStringOption {
    String ID();
    String Name();
    String Description();
    boolean Multiline() default false;
    String[] AvailableChoices() default {};

    String defaultValue();
}
