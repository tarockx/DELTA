package unipd.elia.delta.plugins.networklogger;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaBooleanOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaOptions;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaPluginMetadata;
import unipd.elia.delta.androidsharedlib.deltainterfaces.DeltaStringOption;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPlugin;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPluginMaster;
import unipd.elia.delta.androidsharedlib.deltainterfaces.IDeltaPollingPlugin;
import unipd.elia.delta.androidsharedlib.exceptions.PluginFailedToInitializeException;
import unipd.elia.delta.sharedlib.BooleanOption;
import unipd.elia.delta.sharedlib.PluginConfiguration;
import unipd.elia.delta.sharedlib.StringOption;

/**
 * Created by Elia on 10/07/2015.
 */
@DeltaPluginMetadata(
        PluginName = "Traffic Statistics logger",
        PluginAuthor = "Elia Dal Santo",
        PluginDescription = "Logs the speed of the data networks (Mobile and WiFi) and the amount of data that is transmitted/received through them. Does NOT log actual data.",
        DeveloperDescription = "At every poll, logs the total amount of packets and bytes transmitted/received through the device's network interfaces. " +
                "Note that these values typically reset when the interface is turned off and back on (e.g.: when the user switches Airplane Mode on)" +
                "A rough estimation of the upload/download speed is also performed, based on the last values collected. In the settings, you can also enable logging " +
                "distinct statistics for each specific application."
)
@DeltaOptions(
        BooleanOptions = {
                @DeltaBooleanOption(
                        ID = "log_apps", Name = "Also log per-app traffic statistics", defaultValue = false,
                        Description = "Enable this option to also collect per-app traffic statistic. Only enable if you need it, as it will consume more battery than just collecting total statistics."
                )
        },
        StringOptions = {
                @DeltaStringOption(
                        ID = "packages_to_log", Name = "Packages to log", defaultValue = "", Multiline = true,
                        Description = "Here you can specify which apps you want to log specific statistics for. One Android package name per line, or leave empty to log all apps."
                )
        }
)
public class TrafficStatsLogger implements IDeltaPlugin, IDeltaPollingPlugin {
    Context context;
    IDeltaPluginMaster myDeltaMaster;
    String pluginName;

    PackageManager myPackageManager;
    boolean logPerAppStats;

    private class PackageAndUid {
        public String packageName;
        public int uid;

        public PackageAndUid(String packageName, int uid) {
            this.packageName = packageName;
            this.uid = uid;
        }
    }

    List<PackageAndUid> packagesToLog;
    long last_rx_bytes;
    long last_tx_bytes;
    long last_timestamp;

    @Override
    public void Initialize(Context androidContext, IDeltaPluginMaster deltaPluginUpdateListener, PluginConfiguration pluginConfiguration) throws PluginFailedToInitializeException {
        if (androidContext == null || deltaPluginUpdateListener == null)
            throw new PluginFailedToInitializeException(this, "Null Context or IDeltaPluginMaster detected");
        context = androidContext;
        myDeltaMaster = deltaPluginUpdateListener;
        pluginName = getClass().getName();

        myPackageManager = context.getPackageManager();

        try {
            BooleanOption logPerAppStatsOption = pluginConfiguration.getBooleanOption("log_apps");
            logPerAppStats = logPerAppStatsOption != null && logPerAppStatsOption.Value;

            if (logPerAppStats) {
                StringOption packagesToLogOption = pluginConfiguration.getStringOption("packages_to_log");
                packagesToLog = new LinkedList<>();

                if (packagesToLogOption != null && !packagesToLogOption.Value.isEmpty()) { //log only specific apps
                    String[] packageNamesToLog = packagesToLogOption.Value.split("\\r?\\n");
                    for (int i = 0; i < packageNamesToLog.length; i++) {
                        ApplicationInfo info = myPackageManager.getApplicationInfo(packageNamesToLog[i].trim(), 0);
                        if (info != null)
                            packagesToLog.add(new PackageAndUid(info.packageName, info.uid));
                    }
                } else { //log all
                    for (ApplicationInfo info : myPackageManager.getInstalledApplications(0)) {
                        packagesToLog.add(new PackageAndUid(info.packageName, info.uid));
                    }
                }
            }

            last_timestamp = System.currentTimeMillis();
            last_rx_bytes = TrafficStats.getTotalRxBytes();
            last_tx_bytes = TrafficStats.getTotalTxBytes();

        } catch (Exception ex) {
            throw new PluginFailedToInitializeException(this, ex.getMessage());
        }

    }


    @Override
    public void Terminate() {
        context = null;
        myDeltaMaster = null;
    }


    @Override
    public void Poll() {
        try {
            JSONObject obj = new JSONObject();

            long timestamp = System.currentTimeMillis();

            long rxBytes = TrafficStats.getTotalRxBytes();
            long txBytes = TrafficStats.getTotalTxBytes();
            long rxSpeed = 0, txSpeed = 0;

            if (rxBytes >= last_rx_bytes) {
                long timeshift = timestamp - last_timestamp;
                long rxShift = rxBytes - last_rx_bytes;
                long txShift = txBytes - last_tx_bytes;

                if (rxShift >= 0)
                    rxSpeed = (rxShift / timeshift) * 1000;
                if (txShift >= 0)
                    txSpeed = (txShift / timeshift) * 1000;
            }

            obj.put("total_rx_bytes", rxBytes);
            obj.put("total_rx_packets", TrafficStats.getTotalRxPackets());
            obj.put("total_rx_bytes_per_second", rxSpeed);

            obj.put("total_tx_bytes", txBytes);
            obj.put("total_tx_packets", TrafficStats.getTotalTxPackets());
            obj.put("total_tx_bytes_per_second", txSpeed);

            last_timestamp = timestamp;
            last_rx_bytes = rxBytes;
            last_tx_bytes = txBytes;

            if (logPerAppStats && packagesToLog.size() > 0) { //log per app stats
                for (PackageAndUid packageAndUid : packagesToLog) {
                    JSONObject appObj = new JSONObject();

                    appObj.put("package", packageAndUid.packageName);
                    appObj.put("rx_bytes", TrafficStats.getUidRxBytes(packageAndUid.uid));
                    appObj.put("rx_packets", TrafficStats.getUidRxPackets(packageAndUid.uid));

                    appObj.put("tx_bytes", TrafficStats.getUidTxBytes(packageAndUid.uid));
                    appObj.put("tx_packets", TrafficStats.getUidTxPackets(packageAndUid.uid));

                    obj.accumulate("app_stats", appObj);
                }
            }

            myDeltaMaster.Update(new DeltaDataEntry(timestamp, pluginName, obj));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}