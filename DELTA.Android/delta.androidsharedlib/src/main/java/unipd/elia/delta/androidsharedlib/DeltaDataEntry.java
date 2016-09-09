package unipd.elia.delta.androidsharedlib;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Elia on 18/05/2015.
 */
public class DeltaDataEntry {
    public boolean rawMode = false;

    public long timestamp;
    public String pluginID;
    private String data;
    private byte[] rawData;

    /**
     * Creates a new instance of DeltaDataEntry using raw JSON data as input. It is your responsibility to make sure the input is a valid JSON string.
     * @param timestamp The timestamp indicating when the data was logged/generated. Format MUST be in milliseconds since epoch. Hint: you can use the method "System.currentTimeMillis()" to retrieve a valid timestamp
     * @param pluginID The plugin ID (i.e: qualified class name) of the plugin that generated this data.
     * @param data A valid JSON string containing the logged data
     */
    public DeltaDataEntry(long timestamp, String pluginID, String data){
        this.timestamp = timestamp;
        this.pluginID = pluginID;
        this.data = data;
    }

    /**
     * Creates a new instance of DeltaDataEntry using a {@link JSONObject} as input
     * @param timestamp The timestamp indicating when the data was logged/generated. Format MUST be in milliseconds since epoch. Hint: you can use the method "System.currentTimeMillis()" to retrieve a valid timestamp
     * @param pluginID The plugin ID (i.e: qualified class name) of the plugin that generated this data.
     * @param data A {@link JSONObject} containing the logged data
     */
    public DeltaDataEntry(long timestamp, String pluginID, JSONObject data){
        this.timestamp = timestamp;
        this.pluginID = pluginID;
        this.data = data.toString();
    }

    /**
     * Creates a new instance of DeltaDataEntry using a {@link JSONArray} as input. Useful if your data is a list/array.
     * @param timestamp The timestamp indicating when the data was logged/generated. Format MUST be in milliseconds since epoch. Hint: you can use the method "System.currentTimeMillis()" to retrieve a valid timestamp
     * @param pluginID The plugin ID (i.e: qualified class name) of the plugin that generated this data.
     * @param data A {@link JSONArray} containing the logged data
     */
    public DeltaDataEntry(long timestamp, String pluginID, JSONArray data){
        this.timestamp = timestamp;
        this.pluginID = pluginID;
        this.data = data.toString();
    }

    /**
     * Creates a new instance of DeltaDataEntry that store raw bytes of unstructured data. This kind of entry is recommended if the standard JSON format is unsuitable for the
     * kind of data you are logging (e.g.: you data is binary data, for example an audio stream, or a continuous stream of text that it doesn't make sense to split into distinct JSON
     * entries.). This kind of entry is also slightly more efficient.
     * @param timestamp The timestamp indicating when the data was logged/generated. Format MUST be in milliseconds since epoch. Hint: you can use the method "System.currentTimeMillis()" to retrieve a valid timestamp
     * @param pluginID The plugin ID (i.e: qualified class name) of the plugin that generated this data.
     * @param rawData A byte array containing the data to store
     */
    public DeltaDataEntry(long timestamp, String pluginID, byte[] rawData){
        rawMode = true;

        this.timestamp = timestamp;
        this.pluginID = pluginID;
        this.rawData = rawData;
    }

    public byte[] toByteArray(){
        if(rawMode)
            return rawData;
        else
            return toString().getBytes();
    }

    public String toString(){
        if(rawMode)
            return "{\"timestamp\":" + timestamp + ",\"data\":" + "[raw_data]" + "}\n";
        else
            return "{\"timestamp\":" + timestamp + ",\"data\":" + data + "}\n";
    }

    public int getSize(){
        if(rawMode)
            return rawData.length;
        else
            return  36 + data.length() * 2 + 36 + pluginID.length() * 2 + 8; //sizeof(data) + sizeof(pluginID) + sizeof(timestamp) TODO: verify that this actually is correct
    }

}
