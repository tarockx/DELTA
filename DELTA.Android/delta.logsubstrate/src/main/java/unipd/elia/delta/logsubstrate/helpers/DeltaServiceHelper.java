package unipd.elia.delta.logsubstrate.helpers;

import android.util.Base64;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.FileInputStream;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.androidsharedlib.Logger;

/**
 * Created by Elia on 03/06/2015.
 */
public class DeltaServiceHelper {
    public static boolean uploadLog(String deviceID, String experimentID, String serverUrl, File file){
        byte[] data;
        try {
            data = new byte[(int)file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(data, 0, data.length);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.e(Constants.DEBUGTAG_DELTALOGSUBSTRATE, e.getMessage());
            return false;
        }

        return uploadLog(deviceID, experimentID, file.getName(), serverUrl, data);
    }


    public static boolean uploadLog(String deviceID, String experimentID, String filename, String serverUrl, byte[] data){
        // Create request
        SoapObject request = new SoapObject(Constants.DWS_NAMESPACE, Constants.DWS_METHOD_UPLOADLOGS);

        //add parameters
        addProperty(request, "deviceID", deviceID);
        addProperty(request, "experimentID", experimentID);
        addProperty(request, "filename", filename);
        addProperty(request, "data", Base64.encodeToString(data, Base64.DEFAULT));


        // Create envelope
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setAddAdornments(false);
        envelope.implicitTypes = true;
        // Set output SOAP object
        envelope.setOutputSoapObject(request);
        // Create HTTP call object
        HttpTransportSE androidHttpTransport = new HttpTransportSE(serverUrl);
        androidHttpTransport.debug = true;

        try {
            // Invoke web service
            androidHttpTransport.call(Constants.DWS_SOAP_ACTION, envelope);
            // Get the response
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();

            return Boolean.parseBoolean(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addProperty(SoapObject request, String name, Object value){
        PropertyInfo pi = new PropertyInfo();
        pi.setName(name);
        pi.setValue(value);
        pi.setType(value.getClass());
        request.addProperty(pi);
    }
}
