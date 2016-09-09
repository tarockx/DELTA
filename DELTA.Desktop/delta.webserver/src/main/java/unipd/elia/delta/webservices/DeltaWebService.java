package unipd.elia.delta.webservices;

import delta.desktoptools.library.ExperimentsRepoHelper;
import delta.desktoptools.library.IOHelpers;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Elia on 03/06/2015.
 */
@WebService
public class DeltaWebService {

    @WebMethod
    public boolean uploadLogData(@WebParam(name = "deviceID") String deviceID, @WebParam(name = "experimentID") String experimentID,
                                 @WebParam(name = "filename") String filename, @WebParam(name = "data") String data){

        System.out.println("Incoming log data from device: " + deviceID + ", Experiment ID: " + experimentID);
        return IOHelpers.storeIncomingLogData(deviceID, experimentID, filename, data);
    }


    @WebMethod
    public String[] getAvailableExperiments(){
        System.out.println("Incoming request for the list of available experiments...");
        List<String> allExperimentsInRepo = new LinkedList<>();
        allExperimentsInRepo.addAll(ExperimentsRepoHelper.getAllExperimentsInRepoRaw());
        return allExperimentsInRepo.toArray(new String[allExperimentsInRepo.size()]);
    }

    @WebMethod
    public byte[] downloadExperiment(@WebParam(name = "packageID") String packageID){
        System.out.println("Incoming request for download:" + packageID);
        byte[] res = ExperimentsRepoHelper.getExperiment(packageID);
        return res;
    }

}
