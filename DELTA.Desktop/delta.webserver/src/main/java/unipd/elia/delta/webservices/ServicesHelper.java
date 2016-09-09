package unipd.elia.delta.webservices;

import delta.desktoptools.library.SettingsHelper;

import javax.xml.ws.Endpoint;

/**
 * Created by Elia on 03/06/2015.
 */
public class ServicesHelper {
    public static boolean setupServicesEndpoints(){
        try{
            String bindingURI = SettingsHelper.DELTA_SERVICE_ENDPOINT;
            DeltaWebService service = new DeltaWebService();
            Endpoint.publish(bindingURI, service);
            System.out.println("Server started at: " + bindingURI);
            return true;
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }

    }
}
