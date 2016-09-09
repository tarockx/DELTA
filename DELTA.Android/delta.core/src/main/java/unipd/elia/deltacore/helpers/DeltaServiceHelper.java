package unipd.elia.deltacore.helpers;

import android.util.Base64;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import unipd.elia.delta.androidsharedlib.Constants;
import unipd.elia.delta.sharedlib.ExperimentConfiguration;
import unipd.elia.delta.sharedlib.ExperimentConfigurationIOHelpers;
import unipd.elia.deltacore.ExperimentStoreEntry;

/**
 * Created by Elia on 08/06/2015.
 */
public class DeltaServiceHelper {
    public static List<ExperimentConfiguration> getAvailableExperiments(String serverUrl){
        // Create request
        SoapObject request = new SoapObject(Constants.DWS_NAMESPACE, Constants.DWS_METHOD_GETAVAILABLEEXPERIMENTS);

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

            Object response = envelope.getResponse();
            Vector<SoapPrimitive> responses;
            if(SoapPrimitive.class.isInstance(response)){
                responses = new Vector();
                responses.add((SoapPrimitive) response);
            } else {
                responses = (Vector) response;
            }

            List<ExperimentConfiguration> availableExperiments = new LinkedList<>();
            for(SoapPrimitive soapPrimitive : responses){
                String rawConfigurationData = soapPrimitive.getValue().toString();
                InputStream configurationInputStream = new ByteArrayInputStream(rawConfigurationData.getBytes());
                availableExperiments.add(ExperimentConfigurationIOHelpers.DeserializeExperiment(configurationInputStream));
            }
            return  availableExperiments;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static ExperimentStoreEntry downloadExperiment(String packageID, String serverUrl){
        // Create request
        SoapObject request = new SoapObject(Constants.DWS_NAMESPACE, Constants.DWS_METHOD_DOWNLOAD_EXPERIMENT);

        //Set parameters
        request.addProperty("packageID", packageID);

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
            String data = response.toString();
            byte[] rawData = Base64.decode(data, Base64.DEFAULT);
            return PackageHelper.storeExperiment(new ByteArrayInputStream(rawData), true);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
