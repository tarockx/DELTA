package delta.desktoptools.library;

import java.io.File;

/**
 * Created by Elia on 29/07/2015.
 */
public class SigningInfo {
    public String keystoreFilePath;
    public String keystorePassword;
    public String keyAlias;
    public String keyPassword;

    public boolean isValid(){
        try {
            return keystoreFilePath != null && (new File(keystoreFilePath)).exists() &&
                    keyAlias != null && !keyAlias.isEmpty() &&
                    keystorePassword != null && !keystorePassword.isEmpty() &&
                    keyPassword != null && !keyPassword.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
