package delta.desktoptools.library;

/**
 * Created by Elia on 28/04/2015.
 */
public class AndroidPermission {
    public String name;
    public String maxSdkVersion;

    public AndroidPermission(String name, String maxSdkVersion){
        this.name = name;
        this.maxSdkVersion = maxSdkVersion;
    }
}
