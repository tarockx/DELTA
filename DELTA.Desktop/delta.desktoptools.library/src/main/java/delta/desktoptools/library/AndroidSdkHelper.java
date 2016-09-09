package delta.desktoptools.library;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Created by Elia on 04/08/2015.
 */
public class AndroidSdkHelper {

    public static String getAndroidSdkDirectory(){
        String androidHome = null;
        try{
            File projectLocalFile = new File(SettingsHelper.PATH_ANDROID_PROJECT, "local.properties");
            if(projectLocalFile.exists()){
                List<String> lines = IOUtils.readLines(new FileInputStream(projectLocalFile));
                for(String line : lines){
                    if(line.trim().startsWith("sdk.dir")){
                        File androidHomeDirectory = new File(line.split("=")[1].trim());
                        if(androidHomeDirectory.exists())
                            androidHome = androidHomeDirectory.getCanonicalPath();
                    }
                }
            }
            return androidHome;
            //String androidHome = System.getenv("ANDROID_HOME");
        } catch (Exception ex){
            return null;
        }
    }
}
