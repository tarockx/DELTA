package unipd.elia.delta.androidsharedlib.deltainterfaces;

import unipd.elia.delta.androidsharedlib.DeltaDataEntry;

/**
 * Created by Elia on 11/05/2015.
 */
public interface IDeltaPluginMaster {

    /**
     * Sends logged data back to the experiment. Any logging plugin should call this method whenever it has new data to store, or when the experiment explicitly requests a poll
     * in case of Polling Plugins. Note that, while nothing prevents you from keeping a local cache of logged data, any data that has not been sent to the experiment through this
     * method will be lost if the experiment terminates. The experiment's storage facilities have a built-in cache, so in general you shouldn't have to worry about calling this
     * method too often, as it does not immediately trigger a data write to disk.
     * However, if your plugin generates a huge amount of data very quickly, you should consider caching it temporarily before sending an update, to minimize the overhead caused
     * by the instantiation of many DeltaDataEntry objects.
     * @param data the {@link DeltaDataEntry} instance that contains the logged data.
     */
    void Update(DeltaDataEntry data);
}
