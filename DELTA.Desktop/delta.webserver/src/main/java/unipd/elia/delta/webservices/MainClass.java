package unipd.elia.delta.webservices;

import delta.desktoptools.library.SettingsHelper;

import java.io.File;

/**
 * Created by Elia on 20/05/2015.
 */
public class MainClass {

    public static void main(String [ ] args)
    {
        try {
            SettingsHelper.loadSettings(new File("delta_settings.ini"));
            if(args.length == 1)
                SettingsHelper.DELTA_SERVICE_ENDPOINT = args[0];
            else
                SettingsHelper.loadWebServerSettings(new File("delta_settings.ini"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        //Start the DELTA WebService
        ServicesHelper.setupServicesEndpoints();
    }
}
