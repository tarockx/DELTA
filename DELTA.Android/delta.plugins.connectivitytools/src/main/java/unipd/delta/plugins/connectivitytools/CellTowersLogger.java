package unipd.delta.plugins.connectivitytools;

import android.content.Context;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.PluginConfiguration;

/**
 * Created by Elia on 08/07/2015.
 */
@DeltaPluginMetadata(PluginName = "Cell Towers scanner",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Periodically scans the radio towers around the device, retrieving information about them (id, signal strength, etc.). " +
                "Can potentially be used to track the device location, using cell tower triangulation.",
        DeveloperDescription = "Periodically logs stored information about the cell radio towers around the device. This information includes the tower id, network type " +
                "(edge, cdma, lte, etc.) and network-specific values depending on the type (i.e: CID and LAC for GSM cells, PSC for UMTS cells)",
        MinPollInterval = 30 * 1000
)
public class CellTowersLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    TelephonyManager myTelephonyManager;
    String pluginName;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if(androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        try {
            myTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        } catch (Exception ex){
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

        if(myTelephonyManager == null)
            throw new PluginFailedToInitializeException(this, "Could not obtain TelephonyManager");
    }

    @Override
    public void Terminate() {
        myDeltaMaster = null;
        myTelephonyManager = null;
        context = null;
    }

    @Override
    public void Poll() {
        List<NeighboringCellInfo> cellInfos = myTelephonyManager.getNeighboringCellInfo();
        if(cellInfos != null){
            try{
                JSONArray arr = new JSONArray();
                for (NeighboringCellInfo cellInfo : cellInfos){
                    JSONObject obj = new JSONObject();
                    int networkType = cellInfo.getNetworkType();
                    switch (networkType){
                        case TelephonyManager.NETWORK_TYPE_1xRTT :
                            obj.put("type", "1xRTT");
                            break;
                        case TelephonyManager.NETWORK_TYPE_EHRPD :
                            obj.put("type", "EHRPD");
                            break;
                        case TelephonyManager.NETWORK_TYPE_CDMA :
                            obj.put("type", "CDMA");
                            break;
                        case TelephonyManager.NETWORK_TYPE_EDGE :
                            obj.put("type", "EDGE");
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSDPA :
                            obj.put("type", "HSDPA");
                            break;
                        case TelephonyManager.NETWORK_TYPE_GPRS :
                            obj.put("type", "GPRS");
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSPA :
                            obj.put("type", "HSPA");
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSPAP :
                            obj.put("type", "HSPAP");
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSUPA :
                            obj.put("type", "HSUPA");
                            break;
                        case TelephonyManager.NETWORK_TYPE_LTE :
                            obj.put("type", "LTE");
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS :
                            obj.put("type", "UMTS");
                            break;
                        case TelephonyManager.NETWORK_TYPE_UNKNOWN :
                            obj.put("type", "unknown");
                            break;
                        default:
                            obj.put("type", "N/A");
                    }

                    obj.put("cid", cellInfo.getCid());
                    obj.put("RSSI", cellInfo.getRssi());
                    obj.put("LAC", cellInfo.getLac());
                    obj.put("PSC", cellInfo.getPsc());

                    arr.put(obj);
                }

                myDeltaMaster.Update(new DeltaDataEntry(System.currentTimeMillis(), pluginName, arr));
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
