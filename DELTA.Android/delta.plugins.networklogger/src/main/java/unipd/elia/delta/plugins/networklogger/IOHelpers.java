package unipd.elia.delta.plugins.networklogger;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Elia on 23/06/2015.
 */
public class IOHelpers {
    public static File inflateResourceToInternalStorageCache(Context context, int resourceID, String outputFileName){
        try {
            File cachedir = new File(context.getFilesDir().getAbsolutePath());
            if(!(cachedir != null && cachedir.exists() && cachedir.isDirectory()))
                cachedir.mkdir();

            InputStream is = context.getResources().openRawResource(resourceID);
            File outputFile = new File(cachedir.getAbsolutePath(), outputFileName);
            OutputStream os = new FileOutputStream(outputFile);

            //copying data
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }

            //file copied
            os.close();
            is.close();
            return new File(cachedir, outputFileName);
        }
        catch (Exception e){
            e.printStackTrace();
            return  null;
        }

    }
}
