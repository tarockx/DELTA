package unipd.elia.delta.androidsharedlib;

import android.util.Log;

/**
 * Created by Elia on 24/06/2015.
 */
public class Logger {
    public static boolean DEBUG = true;

    public static void e(String tag, String message){
        if(DEBUG)
            Log.e(tag, message);
    }

    public static void d(String tag, String message){
        if(DEBUG)
            Log.d(tag, message);
    }

    public static void i(String tag, String message){
        if(DEBUG)
            Log.i(tag, message);
    }

    public static void w(String tag, String message){
        if(DEBUG)
            Log.w(tag, message);
    }
}
